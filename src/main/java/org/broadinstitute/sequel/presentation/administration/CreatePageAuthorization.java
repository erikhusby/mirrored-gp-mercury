package org.broadinstitute.sequel.presentation.administration;

import org.broadinstitute.sequel.boundary.authentication.AuthenticationService;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

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
    public List<String> assignedGroup = new LinkedList<String>();


    public String createNewPage() {

        String direction = "/administration/page_administration.xhtml";

        if(null != pagePath) {

            authSvc.addNewPageAuthorization(pagePath, assignedGroup);
        }

        return redirect(direction);

    }

    public List<String> getGroupList() {
        List<String> tempList = new LinkedList<String>();

        tempList.addAll(authSvc.retrieveAllGroups());

        return tempList;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public List<String> getAssignedGroup() {
        return assignedGroup;
    }

    public void setAssignedGroup(List<String> assignedGroupIn) {
        assignedGroup = assignedGroupIn;
    }
}
