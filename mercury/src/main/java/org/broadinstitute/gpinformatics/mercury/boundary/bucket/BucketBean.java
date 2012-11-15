package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
//@RequestScoped
public class BucketBean {

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private BucketEntryDao bucketEntryDao;

    private final static Logger logger = Logger.getLogger ( BucketBean.class.getName () );

    public BucketBean () {
    }

    public BucketBean ( LabEventFactory labEventFactoryIn ) {
        this.labEventFactory = labEventFactoryIn;
    }

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link String product order}
     *
     * TODO SGM Rethink the return.  Doesn't seem to add any value
     *
     * @param vessel
     * @param productOrder
     *
     * @return
     */
    public BucketEntry add ( @Nonnull LabVessel vessel, @Nonnull String productOrder, @Nonnull Bucket bucket ) {

        BucketEntry newEntry = bucket.addEntry ( productOrder, vessel );
        labEventFactory.createFromBatchItems ( productOrder, vessel, 1L, null, LabEventType.BUCKET_ENTRY,
                                               LabEvent.UI_EVENT_LOCATION );
        try {
            ServiceAccessUtility.addJiraComment ( productOrder, vessel.getLabCentricName () +
                    " added to bucket " + bucket.getBucketDefinitionName () );
        } catch ( IOException ioe ) {
            logger.log ( Level.INFO, "error attempting to add a jira comment for adding " +
                    productOrder + ":" + vessel.getLabCentricName () + " to bucket " +
                    bucket.getBucketDefinitionName (), ioe );
        }
        return newEntry;
    }

    public void add ( @Nonnull Collection<BucketEntry> entriesToAdd, @Nonnull Bucket bucket, Person actor ) {

        Map<String, List<LabVessel>> pdoKeyToVesselMap = new HashMap<String, List<LabVessel>> ();
        Set<LabVessel> eventVessels = new HashSet<LabVessel> ();


        for ( BucketEntry currEntry : entriesToAdd ) {
            if ( !pdoKeyToVesselMap.containsKey ( currEntry.getPoBusinessKey () ) ) {
                pdoKeyToVesselMap.put ( currEntry.getPoBusinessKey (), new LinkedList<LabVessel> () );
            }
            pdoKeyToVesselMap.get ( currEntry.getPoBusinessKey () ).add ( currEntry.getLabVessel () );
            eventVessels.add ( currEntry.getLabVessel () );

            bucket.addEntry(currEntry);
        }

        Set<LabEvent> eventList = new HashSet<LabEvent> ();
        eventList.addAll ( labEventFactory.buildFromBatchRequests ( pdoKeyToVesselMap, actor, null,
                                                                    LabEvent.UI_EVENT_LOCATION,
                                                                    LabEventType.BUCKET_EXIT ) );

        for ( String pdo : pdoKeyToVesselMap.keySet () ) {
            try {
                ServiceAccessUtility.addJiraComment ( pdo, "Vessels: " +
                        StringUtils.join (pdoKeyToVesselMap.get(pdo),',') +
                        " added to bucket " + bucket.getBucketDefinitionName () );
            } catch ( IOException ioe ) {
                logger.log ( Level.WARNING, "error attempting to add a jira comment for adding " +
                        pdo + ":" + StringUtils.join (pdoKeyToVesselMap.get(pdo),',') + " to bucket " +
                bucket.getBucketDefinitionName (), ioe );
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
     * {@link #start(java.util.Collection , org.broadinstitute.gpinformatics.mercury.entity.person.Person , String)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param actor                   Reference to the user that is requesting the batch.
     *                                TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
     */
    public void start ( Person actor, @Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket workingBucket,
                        String batchInitiationLocation ) {
        start ( actor, vesselsToBatch, workingBucket, batchInitiationLocation, null );
    }

    /**
     * A version of {@link #start(java.util.Collection , org.broadinstitute.gpinformatics.mercury.entity.person.Person ,
     * String)}
     * for which just the Vessels needed for a batch are referenced.  This is primarily needed to support batching
     * when initiated from messaging
     *
     * @param actor                   Reference to the user that is requesting the batch.
     *                                TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param vesselsToBatch
     * @param workingBucket
     * @param batchInitiationLocation
     * @param batchTicket
     */
    public void start ( Person actor, @Nonnull Collection<LabVessel> vesselsToBatch, @Nonnull Bucket workingBucket,
                        String batchInitiationLocation, String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry> ();

        for ( LabVessel workingVessel : vesselsToBatch ) {
            BucketEntry foundEntry = bucketEntryDao.findByVesselAndBucket ( workingVessel, workingBucket );
            if ( null == foundEntry ) {
                throw new InformaticsServiceException (
                        "Attempting to pull a vessel from a bucket when it does not exist in that bucket" );
            }
            logger.log ( Level.INFO,
                         "Adding entry " + foundEntry.getBucketEntryId () + " for vessel " + foundEntry.getLabVessel ()
                                                                                                       .getLabCentricName () +
                                 " and PDO " + foundEntry.getPoBusinessKey () + " to be popped from bucket." );
            bucketEntrySet.add ( foundEntry );

            // TODO SGM add logic to retrive the containing vessel
            //            LabVessel container = workingVessel.getContainingVessel();
        }

        start ( bucketEntrySet, actor, batchTicket, batchInitiationLocation );
    }

    /**
     * a pared down version of
     * {@link #start(org.broadinstitute.gpinformatics.mercury.entity.person.Person , int ,
     * org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket)}
     * for which an existing Jira Batch does not need to be specified
     *
     * @param actor                Reference to the user that is requesting the batch.
     *                             TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param numberOfBatchSamples
     * @param workingBucket
     */
    public void start ( Person actor, final int numberOfBatchSamples, @Nonnull Bucket workingBucket ) {
        start ( actor, numberOfBatchSamples, workingBucket, null );
    }


    /**
     * a version of the
     * {@link #start(java.util.Collection , org.broadinstitute.gpinformatics.mercury.entity.person.Person , String)}
     * method
     * with which a user can simply request a number of samples to pull from a given bucket wiht which to make a batch.
     * The samples removed will be with respect to the order they are found in the bucket
     *
     * @param actor                Reference to the user that is requesting the batch.
     *                             TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param numberOfBatchSamples
     * @param workingBucket
     * @param batchTicket
     */
    public void start ( Person actor, final int numberOfBatchSamples, @Nonnull Bucket workingBucket,
                        final String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry> ();

        List<BucketEntry> sortedBucketEntries = new LinkedList<BucketEntry> ( workingBucket.getBucketEntries () );

        logger.log ( Level.INFO, "List of Bucket entries is a size of " + sortedBucketEntries.size () );

        Collections.sort ( sortedBucketEntries, BucketEntry.byDate );
        logger.log ( Level.INFO, "List of SORTED Bucket entries is a size of " + sortedBucketEntries.size () );

        Iterator<BucketEntry> bucketEntryIterator = sortedBucketEntries.iterator ();

        for ( int i = 0 ; i < numberOfBatchSamples && bucketEntryIterator.hasNext () ; i++ ) {
            BucketEntry currEntry = bucketEntryIterator.next ();
            logger.log ( Level.INFO,
                         "Adding entry " + currEntry.getBucketEntryId () + " for vessel " + currEntry.getLabVessel ()
                                                                                                     .getLabCentricName () +
                                 " and PDO " + currEntry.getPoBusinessKey () + " to be popped from bucket." );

            bucketEntrySet.add ( currEntry );
        }

        logger.log ( Level.INFO, "Bucket Entry set to pop from bucket is a size of " + bucketEntrySet.size () );
        start ( bucketEntrySet, actor, batchTicket, LabEvent.UI_EVENT_LOCATION );

    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries
     * @param actor                   Reference to the user that is requesting the batch.
     *                                TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param batchInitiationLocation
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, Person actor, String batchInitiationLocation ) {
        start ( bucketEntries, actor, null, batchInitiationLocation );
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param bucketEntries
     * @param actor                   Reference to the user that is requesting the batch.
     *                                TODO SGM replace this with a LONG representing the users BSPUser reference
     * @param batchTicket
     * @param batchInitiationLocation
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, Person actor, String batchTicket,
                        String batchInitiationLocation ) {
        /**
         * Side effect: create a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} for each
         * associated {@link LabVessel} and
         * set {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#setProductOrderId(String)
         * the product order} based on what's in the {@link BucketEntry}
         *
         * Create (if necessary) a new batch
         */

        Map<String, List<LabVessel>> pdoKeyToVesselMap = new HashMap<String, List<LabVessel>> ();
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

                trackBatches = new LinkedList<LabBatch> ();

                List<LabBatch> currBatchList = new LinkedList<LabBatch> ( currEntry.getLabVessel ().getLabBatches () );

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
        eventList.addAll ( labEventFactory.buildFromBatchRequests ( pdoKeyToVesselMap, actor, bucketBatch,
                                                                    batchInitiationLocation,
                                                                    LabEventType.BUCKET_EXIT ) );

        bucketBatch.addLabEvents ( eventList );

        try {
            if ( null == batchTicket ) {
                bucketBatch.createJiraTicket ( actor.getLogin () );
            } else {
                bucketBatch.setJiraTicket ( new JiraTicket ( batchTicket, batchTicket ) );
            }
            for ( String pdo : pdoKeyToVesselMap.keySet () ) {
                bucketBatch.addJiraLink ( pdo );
                ServiceAccessUtility.addJiraComment ( pdo, "New Batch Created: " +
                        bucketBatch.getJiraTicket ().getTicketName () + " " + bucketBatch.getBatchName () );
            }
        } catch ( IOException ioe ) {
            logger.log ( Level.INFO, "Error attempting to create Lab Batch in Jira" );
            throw new InformaticsServiceException ( "Error attempting to create Lab Batch in Jira", ioe );
        }

        logger.log ( Level.INFO, "Size of entries to remove is " + bucketEntries.size () );
        removeEntries ( bucketEntries );
    }

    /**
     * Helper method to encapsulate the removal of (potentially) multiple entries from a bucket
     *
     * @param bucketEntries collection of bucket entries to be removed from the buckets in which they exist
     */
    private void removeEntries ( @Nonnull Collection<BucketEntry> bucketEntries ) {
        for ( BucketEntry currEntry : bucketEntries ) {
            logger.log ( Level.INFO,
                         "Adding entry " + currEntry.getBucketEntryId () + " for vessel " + currEntry.getLabVessel ()
                                                                                                     .getLabCentricName () +
                                 " and PDO " + currEntry.getPoBusinessKey () + " to be popped from bucket." );

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
     * @param user
     * @param reason      textual notes on why the thing is
     *                    being cancelled.
     */
    public void cancel ( @Nonnull BucketEntry bucketEntry, Person user, String reason ) {

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

            ServiceAccessUtility.addJiraComment ( bucketEntry.getPoBusinessKey (),
                                                  bucketEntry.getPoBusinessKey () + ":" +
                                                          bucketEntry.getLabVessel ().getLabCentricName () +
                                                          " Removed from bucket " + bucketEntry.getBucketExistence ()
                                                                                               .getBucketDefinitionName () + ":: " + reason );
        } catch ( IOException ioe ) {
            logger.log ( Level.INFO, "Error attempting to create jira removal comment for " +
                    bucketEntry.getPoBusinessKey () + " " +
                    bucketEntry.getLabVessel ().getLabCentricName (), ioe );
        }
    }
}
