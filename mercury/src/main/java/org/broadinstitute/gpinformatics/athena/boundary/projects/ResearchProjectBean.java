package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;

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

    @Inject
    private BSPCohortList cohortList;

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
            if (createdBy != null) {
                BspUser bspUser = bspUserList.getById(createdBy);
                if (bspUser != null) {
                    owners.add(bspUser);
                }
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
     * Used for auto-complete in the UI, given a search term
     * @param search list of search terms, whitespace separated. If more than one term is present, all terms must
     *               match a substring in the text. Search is case insensitive.
     */
    // FIXME: refactor for common cases
    public List<ResearchProject> getProjectCompletions(String search) {
        List<ResearchProject> list = new ArrayList<ResearchProject>(getAllResearchProjects());
        String[] searchStrings = search.toLowerCase().split("\\s");

        Iterator<ResearchProject> iterator = list.iterator();
        while (iterator.hasNext()) {
            ResearchProject project = iterator.next();
            if (project.getTitle() != null) {
                String label = project.getTitle().toLowerCase();
                for (String s : searchStrings) {
                    if (!label.contains(s)) {
                        iterator.remove();
                        break;
                    }
                }
            } else {
                iterator.remove();
            }
        }

        return list;
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
