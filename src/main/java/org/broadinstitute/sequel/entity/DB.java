package org.broadinstitute.sequel.entity;

import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;

import javax.enterprise.context.ApplicationScoped;
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
public class DB {

    private Map<String, Project> projects = new HashMap<String, Project>();
    private Map<String, WorkflowDescription> workflowDescriptions = new HashMap<String, WorkflowDescription>();

    public DB() {
        addWorkflowDescription(new WorkflowDescription("Hybrid Selection", "v7.2", null, null));
        addWorkflowDescription(new WorkflowDescription("Whole Genome Shotgun", "v7.2", null, null));
    }

    // Project

    public void addProject(Project project) {
        if (project.getProjectName() == null) {
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
}
