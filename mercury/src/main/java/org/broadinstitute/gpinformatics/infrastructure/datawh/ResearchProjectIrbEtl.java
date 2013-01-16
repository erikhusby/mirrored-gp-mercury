package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB_;
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
public class ResearchProjectIrbEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ResearchProjectIRB.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "research_project_irb";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ResearchProjectIRB)entity).getResearchProjectIRBId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        ResearchProjectIRB entity = dao.getEntityManager().find(ResearchProjectIRB.class, entityId);
        if (entity == null) {
            logger.info("Cannot export. ResearchProjectIRB having id " + entityId + " no longer exists.");
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
        List<ResearchProjectIRB> entityList = dao.findAll(ResearchProjectIRB.class,
                new GenericDao.GenericDaoCallback<ResearchProjectIRB>() {
                    @Override
                    public void callback(CriteriaQuery<ResearchProjectIRB> cq, Root<ResearchProjectIRB> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(ResearchProjectIRB_.researchProjectIRBId), startId, endId));
                        }
                    }
                });
        for (ResearchProjectIRB entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ResearchProjectIRB entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectIRBId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getIrb()),
                format(entity.getIrbType() != null ? entity.getIrbType().getDisplayName() : null)
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
