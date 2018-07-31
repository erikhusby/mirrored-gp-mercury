package org.broadinstitute.gpinformatics.mocks;


import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import java.util.List;
import java.util.Set;

@Alternative
public class ExceptionThrowingPDOSampleDao extends ProductOrderSampleDao {

    public ExceptionThrowingPDOSampleDao(){}

    @Override
    public List<ProductOrderSample> findByOrderKeyAndSampleNames(@Nonnull String pdoKey, @Nonnull Set sampleNames) {
        throw new RuntimeException("This mocked method just throws an exception");
    }

}
