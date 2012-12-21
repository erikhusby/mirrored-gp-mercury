package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@Stateless
public class ProductEtl  extends GenericEntityEtl {
    @Inject
    ProductDao dao;

    @Override
    Class getEntityClass() {
        return Product.class;
    }

    @Override
    String getBaseFilename() {
        return "product";
    }

    @Override
    Long entityId(Object entity) {
        return ((Product)entity).getProductId();
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
        Product entity = dao.findById(Product.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  Product having id " + entityId + " no longer exists.");
            return null;
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
            for (Product entity : dao.findAll(Product.class)) {
                allRecords.add(entityRecord(etlDateStr, isDelete, entity));
            }
        } else {
            // Spins through the ids one at a time.
            // TODO change this to specify the range in a GenericDaoCallback
            for (long entityId = startId; entityId <= endId; ++entityId) {
                Product entity = dao.findById(Product.class, entityId);
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
    String entityRecord(String etlDateStr, boolean isDelete, Product entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductId(),
                format(entity.getProductName()),
                format(entity.getPartNumber()),
                format(entity.getAvailabilityDate()),
                format(entity.getDiscontinuedDate()),
                format(entity.getExpectedCycleTimeSeconds()),
                format(entity.getGuaranteedCycleTimeSeconds()),
                format(entity.getSamplesPerWeek()),
                format(entity.isTopLevelProduct()),
                format(entity.getWorkflowName()),
                format(entity.getProductFamily() != null ? entity.getProductFamily().getName() : null)
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
