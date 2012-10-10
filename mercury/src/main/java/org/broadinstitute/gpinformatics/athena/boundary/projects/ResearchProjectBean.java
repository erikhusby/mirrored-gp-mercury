package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
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
public class ResearchProjectBean {

    @Inject
    private ResearchProjectDao researchProjectDao;

    /** All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped). */
    private List<ResearchProject> allResearchProjects;

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
        Set<Long> owners = new HashSet<Long>();
        for (ResearchProject project : getAllResearchProjects()) {
            owners.add(project.getCreatedBy());
        }

        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (Long owner : owners) {
            items.add(new SelectItem(owner));
        }
        return items;
    }

    /**
     * Returns a list of SelectItems for all research project statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllProjectStatuses() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (ResearchProject.Status status : ResearchProject.Status.values()) {
            items.add(new SelectItem(status.name()));
        }
        return items;
    }

}
