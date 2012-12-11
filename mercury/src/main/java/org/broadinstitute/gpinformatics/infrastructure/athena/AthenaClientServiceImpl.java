package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@Impl
public class AthenaClientServiceImpl implements AthenaClientService {

    @Inject
    private ProductOrderDao productOrderDao;

    /**
     * Simple finder to search for product order by key.  Allows Infrastructure level components the ability to use this
     * functionality
     * @param productOrderKey business key of the desired product order.
     * @return if found, and instance of the desired product order.  Otherwise null
     */
    public ProductOrder retrieveProductOrderDetails(@Nonnull String productOrderKey) {
        return productOrderDao.findByBusinessKey(productOrderKey);
    }
}
