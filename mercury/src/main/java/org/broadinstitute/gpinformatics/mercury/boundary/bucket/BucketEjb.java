package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class BucketEjb {

    private LabEventFactory labEventFactory;

    private JiraService jiraService;

    private static final Log logger = LogFactory.getLog(BucketEjb.class);

    private BucketDao bucketDao;

    private AthenaClientService athenaClientService;

    public BucketEjb() {
    }

    @Inject
    public BucketEjb(LabEventFactory labEventFactory, JiraService jiraService, BucketDao bucketDao,
                     AthenaClientService athenaClientService) {
        this.labEventFactory = labEventFactory;
        this.jiraService = jiraService;
        this.bucketDao = bucketDao;
        this.athenaClientService = athenaClientService;
    }

    /**
     * Adds a pre-defined collection of {@link LabVessel}s to the given bucket using the specified pdoBusinessKey.
     *
     * @param entriesToAdd         Collection of LabVessels to be added to a bucket
     * @param bucket               the bucket that will receive the vessels (as bucket entries)
     * @param entryType            the type of bucket entry to add
     * @param operator             Represents the user that initiated adding the vessels to the bucket
     * @param labEventLocation     Machine location from which operator initiated this action
     * @param programName          Name of the program that initiated this action
     * @param eventType            Type of the Lab Event that initiated this bucket add request
     * @param pdoKey               Product order key for all vessels
     */
    public Collection<BucketEntry> add(@Nonnull Collection<LabVessel> entriesToAdd, @Nonnull Bucket bucket,
                                       BucketEntry.BucketEntryType entryType, @Nonnull String operator,
                                       @Nonnull String labEventLocation,
                                       @Nonnull String programName, LabEventType eventType,
                                       @Nonnull String pdoKey) {

        List<BucketEntry> listOfNewEntries = new ArrayList<>(entriesToAdd.size());
        for (LabVessel currVessel : entriesToAdd) {
            listOfNewEntries.add(bucket.addEntry(pdoKey, currVessel, entryType));
        }

        Set<LabEvent> eventList = new HashSet<>();

        //TODO SGM: Pass in Latest Batch?
        eventList.addAll(labEventFactory.buildFromBatchRequests(listOfNewEntries, operator, null, labEventLocation,
                programName, eventType));

        return listOfNewEntries;
    }

    /**
     * Creates batch entries for the vessels, then moves the entries from the given bucket to the given batch.
     * This is primarily used by messaging.
     *
     * @param vesselsToBatch  the vessels to be processed
     * @param bucket   the bucket containing the vessels
     */
    public void createEntriesAndBatchThem(@Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket bucket) {
        moveFromBucketToCommonBatch(selectEntries(vesselsToBatch, bucket));
    }

    /**
     * Puts the bucket entries into the given batch and removes them from the bucket.
     *
     * @param bucketEntries the bucket entries to be batched.
     * @param labBatch      the lab batch receiving the entries.
     */
    public void moveFromBucketToBatch(@Nonnull Collection<BucketEntry> bucketEntries, LabBatch labBatch) {
        removeEntries(bucketEntries);

        for (BucketEntry bucketEntry : bucketEntries) {
            labBatch.addBucketEntry(bucketEntry);
        }
    }

    /** Returns the bucket entries that correspond to the specified vessels in the bucket. */
    public Set<BucketEntry> selectEntries(Collection<LabVessel> vessels, Bucket bucket) {
        Set<BucketEntry> entries = new HashSet<>();

        for (LabVessel vessel : vessels) {

            BucketEntry entry = bucket.findEntry(vessel);
            if (entry != null) {
                logger.debug("Adding entry " + entry.getBucketEntryId() + " for vessel " +
                             entry.getLabVessel().getLabCentricName() + " and PDO " + entry.getPoBusinessKey() +
                             " to be popped from bucket.");
                entries.add(entry);
            } else {
                logger.debug("Attempting to pull a vessel, " + vessel.getLabel() + ", from a bucket, " +
                        bucket.getBucketDefinitionName() + ", when it does not exist in that bucket");
            }
        }
        return entries;
    }

    /**
     * Used for test purposes.  Takes the given number of samples from the bucket having the given product
     * workflow and moves them into a new batch.  The batch may end up having fewer samples if the bucket
     * runs out of suitable entries.
     *
     * @param numberOfSamples the number of samples to be taken.
     * @param bucket the bucket containing the samples.
     * @param workflow the product workflow desired
     * @return the batch that contains the samples.
     */
    @DaoFree
    public LabBatch selectEntriesAndBatchThem(int numberOfSamples, @Nonnull Bucket bucket, Workflow workflow) {
        Set<BucketEntry> bucketEntrySet = new HashSet<>();
        int count = 0;

        // Selects entries whose product workflow matched the specified workflow.
        Set<String> pdoKeys = new HashSet<>();
        for (BucketEntry entry : bucket.getBucketEntries()) {
            pdoKeys.add(entry.getPoBusinessKey());
        }
        Collection<ProductOrder> pdos = athenaClientService.retrieveMultipleProductOrderDetails(pdoKeys);
        for (ProductOrder pdo : pdos) {
            if (workflow != null && pdo.getProduct() == null ||
                workflow == null && pdo.getProduct() != null ||
                workflow != pdo.getProduct().getWorkflow()) {
                    pdoKeys.remove(pdo.getBusinessKey());
            }
        }

        for (BucketEntry entry : bucket.getBucketEntries()) {
            if (pdoKeys.contains(entry.getPoBusinessKey())) {
                bucketEntrySet.add(entry);
                ++count;
            }
            if (count >= numberOfSamples) {
                break;
            }
        }
        return moveFromBucketToCommonBatch(bucketEntrySet);
    }


    /**
     * Selects the appropriate batch for the given bucket entries and moves the entries from the bucket to the batch.
     *
     * @param bucketEntries the bucket entries to be moved.
     * @return the lab batch that the entries were put into.
     */
    @DaoFree
    public LabBatch moveFromBucketToCommonBatch(@Nonnull Collection<BucketEntry> bucketEntries) {

        // Gets the vessels.
        Set<LabVessel> batchVessels = new HashSet<>();
        for (BucketEntry entry : bucketEntries) {
            batchVessels.add(entry.getLabVessel());
        }

        // Determines the appropriate batch to use, and updates the bucket entries' batch.
        LabBatch bucketBatch = null;
        if (!batchVessels.isEmpty()) {
            for (LabBatch batch : batchVessels.iterator().next().getNearestWorkflowLabBatches()) {
                if (LabBatch.isCommonBatch(batch, batchVessels)) {
                    bucketBatch = batch;
                }
            }
        }
        for (BucketEntry currEntry : bucketEntries) {
            currEntry.setLabBatch(bucketBatch);
        }

        // Removes bucket entries from the bucket.
        removeEntries(bucketEntries);

        logger.info("Size of entries to remove is " + bucketEntries.size());
        return bucketBatch;
    }

    /** Removes entries from a bucket and marks them as having been extracted. */
    //TODO SGM  Move to a bucket factory class
    private void removeEntries(@Nonnull Collection<BucketEntry> bucketEntries) {
        removeEntries(bucketEntries, "Extracted for Batch");
    }

    /** Removes entry from a bucket and gives the reason why. */
    public void removeEntry(@Nonnull BucketEntry bucketEntry, String reason) {
        removeEntries(Collections.singletonList(bucketEntry), reason);
    }

    /** Removes entries from a bucket and gives the reason why. */
    private void removeEntries(@Nonnull Collection<BucketEntry> bucketEntries, String reason) {
        for (BucketEntry currEntry : bucketEntries) {
            logger.debug("Removing entry " + currEntry.getBucketEntryId() +
                         " for vessel " + currEntry.getLabVessel().getLabCentricName() +
                         " and PDO " + currEntry.getPoBusinessKey() +
                         " from bucket " + currEntry.getBucket().getBucketDefinitionName());
            currEntry.getBucket().removeEntry(currEntry);
        }
        jiraRemovalUpdate(bucketEntries, reason);
    }

    /** Updates the product order Jira ticket for bucket entry removals. */
    private void jiraRemovalUpdate(@Nonnull Collection<BucketEntry> entries, String reason) {

        boolean isSingleEntry = (entries.size() == 1);

        Map<String, Collection<BucketEntry>> pdoToEntries = new HashMap<>();
        for (BucketEntry entry : entries) {
            Collection<BucketEntry> pdoEntries = pdoToEntries.get(entry.getPoBusinessKey());
            if (pdoEntries == null) {
                pdoEntries = new ArrayList<>();
                pdoToEntries.put(entry.getPoBusinessKey(), pdoEntries);
            }
            pdoEntries.add(entry);
        }
        for (Map.Entry<String, Collection<BucketEntry>> mapEntry : pdoToEntries.entrySet()) {
            BucketEntry firstBucketEntry = mapEntry.getValue().iterator().next();
            String comment;
            if (isSingleEntry) {
                comment = mapEntry.getKey() + ":" +
                          firstBucketEntry.getLabVessel().getLabCentricName() +
                          " removed from bucket " +
                          firstBucketEntry.getBucket().getBucketDefinitionName() + ":: " +
                          reason;
            } else {
                comment = mapEntry.getKey() + ":" +
                          mapEntry.getValue().size() +
                          " samples removed from bucket " +
                          firstBucketEntry.getBucket().getBucketDefinitionName() + ":: " +
                          reason;
            }
            try {
                jiraService.addComment(mapEntry.getKey(), comment);
            } catch (IOException ioe) {
                logger.error("Error attempting to create jira removal comment for " +
                             mapEntry.getKey() + " " +
                             mapEntry.getValue().size() + " samples.", ioe);
            }
        }
    }


    public Bucket findOrCreateBucket(String bucketName) {
        Bucket bucket = bucketDao.findByName(bucketName);
        if (bucket == null) {
            bucket = new Bucket(bucketName);
            bucketDao.persist(bucket);
            logger.debug("Created new bucket " + bucketName);
        }
        return bucket;
    }

}
