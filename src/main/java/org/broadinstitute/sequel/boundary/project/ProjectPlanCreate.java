package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author breilly
 */
@Named
@ConversationScoped
public class ProjectPlanCreate extends AbstractJsfBean implements Serializable {

    @Inject private DB db;
    @Inject private ProjectDetail projectDetail;
    @Inject private Conversation conversation;

    private String projectName;
    private Project project;

    private String projectPlanName;
    private WorkflowDescription workflowDescription;

    public void beginAdd() {
        if (project == null) {
            project = db.findByProjectName(projectName);
            conversation.begin();
        }
    }

    public List<WorkflowDescription> getAllWorkflowDescriptions() {
        return db.getAllWorkflowDescriptions();
    }

    public String addProjectPlan() {
        ProjectPlan plan = new ProjectPlan(project, projectPlanName, workflowDescription);
        conversation.end();
        projectDetail.setProjectName(projectName);
        return redirect("detail");
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

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }
}
