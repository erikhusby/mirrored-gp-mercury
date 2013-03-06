package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Stateful
public class ProductOrderEtl extends GenericEntityEtl {

    private ProductOrderDao dao;
    private BSPUserList userList;

    @Inject
    public void setProductOrderDao(ProductOrderDao dao) {
        this.dao = dao;
    }
    @Inject
    public void setBSPUserList(BSPUserList userList) {
        this.userList = userList;
    }

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return ProductOrder.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return "product_order";
    }

    /** {@inheritDoc} */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrder)entity).getProductOrderId();
    }

    /** {@inheritDoc} */
    @Override
    Collection<String> entityRecords(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        ProductOrder entity = dao.findById(ProductOrder.class, entityId);
        if (entity != null) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        } else {
            logger.info("Cannot export. " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
        }
        return recordList;
    }

    /** {@inheritDoc} */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<ProductOrder> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<ProductOrder>() {
                    @Override
                    public void callback(CriteriaQuery<ProductOrder> cq, Root<ProductOrder> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(ProductOrder_.productOrderId), startId, endId));
                        }
                    }
                });
        for (ProductOrder entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ProductOrder entity) {
        Long personId = entity.getCreatedBy();
        BspUser bspUser = personId != null ? userList.getById(personId) : null;

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getProduct() != null ? entity.getProduct().getProductId() : null),
                format(entity.getOrderStatus().getDisplayName()),
                format(entity.getCreatedDate()),
                format(entity.getModifiedDate()),
                format(entity.getTitle()),
                format(entity.getQuoteId()),
                format(entity.getJiraTicketKey()),
                format(bspUser != null ? bspUser.getUsername() : null)
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
