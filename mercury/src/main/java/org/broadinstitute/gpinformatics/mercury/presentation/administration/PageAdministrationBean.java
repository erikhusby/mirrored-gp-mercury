package org.broadinstitute.gpinformatics.mercury.presentation.administration;

import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthorizationService;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 */
@ManagedBean
@RequestScoped
public class PageAdministrationBean extends AbstractJsfBean {
    @Inject
    private AuthorizationService authSvc;
    private String pagePath;
    private List<String> newroleAuth = new LinkedList<String>();

    public Collection<PageAuthorization> getAllPageAuthorizations() {
        List<PageAuthorization> allPgs = new LinkedList<PageAuthorization>();
        allPgs.addAll(authSvc.getAllAuthorizedPages());
        return allPgs;
    }

    public Collection<String> getRoleList() {
        Collection<String> fullRoleList = null;
        if (null != pagePath) {
            Collection<String> roleList = authSvc.retrieveAuthorizedRoles(pagePath);

            fullRoleList = roleList;
        }

        return fullRoleList;
    }

    public List<String> getFullRoleList() {
        List<String> tempList = new LinkedList<String>();

        tempList.addAll(authSvc.retrieveAllRolesNames());

        return tempList;
    }

    public String addNewRole() {
        authSvc.addRolesToPage(pagePath, newroleAuth);

        newroleAuth = new LinkedList<String>();

        return redirect("/administration/page_admin_detail");
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public List<String> getNewRoleAuth() {
        return newroleAuth;
    }

    public void setNewRoleAuth(List<String> newRoleAuthIn) {
        newroleAuth = newRoleAuthIn;
    }
}
