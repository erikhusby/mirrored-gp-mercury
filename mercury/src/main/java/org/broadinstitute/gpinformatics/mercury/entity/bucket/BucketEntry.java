package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * An entry into a lab {@link Bucket}.  An entry is
 * defined by the {@link LabVessel} that should be worked
 * on and the {@link ProductOrderId product order} for which
 * the work is related.
 */
public class BucketEntry {

    private Long bucketEntryId;

    private LabVessel labVessel;

    private ProductOrderId productOrder;

    private Bucket bucketExistence;

    private Integer productOrderBucketRanking;

    public LabVessel getLabVessel() {
        throw new RuntimeException("not implemented yet");
    }

    public ProductOrderId getProductOrderId() {
        throw new RuntimeException("not implemented yet");
    }

}
