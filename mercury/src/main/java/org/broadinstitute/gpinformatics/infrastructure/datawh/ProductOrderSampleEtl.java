package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
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
public class ProductOrderSampleEtl extends GenericEntityEtl {
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
        return "product_order_sample";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrderSample)entity).getProductOrderSampleId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        ProductOrderSample entity = dao.findById(ProductOrderSample.class, entityId);
        if (entity == null) {
            logger.info("Cannot export. ProductOrderSample having id " + entityId + " no longer exists.");
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
        List<ProductOrderSample> entityList = dao.findAll(ProductOrderSample.class,
                new GenericDao.GenericDaoCallback<ProductOrderSample>() {
                    @Override
                    public void callback(CriteriaQuery<ProductOrderSample> cq, Root<ProductOrderSample> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(ProductOrderSample_.productOrderSampleId), startId, endId));
                        }
                    }
                });
        for (ProductOrderSample entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getSampleName()),
                format(entity.getDeliveryStatus().getDisplayName()),
                format(entity.getSamplePosition())
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
