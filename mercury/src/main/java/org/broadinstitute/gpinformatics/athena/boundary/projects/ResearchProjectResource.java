package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Restful webservice to
 * list the athena research project info.
 */
@Path("/researchProjects")
@Stateful
@RequestScoped
public class ResearchProjectResource {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @XmlRootElement
    public static class ProductOrderData {

    }

    @XmlRootElement
    public static class ResearchProjectData {
        public final String title;
        public final String jiraId;
        public final String synopsis;
        @XmlElementWrapper
        @XmlElement(name = "projectManager")
        public final List<String> projectManagers;
        @XmlElementWrapper
        @XmlElement(name = "broadPI")
        public final List<String> broadPIs;
        @XmlElementWrapper
        @XmlElement(name = "productOrder")
        public final List<String> orders;

        ResearchProjectData() {
            title = null;
            jiraId = null;
            synopsis = null;
            projectManagers = null;
            broadPIs = null;
            orders = null;
        }

        public List<String> createUsernamesFromIds(BSPUserList bspUserList, Long[] ids) {
            List<String> names = new ArrayList<String>(ids.length);
            for (Long id : ids) {
                BspUser user = bspUserList.getById(id);
                if (user != null) {
                    names.add(bspUserList.getById(id).getUsername());
                }
            }
            return names;
        }

        public ResearchProjectData(BSPUserList bspUserList, ResearchProject researchProject) {
            title = researchProject.getTitle();
            jiraId = researchProject.getJiraTicketKey();
            synopsis = researchProject.getSynopsis();
            projectManagers = createUsernamesFromIds(bspUserList, researchProject.getProjectManagers());
            broadPIs = createUsernamesFromIds(bspUserList, researchProject.getBroadPIs());
            orders = new ArrayList<String>(researchProject.getProductOrders().size());
            for (ProductOrder order : researchProject.getProductOrders()) {
                orders.add(order.getJiraTicketKey());
            }
        }
    }

    @XmlRootElement
    public static class ResearchProjects {

        @XmlElementWrapper
        @XmlElement(name = "project")
        public final List<ResearchProjectData> projects;

        public ResearchProjects() {
            projects = Collections.emptyList();
        }

        public ResearchProjects(BSPUserList bspUserList, ResearchProject project) {
            this(bspUserList, Collections.singletonList(project));
        }

        public ResearchProjects(BSPUserList bspUserList, List<ResearchProject> projects) {
            this.projects = new ArrayList<ResearchProjectData>(projects.size());
            for (ResearchProject project : projects) {
                this.projects.add(new ResearchProjectData(bspUserList, project));
            }
        }
    }


    @GET
    @Path("rp/{researchProjectId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProjects findResearchProjectById(@PathParam("researchProjectId") String researchProjectId) {
        return new ResearchProjects(bspUserList, researchProjectDao.findByJiraTicketKey(researchProjectId));
    }

    @GET
    @Path("pm/{pm}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProjects findResearchProjectByPm(@PathParam("pm") String pmUserName) {
        // TODO: find all RPs by PM.
        // One approach:
        // - find all ProjectPerson with role == PM and personId == ID
        // - loop over all ProjectPerson to collect ResearchProjects

        return new ResearchProjects();
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @return The research projects that match
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResearchProjects findAllResearchProjects() {
        return new ResearchProjects(bspUserList, researchProjectDao.findAllResearchProjectsWithOrders());
    }
}
