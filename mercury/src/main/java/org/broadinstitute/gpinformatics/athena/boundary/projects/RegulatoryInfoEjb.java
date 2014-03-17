package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * EJB for managing regulatory information for research projects and product orders.
 */
@Stateful
@RequestScoped
public class RegulatoryInfoEjb {

    private final RegulatoryInfoDao regulatoryInfoDao;

    public RegulatoryInfoEjb() {
        this(null);
    }

    @Inject
    public RegulatoryInfoEjb(RegulatoryInfoDao regulatoryInfoDao) {
        this.regulatoryInfoDao = regulatoryInfoDao;
    }

    public RegulatoryInfo createRegulatoryInfo(String identifier, RegulatoryInfo.Type type, String alias) {
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo(alias, type, identifier);
        regulatoryInfoDao.persist(regulatoryInfo);
        return regulatoryInfo;
    }

    public void addRegulatoryInfoToResearchProject(Long regulatoryInfoId, ResearchProject researchProject) {
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        researchProject.addRegulatoryInfo(regulatoryInfo);
    }
}
