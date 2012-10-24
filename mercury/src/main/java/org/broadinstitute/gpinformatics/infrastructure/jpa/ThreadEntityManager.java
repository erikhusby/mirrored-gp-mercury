package org.broadinstitute.gpinformatics.infrastructure.jpa;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
    /** To avoid LazyInitialization exceptions in JSF pages, the persistence context is extended */
    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mercury_pu")
    private EntityManager entityManager;

    /**
     * Returns an entity manager for the request-scoped extended persistence context, configured for COMMIT flush mode.
     *
     * TODO: fix documentation below once it's proven that SUPPORTS works and is the correct setting
     * The transaction attribute of NOT_SUPPORTED helps prevent the persistence context from eagerly joining any
     * currently active transaction, which would make any changes to managed entities eligible for flushing when the
     * transaction commits.
     *
     * @return the persistence context
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EntityManager getEntityManager() {
        // todo jmt find a way to set this in the configuration
        entityManager.setFlushMode(FlushModeType.COMMIT);
        return entityManager;
    }
}
