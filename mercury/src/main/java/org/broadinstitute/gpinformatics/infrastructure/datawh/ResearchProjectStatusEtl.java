package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ResearchProjectStatusEtl extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Override
    Class getEntityClass() {
        return ResearchProject.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project_status";
    }

    @Override
    Long entityId(Object entity) {
        return ((ResearchProject)entity).getResearchProjectId();
    }

    /** This entity does not make entity records. */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return null;
    }


    /**
     * Makes a data record from entity status fields, and possible the Envers revision date,
     * in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr date
     * @param revDate Envers revision date
     * @param revObject the Envers versioned entity
     * @return delimited SqlLoader record, or null if entity does not support status recording
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject) {
        ResearchProject entity = (ResearchProject)revObject;
        if (entity == null || entity.getStatus() == null) {
            return null;
        } else {
            return genericRecord(etlDateStr, false,
                    entity.getResearchProjectId(),
                    format(revDate),
                    format(entity.getStatus().getDisplayName()));
        }
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
