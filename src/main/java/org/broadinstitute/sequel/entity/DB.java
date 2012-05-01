package org.broadinstitute.sequel.entity;

import org.broadinstitute.sequel.entity.authentication.AuthorizedGroup;
import org.broadinstitute.sequel.entity.authentication.PageAuthorization;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author breilly
 */
@ApplicationScoped
public class DB implements Serializable {

    private Map<String, Project> projects = new HashMap<String, Project>();
    private Map<String, WorkflowDescription> workflowDescriptions = new HashMap<String, WorkflowDescription>();
    private Map<String, PageAuthorization> pageAuthorizationMap = new HashMap<String, PageAuthorization>();
    private Map<String, AuthorizedGroup> authorizedGroupMap = new HashMap<String, AuthorizedGroup>();

    public DB() {
        addWorkflowDescription(new WorkflowDescription("Hybrid Selection", "v7.2", null, null));
        addWorkflowDescription(new WorkflowDescription("Whole Genome Shotgun", "v7.2", null, null));
        addPageAuthorization(new PageAuthorization("/projects/", "Sequel-Developers", "Sequel-ProjectManagers"));
    }

    // Project

    public void addProject(Project project) {
        if (project.getProjectName() == null || project.getProjectName().equals("")) {
            throw new IllegalArgumentException("Non-null constraint violation: Project.projectName");
        }
        if (projects.containsKey(project.getProjectName())) {
            throw new IllegalArgumentException("Unique constraint violation: Project.projectName");
        }
        projects.put(project.getProjectName(), project);
    }

    public Collection<Project> getAllProjects() {
        return Collections.unmodifiableCollection(projects.values());
    }

    public Project findByProjectName(String projectName) {
        return projects.get(projectName);
    }

    public void removeProject(String projectName) {
        projects.remove(projectName);
    }

    // WorkflowDescription

    public void addWorkflowDescription(WorkflowDescription workflowDescription) {
        if (workflowDescription.getWorkflowName() == null) {
            throw new IllegalArgumentException("Non-null constraint violation: WorkflowDescription.workflowName");
        }
        if (workflowDescriptions.containsKey(workflowDescription.getWorkflowName())) {
            throw new IllegalArgumentException("Unique constraint violation: WorkflowDescription.workflowName");
        }
        workflowDescriptions.put(workflowDescription.getWorkflowName(), workflowDescription);
    }

    public List<WorkflowDescription> getAllWorkflowDescriptions() {
        List<WorkflowDescription> result = new ArrayList<WorkflowDescription>(workflowDescriptions.values());
        Collections.sort(result, new Comparator<WorkflowDescription>() {
            @Override
            public int compare(WorkflowDescription w1, WorkflowDescription w2) {
                return w1.getWorkflowName().compareTo(w2.getWorkflowName());
            }
        });
        return result;
    }

    public WorkflowDescription findByWorkflowDescriptionName(String workflowDescriptionName) {
        return workflowDescriptions.get(workflowDescriptionName);
    }

    public void addAuthorizedGroup(AuthorizedGroup groupIn) {
        this.authorizedGroupMap.put(groupIn.getGroupName(), groupIn);
    }

    public void removeAuthorizedGroup(AuthorizedGroup groupIn) {
        this.authorizedGroupMap.remove(groupIn);
    }

    public Map<String, AuthorizedGroup> getAuthorizedGroupMap() {
        return authorizedGroupMap;
    }

    public void addPageAuthorization(PageAuthorization newAuthorizationIn) {

        this.pageAuthorizationMap.put(newAuthorizationIn.getPagePath(), newAuthorizationIn);
    }

    public void removePageAuthorization(PageAuthorization newAuthorizationIn) {
        this.pageAuthorizationMap.remove(newAuthorizationIn);
    }

    public Map<String, PageAuthorization> getPageAuthorizationMap() {
        return pageAuthorizationMap;
    }
}
