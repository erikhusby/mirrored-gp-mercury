package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ResearchProjectEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Override
    Class getEntityClass() {
        return ResearchProject.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project";
    }

    @Override
    Long entityId(Object entity) {
        return ((ResearchProject)entity).getResearchProjectId();
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
        ResearchProject entity = dao.findById(ResearchProject.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  ResearchProject having id " + entityId + " no longer exists.");
            return null;
        }
        return genericRecord(etlDateStr, false,
                entity.getResearchProjectId(),
                format(entity.getResearchProjectId()),
                format(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null),
                format(entity.getCreatedDate()),
                format(entity.getTitle()),
                format(entity.getIrbNotEngaged()),
                format(entity.getJiraTicketKey()));
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }
}
