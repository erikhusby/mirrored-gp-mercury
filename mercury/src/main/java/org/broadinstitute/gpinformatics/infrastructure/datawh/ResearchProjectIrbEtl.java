package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ResearchProjectIrbEtl extends GenericEntityEtl<ResearchProjectIRB, ResearchProjectIRB> {

    public ResearchProjectIrbEtl() {
    }

    @Inject
    public ResearchProjectIrbEtl(ResearchProjectDao dao) {
        super(ResearchProjectIRB.class, "research_project_irb", "athena.research_projectirb_aud",
                "research_projectirbid", dao);
    }

    @Override
    Long entityId(ResearchProjectIRB entity) {
        return entity.getResearchProjectIRBId();
    }

    @Override
    Path rootId(Root<ResearchProjectIRB> root) {
        return root.get(ResearchProjectIRB_.researchProjectIRBId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ResearchProjectIRB.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ResearchProjectIRB entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectIRBId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getIrb()),
                format(entity.getIrbType() != null ? entity.getIrbType().getDisplayName() : null)
        );
    }
}
