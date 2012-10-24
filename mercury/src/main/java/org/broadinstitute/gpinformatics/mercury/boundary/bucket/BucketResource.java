package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;

public class BucketResource {


    private final String bucketReferenceName;


    public BucketResource ( String bucketReferenceName ) {
        this.bucketReferenceName = bucketReferenceName;
    }

    public String getBucketReferenceName () {
        return bucketReferenceName;
    }

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link ProductOrderId product order}
     *
     * @param vessel
     * @param productOrder
     * @return
     */
    public BucketEntry add(@Nonnull LabVessel vessel, @Nonnull String productOrder, @Nonnull Bucket bucket) {

        BucketEntry newEntry = bucket.addEntry(productOrder, vessel);
        return newEntry;
    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     * @param bucketEntry
     */
    public void start(BucketEntry bucketEntry) {
        /**
         * Side effect: create a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} and
         * set {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#setProductOrderId(String)
         * the product order} based on what's in the {@link BucketEntry}
         */
        throw new RuntimeException("not implemeted yet");
    }

    /**
     * For whatever reason, sometimes the lab can't
     * do something, or PDMs decide that they shouldn't
     * do something.  Cancelling the entry removes
     * it from the bucket.
     * @param bucketEntry
     * @param user
     * @param reason textual notes on why the thing is
     *               being cancelled.
     */
    public void cancel(BucketEntry bucketEntry,Person user,String reason) {

        Bucket currentBucket = bucketEntry.getBucketExistence();

        currentBucket.removeEntry(bucketEntry);

    }
}
