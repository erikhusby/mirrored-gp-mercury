package org.broadinstitute.gpinformatics.mercury.entity;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;
import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;

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

    private static final String DEVELOPER_ROLE = "Mercury-Developers";
    private static final String PROJECT_MANAGER_ROLE = "Mercury-ProjectManagers";
    private static final String LAB_USER_ROLE = "Mercury-LabUsers";
    private static final String LAB_MANAGER_ROLE = "Mercury-LabManagers";
    private static final long serialVersionUID = 3344014380008589366L;

    private final Map<String, Project> projects = new HashMap<String, Project>();
    private final Map<String, WorkflowDescription> workflowDescriptions = new HashMap<String, WorkflowDescription>();
    private final Map<String, PageAuthorization> pageAuthorizationMap = new HashMap<String, PageAuthorization>();
    private final Map<String, AuthorizedRole> authorizedRoleMap = new HashMap<String, AuthorizedRole>();

    public DB() {
        addWorkflowDescription(new WorkflowDescription("Hybrid Selection", null, null));
        addWorkflowDescription(new WorkflowDescription("Whole Genome Shotgun", null, null));
        initAuthorizedRoles();
        initPageAuthorizations();
    }

    // Project

    public void addProject(Project project) {
        if (StringUtils.isBlank(project.getProjectName())) {
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

    public void initPageAuthorizations() {
/* Leaving here as a code example, but we currently don't want to enforce any page authorizations.
        PageAuthorization page = new PageAuthorization("/projects/");

        page.addRoleAccess(authorizedRoleMap.get(DEVELOPER_ROLE));
        page.addRoleAccess(authorizedRoleMap.get(PROJECT_MANAGER_ROLE));
        addPageAuthorization(page);
*/
    }

    private void initAuthorizedRoles() {
        AuthorizedRole roleAll = new AuthorizedRole("All");
        addAuthorizedRole(roleAll);
        AuthorizedRole roleDev = new AuthorizedRole(DEVELOPER_ROLE);
        addAuthorizedRole(roleDev);
        AuthorizedRole rolePM = new AuthorizedRole(PROJECT_MANAGER_ROLE);
        addAuthorizedRole(rolePM);
        AuthorizedRole roleLabUser = new AuthorizedRole(LAB_USER_ROLE);
        addAuthorizedRole(roleLabUser);
        AuthorizedRole roleLabManager = new AuthorizedRole(LAB_MANAGER_ROLE);
        addAuthorizedRole(roleLabManager);

    }


    public void addAuthorizedRole(AuthorizedRole roleIn) {
        authorizedRoleMap.put(roleIn.getRoleName(), roleIn);
    }

    public void removeAuthorizedRole(AuthorizedRole roleIn) {
        authorizedRoleMap.remove(roleIn.getRoleName());
    }

    public Map<String, AuthorizedRole> getAuthorizedRoleMap() {
        return authorizedRoleMap;
    }

    public void addPageAuthorization(PageAuthorization newAuthorizationIn) {
        pageAuthorizationMap.put(newAuthorizationIn.getPagePath(), newAuthorizationIn);
    }

    public void removePageAuthorization(PageAuthorization newAuthorizationIn) {
        pageAuthorizationMap.remove(newAuthorizationIn.getPagePath());
    }

    public Map<String, PageAuthorization> getPageAuthorizationMap() {
        return pageAuthorizationMap;
    }
}
