package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.*;

@Impl
@Default
@RequestScoped
public class MercuryClientServiceImpl implements MercuryClientService {

    @Inject
    private MercuryClientEjb mercuryClientEjb;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo) {
        return mercuryClientEjb.addFromProductOrder(pdo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo, @Nonnull Collection<ProductOrderSample> samples) {
        return mercuryClientEjb.addFromProductOrder(pdo, samples);
    }
}

