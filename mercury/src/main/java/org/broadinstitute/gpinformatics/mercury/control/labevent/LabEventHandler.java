package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

// Implements Serializable because it's used by a Stateful session bean.
public class LabEventHandler implements Serializable {

    public enum HandlerResponse {
        OK,
        ERROR /* further refine to "out of order", "bad molecular envelope", critical, warning, etc. */
    }

    private static final Log LOG = LogFactory.getLog(LabEventHandler.class);

//    private PartiallyProcessedLabEventCache unanchored;

//    private PartiallyProcessedLabEventCache invalidMolecularState;

    private WorkflowLoader workflowLoader;

    private AthenaClientService athenaClientService;

    private BSPUserList bspUserList;

    private BucketBean bucketBean;

    private Bucket workingBucket;

    @Inject
    private JiraCommentUtil jiraCommentUtil;

    LabEventHandler() {
    }

    @Inject
    public LabEventHandler(WorkflowLoader workflowLoader, AthenaClientService athenaClientService, BucketBean bucketBean,
             Bucket workingBucket, BSPUserList bspUserList) {
        this.workflowLoader = workflowLoader;
        this.athenaClientService = athenaClientService;
        this.bucketBean = bucketBean;
        this.bspUserList = bspUserList;
        this.workingBucket = workingBucket;
    }

    public HandlerResponse processEvent(LabEvent labEvent) {
        /*
           Happens after the message is actually recorded but before the message is processed (?)
           if the previous step in the workflow is a Bucket, the message will attempt to drain the Bucket
           (create/update a batch) to move the vessels forward.

             This action should not throw an exception in the bucket batching.  Just at least record the fact that
             this action happened

        */

        if (jiraCommentUtil != null) {
            try {
                jiraCommentUtil.postUpdate(labEvent);
            } catch (Exception e) {
                // This is not fatal, so don't rethrow
                LOG.error("Failed to update JIRA", e);
            }
        }
        //notifyCheckpoints(labEvent);

        /*
            Since multiple Workflow Versions can be associated with the collection of vessels, individually determine
            what the previous bucket for the vessels will be
         */
        Map<WorkflowStepDef, Collection<LabVessel>> bucketVessels = itemizeBucketItems(labEvent);

        /*
            If buckets were found, this means that the previous viable step in the workflow is a bucket.  The source vessels
            for this event need to be removed from that bucket.
         */
        for (WorkflowStepDef bucketDef : bucketVessels.keySet()) {
            WorkflowStepDef workingBucketIdentifier = bucketDef;

            /*
                If the bucket previously existed, retrieve it, otherwise create a new one based on the workflow step
                definition
             */
            if (workingBucket == null) {

                //TODO SGM, JMT:  this check should probably be part of the pre process Validation call from the decks
                LOG.error("Bucket " + workingBucketIdentifier.getName() +
                        " is expected to have vessels but no instance of the bucket can be found.");
                workingBucket = new Bucket(workingBucketIdentifier);
            }

            /*
                If the source item is a TubeFormation (for a rack) we really only want to deal with the Tubes
                themselves.  Extract them and use that to pull from the bucket.
             */
            Set<LabVessel> eventVessels = labEvent.getSourceVesselTubes();

            bucketBean.start(bspUserList.getById(labEvent.getEventOperator()).getUsername(), eventVessels,
                    workingBucket, labEvent.getEventLocation());
        }

        /*
            Since multiple Workflow Versions can be associated with the collection of vessels, individually determine
            what the next bucket for the vessels will be
         */
        Map<WorkflowStepDef, Collection<LabVessel>> bucketVesselCandidates = itemizeBucketCandidates(labEvent);

        /*
            If buckets were found, this means that the next viable (Non branch) step in the workflow is a bucket.  The
            source vessels for this event need to be removed from that bucket.
         */
        for (WorkflowStepDef bucketDef : bucketVesselCandidates.keySet()) {
            WorkflowStepDef workingBucketIdentifier = bucketDef;

            /*
               If the bucket previously existed, retrieve it, otherwise create a new one based on the workflow step
               definition
            */

            if (workingBucket == null) {
                workingBucket = new Bucket(workingBucketIdentifier);
            }

            /*
                If the Target item is a TubeFormation (for a rack) we really only want to deal with the Tubes
                themselves.  Extract them and use that to add to the bucket.
             */
            Set<LabVessel> eventVessels = labEvent.getTargetVesselTubes();

            bucketBean.add(eventVessels, workingBucket, bspUserList.getById(labEvent.getEventOperator()).getUsername(),
                    labEvent.getEventLocation(), workingBucketIdentifier.getLabEventTypes().iterator().next());
        }

        return HandlerResponse.OK;

    }

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
//    private void retryInvalidMolecularState(LabEvent labEvent) {
//        for (LabEvent partiallyProcessedEvent : invalidMolecularState.findRelatedEvents(labEvent)) {
//            processEvent(partiallyProcessedEvent);
//        }
//    }

    /**
     * When we receive a lab event which references an
     * "unknown" LabVessel (typically we expect the source
     * to have been registered already, but not necessarily
     * the destination), we record the fluid motion, but we
     * probably don't have enough information to compute
     * and update the sample level metadata (molecular state).
     * <p/>
     * Although we record the fluid motion, we have not fully
     * processed the message.  So we hold the message in a
     * cache of partially processed messages.
     * <p/>
     * Various UI screens and web services need to understand
     * this state of affairs.  On the one hand, we know the
     * chain of custody for the LabVessels; on the other hand,
     * we do not know the metadata.  Our UIs and query services
     * need to keep these things separate so that users can
     * easily see that something weird is afoot.
     * <p/>
     * Probably want this method called from a thread queue
     * that runs every 5-10 minutes or so.
     *
     * @param labEvent
     */
//    private void retryUnanchoredCache(LabEvent labEvent) {
//        // beware that this iteration may recurse to a stack overflow,
//        // as every event that is processed as the potential to
//        // pull back a pile of partially processed events and
//        // reprocess them.
//        for (LabEvent partiallyProcessedEvent : unanchored.findRelatedEvents(labEvent)) {
//            processEvent(partiallyProcessedEvent);
//
//            /*
//            interesting problems arise here.  if you have a compound out of order
//            situation, how do you know the order in which you should process the
//            3 messages you need in order to make sense of the newly arrived
//            message?  with a workflow system, things might become easier.
//            but you can do a pretty good job just relying on the molecular state
//            validation in the event itself.
//
//            maybe findRelatedEvents sorts thing by event time order; pretty good
//            but not perfect.  maybe we randomly sort the list and assume
//            that eventually the molecular envelope validation for each
//            lab event will help you get things processed in the right order.
//             */
//
//        }
//    }

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

    /**
     * getWorkflowVersion will, based on the BusinessKey of a product order, find the defined Workflow Version.  It
     * does this by querying to the "Athena" side of Mercury for the ProductOrder Definition and looks up the
     * workflow definition based on the workflow name defined on the ProductOrder
     *
     * @param productOrderKey Business Key for a previously defined product order
     * @return Workflow Definition for the defined workflow for the product order represented by productOrderKey
     */
    public ProductWorkflowDefVersion getWorkflowVersion(String productOrderKey) {
        WorkflowConfig workflowConfig = workflowLoader.load();

        ProductWorkflowDefVersion versionResult = null;

        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(productOrderKey);

        if (StringUtils.isNotBlank(productOrder.getProduct().getWorkflowName())) {
            ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                    productOrder.getProduct().getWorkflowName());

            versionResult = productWorkflowDef.getEffectiveVersion();
        }
        return versionResult;
    }

    /**
     * Primarily utilized for removing items from a bucket, this method determines the proper Bucket->Vessel Combination.
     * <p/>
     * Across the collection of Vessels found for the given lab event, each vessel may be part of a different
     * workflow version.  This means that to accurately determine the name of the previous available step, we must look
     * at each lab vessel individually to determine the if the previous step is a bucket relative to the workflow
     * step associated with the lab event.
     *
     * @param labEvent Processed lab event for a particular workflow step.  This event will have reference to the
     *                 Vessels for which this event/workflow step has taken action
     * @return a map consisting of all lab vessels referenced in the lab event indexed by the workflow step that
     *         relates to the viable bucket.
     */
    public Map<WorkflowStepDef, Collection<LabVessel>> itemizeBucketItems(LabEvent labEvent) {
        Map<WorkflowStepDef, Collection<LabVessel>> bucketVessels = new HashMap<WorkflowStepDef, Collection<LabVessel>>();

        for (LabVessel currVessel : labEvent.getSourceVesselTubes()) {

            Collection<String> productOrders = currVessel.getNearestProductOrders();
            WorkflowStepDef workingBucketName = null;
            if (productOrders != null && !productOrders.isEmpty()) {
                ProductWorkflowDefVersion workflowDef = getWorkflowVersion(productOrders.iterator().next());

                if (workflowDef != null &&
                        workflowDef.isPreviousStepBucket(labEvent.getLabEventType().getName())) {
                    workingBucketName = workflowDef.getPreviousStep(
                            labEvent.getLabEventType().getName());
                }
            }

            if (workingBucketName != null) {
                if (!bucketVessels.containsKey(workingBucketName)) {
                    bucketVessels.put(workingBucketName, new LinkedList<LabVessel>());
                    if (bucketVessels.keySet().size() > 1) {
                        LOG.warn("Samples are coming from multiple Buckets");
                    }
                }
                bucketVessels.get(workingBucketName).add(currVessel);
            }
        }
        return bucketVessels;
    }

    /**
     * Primarily utilized for adding items to a bucket, this method determines the proper Bucket->Vessel Combination.
     * <p/>
     * Across the collection of Vessels found for the given lab event, each vessel may be part of a different
     * workflow version.  This means that to accurately determine the name of the next available step, we must look
     * at each lab vessel individually to determine if the next step is a bucket relative to the workflow step
     * associated with the lab event.
     *
     * @param labEvent Processed lab event for a particular workflow step.  This event will have reference to the
     *                 Vessels for which this event/workflow step has taken action
     * @return a map consisting of all lab vessels referenced in the lab event indexed by the workflow step that
     *         relates to the viable bucket.
     */
    public Map<WorkflowStepDef, Collection<LabVessel>> itemizeBucketCandidates(LabEvent labEvent) {


        Map<WorkflowStepDef, Collection<LabVessel>> bucketVessels =
                new HashMap<WorkflowStepDef, Collection<LabVessel>>();

        for (LabVessel currVessel : labEvent.getTargetVesselTubes()) {

            /*
                Retrieve product orders related to the lab vessel
             */
            Collection<String> productOrders = currVessel.getNearestProductOrders();
            WorkflowStepDef workingBucketName = null;

            if (productOrders != null && !productOrders.isEmpty()) {

                /*
                    Determine the appropriate workflow version based on the product order
                 */
                ProductWorkflowDefVersion workflowDef = getWorkflowVersion(productOrders.iterator().next());

                /*
                    As long as this step is not on a workflow branch, Find the next workflow step that is not on a
                    workflow branch and determine if it is a bucket.
                 */
                if (workflowDef != null &&
                        !workflowDef.isStepDeadBranch(labEvent.getLabEventType().getName()) &&
                        workflowDef.isNextNonDeadBranchStepBucket(labEvent.getLabEventType().getName())) {

                    /*
                        If the next viable step is a bucket, save it for indexing it to the appropriate lab vessels
                     */
                    workingBucketName = workflowDef.getNextNonDeadBranchStep(
                            labEvent.getLabEventType().getName());
                }
            }

            if (workingBucketName != null) {
                if (!bucketVessels.containsKey(workingBucketName)) {

                    /*
                        if we have found the right bucket, index the vessel with that bucket in the return map
                     */
                    bucketVessels.put(workingBucketName, new LinkedList<LabVessel>());
                    if (bucketVessels.keySet().size() > 1) {
                        LOG.warn("Samples are coming from multiple Buckets");
                    }
                }
                bucketVessels.get(workingBucketName).add(currVessel);
            }
        }
        return bucketVessels;
    }


}
