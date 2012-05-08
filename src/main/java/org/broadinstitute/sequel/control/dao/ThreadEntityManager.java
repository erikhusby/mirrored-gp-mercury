package org.broadinstitute.sequel.control.dao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * Holds a per-thread entity manager.  This is injected into DAOs (Stateful, RequestScoped beans).  If the DAOs
 * were to acquire the entity manager themselves, then during unit tests they would each acquire a unique one,
 * because RequestScoped doesn't have correct meaning during a unit test.
 */
@Stateful
@RequestScoped
public class ThreadEntityManager {
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public EntityManager getEntityManager() {
        // todo jmt find a way to set this in the configuration
        entityManager.setFlushMode(FlushModeType.COMMIT);
        return entityManager;
    }
}
