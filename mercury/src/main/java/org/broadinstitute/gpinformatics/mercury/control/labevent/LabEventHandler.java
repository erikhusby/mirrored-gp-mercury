package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Billable;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.InvalidMolecularStateException;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.PartiallyProcessedLabEventCache;
import org.broadinstitute.gpinformatics.mercury.entity.sample.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

// Implements Serializable because it's used by a Stateful session bean.
public class LabEventHandler implements Serializable {


    public enum HANDLER_RESPONSE {
        OK,
        ERROR /* further refine to "out of order", "bad molecular envelope", critical, warning, etc. */
    }

    PartiallyProcessedLabEventCache unanchored;

    PartiallyProcessedLabEventCache invalidMolecularState;

    @Inject
    Event<Billable> billableEvents;

    @Inject
    QuoteService quoteService;

    @Inject
    WorkflowLoader workflowLoader;

    @Inject
    AthenaClientService athenaClientService;

    @Inject
    private BSPUserList bspUserList;

    private static final Log LOG = LogFactory.getLog(LabEventHandler.class);

    public LabEventHandler() {
    }

    public LabEventHandler(WorkflowLoader workflowLoader, AthenaClientService clientService) {
        this.workflowLoader = workflowLoader;
        this.athenaClientService = clientService;
    }

    public HANDLER_RESPONSE processEvent(LabEvent labEvent) {
        // random thought, which should go onto confluence doc:
        /*

        The first thing event handling should do is persist the message.
        Recording what has actually happened in terms of container
        motion (fluid motion) is paramount.  This means that the
        entire app must tolerate the fact that we might
        have {@link LabVessel}s that have no {@link SampleInstance}s.

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
           Happens after the message is actually recorded but before the message is processed (?)
           if the previous step in the workflow is a Bucket, the message will attempt to drain the Bucket
           (create/update a batch) to move the vessels forward.

             This action should not throw an exception in the bucket batching.  Just at least record the fact that
             this action happened

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

        // todo arz fix this by using LabBatch instead.  maybe strip out this denormalization entirely,
        // and leave the override processing for on-the-fly work in VesselContainer
        //processProjectPlanOverrides(labEvent, workflow);

        JiraCommentUtil.postUpdate(labEvent.getLabEventType().getName() + " Event Applied", null,
                                   labEvent.getAllLabVessels());
        try {
            labEvent.applyMolecularStateChanges();
            enqueueForPostProcessing(labEvent);
            //notifyCheckpoints(labEvent);

            // todo figure out how to get the handler transaction
            // and the billing transaction isolated properly: http://docs.jboss.org/weld/reference/1.1.0.Final/en-US/html/events.html#d0e4075
            // only bill if the persistence succeeds on the mercury side.

        } catch (InvalidMolecularStateException e) {
            return HANDLER_RESPONSE.ERROR;
        }
        return HANDLER_RESPONSE.OK;

    }

    /**
     * If this is the first {@link LabEvent} seen for
     * some {@link LabVessel}s that have been placed in
     * a {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue},
     * this method figures out whether there is a {@link org.broadinstitute.gpinformatics.mercury.entity.queue.WorkQueueEntry#getProjectPlanOverride()}
     * and, if so, applies the override via {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getProjectPlanOverride()}.
     *
     * It also {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue#remove(org.broadinstitute.gpinformatics.mercury.entity.queue.WorkQueueEntry)}s
     * the {@link WorkQueueEntry} from the {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue}.
     *
     * If #labEvent cannot be uniquely mapped to a {@link WorkQueueEntry}, an exception
     * is thrown.
     * @param labEvent
     * @param workflow
     */
    //    private void processProjectPlanOverrides(LabEvent labEvent,
    //                                             WorkflowDescription workflow) {
    //        if (workflow != null) {
    //            for (LabVessel labVessel : labEvent.getAllLabVessels()) {
    //                if (OrmUtil.proxySafeIsInstance(labVessel, VesselContainerEmbedder.class)) {
    //                    Collection<LabVessel> containedVessels = OrmUtil.proxySafeCast(labVessel, VesselContainerEmbedder.class).
    //                            getContainerRole().getContainedVessels();
    //                    if (containedVessels.isEmpty()) {
    //                        processProjectPlanOverrides(labEvent,labVessel,workflow);
    //                    }
    //                    else {
    //                        for (LabVessel vessel : containedVessels) {
    //                            processProjectPlanOverrides(labEvent,vessel,workflow);
    //                        }
    //                    }
    //                }
    //
    //            }
    //        }
    //    }

    /**
     * {@link #processProjectPlanOverrides(org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent, org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel, org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription)}
     * @param labEvent
     * @param vessel
     * @param workflow
     */
    //    private void processProjectPlanOverrides(LabEvent labEvent,
    //                                             LabVessel vessel,
    //                                             WorkflowDescription workflow) {
    //        for (LabWorkQueue labWorkQueue : workQueueDAO.getPendingQueues(vessel, workflow)) {
    //            Collection<WorkQueueEntry> workQueueEntries = labWorkQueue.getEntriesForWorkflow(workflow,vessel);
    //            if (workQueueEntries.size() == 1) {
    //                // not ambiguous: single entry
    //                WorkQueueEntry workQueueEntry = workQueueEntries.iterator().next();
    ////                if (workQueueEntry.getProjectPlanOverride() != null) {
    ////                    labEvent.setProjectPlanOverride(workQueueEntry.getProjectPlanOverride());
    ////                }
    //                workQueueEntry.dequeue();
    //            }
    //            else if (workQueueEntries.size() > 1) {
    //                // todo ambiguous: how do we narrow down the exact queue that this
    //                // vessel was placed in?
    //                throw new RuntimeException("Mercury doesn't know which of "  + workQueueEntries.size() + " work queue entries to pull from.");
    //            }
    //            /** else this vessel wasn't place in a {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue} */
    //        }
    //
    //    }

    /**
     * If a relevant project considers this event a checkpointable
     * event, notify each project, taking care to only post a single
     * message for the entire event, instead of spamming them
     * with a jira ticket comment per aliquot.
     *
     * @param event
     */
    public void notifyCheckpoints(LabEvent event) {
        //        Map<Project,Collection<StartingSample>> samplesForProject = new HashMap<Project,Collection<StartingSample>>();
        for (LabVessel container : event.getAllLabVessels()) {
            for (SampleInstance sampleInstance : container.getSampleInstances()) {
                //                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                //                    Project p = projectPlan.getProject();
                //                    if (!samplesForProject.containsKey(p)) {
                //                        samplesForProject.put(p,new HashSet<StartingSample>());
                //                    }
                //                    if (p.getCheckpointableEvents().contains(event.getEventName())) {
                //                        samplesForProject.get(p).add(sampleInstance.getStartingSample());
                //                    }
                //                }

            }
        }
        //        for (Map.Entry<Project, Collection<StartingSample>> entry : samplesForProject.entrySet()) {
        //            String message = entry.getValue().size() + " aliquots for " + entry.getKey().getProjectName() + " have been processed through the " + event.getEventName() + " event";
        //            entry.getKey().addJiraComment(message);
        //        }
    }

    // todo thread for doing Stalker.stalk() for all active projects, LabVessels?
    // todo or make a separate "Lost" stalker?

    /**
     * Queue this event up for various post processing,
     * like notifing project stalkers and adding the
     * status to each sample
     *
     * @param labEvent
     */
    private void enqueueForPostProcessing(LabEvent labEvent) {

    }

    /**
     * Probably farm this out to a thread queue
     *
     * @param event
     */
    private void updateSampleStatus(LabEvent event) {
        for (LabVessel target : event.getTargetLabVessels()) {
            for (SampleInstance sampleInstance : target.getSampleInstances()) {
                //                sampleInstance.getStartingSample().logNote(new StatusNote(event.getEventName()));
            }
        }
    }

    /**
     * Call this as part of a sweeper thread that runs
     * every 5-10 minutes.
     *
     * @param labEvent
     */
    private void retryInvalidMolecularState(LabEvent labEvent) {
        for (LabEvent partiallyProcessedEvent : invalidMolecularState.findRelatedEvents(labEvent)) {
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
     *
     * @param labEvent
     */
    private void retryUnanchoredCache(LabEvent labEvent) {
        // beware that this iteration may recurse to a stack overflow,
        // as every event that is processed as the potential to
        // pull back a pile of partially processed events and
        // reprocess them.
        for (LabEvent partiallyProcessedEvent : unanchored.findRelatedEvents(labEvent)) {
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
     *
     * @param message
     * @param event
     */
    private void sendAlertToLab(String message, LabEvent event) {
        StringBuilder alertText = new StringBuilder();

        alertText.append(message).append("\n");

        alertText.append(event.getLabEventType().getName() + " from " +
                                 bspUserList.getById(event.getEventOperator()).getUsername() +
                                 " sent on " + event.getEventDate());
    }

    public ProductWorkflowDefVersion getWorkflowVersion(String productOrderKey) {
        WorkflowConfig workflowConfig = workflowLoader.load();

        ProductWorkflowDefVersion versionResult = null;

        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(productOrderKey);

        if(StringUtils.isNotBlank(productOrder.getProduct().getWorkflowName())) {
            ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                    productOrder.getProduct().getWorkflowName());

            versionResult = productWorkflowDef.getEffectiveVersion();
        }
        return versionResult;
    }


    public Map<WorkflowStepDef, Collection<LabVessel>> itemizeBucketItems(LabEvent labEvent) {
        Map<WorkflowStepDef, Collection<LabVessel>> bucketVessels = new HashMap<WorkflowStepDef, Collection<LabVessel>>();

        for (LabVessel currVessel : labEvent.getAllLabVessels()) {

            Collection<String> productOrders = currVessel.getNearestProductOrders();
            WorkflowStepDef workingBucketName =null;
            if (productOrders != null && !productOrders.isEmpty()) {
                ProductWorkflowDefVersion workflowDef = getWorkflowVersion(productOrders.iterator().next());

                //TODO SGM consider Null Case for Workflow Dev

                if (workflowDef != null &&
                    workflowDef.isPreviousStepBucket(labEvent.getLabEventType().getName())) {
                    workingBucketName = /*bucketDao.findByName(*/workflowDef.getPreviousStep(
                            labEvent.getLabEventType().getName())/*)*/;
                    if (workingBucketName == null) {
                        workingBucketName = workflowDef.getPreviousStep(
                                labEvent.getLabEventType().getName());
                    }

                }
            }

            if (workingBucketName != null) {
                if (!bucketVessels.containsKey(workingBucketName)) {
                    bucketVessels.put(workingBucketName, new LinkedList<LabVessel>());
                    if (bucketVessels.keySet().size() > 1) {
                        LOG.warn("Samples are coming from multiple Buckets");
    //                        throw new IllegalStateException("Samples are coming from multiple Buckets");
                    }
                }
                bucketVessels.get(workingBucketName).add(currVessel);
            }
        }
        return bucketVessels;
    }



}
