package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

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
public class LabBatchEtl extends GenericEntityEtl {

    private LabBatchDAO dao;

    @Inject
    public void setLabBatchDAO(LabBatchDAO dao) {
        this.dao = dao;
    }

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return LabBatch.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return "lab_batch";
    }

    /** {@inheritDoc} */
    @Override
    Long entityId(Object entity) {
        return ((LabBatch)entity).getLabBatchId();
    }

    /** {@inheritDoc} */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        LabBatch entity = dao.findById(LabBatch.class, entityId);
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
        List<LabBatch> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<LabBatch>() {
                    @Override
                    public void callback(CriteriaQuery<LabBatch> cq, Root<LabBatch> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(root.get(LabBatch_.labBatchId), startId, endId));
                    }
                });
        for (LabBatch entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, LabBatch entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getLabBatchId(),
                format(entity.getBatchName())
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
