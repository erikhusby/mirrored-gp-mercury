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
    private List<String> newGroupAuth = new LinkedList<String>();

    public Collection<PageAuthorization> getAllPageAuthorizations() {

        List<PageAuthorization> allPgs = new LinkedList<PageAuthorization>();
        allPgs.addAll(authSvc.getAllAuthorizedPages());
        return allPgs;
    }

    public Collection<String>  getGroupList () {


        Collection<String> fullGroupList = null;
//        String fullGroupList = "";
        if(null != pagePath) {
            Collection<String> groupList = authSvc.retrieveAuthorizedGroups(pagePath);

//            StringBuilder listBuilder = new StringBuilder();
//            for(String currGroup:groupList) {
//                if(!listBuilder.toString().isEmpty()) {
//                    listBuilder.append(", ")
//                }
//                listBuilder.append(groupList);
//            }
            fullGroupList = groupList;
        }

        return fullGroupList;
    }

    public List<String> getFullGroupList() {
        List<String> tempList = new LinkedList<String>();

        tempList.addAll(authSvc.retrieveAllGroups());

        return tempList;
    }



    public String addNewGroup() {

        authSvc.addGroupsToPage(pagePath, newGroupAuth);

        newGroupAuth = new LinkedList<String>();

        return redirect("/administration/page_admin_detail");
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public List<String> getNewGroupAuth() {
        return newGroupAuth;
    }

    public void setNewGroupAuth(List<String> newGroupAuthIn) {
        newGroupAuth = newGroupAuthIn;
    }
}
