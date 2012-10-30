package org.broadinstitute.gpinformatics.mercury.entity;

import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.util.HashMap;
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

//    private final Map<String, Project> projects = new HashMap<String, Project>();
    private final Map<String, PageAuthorization> pageAuthorizationMap = new HashMap<String, PageAuthorization>();
    private final Map<String, AuthorizedRole> authorizedRoleMap = new HashMap<String, AuthorizedRole>();

    public DB() {
        initAuthorizedRoles();
        initPageAuthorizations();
    }

    // Project

//    public void addProject(Project project) {
//        if (StringUtils.isBlank(project.getProjectName())) {
//            throw new IllegalArgumentException("Non-null constraint violation: Project.projectName");
//        }
//        if (projects.containsKey(project.getProjectName())) {
//            throw new IllegalArgumentException("Unique constraint violation: Project.projectName");
//        }
//        projects.put(project.getProjectName(), project);
//    }

//    public Collection<Project> getAllProjects() {
//        return Collections.unmodifiableCollection(projects.values());
//    }
//
//    public Project findByProjectName(String projectName) {
//        return projects.get(projectName);
//    }
//
//    public void removeProject(String projectName) {
//        projects.remove(projectName);
//    }

    // WorkflowDescription

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
