package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.StandardPOResolver;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
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

@Stateless
//@RequestScoped
public class BucketBean {

    private static final Log LOG = LogFactory.getLog (BucketBean.class);

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private AthenaClientService athenaClientService;

    public BucketBean () {
    }

    public BucketBean ( LabEventFactory labEventFactoryIn ) {
        this.labEventFactory = labEventFactoryIn;
    }

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link String product order}
     *
     * @param vessel
     * @param productOrder
     *
     * @return
     */
    public BucketEntry add ( @Nonnull LabVessel vessel, @Nonnull String productOrder, @Nonnull Bucket bucket ) {

        BucketEntry newEntry = bucket.addEntry ( productOrder, vessel );
        labEventFactory.createFromBatchItems( productOrder, vessel, 1L, null, LabEventType.BUCKET_ENTRY );
        try {
            ServiceAccessUtility.addJiraComment(productOrder, vessel.getLabCentricName() +
                    " added to bucket " + bucket.getBucketDefinitionName());
        } catch (IOException ioe) {
            LOG.error("error attempting to add a jira comment for adding " +
                      productOrder + ":"+vessel.getLabCentricName() + " to bucket " +
                      bucket.getBucketDefinitionName(), ioe);
        }
        return newEntry;
    }

    public void start ( Person actor, @Nonnull Collection<LabVessel> vesselsToBatch,
                        @Nonnull Bucket workingBucket) {
        start(actor,vesselsToBatch, workingBucket, null);
    }

    public void start ( Person actor, @Nonnull Collection<LabVessel> vesselsToBatch,
                        @Nonnull Bucket workingBucket, String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        for (LabVessel workingVessel:vesselsToBatch) {
            BucketEntry foundEntry = bucketEntryDao.findByVesselAndBucket ( workingVessel, workingBucket );
            if(null == foundEntry) {
                throw new InformaticsServiceException("Attempting to pull a vessel from a bucket when it does not exist in that bucket");
            }
            bucketEntrySet.add ( foundEntry ) ;

            // TODO SGM add logic when getContainingVessel is implemented
//            LabVessel container = workingVessel.getContainingVessel();
        }

        start(bucketEntrySet, actor,batchTicket );
    }

    public void start ( Person actor, int numberOfBatchSamples, Bucket workingBucket) {
         start(actor, numberOfBatchSamples, workingBucket, null);
    }


    public void start ( Person actor, int numberOfBatchSamples, Bucket workingBucket, String batchTicket ) {

        Set<BucketEntry> bucketEntrySet = new HashSet<BucketEntry>();

        List<BucketEntry> sortedBucketEntries = new LinkedList<BucketEntry>(workingBucket.getBucketEntries());

        Collections.sort (sortedBucketEntries, BucketEntry.byDate);

        Iterator<BucketEntry> bucketEntryIterator = sortedBucketEntries.iterator();

        for(int i=0;i<=numberOfBatchSamples;i++) {
            bucketEntrySet.add(bucketEntryIterator.next());
        }
        start(bucketEntrySet, actor,batchTicket);

    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param actor   TODO SGM or JMT, replace with User ID string or BSPUserList ID
     * @param bucketEntries
     *
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, Person actor ) {
        start(bucketEntries, actor, null);
    }
    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param actor
     * @param bucketEntries
     * @param batchTicket
     */
    public void start ( @Nonnull Collection<BucketEntry> bucketEntries, Person actor , String batchTicket ) {
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

            if(!currEntry.getLabVessel().getLabBatches().isEmpty()) {

                trackBatches  = new LinkedList<LabBatch>();

                List<LabBatch> currBatchList = new LinkedList<LabBatch>(currEntry.getLabVessel().getLabBatches());

                Collections.sort(currBatchList, LabBatch.byDate);

                trackBatches.add(currBatchList.get(currBatchList.size()-1));
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

        if( allHaveBatch && trackBatches != null && trackBatches.size()==1) {
            bucketBatch = trackBatches.get(0);
        } else {
            bucketBatch = new LabBatch(/*TODO SGM Pull ProductOrder details to get title */ " ",
                                       batchVessels);
        }

        Set<GenericLabEvent> eventList = new HashSet<GenericLabEvent>();
        eventList.addAll(labEventFactory.buildFromBatchRequestsDBFree ( pdoKeyToVesselMap, actor, bucketBatch ));

        bucketBatch.addLabEvents(eventList);

        try {
            if(null == batchTicket) {
                bucketBatch.createJiraTicket ( actor.getLogin() );
            } else {
                bucketBatch.setJiraTicket(new JiraTicket(batchTicket, batchTicket));
            }
            for(String pdo:pdoKeyToVesselMap.keySet()) {
                bucketBatch.addJiraLink(pdo);
                ServiceAccessUtility.addJiraComment(pdo, "New Batch Created: " +
                        bucketBatch.getJiraTicket().getTicketName() + " " +bucketBatch.getBatchName());
            }
        } catch (IOException ioe ) {
            LOG.error("Error attempting to create Lab Batch in Jira");
            throw new InformaticsServiceException("Error attempting to create Lab Batch in Jira", ioe);
        }

        removeEntries ( bucketEntries );
    }

    private void removeEntries ( Collection<BucketEntry> bucketEntries ) {
        for ( BucketEntry currEntry : bucketEntries ) {
            currEntry.getBucketExistence().removeEntry(currEntry);
            bucketEntryDao.remove(currEntry);

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

        List<BucketEntry> singleRemoval = new LinkedList<BucketEntry>();
        singleRemoval.add(bucketEntry);

        removeEntries ( singleRemoval );

        jiraRemovalUpdate ( bucketEntry, reason );
    }

    private void jiraRemovalUpdate ( BucketEntry bucketEntry, String reason ) {
        try {

        ServiceAccessUtility.addJiraComment ( bucketEntry.getPoBusinessKey (), bucketEntry.getPoBusinessKey () + ":" +
                bucketEntry.getLabVessel ().getLabCentricName () +
                " Removed from bucket " + bucketEntry.getBucketExistence ()
                                                     .getBucketDefinitionName () + ":: " + reason );
        } catch (IOException ioe) {
            LOG.error("Error attempting to create jira removal comment for " +
                              bucketEntry.getPoBusinessKey() + " " +
                              bucketEntry.getLabVessel().getLabCentricName(), ioe);
        }
    }
}
