package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
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

    private final ResearchProjectDao researchProjectDao;

    @SuppressWarnings("unused")
    public RegulatoryInfoEjb() {
        this(null, null);
    }

    @Inject
    public RegulatoryInfoEjb(RegulatoryInfoDao regulatoryInfoDao, ResearchProjectDao researchProjectDao) {
        this.regulatoryInfoDao = regulatoryInfoDao;
        this.researchProjectDao = researchProjectDao;
    }

    /**
     * Create a new RegulatoryInfo add add it to a ResearchProject.
     *
     * @param identifier            the identifier (ideally an ORSP ID) for the new RegulatoryInfo
     * @param type                  the type of the new RegulatoryInfo
     * @param alias                 the alias/title/name for the new RegulatoryInfo
     * @param researchProjectKey    the business key of the ResearchProject to add the new RegulatoryInfo to
     */
    public void createAndAddRegulatoryInfoToResearchProject(String identifier, RegulatoryInfo.Type type, String alias,
                                                            String researchProjectKey) {
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo(alias, type, identifier);
        regulatoryInfoDao.persist(regulatoryInfo);
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        researchProject.addRegulatoryInfo(regulatoryInfo);
    }

    /**
     * Add an existing RegulatoryInfo to a ResearchProject.
     *
     * @param regulatoryInfoId      the primary key of the RegulatoryInfo to add
     * @param researchProjectKey    the business key of the ResearchProject to add the RegulatoryInfo to
     */
    public void addRegulatoryInfoToResearchProject(Long regulatoryInfoId, String researchProjectKey) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        researchProject.addRegulatoryInfo(regulatoryInfo);
    }

    /**
     * Remove a RegulatoryInfo from a ResearchProject. The RegulatoryInfo itself will not be removed from Mercury.
     *
     * @param regulatoryInfoId      the primary key of the RegulatoryInfo to remove
     * @param researchProjectKey    the business key of the ResearchProject to remove the RegulatoryInfo from
     */
    public void removeRegulatoryInfoFromResearchProject(Long regulatoryInfoId, String researchProjectKey) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        researchProject.removeRegulatoryInfo(regulatoryInfo);
    }

    /**
     * Edit the title of a RegulatoryInfo.
     *
     * @param regulatoryInfoId    the primary key of the RegulatoryInfo to edit
     * @param newTitle            the new title for the RegulatoryInfo
     */
    public void editRegulatoryInfo(Long regulatoryInfoId, String newTitle) {
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        regulatoryInfo.setName(newTitle);
    }
}
