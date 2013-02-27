package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of MercuryClientProducer, for test purposes.
 * @author epolk
 */
@Stub
@Alternative
public class MercuryClientServiceStub implements MercuryClientService {
    private static final Long TEST_CREATOR = 1111L;

    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo) {
        return addSampleToPicoBucket(pdo, pdo.getSamples());
    }

    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo,
                                                                Collection<ProductOrderSample> samples) {
        Collection<ProductOrderSample> addedSamples = new ArrayList<ProductOrderSample>();
        for (ProductOrderSample sample : samples) {
            addedSamples.add(sample);
            // Pretends to put half the samples in the pico bucket.
            if (addedSamples.size() >= samples.size() / 2) {
                break;
            }
        }
        return addedSamples;
    }
}
