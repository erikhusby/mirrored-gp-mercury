package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Stateful
public class ResearchProjectFundingEtl  extends GenericEntityEtl {

    private ResearchProjectDao dao;

    @Inject
    public void setResearchProjectDao(ResearchProjectDao dao) {
        this.dao = dao;
    }

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

    /** {@inheritDoc} */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        ResearchProjectFunding entity = dao.findById(ResearchProjectFunding.class, entityId);
        if (entity != null) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        } else {
            logger.info("Cannot export. " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
        }
        return recordList;
    }

    /** {@inheritDoc} */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<ResearchProjectFunding> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<ResearchProjectFunding>() {
                    @Override
                    public void callback(CriteriaQuery<ResearchProjectFunding> cq, Root<ResearchProjectFunding> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(root.get(ResearchProjectFunding_.researchProjectFundingId), startId, endId));
                    }
                });
        for (ResearchProjectFunding entity : entityList) {
            recordList.add(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, ResearchProjectFunding entity) {
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
