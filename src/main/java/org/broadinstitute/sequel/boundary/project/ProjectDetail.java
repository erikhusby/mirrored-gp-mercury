package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ProjectDetail extends AbstractJsfBean {

    @Inject private DB db;

    private String projectName;
    private Project project;

    public void loadProject() {
        if (project == null) {
            project = db.findByProjectName(projectName);
        }
    }

    public List<ProjectPlan> getProjectPlans() {
        return new ArrayList<ProjectPlan>(project.getProjectPlans());
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
