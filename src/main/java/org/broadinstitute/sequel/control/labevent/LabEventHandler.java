package org.broadinstitute.sequel.control.labevent;


import org.broadinstitute.sequel.control.dao.labevent.LabEventDao;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.labevent.PartiallyProcessedLabEventCache;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.labevent.InvalidMolecularStateException;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventMessage;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LabEventHandler {

    public enum HANDLER_RESPONSE {
        OK,
        ERROR /* further refine to "out of order", "bad molecular envelope", critical, warning, etc. */
    }


    PartiallyProcessedLabEventCache unanchored;

    PartiallyProcessedLabEventCache invalidMolecularState;

    @Inject
    private LabEventDao labEventDao;

    public HANDLER_RESPONSE handleEvent(LabEventMessage eventMessage) {
        // 0. write out the message to stable server-side storage,
        // either relational or otherwise.
        // 1. check for sources in some way, error out if they aren't there.

        // then on to the good stuff..
        LabEvent labEvent = createEvent(eventMessage);
        HANDLER_RESPONSE response = processEvent(labEvent);
        if (response == HANDLER_RESPONSE.OK) {
            this.labEventDao.persist(labEvent);
        }
        return response;
    }

    /**
     * Lots of magic in here.  From the XML/JSON representation
     * of the event, create a LabEvent.  New up new vessels
     * or reference existing ones from the database.
     * 
     * This is the main automation entry point.  
     * @param eventMessage
     * @return
     */
    public LabEvent createEvent(LabEventMessage eventMessage) {
        throw new RuntimeException("Method not yet implemented.");
    }

    public HANDLER_RESPONSE processEvent(LabEvent labEvent) {
        // random thought, which should go onto confluence doc:
        /*

        The first thing event handling should do is persist the message.
        Recording what has actually happened in terms of container
        motion (fluid motion) is paramount.  This means that the
        entire app must tolerate the fact that we might
        have {@link LabVessel}s that have no {@link SampleSheet}s.

        We need to support this because {@link LabEvents} tend to be
        sent to us *after* they have happened, and subsequent events,
        which refer to the destinations of previous events, will be
        rejected if we reject the first message.  This is intolerable
        for lab operations.  They need to be able to record their
        work and move on, while asynchronously (or synchronously--this
        is a business decision that will flip flop over time) we
        trouble shoot what has happened.

        we can do molecular state updates, validation, error checking, status updates for PMs, and alerts
        after we've saved as much as we can about the event.

        events arrive *after* the event has happened, so we can't reject
        the event even if it violates some rules/workflow.

        also as we scale up, our first mission is to store event
        information as rapidly as possible so we don't slow
        down clients (decks or web tier).  so we want to be able
        to store event data first, and then deal with alerts, validation, and computing
        sample metadata (molecular state) 2nd.  ideally this could all be
        tied up together, but if we can't hit this in < 1s per event,
        we have to break it up.


         */       



        /*
        LabWorkflowInstance workflow = findWorkflowInstance(labEvent);
        if (workflow == null) {
            sendAlertToLab("No workflow found.",labEvent);
        }
        else {
            if (!workflow.isExpecting(labEvent)) {
                sendAlertToLab("Event out of order",workflow,labEvent);
                sendAlertToProjectManagement("Event out of order",workflow,labEvent);
                addToOutOfOrderCache(labEvent);
                return HANDLER_RESPONSE.ERROR;
            }
        }
        */

        try {
            labEvent.validateSourceMolecularState();
        }
        catch(InvalidMolecularStateException e) {
            invalidMolecularState.addEvent(labEvent);
            return HANDLER_RESPONSE.ERROR;
        }
        try {
            labEvent.validateTargetMolecularState();
        }
        catch(InvalidMolecularStateException e) {
            invalidMolecularState.addEvent(labEvent);
            return HANDLER_RESPONSE.ERROR;
        }

        // have the event alter the molecular state
        // of the target samples
        try {
            labEvent.applyMolecularStateChanges();
            enqueueForPostProcessing(labEvent);
            notifyCheckpoints(labEvent);
            return HANDLER_RESPONSE.OK;
        }
        catch(InvalidMolecularStateException e) {
            return HANDLER_RESPONSE.ERROR;
        }
    }

    /**
     * If a relevant project considers this event a checkpointable
     * event, notify each project, taking care to only post a single
     * message for the entire event, instead of spamming them
     * with a jira ticket comment per aliquot.
     * @param event
     */
    public void notifyCheckpoints(LabEvent event) {
        Map<Project,Collection<StartingSample>> samplesForProject = new HashMap<Project,Collection<StartingSample>>();
        for (LabVessel container : event.getAllLabVessels()) {
            for (SampleInstance sampleInstance: container.getSampleInstances()) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    Project p = projectPlan.getProject();
                    if (!samplesForProject.containsKey(p)) {
                        samplesForProject.put(p,new HashSet<StartingSample>());
                    }
                    if (p.getCheckpointableEvents().contains(event.getEventName())) {
                        samplesForProject.get(p).add(sampleInstance.getStartingSample());
                    }
                }

            }
        }
        for (Map.Entry<Project, Collection<StartingSample>> entry : samplesForProject.entrySet()) {
            String message = entry.getValue().size() + " aliquots for " + entry.getKey().getProjectName() + " have been processed through the " + event.getEventName() + " event";
            entry.getKey().addJiraComment(message);
        }
    }
    
    // todo thread for doing Stalker.stalk() for all active projects, LabVessels?
    // todo or make a separate "Lost" stalker?

    /**
     * Queue this event up for various post processing,
     * like notifing project stalkers and adding the
     * status to each sample
     * @param labEvent
     */
    private void enqueueForPostProcessing(LabEvent labEvent) {
        
    }
    
    

    /**
     * Probably farm this out to a thread queue
     * @param event
     */
    private void updateSampleStatus(LabEvent event) {
        for (LabVessel target: event.getTargetLabVessels()) {
            for (SampleInstance sampleInstance: target.getSampleInstances()) {
                sampleInstance.getStartingSample().logNote(new StatusNote(event.getEventName()));
            }
        }
    }



    /**
     * Call this as part of a sweeper thread that runs
     * every 5-10 minutes.
     * @param labEvent
     */
    private void retryInvalidMolecularState(LabEvent labEvent) {
        for (LabEvent partiallyProcessedEvent: invalidMolecularState.findRelatedEvents(labEvent)) {
            processEvent(partiallyProcessedEvent);
        }
    }
    
    /**
     * When we receive a lab event which references an
     * "unknown" LabVessel (typically we expect the source
     * to have been registered already, but not necessarily
     * the destination), we record the fluid motion, but we
     * probably don't have enough information to compute
     * and update the sample level metadata (molecular state).
     *
     * Although we record the fluid motion, we have not fully
     * processed the message.  So we hold the message in a
     * cache of partially processed messages.
     *
     * Various UI screens and web services need to understand
     * this state of affairs.  On the one hand, we know the
     * chain of custody for the LabVessels; on the other hand,
     * we do not know the metadata.  Our UIs and query services
     * need to keep these things separate so that users can
     * easily see that something weird is afoot.
     *
     * Probably want this method called from a thread queue
     * that runs every 5-10 minutes or so.
     * @param labEvent
     */
    private void retryUnanchoredCache(LabEvent labEvent) {
        // beware that this iteration may recurse to a stack overflow,
        // as every event that is processed as the potential to
        // pull back a pile of partially processed events and
        // reprocess them.
        for (LabEvent partiallyProcessedEvent: unanchored.findRelatedEvents(labEvent)) {
            processEvent(partiallyProcessedEvent);

            /*
            interesting problems arise here.  if you have a compound out of order
            situation, how do you know the order in which you should process the
            3 messages you need in order to make sense of the newly arrived
            message?  with a workflow system, things might become easier.
            but you can do a pretty good job just relying on the molecular state
            validation in the event itself.

            maybe findRelatedEvents sorts thing by event time order; pretty good
            but not perfect.  maybe we randomly sort the list and assume
            that eventually the molecular envelope validation for each
            lab event will help you get things processed in the right order.
             */

        }
    }

    /**
     * Sends an alert message to the lab somehow.  Email list?
     * @param message
     * @param event
     */
    private void sendAlertToLab(String message,LabEvent event) {
        StringBuilder alertText = new StringBuilder();

        alertText.append(message).append("\n");
        alertText.append(event.getEventName() + " from " + event.getEventOperator().getLogin() + " sent on " + event.getEventDate());
    }

}
