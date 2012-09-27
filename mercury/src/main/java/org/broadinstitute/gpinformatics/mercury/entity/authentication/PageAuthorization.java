package org.broadinstitute.gpinformatics.mercury.entity.authentication;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * PageAuthorization is an entity class
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:02 PM
 */
public class PageAuthorization {

    private Long authorizationId;
    private String pagePath;
    private List<AuthorizedRole> roleAccess = new LinkedList<AuthorizedRole>();

    public PageAuthorization(String pagePathIn) {
        setPagePath(pagePathIn);
    }

    public List<String> getRoleList() {
        List<String> authorizedRoles = new LinkedList<String>();

        for(AuthorizedRole currRole: this.getRoleAccess()) {
            authorizedRoles.add(currRole.getRoleName());
        }
        return authorizedRoles;
    }

    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationIdIn) {
        authorizationId = authorizationIdIn;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public List<AuthorizedRole> getRoleAccess() {
        return roleAccess;
    }

    public void setRoleAccess(List<AuthorizedRole> roleAccessIn) {
        roleAccess = roleAccessIn;
    }

    public void addRoleAccess(AuthorizedRole newRoleIn) {
        this.roleAccess.add(newRoleIn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PageAuthorization))
            return false;

        PageAuthorization that = (PageAuthorization) o;

        if (!pagePath.equals(that.pagePath))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pagePath.hashCode();
    }
}
