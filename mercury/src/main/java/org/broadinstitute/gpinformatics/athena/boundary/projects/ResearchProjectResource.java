package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDAO;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Restful webservice to list the research project info.
 * Used by Squid, PMBridge reports, perhaps BSP and Quotes.
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/17/12
 * Time: 5:16 PM
 */
@Path("/researchProjects")
@Stateless
public class ResearchProjectResource {

    @Inject
    private ResearchProjectDAO researchProjectDAO;

    @GET
    @Path("{researchProjectId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectById(@PathParam("researchProjectId") String researchProjectId) {
        return findRPById(researchProjectId);


    }

    private ResearchProject findRPById(final String researchProjectId) {
        // Check for content
        if (StringUtils.isBlank(researchProjectId)) {
            throw new RuntimeException("ResearchProject Id is invalid.");
        }

        // check is it a number
        boolean isNumeric = Pattern.matches("[\\d]+", researchProjectId);
        if (!isNumeric) {
            throw new RuntimeException("ResearchProject Id is not numeric.");
        }

        // Try to find research project by number
        ResearchProject researchProject = researchProjectDAO.findById(new ResearchProjectID(researchProjectId));

        if (researchProject == null) {
            throw new RuntimeException("Could not retrieve research project with id " + researchProjectId);
        }
        return researchProject;
    }


    // For testing in a browser - dev only !!
    @GET
    @Path("{researchProjectId}")
    @Produces({MediaType.TEXT_HTML})
    public ResearchProject findResearchProjectByIdHtml(@PathParam("researchProjectId") String researchProjectId) {

        // Check for content
        return findRPById(researchProjectId);
    }


    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @param creator
     * @return
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<ResearchProject> findAllResearchProjects(@MatrixParam("creator") String creator) {

        ArrayList<ResearchProject> foundProjects = null;

        if (StringUtils.isNotBlank(creator)) {
            foundProjects = researchProjectDAO.findResearchProjectsByOwner(creator);
        } else {
            foundProjects = researchProjectDAO.findAllResearchProjects();
        }

        return foundProjects;

    }


//    public Collection<ResearchProject> findResearchProjects(Person person) {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//
//    @Override
//    public ResearchProject createResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis) {
//
//        ResearchProject researchProject = new ResearchProject( creator, title, id, synopsis  );
//
//        return researchProject;
//    }


}
