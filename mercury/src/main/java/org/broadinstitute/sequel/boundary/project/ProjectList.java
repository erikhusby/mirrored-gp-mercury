package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.Project;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author breilly
 */
@Model
public class ProjectList {

    @Inject private DB db;

    public Collection<Project> getAllProjects() {
        List<Project> projects = new ArrayList<Project>(db.getAllProjects());
        Collections.sort(projects, new Comparator<Project>() {
            @Override
            public int compare(Project p1, Project p2) {
                return p1.getProjectName().compareTo(p2.getProjectName());
            }
        });
        return projects;
    }
}
