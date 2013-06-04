package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
public class ResearchProjectCohortEtl extends GenericEntityEtl<ResearchProjectCohort, ResearchProjectCohort> {

    public ResearchProjectCohortEtl() {
    }

    @Inject
    public ResearchProjectCohortEtl(ResearchProjectDao dao) {
        super(ResearchProjectCohort.class, "research_project_cohort", dao);
    }

    @Override
    Long entityId(ResearchProjectCohort entity) {
        return entity.getResearchProjectCohortId();
    }

    @Override
    Path rootId(Root<ResearchProjectCohort> root) {
        return root.get(ResearchProjectCohort_.researchProjectCohortId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ResearchProjectCohort.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ResearchProjectCohort entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectCohortId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null)
        );
    }
}
