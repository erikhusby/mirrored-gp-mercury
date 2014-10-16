package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Restful webservice to list and create research projects.
 */
@Path("researchProjects")
@Stateful
@RequestScoped
public class ResearchProjectResource {
    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ResearchProjectEjb researchProjectEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    @XmlRootElement
    public static class ResearchProjectData {
        public final String title;

        public final String id;

        public final String synopsis;

        public final Date modifiedDate;

        public final String username;

        @XmlElementWrapper
        @XmlElement(name = "collections")
        public final List<Long> collections;

        @XmlElementWrapper
        @XmlElement(name = "projectManager")
        public final List<String> projectManagers;

        @XmlElementWrapper
        @XmlElement(name = "broadPI")
        public final List<String> broadPIs;

        @XmlElementWrapper
        @XmlElement(name = "order")
        public final List<String> orders;

        public ResearchProjectData(String title, String synopsis, String username) {
            this.title = title;
            this.synopsis = synopsis;
            this.username = username;
            id = null;
            projectManagers = null;
            collections = null;
            broadPIs = null;
            orders = null;
            modifiedDate = null;
        }

        // Required by JAXB.
        ResearchProjectData() {
            title = null;
            synopsis = null;
            username = null;
            id = null;
            projectManagers = null;
            collections = null;
            broadPIs = null;
            orders = null;
            modifiedDate = null;
        }

        private static List<String> createUsernamesFromIds(BSPUserList bspUserList, Long[] ids) {
            List<String> names = new ArrayList<>(ids.length);
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

        /**
         *
         * @param researchProject the research project and its associated data
         * @return a list of cohort Ids
         */

        private List<Long> createCollections(ResearchProject researchProject) {
            List<Long> collectionIds = new ArrayList<>(researchProject.getCohorts().length);
            for (ResearchProjectCohort researchProjectCohort : researchProject.getCohorts()) {
                collectionIds.add(researchProjectCohort.getDatabaseId());
            }
            return collectionIds;
        }

        public ResearchProjectData(BSPUserList bspUserList, ResearchProject researchProject) {
            title = researchProject.getTitle();
            id = researchProject.getJiraTicketKey();
            synopsis = researchProject.getSynopsis();
            projectManagers = createUsernamesFromIds(bspUserList, researchProject.getProjectManagers());
            collections = createCollections(researchProject);
            broadPIs = createUsernamesFromIds(bspUserList, researchProject.getBroadPIs());
            orders = new ArrayList<>(researchProject.getProductOrders().size());
            BspUser user = bspUserList.getById(researchProject.getCreatedBy());

            if (user != null) {
                username = user.getUsername();
            } else {
                username = null;
            }

            for (ProductOrder order : researchProject.getProductOrders()) {
                // We omit draft orders from the report. At this point there is no requirement to expose draft
                // orders to client of this web service.
                if (!order.isDraft()) {
                    orders.add(order.getJiraTicketKey());
                }
            }

            modifiedDate = researchProject.getModifiedDate();
        }
    }

    @XmlRootElement
    public static class ResearchProjects {
        @XmlElement(name = "project")
        public final List<ResearchProjectData> projects;

        public ResearchProjects() {
            projects = null;
        }

        public ResearchProjects(BSPUserList bspUserList, List<ResearchProject> projects) {
            this.projects = new ArrayList<>(projects.size());
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
     * <p/>
     * Only one filter can be applied at a time. If no filters are provided, all research projects are returned.
     *
     * @param projectIds  one or more RP IDs, separated by commas
     * @param pmUserNames one or more PM user names, separated by commas. Names must match the user name in BSP.
     *
     * @return The research projects that match
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ResearchProjects findProjects(@QueryParam("withId") String projectIds,
                                         @QueryParam("withPm") String pmUserNames) {
        List<ResearchProject> projects;
        if (!StringUtils.isBlank(projectIds)) {
            projects = researchProjectDao.findByJiraTicketKeys(Arrays.asList(projectIds.split(",")));
        } else if (!StringUtils.isBlank(pmUserNames)) {
            String[] userNames = pmUserNames.split(",");
            Long[] ids = new Long[userNames.length];
            for (int i = 0; i < userNames.length; i++) {
                BspUser bspUser = bspUserList.getByUsername(userNames[i]);
                if (bspUser == null) {
                    throw new InformaticsServiceException("No user name found for " + userNames[i]);
                }
                ids[i] = bspUser.getUserId();
            }
            projects = researchProjectDao.findByProjectManagerIds(ids);
        } else {
            projects = researchProjectDao.findAllResearchProjectsWithOrders();
        }
        return new ResearchProjects(bspUserList, projects);
    }

    /**
     * Method to create a new research project with the given title and synopsis.
     *
     * @param data Object containing the relevant information needed to create a research project.
     *
     * @return Returns the research project data for the project created.
     */
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public ResearchProjectData create(@Nonnull ResearchProjectData data) {
        if (researchProjectDao.findByTitle(data.title) != null) {
            throw new ResourceException(
                    "Cannot create a project that already exists - the project name is already being used.",
                    Response.Status.BAD_REQUEST);
        }

        userBean.login(data.username);

        if(userBean.getBspUser() == UserBean.UNKNOWN) {
            throw new ResourceException("A valid Username is required to complete this request",
                    Response.Status.UNAUTHORIZED);
        }

        ResearchProject project = new ResearchProject(null, data.title, data.synopsis, false,
                                                      ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        BspUser user = userBean.getBspUser();
        project.setCreatedBy(user.getUserId());

        try {
            researchProjectEjb.submitToJira(project);
        } catch (IOException | NoJiraTransitionException e) {
            throw new InformaticsServiceException(
                    String.format("Could not submit new research project to JIRA (%s)", e.getMessage()));
        }

        // Save and flush to persist and make sure all DB constraints are met.
        researchProjectDao.persist(project);
        researchProjectDao.flush();

        return new ResearchProjectData(bspUserList, project);
    }

    /**
     * This method searches for research projects by either title or JIRA ticket key based on a search term.
     * This executes a like search and is case insensitive.
     *
     * @param searchTerm The term to search by.
     *
     * @return ResearchProjects object that contains all of the matching projects.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("match/{searchTerm}")
    public ResearchProjects getResearchProjectsLike(@PathParam("searchTerm") String searchTerm) {
        // Since the search term could be either title or JIRA id we search both.
        List<ResearchProject> foundProjects = researchProjectDao.findLikeTitle(searchTerm);
        foundProjects.addAll(researchProjectDao.findLikeJiraTicketKey(searchTerm));

        return new ResearchProjects(bspUserList, foundProjects);
    }
}
