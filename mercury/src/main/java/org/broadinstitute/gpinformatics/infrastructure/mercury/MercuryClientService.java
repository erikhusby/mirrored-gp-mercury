package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import java.io.Serializable;
import java.util.Collection;

/**
 * Interface of Mercury Services available to Athena.
 *
 * @author epolk
 */
public interface MercuryClientService extends Serializable {

    /**
     * Attempts to add all product order's samples to the pico bucket.
     * @param pdo with samples to be added
     * @return ProductOrderSamples that were successfully added to pico bucket.
     */
    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo);

    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo, Collection<ProductOrderSample> samples);
}
