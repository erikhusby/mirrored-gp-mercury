package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

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
    @Path("{researchProjectId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectById(@PathParam("researchProjectId") Long researchProjectId) {
        return findRPById(researchProjectId);
    }

    private ResearchProject findRPById(Long researchProjectId) {
        // Check for content
        if (researchProjectId == null) {
            throw new RuntimeException("ResearchProject Id is invalid.");
        }

        // Try to find research project by number
        ResearchProject researchProject = researchProjectDao.findById(researchProjectId);
        if (researchProject == null) {
            throw new RuntimeException("Could not retrieve research project with id " + researchProjectId);
        }

        return researchProject;
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @param creatorId The createdBy to look up
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
