package org.broadinstitute.gpinformatics.mercury.entity;

import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author breilly
 */
@ApplicationScoped
public class DB implements Serializable {

    public enum Role {
        Developer("Mercury-Developers"),
        PM("Mercury-ProjectManagers"),
        PDM("Mercury-ProductManagers"),
        LabUser("Mercury-LabUsers"),
        LabManager("Mercury-LabManagers"),
        BillingManager("Mercury-BillingManagers"),
        PipelineManager("Mercury-PipelineAdmins"),
        All("All");

        public final String name;

        Role(String name) {
            this.name = name;
        }
    }

    private static final long serialVersionUID = 3344014380008589366L;

    private final Map<String, PageAuthorization> pageAuthorizationMap = new HashMap<String, PageAuthorization>();
    private final Map<String, AuthorizedRole> authorizedRoleMap = new HashMap<String, AuthorizedRole>();

    public DB() {
        initAuthorizedRoles();
        initPageAuthorizations();
    }

    public void initPageAuthorizations() {
        PageAuthorization page = new PageAuthorization("/products/create.xhtml");

        page.addRoleAccess(authorizedRoleMap.get(Role.Developer.name));
        page.addRoleAccess(authorizedRoleMap.get(Role.PDM.name));
        addPageAuthorization(page);
    }

    private void initAuthorizedRoles() {
        for (Role role : Role.values()) {
            addAuthorizedRole(new AuthorizedRole(role.name));
        }
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

    /**
     * Return an array of the Role names for the supplied roles.
     *
     * @param roles whose names are to be extracted.
     *
     * @return array of comma delimited role names.
     */
    public static String[] roles(@Nonnull Role... roles) {
        String[] roleNames = new String[roles.length];

        for (int i = 0; i < roles.length; i++) {
            roleNames[i] = roles[i].name;
        }

        return roleNames;
    }
}
