package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BucketResource {

    private static final Log LOG = LogFactory.getLog (BucketResource.class);

    @Inject
    private LabEventFactory labEventFactory;

    public BucketResource () {
    }

    public BucketResource(LabEventFactory labEventFactoryIn) {
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
        return newEntry;
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     *
     * @param actor
     */
    public void start ( Collection<BucketEntry> bucketEntries, Person actor ) {
        /**
         * Side effect: create a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} for each
         * associated {@link LabVessel} and
         * set {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#setProductOrderId(String)
         * the product order} based on what's in the {@link BucketEntry}
         *
         * Create (if necessary) a new batch consisting of the
         */

        Map<String, List<LabVessel>> pdoKeyToVesselMap = new HashMap<String, List<LabVessel>> ();
        Set<LabVessel> batchVessels = new HashSet<LabVessel> ();

        for ( BucketEntry currEntry : bucketEntries ) {
            if ( !pdoKeyToVesselMap.containsKey ( currEntry.getPoBusinessKey () ) ) {
                pdoKeyToVesselMap.put ( currEntry.getPoBusinessKey (), new LinkedList<LabVessel> () );
            }
            pdoKeyToVesselMap.get ( currEntry.getPoBusinessKey () ).add ( currEntry.getLabVessel () );
            batchVessels.add ( currEntry.getLabVessel () );
        }

        //TODO SGM Only do the following if this pull is NOT currently a batch

        LabBatch bucketBatch = new LabBatch(/*TODO SGM Pull ProductOrder details to get title */"",
                                            batchVessels);

        Set<GenericLabEvent> eventList = new HashSet<GenericLabEvent>();
        eventList.addAll(labEventFactory.buildFromBatchRequestsDBFree ( pdoKeyToVesselMap, actor, bucketBatch ));

        for ( BucketEntry currEntry : bucketEntries ) {
            currEntry.getBucketExistence().removeEntry(currEntry);
        }


//        try {
//            bucketBatch.submit();
//        } catch (IOException ioe ) {
//            LOG.error("Error attempting to create Lab Batch in Jira");
//            throw new InformaticsServiceException("Error attempting to create Lab Batch in Jira", ioe);
//        }
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
    public void cancel ( BucketEntry bucketEntry, Person user, String reason ) throws IOException {

        Bucket currentBucket = bucketEntry.getBucketExistence ();

        currentBucket.removeEntry ( bucketEntry );

        ServiceAccessUtility.addJiraComment ( bucketEntry.getPoBusinessKey (), bucketEntry.getPoBusinessKey () + ":" +
                bucketEntry.getLabVessel ().getLabCentricName () +
                " Removed from bucket:: " + reason );
    }
}
