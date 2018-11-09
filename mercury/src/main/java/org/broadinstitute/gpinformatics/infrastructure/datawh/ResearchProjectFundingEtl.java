package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ResearchProjectFundingEtl extends GenericEntityEtl<ResearchProjectFunding, ResearchProjectFunding> {

    public ResearchProjectFundingEtl() {
    }

    @Inject
    public ResearchProjectFundingEtl(ResearchProjectDao dao) {
        super(ResearchProjectFunding.class, "research_project_funding", "athena.research_project_funding_aud",
                "research_project_funding_id", dao);
    }

    @Override
    Long entityId(ResearchProjectFunding entity) {
        return entity.getResearchProjectFundingId();
    }

    @Override
    Path rootId(Root<ResearchProjectFunding> root) {
        return root.get(ResearchProjectFunding_.researchProjectFundingId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ResearchProjectFunding.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ResearchProjectFunding entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getResearchProjectFundingId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getFundingId())
        );
    }
}
