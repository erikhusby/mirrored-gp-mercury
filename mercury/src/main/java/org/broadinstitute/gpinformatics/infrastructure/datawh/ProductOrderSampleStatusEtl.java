package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Stateless
public class ProductOrderSampleStatusEtl extends GenericEntityEtl {
    @Inject
    ProductOrderSampleDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ProductOrderSample.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "product_order_sample_status";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrderSample)entity).getProductOrderSampleId();
    }

    /** This entity does not make entity records. */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return null;
    }

    /** This entity does not make entity records. */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        return Collections.EMPTY_LIST;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        ProductOrderSample entity = (ProductOrderSample)revObject;
        if (entity == null) {
            logger.info("Cannot export. Audited ProductOrderSample object is null.");
            return null;
        }

        return null;

/*
        // Skips entity changes that don't affect status (i.e. status will be null in the Envers entity).
        if (entity.getBillingStatus() == null) {
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(revDate),
                format(entity.getBillingStatus().getDisplayName())
        );
*/
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
