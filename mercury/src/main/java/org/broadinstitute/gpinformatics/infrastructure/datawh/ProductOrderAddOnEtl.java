package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
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
        return entityRecord(etlDateStr, isDelete, entity);
    }

    /**
     * Returns data records for all entity instances of this class.
     * @return
     */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        Collection<String> allRecords = new ArrayList<String>();
        if (startId == 0 && endId == Long.MAX_VALUE) {
            // Default case gets all entities.
            for (ProductOrderAddOn entity : dao.findAll(ProductOrderAddOn.class)) {
                allRecords.add(entityRecord(etlDateStr, isDelete, entity));
            }
        } else {
            // Spins through the ids one at a time.
            // TODO change this to specify the range in a GenericDaoCallback
            for (long entityId = startId; entityId <= endId; ++entityId) {
                ProductOrderAddOn entity = dao.findById(ProductOrderAddOn.class, entityId);
                if (entity != null) {
                    allRecords.add(entityRecord(etlDateStr, isDelete, entity));
                }
            }
        }
        return allRecords;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ProductOrderAddOn entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderAddOnId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getAddOn().getProductId())
        );
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
