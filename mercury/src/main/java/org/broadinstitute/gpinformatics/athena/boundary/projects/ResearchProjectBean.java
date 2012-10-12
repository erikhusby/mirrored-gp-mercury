package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boundary bean for working with research projects.
 *
 * @author breilly
 */
@Named
@RequestScoped
//@ViewAccessScoped
//@ConversationScoped
public class ResearchProjectBean implements Serializable {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    /** All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped). */
    private List<ResearchProject> allResearchProjects;

    private List<ResearchProject> filteredResearchProjects;

    /**
     * Returns a list of all research projects. Only actually fetches the list from the database once per request
     * (as a result of this bean being @RequestScoped).
     *
     * @return list of all research projects
     */
    public List<ResearchProject> getAllResearchProjects() {
        if (allResearchProjects == null) {
            allResearchProjects = researchProjectDao.findAllResearchProjects();
        }
        return allResearchProjects;
    }

    /**
     * Returns a list of SelectItems for all people who are owners of research projects.
     *
     * @return list of research project owners
     */
    public List<SelectItem> getAllProjectOwners() {
        Set<BspUser> owners = new HashSet<BspUser>();
        for (ResearchProject project : getAllResearchProjects()) {
            Long createdBy = project.getCreatedBy();
            BspUser bspUser = bspUserList.getById(createdBy);
            if (bspUser != null) {
                owners.add(bspUser);
            }
        }

        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (BspUser owner : owners) {
            items.add(new SelectItem(owner.getUserId(), owner.getFirstName() + " " + owner.getLastName()));
        }
        return items;
    }

    /**
     * Returns a list of SelectItems for all research project statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllProjectStatuses() {
        return BoundaryUtils.buildEnumFilterList(ResearchProject.Status.values());
    }

    public List<ResearchProject> getFilteredResearchProjects() {
        return filteredResearchProjects;
    }

    public void setFilteredResearchProjects(List<ResearchProject> filteredResearchProjects) {
        this.filteredResearchProjects = filteredResearchProjects;
    }
}
