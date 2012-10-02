package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDAO;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Restful webservice to list the athena research project info.
 */
@Path("/researchProjects")
@Stateless
public class ResearchProjectResource {

    @Inject
    private ResearchProjectDAO researchProjectDAO;

    @GET
    @Path("{researchProjectId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectById(@PathParam("researchProjectId") Long researchProjectId) {
        return findRPById(researchProjectId);
    }

    private ResearchProject findRPById(final Long researchProjectId) {
        // Check for content
        if ((researchProjectId == null) || (researchProjectId < 1)) {
            throw new RuntimeException("ResearchProject Id is invalid.");
        }

        // Try to find research project by number
        ResearchProject researchProject = researchProjectDAO.findById(researchProjectId);

        if (researchProject == null) {
            throw new RuntimeException("Could not retrieve research project with id " + researchProjectId);
        }

        return researchProject;
    }

    // For testing in a browser - dev only !!
    @GET
    @Path("{researchProjectId}")
    @Produces({MediaType.TEXT_HTML})
    public ResearchProject findResearchProjectByIdHtml(@PathParam("researchProjectId") Long researchProjectId) {

        // Check for content
        return findRPById(researchProjectId);
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @param creator The creator to look up
     *
     * @return The research projects that match
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<ResearchProject> findAllResearchProjects(@MatrixParam("creator") String creator) {

        ArrayList<ResearchProject> foundProjects;

        if (StringUtils.isNotBlank(creator)) {
            foundProjects = researchProjectDAO.findResearchProjectsByOwner(creator);
        } else {
            foundProjects = researchProjectDAO.findAllResearchProjects();
        }

        return foundProjects;

    }

}
