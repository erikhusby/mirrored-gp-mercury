package org.broadinstitute.sequel.entity.authentication;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:02 PM
 */
public class PageAuthorization {

    private Long authorizationId;
    private String pagePath;
    private List<AuthorizedGroup> groupAccess = new LinkedList<AuthorizedGroup>();

    public PageAuthorization(String pagePathIn) {
        pagePath = pagePathIn;
    }

    public PageAuthorization(String pagePathIn, String ... groupsIn) {
        this(pagePathIn);
        for(String currGrp:groupsIn) {
            groupAccess.add(new AuthorizedGroup(currGrp));
        }
    }


    public List<String> getGroupList() {
        List<String> authorizedGroups = new LinkedList<String>();

        for(AuthorizedGroup currGroup: this.getGroupAccess()) {
            authorizedGroups.add(currGroup.getGroupName());
        }
        return authorizedGroups;
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

    public List<AuthorizedGroup> getGroupAccess() {
        return groupAccess;
    }

    public void setGroupAccess(List<AuthorizedGroup> groupAccessIn) {
        groupAccess = groupAccessIn;
    }

    public void addGroupAccess(AuthorizedGroup newGroupIn) {
        this.groupAccess.add(newGroupIn);
    }
}
