package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ResearchProjectEtl extends GenericEntityAndStatusEtl<ResearchProject, ResearchProject> {

    public ResearchProjectEtl() {
    }

    @Inject
    public ResearchProjectEtl(ResearchProjectDao dao) {
        super(ResearchProject.class, "research_project", "research_project_status", "athena.research_project_aud",
                "research_project_id", dao);
    }

    @Override
    Long entityId(ResearchProject entity) {
        return entity.getResearchProjectId();
    }

    @Override
    Path rootId(Root<ResearchProject> root) {
        return root.get(ResearchProject_.researchProjectId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ResearchProject.class, entityId));
    }

    @Override
    String statusRecord(String etlDateStr, boolean isDelete, ResearchProject entity, Date statusDate) {
        if (entity != null && entity.getStatus() != null) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getResearchProjectId(),
                    format(statusDate),
                    format(entity.getStatus().getDisplayName())
            );
        } else {
            return null;
        }
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ResearchProject entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectId(),
                format(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null),
                format(entity.getCreatedDate()),
                format(entity.getTitle()),
                format(entity.getIrbNotEngaged()),
                format(entity.getJiraTicketKey()),
                format(entity.getParentResearchProject() != null ? entity.getParentResearchProject().getResearchProjectId() : null),
                format(entity.getRootResearchProject() != null ? entity.getRootResearchProject().getResearchProjectId() : null)
        );
    }
}
