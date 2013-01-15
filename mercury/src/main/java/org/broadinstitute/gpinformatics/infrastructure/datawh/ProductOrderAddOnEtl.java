package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn_;
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
public class ProductOrderAddOnEtl extends GenericEntityEtl {

    @Inject
    ProductOrderDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ProductOrderAddOn.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "product_order_add_on";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrderAddOn)entity).getProductOrderAddOnId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        ProductOrderAddOn entity = dao.findById(ProductOrderAddOn.class, entityId);
        if (entity != null) {
	    recordList.add(entityRecord(etlDateStr, isDelete, entity));
	} else {
            logger.info("Cannot export. " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
        }
        return recordList;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<ProductOrderAddOn> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<ProductOrderAddOn>() {
                    @Override
                    public void callback(CriteriaQuery<ProductOrderAddOn> cq, Root<ProductOrderAddOn> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(ProductOrderAddOn_.productOrderAddOnId), startId, endId));
                        }
                    }
                });
        for (ProductOrderAddOn entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
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
