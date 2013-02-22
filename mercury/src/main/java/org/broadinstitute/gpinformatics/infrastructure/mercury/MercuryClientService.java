package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import java.io.Serializable;

/**
 * Interface of Mercury Services available to Athena.
 *
 * @author epolk
 */
public interface MercuryClientService extends Serializable {

    /**
     * Adds sample to the PicoBucket provided the sample has already been received by BSP and
     * passes validation for entry into the bucket.
     *
     * @param pdoSample to be added
     * @return true if added to bucket, false if not added.
     */
    public boolean addSampleToPicoBucket(ProductOrderSample pdoSample);

}
