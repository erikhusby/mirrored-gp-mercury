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
 * Transaction is TransactionAttributeType.REQUIRED (default for EJBs) since these operations primarily deal with making
 * database changes. No explicit action needs to be taken to save any changes to managed entities (even those fetched
 * and modified outside of the transaction) because the extended persistence context is automatically propagated
 * through to this stateful session bean.
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

    /**
     * Create a new research project along with its associated JIRA ticket. Will not persist if the research project's
     * name is not unique.
     *
     * @param project    the project to persist
     * @throws ApplicationValidationException when the project's name is not unique; rolls-back transaction
     */
    public void createResearchProject(ResearchProject project) throws ApplicationValidationException {
        validateUniqueProjectTitle(project);

        // Create JIRA issue first, since it is more likely to fail than persist
        createJiraIssue(project);

        // Persist research project, which should succeed assuming all validation has been done up-front
        try {
            researchProjectDao.persist(project);

            // Force as much work here as possible to catch conditions where we would want to close the JIRA ticket
            researchProjectDao.flush();
        } catch (RuntimeException e) {

            // TODO: close already-created JIRA ticket
            throw e;
        }
    }

    /**
     * Updates a research project. Will not save changes if the research project's name is not unique.
     *
     * @param project    the project to save
     * @throws ApplicationValidationException when the project's name is not unique; rolls-back transaction
     */
    public void updateResearchProject(ResearchProject project) throws ApplicationValidationException {
        validateUniqueProjectTitle(project);
    }

    /**
     * Deletes a research project.
     *
     * @param project    the project to delete
     */
    public void deleteResearchProject(ResearchProject project) {
        researchProjectDao.remove(project);
    }

    private void validateUniqueProjectTitle(ResearchProject project) throws ApplicationValidationException {
        ResearchProject existingProject = researchProjectDao.findByTitle(project.getTitle());
        if (existingProject != null && !existingProject.getResearchProjectId().equals(project.getResearchProjectId())) {
            throw new ApplicationValidationException("Research project name is already in use for another project.");
        }
    }

    private void createJiraIssue(ResearchProject project) {
        try {
            project.submit();
        } catch (IOException e) {
            logger.error("Error creating JIRA ticket for research project", e);
            throw new RuntimeException("Unable to create JIRA issue: " + e.getMessage(), e);
        }
    }
}
