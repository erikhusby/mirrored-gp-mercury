package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ResearchProjectCohortEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Override
    Class getEntityClass() {
        return ResearchProjectCohort.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project_cohort";
    }

    @Override
    Long entityId(Object entity) {
        return ((ResearchProjectCohort)entity).getResearchProjectCohortId();
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
        ResearchProjectCohort entity = dao.getEntityManager().find(ResearchProjectCohort.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  ResearchProjectCohort having id " + entityId + " no longer exists.");
            return null;
        }
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
