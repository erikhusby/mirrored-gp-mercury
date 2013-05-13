package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
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
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Web service to provide research projects and ACL data for BASS.
 * <p/>
 * BASS has a specific set of operations they need supported:
 * <ul>
 * <li>Retrieve a list of research projects for a given user<p/>
 * INPUT: A user name. This is the identical to the Crowd user name<p/>
 * OUTPUT: An array of research project ID/name/protected status tuples</li>
 * <li>Retrieve a single research project by ID<p/>
 * INPUT: A research project ID<p/>
 * OUTPUT: A single research project ID/name/protected status tuple</li>
 * <li>Retrieve a single research project by name<p/>
 * INPUT: A research project name<p/>
 * OUTPUT: A single research project ID/name/protected status tuple</li>
 * <li>Retrieve all research projects<p/>
 * INPUT: none<p/>
 * OUTPUT: An array of research project ID/name/protected status tuples</li>
 * </ul>
 */
@Path("researchProjectsForBass")
@Stateful
@RequestScoped
public class ResearchProjectResourceForBass {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @XmlRootElement
    public static class ResearchProjectData {
        public final String name;
        public final String id;
        public final boolean isProtected;

        // Required by JAXB.
        ResearchProjectData() {
            name = null;
            id = null;
            isProtected = false;
        }

        public ResearchProjectData(ResearchProject researchProject) {
            name = researchProject.getTitle();
            id = researchProject.getJiraTicketKey();
            isProtected = researchProject.isAccessControlEnabled();
        }
    }

    @XmlRootElement
    public static class ResearchProjects {

        @XmlElement(name = "project")
        public final List<ResearchProjectData> projects;

        public ResearchProjects() {
            projects = null;
        }

        public ResearchProjects(Collection<ResearchProject> projects) {
            this.projects = new ArrayList<ResearchProjectData>(projects.size());
            for (ResearchProject project : projects) {
                this.projects.add(new ResearchProjectData(project));
            }
        }
    }

    @GET
    @Path("rp")
    @Produces(MediaType.APPLICATION_JSON)
    public ResearchProjects getAllProjects() {
        return new ResearchProjects(researchProjectDao.findAllResearchProjects());
    }

    @GET
    @Path("rp/{researchProjectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResearchProjectData findById(@PathParam("researchProjectId") String researchProjectId) {
        return new ResearchProjectData(researchProjectDao.findByJiraTicketKey(researchProjectId));
    }

    @GET
    @Path("name/{researchProjectName}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResearchProjectData findByName(@PathParam("researchProjectName") String name) {
        return new ResearchProjectData(researchProjectDao.findByTitle(name));
    }

    /**
     * Method to GET the list of research projects visible to a user.
     *
     * @param userName a user name
     * @return The research projects that this user has access to.
     */
    @GET
    @Path("user/{userName}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResearchProjects findProjectsByUserName(@PathParam("userName") String userName) {
        Collection<ResearchProject> projects = Collections.emptyList();
        if (!StringUtils.isBlank(userName)) {
            BspUser bspUser = bspUserList.getByUsername(userName);
            if (bspUser == null) {
                throw new RuntimeException("No user name found for " + userName);
            }
            projects = researchProjectDao.findAllAccessibleByUser(bspUser.getUserId());
        }
        return new ResearchProjects(projects);
    }
}
