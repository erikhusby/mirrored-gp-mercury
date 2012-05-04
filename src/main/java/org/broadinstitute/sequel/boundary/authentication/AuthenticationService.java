package org.broadinstitute.sequel.boundary.authentication;

import org.broadinstitute.sequel.control.dao.authentication.AuthorizedRoleDao;
import org.broadinstitute.sequel.control.dao.authentication.PageAuthorizationDao;
import org.broadinstitute.sequel.entity.authentication.AuthorizedRole;
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
    @Inject private AuthorizedRoleDao roleDao;


    /**
     *
     * retrieveAuthorizedRoles provides callers with a way to, based on a page access path, access the authorized
     * roles for that path.  The current implementation is flexible enough to support both directory paths and full
     * page paths
     *
     *
     * @param pagePath
     * @return
     */
    public Collection<String> retrieveAuthorizedRoles(String pagePath) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);

        List<String> roleList = authorization.getRoleList();
        return roleList;
    }

    public void addNewPageAuthorization(String pagePathIn, List<String> authRoleIn) {
        PageAuthorization page = new PageAuthorization(pagePathIn);
        for(String currRole:authRoleIn) {
            AuthorizedRole role = roleDao.findRoleByName(currRole);
            page.addRoleAccess(role);
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

    public void addRolesToPage(String pagePath, List<String> rolesIn) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);

        for(String currRole:rolesIn) {
            authorization.addRoleAccess(roleDao.findRoleByName(currRole));
        }
    }


    public Collection<String> retrieveAllRoles() {
        Collection<AuthorizedRole> roleList = roleDao.findAllRoles();

        List<String> roleStringList = new LinkedList<String>();

        for(AuthorizedRole role:roleList) {
            roleStringList.add(role.getRoleName());
        }

        return roleStringList;
    }


}
