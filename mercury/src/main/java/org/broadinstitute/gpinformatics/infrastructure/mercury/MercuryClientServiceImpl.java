package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
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

    @Inject
    private Deployment deployment;

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo) {
        if (deployment != Deployment.PROD) {
            return mercuryClientEjb.addFromProductOrder(pdo);
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo, @Nonnull Collection<ProductOrderSample> samples) {
        if (deployment != Deployment.PROD) {
            return mercuryClientEjb.addFromProductOrder(pdo, samples);
        }
        return Collections.emptyList();
    }
}
