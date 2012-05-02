package org.broadinstitute.sequel.boundary.authentication;

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

        List<String> groupList = null;
        if(null != authorization) {

            groupList = authorization.getGroupList();
        }

        return groupList;
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



}
