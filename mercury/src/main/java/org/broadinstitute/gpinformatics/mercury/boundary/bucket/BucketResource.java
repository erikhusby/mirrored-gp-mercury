package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class BucketResource {

    /**
     * Put a {@link LabVessel vessel} into the bucket
     * and remember that the work is for the given {@link ProductOrderId product order}
     * @param vessel
     * @param productOrder
     * @return
     */
    public BucketEntry add(LabVessel vessel,ProductOrderId productOrder) {

    }

    /**
     * Start the work for the given {@link BucketEntry entry}.  Lab users
     * can make this gesture explicitly.  The {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler handler}
     * can also make this gesture when it processes a message.
     * @param bucketEntry
     */
    public void start(BucketEntry bucketEntry) {

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

    }
}
