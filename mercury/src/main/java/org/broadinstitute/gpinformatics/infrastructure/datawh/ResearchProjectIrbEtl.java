package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ResearchProjectIrbEtl  extends GenericEntityEtl {
    @Inject
    ResearchProjectDao dao;

    @Override
    Class getEntityClass() {
        return ResearchProjectIRB.class;
    }

    @Override
    String getBaseFilename() {
        return "research_project_irb";
    }

    @Override
    Long entityId(Object entity) {
        return ((ResearchProjectIRB)entity).getResearchProjectIRBId();
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
        ResearchProjectIRB entity = dao.getEntityManager().find(ResearchProjectIRB.class, entityId);
        if (entity == null || entity.getIrbType() == null) {
            return null;
        } else {
            return genericRecord(etlDateStr, false,
                    entity.getResearchProjectIRBId(),
				 format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null)<
                    format(entity.getIrb()),
                    format(entity.getIrbType().getDisplayName()));
        }
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
