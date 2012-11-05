package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProductOrderAddOnEtl extends GenericEntityEtl {
    @Inject
    ProductOrderDao dao;

    @Override
    Class getEntityClass() {
        return ProductOrderAddOn.class;
    }

    @Override
    String getBaseFilename() {
        return "product_order_add_on";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProductOrderAddOn)entity).getProductOrderAddOnId();
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
        ProductOrderAddOn entity = dao.findById(ProductOrderAddOn.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  ProductOrderAddOn having id " + entityId + " no longer exists.");
            return null;
        } else {
            if (entity.getAddOn() == null) {
                logger.info("Cannot export. ProductOrderAddOn having id " + entityId + " has null AddOn.");
                return null;
            }
        }
        return genericRecord(etlDateStr, false,
                entity.getProductOrderAddOnId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getAddOn().getProductId()));
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
