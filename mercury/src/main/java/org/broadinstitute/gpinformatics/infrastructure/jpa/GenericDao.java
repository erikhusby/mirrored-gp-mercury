package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.exception.ConstraintViolationException;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for Data Access Objects. Makes use of a request-scoped extended persistence context. Scoped session beans
 * can't be parameterized types (JSR-299 3.2), so this DAO can't be type-safe.
 *
 * Transaction is SUPPORTS so as to apply to all find methods to let them see any currently active transaction but not
 * begin, and therefore commit (along with any changes queued up in the persistence context), their own transaction.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class GenericDao {

    @Inject
    private ThreadEntityManager threadEntityManager;

    // TODO: replace usages of this method with getEntityManager(), then remove this method
    public ThreadEntityManager getThreadEntityManager() {
        return threadEntityManager;
    }

    /**
     * Flushes changes in the extended persistence context to the database.
     *
     * Transaction is MANDATORY because flush does not make sense outside the context of a transaction.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void flush() {
        getEntityManager().flush();
    }

    /**
     * Clears the extended persistence context causing all managed entities to become detached.
     *
     * Transaction is SUPPORTS (default).
     */
    public void clear() {
        getEntityManager().clear();
    }

    /**
     * Adds the given object as a managed entity in the extended persistence context.
     *
     * Transaction is REQUIRED for write operations, but wider transactions can still be used for larger units of work
     *
     * @param entity    the entity to persist
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(Object entity) {
        getEntityManager().persist(entity);
    }

    /**
     * Adds the given objects as a managed entities in the extended persistence context.
     *
     * Transaction is REQUIRED for write operations, but wider transactions can still be used for larger units of work
     *
     * @param entities  the entities to persist
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistAll(List<?> entities) {
        EntityManager entityManager = threadEntityManager.getEntityManager();
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    /**
     * Marks the given entity for removal from the underlying data store.
     *
     * Transaction is REQUIRED for write operations, but wider transactions can still be used for larger units of work
     *
     * @param entity    the entity to remove
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(Object entity) {
        getEntityManager().remove(entity);
    }

    /**
     * Returns an entity manager for the request-scoped extended persistence context.
     *
     * Transaction is SUPPORTS (default).
     *
     * @return the persistence context
     * @see org.broadinstitute.gpinformatics.infrastructure.jpa.ThreadEntityManager#getEntityManager()
     */
    public EntityManager getEntityManager() {
        return threadEntityManager.getEntityManager();
    }

    /**
     * Returns a criteria builder for the request-scoped extended persistence context.
     *
     * Transaction is SUPPORTS (default).
     *
     * @return the criteria builder
     */
    protected CriteriaBuilder getCriteriaBuilder() {
        return getEntityManager().getCriteriaBuilder();
    }

    /**
     * Returns all entities of the specified entity type.
     * @param entity the class of entity to return
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities, or empty list if none found
     */
    public <ENTITY_TYPE> List<ENTITY_TYPE> findAll(Class<ENTITY_TYPE> entity) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        criteriaQuery.from(entity);
        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns all entities of the specified entity type in the specified range.
     * @param entity the class of entity to return
     * @param first index to start at
     * @param max maximum number of entities to return
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities, or empty list if none found
     */
    public <ENTITY_TYPE> List<ENTITY_TYPE> findAll(Class<ENTITY_TYPE> entity, int first, int max) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        TypedQuery<ENTITY_TYPE> typedQuery = getEntityManager().createQuery(criteriaQuery);
        typedQuery.setFirstResult(first);
        typedQuery.setMaxResults(max);
        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns a single entity that matches a specified value for a specified property.
     * @param entity the class of entity to return
     * @param singularAttribute the metadata field for the property to query
     * @param value the value to query
     * @param <VALUE_TYPE> the type of the value in the query, e.g. String
     * @param <METADATA_TYPE> the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                       there is inheritance
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return entity that matches the value, or null if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> ENTITY_TYPE findSingle(
            Class<ENTITY_TYPE> entity, SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, VALUE_TYPE value) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);
        criteriaQuery.where(criteriaBuilder.equal(root.get(singularAttribute), value));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * Returns a list of entities that matches a list of values for a specified property.
     * @param entity the class of entity to return
     * @param singularAttribute the metadata field for the property to query
     * @param values list of values to query
     * @param <VALUE_TYPE> the type of the value in the query, e.g. String
     * @param <METADATA_TYPE> the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                       there is inheritance
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities that match the value, or empty list if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> List<ENTITY_TYPE> findListByList(
            Class<ENTITY_TYPE> entity, SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, List<VALUE_TYPE> values) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);

        // Break the list into chunks of 1000, because of the limit on the number of items in
        // an Oracle IN clause
        List<ENTITY_TYPE> resultList = new ArrayList<ENTITY_TYPE>();
        for(int i = 0; i < values.size(); i += 1000) {
            criteriaQuery.where(root.get(singularAttribute).in(values.subList(i, Math.min(values.size(), i + 1000))));
            try {
                resultList.addAll(getEntityManager().createQuery(criteriaQuery).getResultList());
            } catch (NoResultException ignored) {
                return resultList;
            }
        }
        return resultList;
    }

    /**
     * Returns a list of entities that matches a specified value for a specified property.
     * @param entity the class of entity to return
     * @param singularAttribute the metadata field for the property to query
     * @param value the value to query
     * @param <VALUE_TYPE> the type of the value in the query, e.g. String
     * @param <METADATA_TYPE> the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                       there is inheritance
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities that match the value, or empty list if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> List<ENTITY_TYPE> findList(
            Class<ENTITY_TYPE> entity, SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, VALUE_TYPE value) {
        return findListByList(entity, singularAttribute, Collections.singletonList(value));
    }

    public static boolean IsConstraintViolationException(final Exception e) {

        Throwable currentCause = e;
        while (currentCause != null) {
            if (currentCause instanceof ConstraintViolationException) {
                return true;
            }
            currentCause = currentCause.getCause();
        }

        return false;
    }

}
