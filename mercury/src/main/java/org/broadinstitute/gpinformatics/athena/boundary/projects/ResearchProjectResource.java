package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 * Restful webservice to list research projects.
 */
@Path("researchProjects")
@Stateful
@RequestScoped
public class ResearchProjectResource {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @XmlRootElement
    public static class ResearchProjectData {
        public final String title;
        public final String id;
        public final String synopsis;
        public final Date modifiedDate;
        @XmlElementWrapper
        @XmlElement(name = "projectManager")
        public final List<String> projectManagers;
        @XmlElementWrapper
        @XmlElement(name = "broadPI")
        public final List<String> broadPIs;
        @XmlElementWrapper
        @XmlElement(name = "order")
        public final List<String> orders;

        // Required by JAXB.
        ResearchProjectData() {
            title = null;
            id = null;
            synopsis = null;
            projectManagers = null;
            broadPIs = null;
            orders = null;
            modifiedDate = null;
        }

        private static List<String> createUsernamesFromIds(BSPUserList bspUserList, Long[] ids) {
            List<String> names = new ArrayList<String>(ids.length);
            for (Long id : ids) {
                BspUser user = bspUserList.getById(id);
                if (user != null) {
                    String username = user.getUsername();
                    // Special case inactive users. This should only happen for PI users.
                    if (username.startsWith("inactive_user_")) {
                        username = user.getEmail();
                    }
                    names.add(username);
                } else {
                    // This can happen if a user has left the Broad.
                    names.add("(Unknown user: " + id + ")");
                }
            }
            return names;
        }

        public ResearchProjectData(BSPUserList bspUserList, ResearchProject researchProject) {
            title = researchProject.getTitle();
            id = researchProject.getJiraTicketKey();
            synopsis = researchProject.getSynopsis();
            projectManagers = createUsernamesFromIds(bspUserList, researchProject.getProjectManagers());
            broadPIs = createUsernamesFromIds(bspUserList, researchProject.getBroadPIs());
            orders = new ArrayList<String>(researchProject.getProductOrders().size());
            for (ProductOrder order : researchProject.getProductOrders()) {
                // Using the JIRA ticket key omits draft orders from the report. At this point there is no
                // requirement to expose draft orders to client of this web service. Supporting draft will
                // require adding a DAO API to map from Draft IDs to entities.
                orders.add(order.getJiraTicketKey());
            }
            modifiedDate = researchProject.getModifiedDate();
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

        public ResearchProjects(BSPUserList bspUserList, List<ResearchProject> projects) {
            this.projects = new ArrayList<ResearchProjectData>(projects.size());
            for (ResearchProject project : projects) {
                this.projects.add(new ResearchProjectData(bspUserList, project));
            }
        }
    }

    @GET
    @Path("rp/{researchProjectIds}")
    @Produces(MediaType.APPLICATION_XML)
    public ResearchProjects findByIds(@PathParam("researchProjectIds") String researchProjectIds) {
        return new ResearchProjects(bspUserList, researchProjectDao.findByJiraTicketKeys(
                Arrays.asList(researchProjectIds.split(","))));
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @return The research projects that match
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ResearchProjects findProjects(@QueryParam("withId") String projectIds,
                                         @QueryParam("withPm") String pmUserName) {
        if (!StringUtils.isBlank(projectIds)) {
            return new ResearchProjects(bspUserList, researchProjectDao.findByJiraTicketKeys(
                Arrays.asList(projectIds.split(","))));
        }
        if (!StringUtils.isBlank(pmUserName)) {
            // Find by pmUserName not yet implemented.
        }
        return new ResearchProjects(bspUserList, researchProjectDao.findAllResearchProjectsWithOrders());
    }
}
