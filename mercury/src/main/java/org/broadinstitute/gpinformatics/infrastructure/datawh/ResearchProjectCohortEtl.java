package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort_;
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
public class ResearchProjectCohortEtl  extends GenericEntityEtl {

    private ResearchProjectDao dao;

    @Inject
    public void setResearchProjectDao(ResearchProjectDao dao) {
	this.dao = dao;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ResearchProjectCohort.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "research_project_cohort";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ResearchProjectCohort)entity).getResearchProjectCohortId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        ResearchProjectCohort entity = dao.findById(ResearchProjectCohort.class, entityId);
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
        List<ResearchProjectCohort> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<ResearchProjectCohort>() {
                    @Override
                    public void callback(CriteriaQuery<ResearchProjectCohort> cq, Root<ResearchProjectCohort> root) {
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(ResearchProjectCohort_.researchProjectCohortId), startId, endId));
                        }
                    }
                });
        for (ResearchProjectCohort entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ResearchProjectCohort entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectCohortId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null)
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
