package org.broadinstitute.gpinformatics.mercury.boundary.authentication;

import org.broadinstitute.gpinformatics.mercury.boundary.SequelServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.authentication.AuthorizedRoleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.authentication.PageAuthorizationDao;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.AuthorizedRole;
import org.broadinstitute.gpinformatics.mercury.entity.authentication.PageAuthorization;

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

        List<String> roleList = new LinkedList<String>();
        if(null != authorization){
            roleList.addAll(authorization.getRoleList());
        }
        return roleList;
    }

    /**
     *
     * addNewPageAuthorization exposes a method for callers to immediately associate a collection of roles with
     * a new page listing.
     *
     * @param pagePathIn Relative path to the presentation resource to be protected
     * @param authRoleIn Collection of Roles that are allowed to have access to the resource defined by pagePathIn
     */
    public void addNewPageAuthorization(String pagePathIn, Collection<String> authRoleIn) {

        if(null != authorizationDao.findPageAuthorizationByPage(pagePathIn)) {
            throw new SequelServiceException("This Page is already registered");
        }

        PageAuthorization page = new PageAuthorization(pagePathIn);
        for(String currRole:authRoleIn) {
            AuthorizedRole role = roleDao.findRoleByName(currRole);
            page.addRoleAccess(role);
        }

        authorizationDao.persist(page);
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

    /**
     * getAllAuthorizedPages allows callers to retrieve a list of all presentation resources that are currently
     * registered as protected resources.  If a resource comes up on this list, not only will a user be forced to be
     * logged into the system, they will also have to be in at least one of the roles that is associated with the page
     * @return Collection of {@link PageAuthorization} entities
     */
    public Collection<PageAuthorization> getAllAuthorizedPages() {
        return authorizationDao.getAllPageAuthorizations();
    }

    /**
     *
     * findByPage retrieves the registered authorization entity for a given presentation resource.  A user of this
     * method will gain access to the the defined roles associated with a registered protected resource
     *
     * @param pagePath Relative path to the presentation resource to be protected
     * @return
     */
    public PageAuthorization findByPage(String pagePath) {
        return authorizationDao.findPageAuthorizationByPage(pagePath);
    }

    /**
     *
     * addRolesToPage will retrieve the requested registered presentation resource and add the given roles to that
     * resources registration.
     *
     * @param pagePath Relative path to the presentation resource to be protected
     * @param rolesIn Additional Collection of Roles that are allowed to have access to the resource defined by
     */
    public void addRolesToPage(String pagePath, Collection<String> rolesIn) {
        PageAuthorization authorization = authorizationDao.findPageAuthorizationByPage(pagePath);

        if(null == authorization) {
            throw new SequelServiceException("This Page is not currently registered");
        }

        for(String currRole:rolesIn) {
            authorization.addRoleAccess(roleDao.findRoleByName(currRole));
        }
    }

    /**
     *
     * retrieveAllRoles allows a caller to retrieve the list of all roles that are recognized by the system.  The
     * collection returned is not limited to roles that are currently assigned to registered resources, but rather all
     * roles that may be applied to any resource.
     *
     * @return Collection of all roles that are known to the application
     */
    public Collection<String> retrieveAllRolesNames() {
        Collection<AuthorizedRole> roleList = roleDao.findAllRoles();

        List<String> roleStringList = new LinkedList<String>();

        for(AuthorizedRole role:roleList) {
            roleStringList.add(role.getRoleName());
        }

        return roleStringList;
    }

    public Collection<AuthorizedRole> retrieveAllRoles() {
        return roleDao.findAllRoles();
    }

    /**
     *
     * addNewRole allows a user to defined a new role for the system to recognize.  This action will make it possible
     * for a presentation resource to be able to be registered with the new role.
     *
     * @param roleName Name of a new role for the system to recognize
     */
    public void addNewRole(String roleName) {

        if(null != roleDao.findRoleByName(roleName)) {
            throw new SequelServiceException("This role is already registered");
        }

        AuthorizedRole newRole = new AuthorizedRole(roleName);
        roleDao.persist(newRole);

    }

    /**
     * removeRole allows a user to eliminate a previously defined role from the system.
     *
     * @param defunctRoleIn Role to be removed
     */
    public void removeRole(AuthorizedRole defunctRoleIn) {
        roleDao.removeRole(defunctRoleIn);
    }
}
