package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of MercuryClientProducer, for test purposes.
 * @author epolk
 */
@Stub
@Alternative
public class MercuryClientServiceStub implements MercuryClientService {
    private static final Long TEST_CREATOR = 1111L;

    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo,
                                                                @Nonnull Collection<ProductOrderSample> samples) {
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

    @Override
    public Collection<DisplayableItem> getReferenceSequences() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getSequenceAligners() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getAnalysisTypes() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getReagentDesigns() {
        return Collections.emptyList();
    }

    @Override
    public DisplayableItem getReagentDesign(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getSequenceAligner(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getReferenceSequence(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getAnalysisType(String businessKey) {
        return null;
    }
}
