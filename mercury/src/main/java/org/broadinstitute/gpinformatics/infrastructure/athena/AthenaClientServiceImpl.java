package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Impl
@Default
public class AthenaClientServiceImpl implements AthenaClientService {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    /**
     * Simple finder to search for product order by key.  Allows Infrastructure level components the ability to use this
     * functionality
     * @param productOrderKey business key of the desired product order.
     * @return if found, and instance of the desired product order.  Otherwise null
     */
    @Override
    public ProductOrder retrieveProductOrderDetails(@Nonnull String productOrderKey) {
        return productOrderDao.findByBusinessKey(productOrderKey);
    }

    /**
     * see interface
     */
    @Override
    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
        return productOrderSampleDao.findMapBySamples(sampleNames);
    }

    @Override
    public Collection<ProductOrder> retrieveMultipleProductOrderDetails(@Nonnull Collection<String> poBusinessKeys) {
        return productOrderDao.findListByBusinessKeyList(new ArrayList<>(poBusinessKeys));
    }
}
