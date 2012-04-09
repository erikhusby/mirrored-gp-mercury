package org.broadinstitute.sequel.entity;

import org.broadinstitute.sequel.entity.project.Project;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author breilly
 */
@ApplicationScoped
public class DB {

    private Map<String, Project> projects = new HashMap<String, Project>();

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
}
