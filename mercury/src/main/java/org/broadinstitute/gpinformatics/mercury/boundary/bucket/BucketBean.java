package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
//@RequestScoped
public class BucketBean {

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    JiraService jiraService;

    private final static Log logger = LogFactory.getLog(BucketBean.class);

    public BucketBean () {
    }

    public BucketBean(LabEventFactory labEventFactoryIn, JiraService testjiraService) {
        this.labEventFactory = labEventFactoryIn;
        jiraService = testjiraService;
    }

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link String product order}
     *
     * TODO SGM Rethink the return.  Doesn't seem to add any value
     *
     *
     * @param vessel
     * @param productOrder
     *
     * @param operator
     * @return
     */
    public BucketEntry add ( @Nonnull LabVessel vessel, @Nonnull String productOrder, @Nonnull Bucket bucket,
                             String operator ) {

        BucketEntry newEntry = bucket.addEntry ( productOrder, vessel );
        labEventFactory.createFromBatchItems ( productOrder, vessel, 1L, operator, LabEventType.SHEARING_BUCKET_ENTRY,
                                               LabEvent.UI_EVENT_LOCATION );
        try {
            jiraService.addComment ( productOrder, vessel.getLabCentricName () +
                    " added to bucket " + bucket.getBucketDefinitionName () );
        } catch ( IOException ioe ) {
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
    public void add ( String productOrder, @Nonnull List<LabVessel> entriesToAdd, @Nonnull Bucket bucket, String operator,
                      @Nonnull String labEventLocation ) {

        for(LabVessel currVessel:entriesToAdd) {
            bucket.addEntry(productOrder, currVessel);
        }

        Map<String, Collection<LabVessel>> pdoKeyToVesselMap =
                new HashMap<String, Collection<LabVessel>>();

        pdoKeyToVesselMap.put(productOrder, entriesToAdd);

        Set<LabEvent> eventList = new HashSet<LabEvent> ();
        eventList.addAll ( labEventFactory.buildFromBatchRequests ( pdoKeyToVesselMap, operator, null,
                                                                    labEventLocation,
                                                                    LabEventType.SHEARING_BUCKET_EXIT ) );

        for ( String pdo : pdoKeyToVesselMap.keySet () ) {
            try {
                jiraService.addComment ( pdo, "Vessels: " +
                        StringUtils.join ( pdoKeyToVesselMap.get ( pdo ), ',' ) +
                        " added to bucket " + bucket.getBucketDefinitionName () );
            } catch ( IOException ioe ) {
                logger.error("error attempting to add a jira comment for adding " +
                                     pdo + ":" + StringUtils.join(pdoKeyToVesselMap.get(pdo), ',') + " to bucket " +
                                     bucket.getBucketDefinitionName(), ioe);
            }
        }

    }

    private Collection<String> getVesselNameList(Collection<LabVessel> vessels) {

        List<String> vesselNames = new LinkedList<String>();

        for(LabVessel currVessel:vessels) {
            vesselNames.add(currVessel.getLabCentricName());
        }

        return vesselNames;
    }

    /**
     * A pared down version of
     * {@link #start(java.util.Collection, String, String)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator                   Reference to the user that is requesting the batch.
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
     */
    public void start ( String operator, @Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket workingBucket,
                        String batchInitiationLocation ) {
        start ( operator, vesselsToBatch, workingBucket, batchInitiationLocation, null );
    }

    /**
     * A version of {@link #start(java.util.Collection, String, String)}
     * for which just the Vessels needed for a batch are referenced.  This is primarily needed to support batching
     * when initiated from messaging
     *
     * @param operator                   Reference to the user that is requesting the batch.
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
     * @param batchTicket
     */
    public void start ( String operator, @Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket workingBucket,
                        String batchInitiationLocation, String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry> ();

        for ( LabVessel workingVessel : vesselsToBatch ) {

            BucketEntry foundEntry = workingBucket.findEntry( workingVessel);
            if ( null == foundEntry ) {
                throw new InformaticsServiceException (
                        "Attempting to pull a vessel from a bucket when it does not exist in that bucket" );
            }
            logger.info("Adding entry " + foundEntry.getBucketEntryId() + " for vessel " + foundEntry.getLabVessel()
                                                                                                     .getLabCentricName() +
                                " and PDO " + foundEntry.getPoBusinessKey() + " to be popped from bucket.");
            bucketEntrySet.add ( foundEntry );

            // TODO SGM add logic to retrive the containing vessel
            //            LabVessel container = workingVessel.getContainingVessel();
        }

        start ( bucketEntrySet, operator, batchTicket, batchInitiationLocation );
    }

    /**
     * a pared down version of
     * {@link #start(String , int , org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples
     * @param workingBucket
     */
    public void start ( String operator, final int numberOfBatchSamples, @Nonnull Bucket workingBucket ) {
        start ( operator, numberOfBatchSamples, workingBucket, null );
    }


    /**
     * a version of the
     * {@link #start(java.util.Collection, String, String)}
     * method
     * with which a user can simply request a number of samples to pull from a given bucket wiht which to make a batch.
     * The samples removed will be with respect to the order they are found in the bucket
     *
     * @param operator                Reference to the user that is requesting the batch.
     * @param numberOfBatchSamples
     * @param workingBucket
     * @param batchTicket
     */
    public void start ( String operator, final int numberOfBatchSamples, @Nonnull Bucket workingBucket,
                        final String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry> ();

        List<BucketEntry> sortedBucketEntries = new LinkedList<BucketEntry> ( workingBucket.getBucketEntries () );

        logger.info("List of Bucket entries is a size of " + sortedBucketEntries.size());

        Iterator<BucketEntry> bucketEntryIterator = sortedBucketEntries.iterator ();

        for ( int i = 0 ; i < numberOfBatchSamples && bucketEntryIterator.hasNext () ; i++ ) {
            BucketEntry currEntry = bucketEntryIterator.next ();
            logger.info("Adding entry " + currEntry.getBucketEntryId() + " for vessel " + currEntry.getLabVessel()
                                                                                                   .getLabCentricName() +
                                " and PDO " + currEntry.getPoBusinessKey() + " to be popped from bucket.");

            bucketEntrySet.add ( currEntry );
        }

        logger.info("Bucket Entry set to pop from bucket is a size of " + bucketEntrySet.size());
        start ( bucketEntrySet, operator, batchTicket, LabEvent.UI_EVENT_LOCATION );

    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries
     * @param operator                   Reference to the user that is requesting the batch.
     * @param batchInitiationLocation
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, String operator, String batchInitiationLocation ) {
        start ( bucketEntries, operator, null, batchInitiationLocation );
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries
     * @param operator                   Reference to the user that is requesting the batch.
     * @param batchTicket
     * @param batchInitiationLocation
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, String operator, String batchTicket,
                        String batchInitiationLocation ) {
        /**
         * Side effect: create a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} for each
         * associated {@link LabVessel} and
         * set {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#setProductOrderId(String)
         * the product order} based on what's in the {@link BucketEntry}
         *
         * Create (if necessary) a new batch
         */

        Map<String, Collection<LabVessel>> pdoKeyToVesselMap =
                new HashMap<String, Collection<LabVessel>>();
        Set<LabVessel> batchVessels = new HashSet<LabVessel> ();

        List<LabBatch> trackBatches = null;

        boolean allHaveBatch = true;

        for ( BucketEntry currEntry : bucketEntries ) {
            if ( !pdoKeyToVesselMap.containsKey ( currEntry.getPoBusinessKey () ) ) {
                pdoKeyToVesselMap.put ( currEntry.getPoBusinessKey (), new LinkedList<LabVessel> () );
            }
            pdoKeyToVesselMap.get ( currEntry.getPoBusinessKey () ).add ( currEntry.getLabVessel () );
            batchVessels.add ( currEntry.getLabVessel () );

            if ( !currEntry.getLabVessel ().getLabBatches ().isEmpty () ) {

                if(trackBatches == null)
                    trackBatches = new LinkedList<LabBatch> ();

                List<LabBatch> currBatchList =
                        new LinkedList<LabBatch>(currEntry.getLabVessel ().getNearestLabBatches());

                Collections.sort ( currBatchList, LabBatch.byDate );

                trackBatches.add ( currBatchList.get ( currBatchList.size () - 1 ) );
            } else {
                allHaveBatch = false;
            }
        }

        /*
            If the tubes being pulled from the Bucket are all from one LabBatch,  just update that LabBatch and move
            forward.

            otherwise (no previous batch, multiple lab batches, existing batch with samples that are not in an
            existing batch) create a new Lab Batch.
         */
        LabBatch bucketBatch = null;

        if ( allHaveBatch && trackBatches != null && trackBatches.size () == 1 ) {
            bucketBatch = trackBatches.get ( 0 );
        } else {
            bucketBatch = new LabBatch (/*TODO SGM Pull ProductOrder details to get title */ " ", batchVessels );
        }

        Set<LabEvent> eventList = new HashSet<LabEvent> ();
        eventList.addAll ( labEventFactory.buildFromBatchRequests ( pdoKeyToVesselMap, operator, bucketBatch,
                                                                    batchInitiationLocation,
                                                                    LabEventType.SHEARING_BUCKET_EXIT ) );

        bucketBatch.addLabEvents ( eventList );

        try {
            if ( null == batchTicket ) {

                //TODO SGM make a Create Jira Ticket method in BatchBean
                bucketBatch.createJiraTicket ( operator, CreateFields.IssueType.EXOME_EXPRESS,
                                               CreateFields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix());
            } else {
                bucketBatch.setJiraTicket ( new JiraTicket ( batchTicket ) );
            }
            for ( String pdo : pdoKeyToVesselMap.keySet () ) {
                bucketBatch.addJiraLink ( pdo );
                jiraService.addComment ( pdo, "New Batch Created: " +
                        bucketBatch.getJiraTicket ().getTicketName () + " " + bucketBatch.getBatchName () );
            }
        } catch ( IOException ioe ) {
            logger.error("Error attempting to create Lab Batch in Jira");
            throw new InformaticsServiceException ( "Error attempting to create Lab Batch in Jira", ioe );
        }

        logger.info("Size of entries to remove is " + bucketEntries.size());
        removeEntries ( bucketEntries );
    }

    /**
     * Helper method to encapsulate the removal of (potentially) multiple entries from a bucket
     *
     * @param bucketEntries collection of bucket entries to be removed from the buckets in which they exist
     */
    private void removeEntries ( @Nonnull Collection<BucketEntry> bucketEntries ) {
        for ( BucketEntry currEntry : bucketEntries ) {
            logger.info("Adding entry " + currEntry.getBucketEntryId() + " for vessel " + currEntry.getLabVessel()
                                                                                                   .getLabCentricName() +
                                " and PDO " + currEntry.getPoBusinessKey() + " to be popped from bucket.");

            currEntry.getBucketExistence ().removeEntry ( currEntry );
            bucketEntryDao.remove ( currEntry );

            jiraRemovalUpdate ( currEntry, "Extracted for Batch" );

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
    public void cancel ( @Nonnull BucketEntry bucketEntry, String operator, String reason ) {

        List<BucketEntry> singleRemoval = new LinkedList<BucketEntry> ();
        singleRemoval.add ( bucketEntry );

        removeEntries ( singleRemoval );

        jiraRemovalUpdate ( bucketEntry, reason );
    }

    /**
     * helper method to make a comment on the Jira ticket for the Product Order associated with the entry being
     * removed
     *
     * @param bucketEntry entry in the bucket being removed
     * @param reason      captures the information about why this entry is being removed from the bucket
     */
    private void jiraRemovalUpdate ( @Nonnull BucketEntry bucketEntry, String reason ) {
        try {

            jiraService.addComment ( bucketEntry.getPoBusinessKey (), bucketEntry.getPoBusinessKey () + ":" +
                    bucketEntry.getLabVessel ().getLabCentricName () +
                    " Removed from bucket " + bucketEntry.getBucketExistence ()
                                                         .getBucketDefinitionName() + ":: " + reason );
        } catch ( IOException ioe ) {
            logger.error("Error attempting to create jira removal comment for " +
                                 bucketEntry.getPoBusinessKey() + " " +
                                 bucketEntry.getLabVessel().getLabCentricName(), ioe);
        }
    }
}
