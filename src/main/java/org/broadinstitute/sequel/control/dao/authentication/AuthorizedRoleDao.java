package org.broadinstitute.sequel.control.dao.authentication;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.authentication.AuthorizedRole;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;

/**
 * @author Scott Matthews
 *         Date: 5/3/12
 *         Time: 2:40 PM
 */
@Stateful
@RequestScoped
public class AuthorizedRoleDao {


    @Inject
    private DB db;


    public AuthorizedRole findRoleByName(String roleNameIn) {
        return db.getAuthorizedRoleMap().get(roleNameIn);
    }

    public Collection<AuthorizedRole> findAllRoles() {
        return db.getAuthorizedRoleMap().values();
    }


}
