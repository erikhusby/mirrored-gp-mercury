package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;

/**
 * Boundary bean for managing research projects.
 *
 * @author breilly
 */
@Stateful
@RequestScoped
public class ResearchProjectManager {

    @Inject
    private Log logger;

    @Inject
    private ResearchProjectDao researchProjectDao;

    public void createResearchProject(ResearchProject project) {

        try {
            project.submit();
        } catch (IOException e) {
            logger.error("Error creating JIRA ticket for research project", e);
            throw new RuntimeException("Unable to create JIRA issue: " + e.getMessage(), e);
        }

        researchProjectDao.persist(project);
    }

    public void updateResearchProject(ResearchProject project) {
        if (!researchProjectDao.getEntityManager().contains(project)) {
            researchProjectDao.getEntityManager().merge(project);
        }
        researchProjectDao.saveAll();
    }

    public void deleteResearchProject(ResearchProject project) {
        researchProjectDao.remove(project);
    }
}
