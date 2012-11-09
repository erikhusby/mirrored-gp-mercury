package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProductOrderStatusEtl extends GenericEntityEtl {
    @Inject
    ProductOrderDao dao;

    @Override
    Class getEntityClass() {
        return ProductOrder.class;
    }

    @Override
    String getBaseFilename() {
        return "product_order_status";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProductOrder)entity).getProductOrderId();
    }

    /** This entity does not make entity records. */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return null;
    }


    /**
     * Makes a data record from entity status fields, and possible the Envers revision date,
     * in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr date
     * @param revDate Envers revision date
     * @param revObject the Envers versioned entity
     * @param isDelete indicates deleted entity
     * @return delimited SqlLoader record, or null if entity does not support status recording
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        ProductOrder entity = (ProductOrder)revObject;
        if (entity == null) {
            logger.info("Cannot export.  Audited ProductOrder object is null.");
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderId(),
                format(revDate),
                format(entity.getOrderStatus() != null ? entity.getOrderStatus().getDisplayName() : "unknown")
        );
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
