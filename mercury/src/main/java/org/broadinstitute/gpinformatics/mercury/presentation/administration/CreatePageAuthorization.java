package org.broadinstitute.gpinformatics.mercury.presentation.administration;

import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthenticationService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 5/3/12
 *         Time: 3:09 PM
 */

@ManagedBean
@RequestScoped
public class CreatePageAuthorization extends AbstractJsfBean {

    @Inject private AuthenticationService authSvc;

    public String pagePath;
    public List<String> assignedRole = new LinkedList<String>();


    public String createNewPage() {
        String direction = "/administration/page_administration.xhtml";

        if(null != pagePath) {
            authSvc.addNewPageAuthorization(pagePath, assignedRole);
        }

        return redirect(direction);

    }

    public List<String> getRoleList() {
        List<String> tempList = new LinkedList<String>();

        tempList.addAll(authSvc.retrieveAllRolesNames());

        return tempList;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public List<String> getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(List<String> assignedRoleIn) {
        assignedRole = assignedRoleIn;
    }
}
