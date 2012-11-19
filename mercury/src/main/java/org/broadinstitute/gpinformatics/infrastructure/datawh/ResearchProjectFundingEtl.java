package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ResearchProjectFundingEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Override
    Class getEntityClass() {
        return ResearchProjectFunding.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project_funding";
    }

    @Override
    Long entityId(Object entity) {
        return ((ResearchProjectFunding)entity).getResearchProjectFundingId();
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
        ResearchProjectFunding entity = dao.getEntityManager().find(ResearchProjectFunding.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  ResearchProjectFunding having id " + entityId + " no longer exists.");
            return null;
        }
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectFundingId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getFundingId())
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
