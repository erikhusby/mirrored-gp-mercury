package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateless;
import javax.inject.Inject;
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
