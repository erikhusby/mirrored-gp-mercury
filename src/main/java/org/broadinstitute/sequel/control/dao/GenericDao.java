package org.broadinstitute.sequel.control.dao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Superclass for Data Access Objects.  Managed beans can't be parameterized types, so this DAO can't be typesafe.
 */
@Stateful
@RequestScoped
public class GenericDao {
    @Inject
    private ThreadEntityManager threadEntityManager;

    public ThreadEntityManager getThreadEntityManager() {
        return threadEntityManager;
    }

    public void flush() {
        this.threadEntityManager.getEntityManager().flush();
    }

    public void clear() {
        this.threadEntityManager.getEntityManager().clear();
    }

    public void persist(Object entity) {
        this.threadEntityManager.getEntityManager().persist(entity);
    }
}
