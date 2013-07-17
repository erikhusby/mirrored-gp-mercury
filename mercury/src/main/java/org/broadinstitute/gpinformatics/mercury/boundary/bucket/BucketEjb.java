package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class BucketEjb {

    // todo jmt rename to BucketEjb?  Many unused parameters and an unused field.

    private LabEventFactory labEventFactory;

    private JiraService jiraService;

    private LabBatchEjb batchEjb;

    private static final Log logger = LogFactory.getLog(BucketEjb.class);

    public BucketEjb() {
    }

    @Inject
    public BucketEjb(LabEventFactory labEventFactory, JiraService jiraService, LabBatchEjb batchEjb) {
        this.labEventFactory = labEventFactory;
        this.jiraService = jiraService;
        this.batchEjb = batchEjb;
    }

    /**
     * Adds a pre-defined collection of {@link LabVessel}s to a given bucket.  When the vessels do not already
     * have a product order association, uses the specified pdoBusinessKey instead.
     *
     * @param entriesToAdd         Collection of LabVessels to be added to a bucket
     * @param bucket               instance of a bucket entity associated with a workflow bucket step
     * @param entryType            the type of bucket entry to add
     * @param operator             Represents the user that initiated adding the vessels to the bucket
     * @param labEventLocation     Machine location from which operator initiated this action
     * @param eventType            Type of the Lab Event that initiated this bucket add request
     * @param singlePdoBusinessKey Product order key for all vessels
     */
    public Collection<BucketEntry> add(@Nonnull Collection<LabVessel> entriesToAdd, @Nonnull Bucket bucket,
                                       BucketEntry.BucketEntryType entryType, @Nonnull String operator,
                                       @Nonnull String labEventLocation,
                                       LabEventType eventType,
                                       @Nonnull String singlePdoBusinessKey) {

        List<BucketEntry> listOfNewEntries = new LinkedList<BucketEntry>();
        Map<String, Collection<LabVessel>> pdoKeyToVesselMap = new HashMap<String, Collection<LabVessel>>();

        for (LabVessel currVessel : entriesToAdd) {
            listOfNewEntries.add(bucket.addEntry(singlePdoBusinessKey, currVessel, entryType));

            if (!pdoKeyToVesselMap.containsKey(singlePdoBusinessKey)) {
                pdoKeyToVesselMap.put(singlePdoBusinessKey, new LinkedList<LabVessel>());
            }
            pdoKeyToVesselMap.get(singlePdoBusinessKey).add(currVessel);
        }

        Set<LabEvent> eventList = new HashSet<LabEvent>();

        //TODO SGM: Pass in Latest Batch?
        eventList.addAll(labEventFactory.buildFromBatchRequests(listOfNewEntries, operator, null, labEventLocation,
                eventType));

        return listOfNewEntries;
    }

    /**
     * A pared down version of
     * {@link #start(java.util.Collection, String, String)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param vesselsToBatch          Collection of LabVessels to be removed to a bucket
     * @param workingBucket           instance of a bucket entity associated with a workflow bucket step
     * @param batchInitiationLocation Machine location from which operator initiated this action
     */
    public void start(@Nonnull String operator, @Nonnull Collection<LabVessel> vesselsToBatch,
                      @Nonnull Bucket workingBucket, String batchInitiationLocation) {
        start(operator, vesselsToBatch, workingBucket, batchInitiationLocation, null);
    }

    /**
     * A pared down version of
     * {@link #start(java.util.Collection, String, String)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param vesselsToBatch          Collection of LabVessels to be removed from a bucket
     * @param workingBucket           instance of a bucket entity associated with a workflow bucket step
     * @param batchInitiationLocation Machine location from which operator initiated this action
     */
    @DaoFree
    public void startDBFree(@Nonnull String operator, @Nonnull Collection<LabVessel> vesselsToBatch,
                            @Nonnull Bucket workingBucket, String batchInitiationLocation) {

        Set<BucketEntry> bucketEntrySet = buildBatchListByVessels(vesselsToBatch, workingBucket);
        startBucketDrain(bucketEntrySet, operator, batchInitiationLocation, false);

    }

    /**
     * A version of {@link #start(java.util.Collection, String, String)}
     * for which just the Vessels needed for a batch are referenced.  This is primarily needed to support batching
     * when initiated from messaging
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param vesselsToBatch          Collection of LabVessels to be removed from a bucket
     * @param workingBucket           instance of a bucket entity associated with a workflow bucket step
     * @param batchInitiationLocation Machine location from which operator initiated this action
     * @param batchTicket             Key of the Jira Ticket to be associated with a batch for the vessels processed
     */
    public void start(@Nonnull String operator, @Nonnull Collection<LabVessel> vesselsToBatch,
                      @Nonnull Bucket workingBucket, String batchInitiationLocation, String batchTicket) {

        Set<BucketEntry> bucketEntrySet = buildBatchListByVessels(vesselsToBatch, workingBucket);

//        LabBatch bucketBatch =
        startBucketDrain(bucketEntrySet, operator, batchInitiationLocation, false);

//        if (bucketBatch.getJiraTicket() == null) {
//            batchEjb.batchToJira(operator, batchTicket, bucketBatch);
//        }

//        batchEjb.jiraBatchNotification(bucketBatch);


    }

    /**
     * Returns a Set of bucket entries that correspond to a given collection of vessels in the context of a given
     * bucket
     *
     * @param vesselsToBatch {@link LabVessel}s for which the user needs to find bucket entries
     * @param workingBucket  The bucket from which to find the the bucket entries
     *
     * @return
     */
    public Set<BucketEntry> buildBatchListByVessels(Collection<LabVessel> vesselsToBatch, Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        for (LabVessel workingVessel : vesselsToBatch) {

            BucketEntry foundEntry = workingBucket.findEntry(workingVessel);
            if (foundEntry != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Adding entry " + foundEntry.getBucketEntryId() + " for vessel " + foundEntry.getLabVessel()
                                    .getLabCentricName() +
                            " and PDO " + foundEntry.getPoBusinessKey() + " to be popped from bucket.");
                }
                bucketEntrySet.add(foundEntry);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Attempting to pull a vessel, " + workingVessel.getLabel() + ", from a bucket, " +
                                 workingBucket.getBucketDefinitionName() + ", when it does not exist in that bucket");
                }
            }
        }
        return bucketEntrySet;
    }

    /**
     * a pared down version of
     * {@link #start(String, int, org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator             Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples represents how many samples are to be removed from the bucket and added to a batch.
     *                             the system will take the samples from the bucket starting from the top most
     *                             prioritized item and work down the queue from there.
     * @param workingBucket        instance of a bucket entity associated with a workflow bucket step
     */
    public void start(String operator, int numberOfBatchSamples, @Nonnull Bucket workingBucket) {
        start(operator, numberOfBatchSamples, workingBucket, null);
    }

    /**
     * a version of the
     * {@link #start(java.util.Collection, String, String)}
     * method
     * with which a user can simply request a number of samples to pull from a given bucket wiht which to make a batch.
     * The samples removed will be with respect to the order they are found in the bucket
     *
     * @param operator             Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples represents how many samples are to be removed from the bucket and added to a batch.
     *                             the system will take the samples from the bucket starting from the top most
     *                             prioritized item and work down the queue from there.
     * @param workingBucket        instance of a bucket entity associated with a workflow bucket step
     */
    @DaoFree
    public LabBatch startDBFree(@Nonnull String operator, int numberOfBatchSamples,
                                @Nonnull Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = buildBatchListBySize(numberOfBatchSamples, workingBucket);
        return startBucketDrain(bucketEntrySet, operator, LabEvent.UI_EVENT_LOCATION, true);
    }

    /**
     * a version of the
     * {@link #start(java.util.Collection, String, String)}
     * method
     * with which a user can simply request a number of samples to pull from a given bucket wiht which to make a batch.
     * The samples removed will be with respect to the order they are found in the bucket
     *
     * @param operator             Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples represents how many samples are to be removed from the bucket and added to a batch.
     *                             the system will take the samples from the bucket starting from the top most
     *                             prioritized item and work down the queue from there.
     * @param workingBucket        instance of a bucket entity associated with a workflow bucket step
     * @param batchTicket          Key of the Jira Ticket to be associated with a batch for the vessels processed
     */
    public void start(@Nonnull String operator, int numberOfBatchSamples, @Nonnull Bucket workingBucket,
                      String batchTicket) {

//        LabBatch bucketBatch = null;
//
//        bucketBatch =
        startDBFree(operator, numberOfBatchSamples, workingBucket);

    }

    /**
     * Given a bucket and a batch size, this method will return as many entries from the top of the bucket as is
     * defined by the batch size.
     *
     * @param numberOfBatchSamples Batch size/Nuber of entries to retrieve from the bucket
     * @param workingBucket        bucket from which to create a batch of entries
     *
     * @return a set of bucket entries found in the given bucket.  The size of the set is defined by
     *         numberOfBatchSamples
     */
    public Set<BucketEntry> buildBatchListBySize(int numberOfBatchSamples, Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        List<BucketEntry> sortedBucketEntries = new ArrayList<BucketEntry>(workingBucket.getBucketEntries());

        Iterator<BucketEntry> bucketEntryIterator = sortedBucketEntries.iterator();

        for (int i = 0; i < numberOfBatchSamples && bucketEntryIterator.hasNext(); i++) {
            BucketEntry currEntry = bucketEntryIterator.next();

            bucketEntrySet.add(currEntry);
        }

        return bucketEntrySet;
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries           Collection of Entities that represent the PDO->lab Vessel combination that is to
     *                                be removed from the bucket.
     * @param operator                Reference to the user that is requesting the batch.
     * @param batchInitiationLocation Machine location from which operator initiated this action
     */
    public void start(@Nonnull Collection<BucketEntry> bucketEntries, @Nonnull String operator,
                      String batchInitiationLocation) {
        start(bucketEntries, operator, batchInitiationLocation, null);
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries           Collection of Entities that represent the PDO->lab Vessel combination that is to
     *                                be removed from the bucket.
     * @param operator                Reference to the user that is requesting the batch.
     * @param batchInitiationLocation Machine location from which operator initiated this action
     * @param batchTicket             Key of the Jira Ticket to be associated with a batch for the vessels processed
     */
    public void start(@Nonnull Collection<BucketEntry> bucketEntries, @Nonnull String operator,
                      String batchInitiationLocation, String batchTicket) {
        /**
         * Side effect: create a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} for each
         * associated {@link LabVessel} and
         * set {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#setProductOrderId(String)
         * the product order} based on what's in the {@link BucketEntry}
         *
         * Create (if necessary) a new batch
         */
//        LabBatch bucketBatch =
        startBucketDrain(bucketEntries, operator, batchInitiationLocation, false);

    }

    /**
     * Core batch "Start" logic.
     * <p/>
     * Contains all the logic for removing vessels from a bucket and associating events with that removal
     *
     * @param bucketEntries           Collection of Entities that represent the PDO->lab Vessel combination that is to
     *                                be removed from the bucket.
     * @param operator                Represents the user that initiated adding the vessels to the bucket
     * @param batchInitiationLocation Machine location from which operator initiated this action
     * @param autoBatch               Indicator to let the system know if they should even perform Auto Batching.
     *
     * @return Either a newly created batch object, or the most recent one found that incorporates all
     *         lab vessels being processed in this request.
     */
//    @DaoFree
    private LabBatch startBucketDrain(@Nonnull Collection<BucketEntry> bucketEntries, @Nonnull String operator,
                                      String batchInitiationLocation, boolean autoBatch) {
        Set<LabVessel> batchVessels = new HashSet<LabVessel>();

        for (BucketEntry currEntry : bucketEntries) {
            batchVessels.add(currEntry.getLabVessel());
        }

        LabBatch bucketBatch = null;
        if (!batchVessels.isEmpty()) {
            for (LabBatch currBatch : batchVessels.iterator().next().getNearestWorkflowLabBatches()) {
                if (LabBatch.isCommonBatch(currBatch, batchVessels)) {
                    bucketBatch = currBatch;
                }
            }
        }
        /*
            If the tubes being pulled from the Bucket are all from one LabBatch,  just update that LabBatch and move
            forward.

            otherwise (no previous batch, multiple lab batches, existing batch with samples that are not in an
            existing batch) create a new Lab Batch.
         */
        if (bucketBatch == null) {
//            throw new InformaticsServiceException("There should be an existing Batch");
        }

        for (BucketEntry currEntry : bucketEntries) {
            currEntry.setLabBatch(bucketBatch);
        }
        archiveEntries(bucketEntries);

        logger.info("Size of entries to remove is " + bucketEntries.size());
        return bucketBatch;
    }

    /**
     * Helper method to encapsulate the removal of (potentially) multiple entries from a bucket
     *
     * @param bucketEntries collection of bucket entries to be removed from the buckets in which they exist
     */
    //TODO SGM  Move to a bucket factory class
    private void archiveEntries(@Nonnull Collection<BucketEntry> bucketEntries) {
        for (BucketEntry currEntry : bucketEntries) {
            logger.info("Adding entry " + currEntry.getBucketEntryId() + " for vessel " + currEntry.getLabVessel()
                    .getLabCentricName() +
                        " and PDO " + currEntry.getPoBusinessKey() + " to be popped from bucket.");
            currEntry.getBucket().removeEntry(currEntry);
        }
        jiraRemovalUpdate(bucketEntries, "Extracted for Batch");
    }

    /**
     * All the samples in this vessel are being reworked, so mark them as such
     * so they don't show up in the bucket.
     *
     * @param labVessel vessel full of samples for rework.
     */
    public void removeRework(LabVessel labVessel) {
        labVessel.deactivateRework();
    }


    /**
     * For whatever reason, sometimes the lab can't
     * do something, or PDMs decide that they shouldn't
     * do something.  Cancelling the entry removes
     * it from the bucket.
     *
     * @param bucketEntry Entity that represent the PDO->lab Vessel combination that is to be removed from the bucket.
     * @param operator    Represents the user that initiated adding the vessels to the bucket
     * @param reason      textual notes on why the thing is
     */
    public void cancel(@Nonnull BucketEntry bucketEntry, String operator, String reason) {

        Collection<BucketEntry> singleRemoval = Collections.singletonList(bucketEntry);

        archiveEntries(singleRemoval);

        jiraRemovalUpdate(bucketEntry, reason);
    }

    /**
     * helper method to make a comment on the Jira ticket for the Product Order associated with the entry being
     * removed
     *
     * @param bucketEntry entry in the bucket being removed
     * @param reason      captures the information about why this entry is being removed from the bucket
     */
    private void jiraRemovalUpdate(@Nonnull BucketEntry bucketEntry, String reason) {
        try {

            jiraService.addComment(bucketEntry.getPoBusinessKey(), bucketEntry.getPoBusinessKey() + ":" +
                                                                   bucketEntry.getLabVessel().getLabCentricName() +
                                                                   " Removed from bucket " + bucketEntry
                    .getBucket()
                    .getBucketDefinitionName() + ":: " + reason);
        } catch (IOException ioe) {
            logger.error("Error attempting to create jira removal comment for " +
                         bucketEntry.getPoBusinessKey() + " " +
                         bucketEntry.getLabVessel().getLabCentricName(), ioe);
        }
    }

    /**
     * This is a helper method to make a comment on the Jira ticket for the Product Order associated with the entry being
     * removed. This method takes in a collection of bucket entries and rolls them up into one JIRA comment.
     *
     * @param entries All of the entries rolled up in this message.
     * @param reason  captures the information about why this entry is being removed from the bucket
     */
    private void jiraRemovalUpdate(@Nonnull Collection<BucketEntry> entries, String reason) {
        Map<String, Collection<BucketEntry>> pdoToEntries = new HashMap<String, Collection<BucketEntry>>();
        for (BucketEntry entry : entries) {
            Collection<BucketEntry> pdoEntries = pdoToEntries.get(entry.getPoBusinessKey());
            if (pdoEntries == null) {
                pdoEntries = new ArrayList<BucketEntry>();
                pdoToEntries.put(entry.getPoBusinessKey(), pdoEntries);
            }
            pdoEntries.add(entry);
        }
        for (Map.Entry<String, Collection<BucketEntry>> entry : pdoToEntries.entrySet()) {
            try {
                jiraService.addComment(entry.getKey(), entry.getKey() + ":" + entry.getValue().size()
                                                       + " sample(s) removed from bucket "
                                                       + entry.getValue().iterator().next().getBucket()
                        .getBucketDefinitionName()
                                                       + ":: " + reason);
            } catch (IOException ioe) {
                logger.error("Error attempting to create jira removal comment for " +
                             entry.getKey() + " " +
                             entry.getValue().size() + " samples.", ioe);
            }
        }
    }

    /**
     * Helper method to extract the Pdo Business Keys for a given list of Bucket Entries
     *
     * @param entries Collection of Entities that represent the PDO->lab Vessel combination that is to
     *                be removed from the bucket.
     *
     * @return Set of all PDO business keys that are references in the collection of bucket entries being
     *         processed
     */
    private static Set<String> extractProductOrderSet(Collection<BucketEntry> entries) {
        Set<String> pdoSet = new HashSet<String>();

        for (BucketEntry currEntry : entries) {
            pdoSet.add(currEntry.getPoBusinessKey());
        }

        return pdoSet;
    }

    /**
     * Helper method to extract all LabVessels associated with a given PDO Business Key found in a given list of Bucket
     * Entries
     *
     * @param pdo     Product Order Business key associated with the returned vessels
     * @param entries
     *
     * @return
     */
    private static Collection<LabVessel> extractPdoLabVessels(String pdo, Collection<BucketEntry> entries) {
        List<LabVessel> labVessels = new LinkedList<LabVessel>();

        for (BucketEntry currEntry : entries) {
            if (currEntry.getPoBusinessKey().equals(pdo)) {
                labVessels.add(currEntry.getLabVessel());
            }
        }

        return labVessels;
    }

}
