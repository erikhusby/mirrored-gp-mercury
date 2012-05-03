package org.broadinstitute.sequel.boundary.authentication;

import org.broadinstitute.sequel.control.dao.authentication.AuthorizedGroupDao;
import org.broadinstitute.sequel.control.dao.authentication.PageAuthorizationDao;
import org.broadinstitute.sequel.entity.authentication.AuthorizedGroup;
import org.broadinstitute.sequel.entity.authentication.PageAuthorization;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * AuthenticationService is intended to support the front end Authentication and Authorization process.  Through
 * this service.
 *
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 9:44 AM
 */
@Stateless
public class AuthenticationService {

    @Inject private PageAuthorizationDao authorizationDao;
    @Inject private AuthorizedGroupDao groupDao;
    /**
     *
     * retrieveAuthorizedGroups provides callers with a way to, based on a page access path, access the authorized
     * groups for that path.  The current implementation is flexible enough to support both directory paths and full
     * page paths
     *
     *
     * @param pagePath
     * @return
     */
    public Collection<String> retrieveAuthorizedGroups(String pagePath) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);

        List<String> groupList = authorization.getGroupList();
        return groupList;
    }

    public void addNewPageAuthorization(String pagePathIn, List<String> authGroupIn) {
        PageAuthorization page = new PageAuthorization(pagePathIn);
        for(String currGroup:authGroupIn) {
            AuthorizedGroup grp = groupDao.findGroupByName(currGroup);
            page.addGroupAccess(grp);
        }

        authorizationDao.addNewPageAuthorization(page);
    }

    /**
     *
     * isPageProtected uses the authorization registrations to determine whether a page is protected.  if there is no
     * current registration for the page, the application does not assume that it needs authentication OR
     * 'authorization.
     *
     * @param pagePath
     * @return
     */
    public boolean isPageProtected(String pagePath) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);
        return (null != authorization);
    }

    public Collection<PageAuthorization> getAllAuthorizedPages() {
        return authorizationDao.getAllPageAuthorizations();
    }


    public PageAuthorization findByPage(String pagePath) {
        return authorizationDao.findPageAuthorizationByPage(pagePath);
    }

    public void addGroupsToPage(String pagePath, List<String> groupsIn) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);

        for(String currGroup:groupsIn) {
            authorization.addGroupAccess(groupDao.findGroupByName(currGroup));
        }
    }


    public Collection<String> retrieveAllGroups() {
        Collection<AuthorizedGroup> groupList = groupDao.findAllGroups();

        List<String> grpStringList = new LinkedList<String>();

        for(AuthorizedGroup group:groupList) {
            grpStringList.add(group.getGroupName());
        }

        return grpStringList;
    }


}
