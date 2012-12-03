package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderBean;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.inject.Inject;

@Impl
public class AthenaClientServiceImpl implements AthenaClientService {

    @Inject
    ProductOrderBean productOrderBean;

    public AthenaClientServiceImpl () {
    }

    @Override
    public ProductOrder retrieveProductOrderDetails ( String poBusinessKey ) {
        return productOrderBean.getProductOrderByKey(poBusinessKey);
    }
}
