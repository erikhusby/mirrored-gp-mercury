package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateless;
import javax.inject.Inject;

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
    String makeRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Product entity = dao.findById(Product.class, entityId);
        if (entity == null) {
            return null;
        } else {
            return Util.makeRecord(etlDateStr, false,
                    entity.getProductId(),
                    Util.format(entity.getProductName()),
                    Util.format(entity.getPartNumber()),
                    Util.format(entity.getAvailabilityDate()),
                    Util.format(entity.getDiscontinuedDate()),
                    Util.format(entity.getExpectedCycleTimeSeconds()),
                    Util.format(entity.getGuaranteedCycleTimeSeconds()),
                    Util.format(entity.getSamplesPerWeek()),
                    Util.format(entity.isTopLevelProduct()),
                    Util.format(entity.getWorkflowName()));
        }
    }
}
