package org.broadinstitute.sequel.control.dao.authentication;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.authentication.AuthorizedGroup;

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
public class AuthorizedGroupDao {


    @Inject
    private DB db;


    public AuthorizedGroup findGroupByName(String groupNameIn) {
        return db.getAuthorizedGroupMap().get(groupNameIn);
    }

    public Collection<AuthorizedGroup> findAllGroups() {
        return db.getAuthorizedGroupMap().values();
    }


}
