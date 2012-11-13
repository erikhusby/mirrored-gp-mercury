package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Restful webservice to
 * list the athena research project info.
 */
@Path("/researchProjects")
@Stateless
public class ResearchProjectResource {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @GET
    @Path("{researchProjectTitle}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectByTitle(@PathParam("researchProjectTitle") String researchProjectTitle) {
        return findRPByTitle(researchProjectTitle);
    }

    @GET
    @Path("{researchProjectId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectById(@PathParam("researchProjectId") String researchProjectId) {
        return findRPById(researchProjectId);
    }

    /**
     * Get the research project by the project title.
     *
     * @param researchProjectTitle The name given to the research project being looked up
     *
     * @return null if not found, otherwise the matching research project
     */
    private ResearchProject findRPByTitle(@Nonnull String researchProjectTitle) {
        // Try to find research project by number
        return researchProjectDao.findByTitle(researchProjectTitle);
    }

    /**
     * Get the research project by the id (jira ticket key). If not found, returns null.
     *
     * @param jiraTicketKey The jira ticket key for the research project.
     *
     * @return null if not found, otherwise the matching research project
     */
    private ResearchProject findRPById(@Nonnull String jiraTicketKey) {
        return researchProjectDao.findByJiraTicketKey(jiraTicketKey);
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @param creatorId The createdBy to look up, If null, gets all research projects
     *
     * @return The research projects that match
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<ResearchProject> findAllResearchProjectsByCreator(@MatrixParam("creator") Long creatorId) {

        List<ResearchProject> foundProjects;

        if ((creatorId != null) && (creatorId > 0)) {
            foundProjects = researchProjectDao.findResearchProjectsByOwner(creatorId);
        } else {
            foundProjects = researchProjectDao.findAllResearchProjects();
        }

        return foundProjects;

    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @return The research projects that match
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<ResearchProject> findAllResearchProjects() {
        return researchProjectDao.findAllResearchProjects();
    }
}
