package org.broadinstitute.pmbridge.boundary.projects;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.pmbridge.control.dao.ResearchProjectDAO;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.entity.project.ResearchProjects;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Restful webservice to list the research project info.
 * Used by Squid, PMBridge reports, perhaps BSP and Quotes.
 *
 * TODO: Need to replace reponse with actual ResearchProject xml bean
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
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public ResearchProject findResearchProjectById(@PathParam("researchProjectId") String researchProjectId) {

        // Check for content
        if (StringUtils.isBlank(researchProjectId )) {
            throw new RuntimeException("ResearchProject Id is invalid.");
        }

        // check is it a number
        boolean isNumeric = Pattern.matches("[\\d]+", researchProjectId);
        if (! isNumeric ) {
            throw new RuntimeException("ResearchProject Id is not numeric.");
        }
        Long researchProjectNum = Long.parseLong(researchProjectId);

        // Try to find research project by number
        ResearchProject researchProject = researchProjectDAO.findById( researchProjectNum );

        if ( researchProject == null ) {
            throw new RuntimeException("Could not retrieve research project with id " + researchProjectId);
        }

        return researchProject;
    }

    /**
     * Method to GET the list of research projects. OPtionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @param creator
     * @return
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public ResearchProjects findAllResearchProjects(@MatrixParam("creator") String creator) {

        ArrayList<String> results = new ArrayList<String>();
        ArrayList<ResearchProject> foundProjects = null;
        boolean creatorSupplied = ( StringUtils.isNotBlank(creator) ? true : false );

        if (creatorSupplied) {
            foundProjects = researchProjectDAO.findResearchProjectsByOwner(creator);
        } else {
            foundProjects = researchProjectDAO.findAllResearchProjects();
        }

//        ResearchProjects researchProjects = null;
//        if ( foundProjects != null && foundProjects.size() > 0 ) {
//            StringBuilder msgBuffer = new StringBuilder("Could not retrieve any research projects");
//            if ( creatorSupplied) {
//                msgBuffer.append( " for user " + creator);
//            }
//            throw new RuntimeException(msgBuffer.toString());
//        }

//        //Get all the sequencing experiments from "sequid" for this user.
//        List<ExperimentRequestSummary> expRequestSummaries = sequencingService.getRequestSummariesByCreator(person);
//
//        //Get all the gap experiments from GAP for this user.
//        List<ExperimentRequestSummary> expRequestSummaries = sequencingService.getRequestSummariesByCreator(person);
//
//        //Get all of the research project from
//
//        //For each summary get the corresponding research project.

        return  new ResearchProjects(foundProjects);

    }



//    public Collection<ResearchProject> findResearchProjects(Person person) {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//
//    @Override
//    public ResearchProject createResearchProject(Person creator, Name title, ResearchProjectId id, String synopsis) {
//
//        //TODO - May need to remove Id from params and generate it internally
//        ResearchProject researchProject = new ResearchProject( creator, title, id, synopsis  );
//
//        return researchProject;
//    }


}
