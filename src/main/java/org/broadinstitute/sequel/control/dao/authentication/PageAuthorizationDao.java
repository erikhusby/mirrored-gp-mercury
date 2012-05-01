package org.broadinstitute.sequel.control.dao.authentication;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.authentication.PageAuthorization;
import org.broadinstitute.sequel.entity.labevent.LabEvent;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 4:14 PM
 */

@Stateful
@RequestScoped
public class PageAuthorizationDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;


    @Inject
    private DB db;


    public void persist(PageAuthorization pageAuthorizationIn)  {
        // todo SGM implement
    }

    public PageAuthorization findPageAuthorizationByPage(String pageNameIn) {
        return db.getPageAuthorizationMap().get(pageNameIn);
    }

    public void addNewPageAuthorization(PageAuthorization pageAuthorizationIn) {
        db.addPageAuthorization(pageAuthorizationIn);
    }

    public void removePageAuthorization(String pageNameIn) {
        db.removePageAuthorization(findPageAuthorizationByPage(pageNameIn));
    }

}
