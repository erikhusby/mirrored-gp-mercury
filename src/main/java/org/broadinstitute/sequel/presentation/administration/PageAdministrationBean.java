package org.broadinstitute.sequel.presentation.administration;

import org.broadinstitute.sequel.boundary.authentication.AuthenticationService;
import org.broadinstitute.sequel.entity.authentication.PageAuthorization;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 5/3/12
 *         Time: 1:29 PM
 */
@ManagedBean
@RequestScoped
public class PageAdministrationBean extends AbstractJsfBean {

    @Inject
    private AuthenticationService authSvc;
    private String pagePath;
    private List<String> newroleAuth = new LinkedList<String>();

    public Collection<PageAuthorization> getAllPageAuthorizations() {

        List<PageAuthorization> allPgs = new LinkedList<PageAuthorization>();
        allPgs.addAll(authSvc.getAllAuthorizedPages());
        return allPgs;
    }

    public Collection<String> getRoleList() {

        Collection<String> fullRoleList = null;
        if(null != pagePath) {
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
