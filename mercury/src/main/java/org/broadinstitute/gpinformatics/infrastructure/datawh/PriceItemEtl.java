package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem_;
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
public class PriceItemEtl  extends GenericEntityEtl {
    @Inject
    PriceItemDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return PriceItem.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "price_item";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((PriceItem)entity).getPriceItemId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        PriceItem entity = dao.findById(PriceItem.class, entityId);
        if (entity == null) {
            logger.info("Cannot export. PriceItem having id " + entityId + " no longer exists.");
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
        List<PriceItem> entityList = dao.findAll(PriceItem.class,
                new GenericDao.GenericDaoCallback<PriceItem>() {
                    @Override
                    public void callback(CriteriaQuery<PriceItem> cq, Root<PriceItem> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(PriceItem_.priceItemId), startId, endId));
                        }
                    }
                });
        for (PriceItem entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, PriceItem entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getPriceItemId(),
                format(entity.getPlatform()),
                format(entity.getCategory() != null ? entity.getCategory() : "none"),
                format(entity.getName()),
                format(entity.getQuoteServerId()),
                format(entity.getPrice()),
                format(entity.getUnits())
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
