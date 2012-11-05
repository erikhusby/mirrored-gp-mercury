package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProductOrderSampleEtl extends GenericEntityEtl {
    @Inject
    ProductOrderSampleDao dao;

    @Override
    Class getEntityClass() {
        return ProductOrderSample.class;
    }

    @Override
    String getBaseFilename() {
        return "product_order_sample";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProductOrderSample)entity).getProductOrderSampleId();
    }

    /**
     * Makes a data record from selected entity fields, in a format that matches the corresponding
     * SqlLoader control file.
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        ProductOrderSample entity = dao.findById(ProductOrderSample.class, entityId);
        if (entity == null) {
            logger.info("Cannot export. ProductOrderSample having id " + entityId + " no longer exists.");
            return null;
        }
        return genericRecord(etlDateStr, false,
                entity.getProductOrderSampleId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getSampleName()),
                format(entity.getBillingStatus().getDisplayName()));
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
