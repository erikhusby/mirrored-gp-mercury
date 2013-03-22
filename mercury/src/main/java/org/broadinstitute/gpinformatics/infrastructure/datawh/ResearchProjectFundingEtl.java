package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;

@Stateful
public class ResearchProjectFundingEtl extends GenericEntityEtl<ResearchProjectFunding, ResearchProjectFunding> {

    private ResearchProjectDao dao;

    @Inject
    public void setResearchProjectDao(ResearchProjectDao dao) {
        this.dao = dao;
    }

    public ResearchProjectFundingEtl() {
        entityClass = ResearchProjectFunding.class;
        baseFilename = "research_project_funding";
    }

    @Override
    Long entityId(ResearchProjectFunding entity) {
        return entity.getResearchProjectFundingId();
    }

    @Override
    Path rootId(Root root) {
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
