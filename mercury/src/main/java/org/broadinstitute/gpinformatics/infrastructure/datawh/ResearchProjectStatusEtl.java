package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Stateless
public class ResearchProjectStatusEtl extends GenericEntityEtl {

    @Inject
    ResearchProjectDao dao;

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return ResearchProject.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "research_project_status";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((ResearchProject)entity).getResearchProjectId();
    }

    /** This class does not make entity records. */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return Collections.EMPTY_LIST;
    }

    /** This class does not make entity records. */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        return Collections.EMPTY_LIST;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        ResearchProject entity = (ResearchProject)revObject;
        if (entity == null) {
            logger.info("Cannot export.  Audited ResearchProject object is null.");
            return null;
        }
        // Skips entity changes that don't affect status (i.e. status will be null in the Envers entity).
        if (entity.getStatus() == null) {
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectId(),
                format(revDate),
                format(entity.getStatus().getDisplayName())
        );
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
