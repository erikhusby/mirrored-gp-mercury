package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ProjectPlanForm extends AbstractJsfBean implements Serializable {

//    @Inject private DB db;
    @Inject private ProjectDetail projectDetail;

    private String projectPlanName;
    private WorkflowDescription workflowDescription;

    public void loadData() {
        projectDetail.loadProject();
    }

    public String create() {
        projectDetail.loadProject();

        try {
            ProjectPlan plan = new ProjectPlan(projectDetail.getProject(), projectPlanName, workflowDescription);
        } catch (Exception e) {

            // should probably not be using FacesContext in a boundary object
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }

        // "detail" refers to a JSF view, which probably doesn't belong in a boundary object
        return redirect("detail");
    }

    public String getProjectPlanName() {
        return projectPlanName;
    }

    public void setProjectPlanName(String projectPlanName) {
        this.projectPlanName = projectPlanName;
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }
}
