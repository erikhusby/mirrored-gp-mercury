package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Stateless
public class ProductEtl  extends GenericEntityEtl {
    @Inject
    ProductDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return Product.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "product";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((Product)entity).getProductId();
    }

    /**
     * @{inheritDoc}
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
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<Product> entityList = dao.findAll(Product.class,
                new GenericDao.GenericDaoCallback<Product>() {
                    @Override
                    public void callback(CriteriaQuery<Product> cq, Root<Product> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(Product_.productId), startId, endId));
                        }
                    }
                });
        for (Product entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
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
