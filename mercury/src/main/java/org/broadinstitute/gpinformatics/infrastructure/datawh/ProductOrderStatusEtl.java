package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Stateless
public class ProductOrderStatusEtl extends GenericEntityEtl {
    @Inject
    ProductOrderDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ProductOrder.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "product_order_status";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrder)entity).getProductOrderId();
    }

    /** This entity does not make entity records. */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return Collections.EMPTY_LIST;
    }

    /** This entity etl does not make entity records. */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        return Collections.EMPTY_LIST;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        ProductOrder entity = (ProductOrder)revObject;
        if (entity == null) {
            logger.info("Cannot export.  Audited ProductOrder object is null.");
            return null;
        }
        // Skips entity changes that don't affect status (i.e. status will be null in the Envers entity).
        if (entity.getOrderStatus() == null) {
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderId(),
                format(revDate),
                format(entity.getOrderStatus().getDisplayName())
        );
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
