package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.Project;

import javax.enterprise.inject.Model;
import javax.inject.Inject;

/**
 * @author breilly
 */
@Model
public class ProjectPlanCreate {

    @Inject private DB db;

    private String projectName;
    private Project project;

    private String projectPlanName;

    public void loadProject() {
        project = db.findByProjectName(projectName);
    }

    public String addProjectPlan() {
        return null;
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

    public String getProjectPlanName() {
        return projectPlanName;
    }

    public void setProjectPlanName(String projectPlanName) {
        this.projectPlanName = projectPlanName;
    }
}
