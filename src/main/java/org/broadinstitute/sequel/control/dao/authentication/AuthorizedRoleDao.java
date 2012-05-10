package org.broadinstitute.sequel.control.dao.authentication;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.authentication.AuthorizedRole;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;

/**
 *
 * AuthorizedRoleDao provides boundary objects with a mechanism to access, insert and manipulate {@link AuthorizedRole}s
 * defined within the system
 *
 * @author Scott Matthews
 *         Date: 5/3/12
 *         Time: 2:40 PM
 */
@Stateful
@RequestScoped
public class AuthorizedRoleDao {


    @Inject
    private DB db;

    /**
     *
     * findRoleByName allows boundary objects to find an existing {@link AuthorizedRole} based on the unique
     * role name
     *
     * @param roleNameIn Unique name of the role to be found
     * @return A pre defined {@link AuthorizedRole} object related to the name given
     */
    public AuthorizedRole findRoleByName(String roleNameIn) {
        return db.getAuthorizedRoleMap().get(roleNameIn);
    }

    /**
     * findAllRoles will return all previously defined {@link AuthorizedRole}s within the system
     *
     * @return A {@link Collection} of all possible {@link AuthorizedRole}s available
     */
    public Collection<AuthorizedRole> findAllRoles() {
        return db.getAuthorizedRoleMap().values();
    }

    /**
     *
     * persist saves a newly defined {@link AuthorizedRole} to the system
     *
     * @param newRoleIn Newly defined {@link AuthorizedRole} to be saved to the system
     */
    public void persist(AuthorizedRole newRoleIn) {
        db.addAuthorizedRole(newRoleIn);
    }

    /**
     *
     * removeRole removes a previously defined {@link AuthorizedRole} instance from the system
     *
     * @param defunctRoleIn Previously defined role to be removed from the system.
     */
    public void removeRole(AuthorizedRole defunctRoleIn) {
        db.removeAuthorizedRole(defunctRoleIn);
    }
}
