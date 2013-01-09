package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@Stateless
//@RequestScoped
public class BucketBean {

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    JiraService jiraService;

    @Inject
    LabBatchResource batchResource;

    @Inject
    LabBatchEjb batchEjb;

    @Inject
    JiraTicketDao jiraTicketDao;

    private final static Log logger = LogFactory.getLog(BucketBean.class);

    public BucketBean() {
    }

    public BucketBean(LabEventFactory labEventFactoryIn, JiraService testjiraService) {
        this.labEventFactory = labEventFactoryIn;
        jiraService = testjiraService;
    }

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link String product order}
     * <p/>
     * TODO SGM Rethink the return.  Doesn't seem to add any value
     *
     * @param vessel
     * @param productOrder
     * @param operator
     * @return
     */
    public BucketEntry add(@Nonnull LabVessel vessel, @Nonnull String productOrder, @Nonnull Bucket bucket,
                           @Nonnull String operator) {

        BucketEntry newEntry = bucket.addEntry(productOrder, vessel);

        //TODO SGM: Event type is incorrect. Should be the next step
        labEventFactory.createFromBatchItems(productOrder, vessel, 1L, operator, LabEventType.SHEARING_BUCKET_ENTRY,
                LabEvent.UI_EVENT_LOCATION);
        try {
            jiraService.addComment(productOrder, vessel.getLabCentricName() +
                                                 " added to bucket " + bucket.getBucketDefinitionName());
        } catch (IOException ioe) {
            logger.error("error attempting to add a jira comment for adding " +
                         productOrder + ":" + vessel.getLabCentricName() + " to bucket " +
                         bucket.getBucketDefinitionName(), ioe);
        }
        return newEntry;
    }

    /**
     * adds a pre-defined collection of {@link LabVessel}s to a given bucket
     *
     * @param productOrder
     * @param entriesToAdd
     * @param bucket
     * @param operator
     * @param labEventLocation
     */
    public void add(@Nonnull String productOrder, @Nonnull Collection<LabVessel> entriesToAdd, @Nonnull Bucket bucket,
                    @Nonnull String operator, @Nonnull String labEventLocation) {

        List<BucketEntry> listOfNewEntries = new LinkedList<BucketEntry>();

        for (LabVessel currVessel : entriesToAdd) {
            listOfNewEntries.add(bucket.addEntry(productOrder, currVessel));
        }

        Map<String, Collection<LabVessel>> pdoKeyToVesselMap = new HashMap<String, Collection<LabVessel>>();

        pdoKeyToVesselMap.put(productOrder, entriesToAdd);

        Set<LabEvent> eventList = new HashSet<LabEvent>();
        eventList.addAll(labEventFactory.buildFromBatchRequests(listOfNewEntries, operator, null, labEventLocation,
                LabEventType.SHEARING_BUCKET_EXIT));

        for (String pdo : pdoKeyToVesselMap.keySet()) {
            try {
                jiraService.addComment(pdo, "Vessels: " +
                                            StringUtils.join(pdoKeyToVesselMap.get(pdo), ',') +
                                            " added to bucket " + bucket.getBucketDefinitionName());
            } catch (IOException ioe) {
                logger.error("error attempting to add a jira comment for adding " +
                             pdo + ":" + StringUtils.join(pdoKeyToVesselMap.get(pdo), ',') + " to bucket " +
                             bucket.getBucketDefinitionName(), ioe);
            }
        }

    }


    /**
     * A pared down version of
     * {@link #start(java.util.Collection, String, String)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
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
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
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
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
     * @param batchTicket
     */
    public void start(@Nonnull String operator, @Nonnull Collection<LabVessel> vesselsToBatch,
                      @Nonnull Bucket workingBucket, String batchInitiationLocation, String batchTicket) {

        Set<BucketEntry> bucketEntrySet = buildBatchListByVessels(vesselsToBatch, workingBucket);

        LabBatch bucketBatch = startBucketDrain(bucketEntrySet, operator, batchInitiationLocation, false);
//        batchEjb.batchToJira(operator, batchTicket, bucketBatch);
        batchEjb.jiraBatchNotification(bucketBatch);


    }

    /**
     * Returns a Set of bucket entries that correspond to a given collection of vessels in the context of a given
     * bucket
     *
     * @param vesselsToBatch {@link LabVessel}s for which the user needs to find bucket entries
     * @param workingBucket  The bucket from which to find the the bucket entries
     * @return
     */
    public static Set<BucketEntry> buildBatchListByVessels(Collection<LabVessel> vesselsToBatch, Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        for (LabVessel workingVessel : vesselsToBatch) {

            BucketEntry foundEntry = workingBucket.findEntry(workingVessel);
            if (null == foundEntry) {
                throw new InformaticsServiceException(
                        "Attempting to pull a vessel from a bucket when it does not exist in that bucket");
            }
            logger.info("Adding entry " + foundEntry.getBucketEntryId() + " for vessel " + foundEntry.getLabVessel()
                    .getLabCentricName() +
                        " and PDO " + foundEntry.getPoBusinessKey() + " to be popped from bucket.");
            bucketEntrySet.add(foundEntry);

        }
        return bucketEntrySet;
    }

    /**
     * a pared down version of
     * {@link #start(String, int, org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator             Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples
     * @param workingBucket
     */
    public void start(String operator, final int numberOfBatchSamples, @Nonnull Bucket workingBucket) {
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
     * @param numberOfBatchSamples
     * @param workingBucket
     */
    @DaoFree
    public LabBatch startDBFree(@Nonnull String operator, final int numberOfBatchSamples,
                                @Nonnull Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = buildBatchListBySize(numberOfBatchSamples, workingBucket);
//        return startBucketDrain(bucketEntrySet, operator, LabEvent.UI_EVENT_LOCATION, true);
        return startBucketDrain(bucketEntrySet, operator, null, true);
    }

    /**
     * a version of the
     * {@link #start(java.util.Collection, String, String)}
     * method
     * with which a user can simply request a number of samples to pull from a given bucket wiht which to make a batch.
     * The samples removed will be with respect to the order they are found in the bucket
     *
     * @param operator             Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples
     * @param workingBucket
     * @param batchTicket
     */
    public void start(@Nonnull String operator, final int numberOfBatchSamples, @Nonnull Bucket workingBucket,
                      final String batchTicket) {

        LabBatch bucketBatch = null;

        bucketBatch = startDBFree(operator, numberOfBatchSamples, workingBucket);

        batchEjb.batchToJira(operator, batchTicket, bucketBatch);
        batchEjb.jiraBatchNotification(bucketBatch);
    }

    /**
     * Given a bucket and a batch size, this method will return as many entries from the top of the bucket as is
     * defined by the batch size.
     *
     * @param numberOfBatchSamples Batch size/Nuber of entries to retrieve from the bucket
     * @param workingBucket        bucket from which to create a batch of entries
     * @return
     */
    public static Set<BucketEntry> buildBatchListBySize(int numberOfBatchSamples, Bucket workingBucket) {
        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        List<BucketEntry> sortedBucketEntries = new LinkedList<BucketEntry>(workingBucket.getBucketEntries());

        logger.info("List of Bucket entries is a size of " + sortedBucketEntries.size());

        Iterator<BucketEntry> bucketEntryIterator = sortedBucketEntries.iterator();

        for (int i = 0; i < numberOfBatchSamples && bucketEntryIterator.hasNext(); i++) {
            BucketEntry currEntry = bucketEntryIterator.next();
            logger.info("Adding entry " + currEntry.getBucketEntryId() + " for vessel " + currEntry.getLabVessel()
                    .getLabCentricName() +
                        " and PDO " + currEntry.getPoBusinessKey() + " to be popped from bucket.");

            bucketEntrySet.add(currEntry);
        }

        logger.info("Bucket Entry set to pop from bucket is a size of " + bucketEntrySet.size());
        return bucketEntrySet;
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries
     * @param operator                Reference to the user that is requesting the batch.
     * @param batchInitiationLocation
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
     * @param bucketEntries
     * @param operator                Reference to the user that is requesting the batch.
     * @param batchInitiationLocation
     * @param batchTicket
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
        LabBatch bucketBatch = startBucketDrain(bucketEntries, operator, batchInitiationLocation, false);

        batchEjb.batchToJira(operator,
                bucketBatch.getJiraTicket() != null ? bucketBatch.getJiraTicket().getTicketName() :
                        batchTicket,
                bucketBatch);
        batchEjb.jiraBatchNotification(bucketBatch);


    }

    /**
     * Core batch "Start" logic.
     * <p/>
     * Contains all the logic for removing vessels from a bucket and associating events with that removal
     *
     * @param bucketEntries
     * @param operator
     * @param batchInitiationLocation
     * @param autoBatch
     * @return
     */
//    @DaoFree
    private LabBatch startBucketDrain(@Nonnull Collection<BucketEntry> bucketEntries, @Nonnull String operator,
                                      String batchInitiationLocation, boolean autoBatch) {
        LabBatch bucketBatch = null;
        Set<LabVessel> batchVessels = new HashSet<LabVessel>();

        for (BucketEntry currEntry : bucketEntries) {
            batchVessels.add(currEntry.getLabVessel());

        }

        if (!batchVessels.isEmpty()) {
            for (LabBatch currBatch : batchVessels.iterator().next().getNearestLabBatches()) {

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

            //TODO SGM  Should use logic in LabBatchEJB

            bucketBatch = new LabBatch(LabBatch.generateBatchName(CreateFields.IssueType.EXOME_EXPRESS.getJiraName(),
                    LabVessel.extractPdoKeyList(batchVessels)),
                    batchVessels);
        }



        //TODO SGM:  Temporarily removing until after March.  auto drain will not create or associate Batches and Events
/*
        Set<LabEvent> eventList = new HashSet<LabEvent>();
        eventList.addAll(labEventFactory.buildFromBatchRequests(bucketEntries, operator, bucketBatch,
                                                                batchInitiationLocation,
                                                                LabEventType.SHEARING_BUCKET_EXIT));

        bucketBatch.addLabEvents(eventList);
*/


        removeEntries(bucketEntries);

        logger.info("Size of entries to remove is " + bucketEntries.size());
        return bucketBatch;
    }

    /**
     * Helper method to encapsulate the removal of (potentially) multiple entries from a bucket
     *
     * @param bucketEntries collection of bucket entries to be removed from the buckets in which they exist
     */
    //TODO SGM  Move to a bucket factory class
    private void removeEntries(@Nonnull Collection<BucketEntry> bucketEntries) {
        for (BucketEntry currEntry : bucketEntries) {
            logger.info("Adding entry " + currEntry.getBucketEntryId() + " for vessel " + currEntry.getLabVessel()
                    .getLabCentricName() +
                        " and PDO " + currEntry.getPoBusinessKey() + " to be popped from bucket.");

            currEntry.getBucketExistence().removeEntry(currEntry);

            jiraRemovalUpdate(currEntry, "Extracted for Batch");

        }
    }

    /**
     * For whatever reason, sometimes the lab can't
     * do something, or PDMs decide that they shouldn't
     * do something.  Cancelling the entry removes
     * it from the bucket.
     *
     * @param bucketEntry
     * @param operator
     * @param reason      textual notes on why the thing is
     */
    public void cancel(@Nonnull BucketEntry bucketEntry, String operator, String reason) {

        Collection<BucketEntry> singleRemoval = Collections.singletonList(bucketEntry);

        removeEntries(singleRemoval);

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
                    .getBucketExistence()
                    .getBucketDefinitionName() + ":: " + reason);
        } catch (IOException ioe) {
            logger.error("Error attempting to create jira removal comment for " +
                         bucketEntry.getPoBusinessKey() + " " +
                         bucketEntry.getLabVessel().getLabCentricName(), ioe);
        }
    }

    /**
     * Helper method to extract the Pdo Business Keys for a given list of Bucket Entries
     *
     * @param entries
     * @return
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
     * @param pdo
     * @param entries
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
