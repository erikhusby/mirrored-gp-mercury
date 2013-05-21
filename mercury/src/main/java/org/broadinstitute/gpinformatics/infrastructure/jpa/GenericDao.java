package org.broadinstitute.gpinformatics.infrastructure.jpa;

import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * Interface for callbacks that want to specify fetches from the specified {@link Root}, make the query distinct,
     * etc.
     *
     * @param <ENTITY_TYPE>
     */
    public interface GenericDaoCallback<ENTITY_TYPE> {
        void callback(CriteriaQuery<ENTITY_TYPE> criteriaQuery, Root<ENTITY_TYPE> root);
    }


    @Inject
    private ThreadEntityManager threadEntityManager;

    /**
     * Flushes changes in the extended persistence context to the database.
     *
     * Transaction is MANDATORY because flush does not make sense outside the context of a transaction.
     * MLC changing this to REQUIRED as it does make sense outside a transaction for Arquillian tests.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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
     *
     * @param entity the class of entity to return
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities, or empty list if none found
     */
    public <ENTITY_TYPE> List<ENTITY_TYPE> findAll(Class<ENTITY_TYPE> entity) {
        return findAll(entity, null);
    }

    /**
    * Returns all entities of the specified entity type.
     *
    * @param entity the class of entity to return
    * @param <ENTITY_TYPE> the type of the entity to return
    * @return list of entities, or empty list if none found
    */
    public <ENTITY_TYPE> List<ENTITY_TYPE> findAll(Class<ENTITY_TYPE> entity, @Nullable GenericDaoCallback<ENTITY_TYPE> callback) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);

        if (callback != null) {
            callback.callback(criteriaQuery, root);
        }

        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns all entities of the specified entity type in the specified range.
     *
     * @param entity the class of entity to return
     * @param first index to start at
     * @param max maximum number of entities to return
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities, or empty list if none found
     */
    public <ENTITY_TYPE> List<ENTITY_TYPE> findAll(Class<ENTITY_TYPE> entity, int first, int max) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> from = criteriaQuery.from(entity);
        CriteriaQuery<ENTITY_TYPE> select = criteriaQuery.select(from);
        TypedQuery<ENTITY_TYPE> typedQuery = getEntityManager().createQuery(select);
        typedQuery.setFirstResult(first);
        typedQuery.setMaxResults(max);
        try {
            return typedQuery.getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }


    /**
     * Returns a single entity that matches a specified value for a specified property.
     *
     * @param entity the class of entity to return
     * @param singularAttribute the metadata field for the property to query
     * @param value the value to query
     * @param <VALUE_TYPE> the type of the value in the query, e.g. String
     * @param <METADATA_TYPE> the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                       there is inheritance
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return entity that matches the value, or null if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE>
    ENTITY_TYPE findSingle(Class<ENTITY_TYPE> entity,
                           SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, VALUE_TYPE value) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);

        Predicate predicate;
        if (value == null) {
            predicate = criteriaBuilder.isNull(root.get(singularAttribute));
        }
        else {
            predicate = criteriaBuilder.equal(root.get(singularAttribute), value);
        }
        criteriaQuery.where(predicate);

        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * Returns a single entity that matches a specified value for a specified property.
     *
     * @param entity             the class of entity to return
     * @param genericDaoCallback optional callback to add fetches to the specified {@link Root}
     * @param <METADATA_TYPE>    the type on which the property is defined, this can be different from the ENTITY_TYPE
     *                           if
     *                           there is inheritance
     * @param <ENTITY_TYPE>      the type of the entity to return
     *
     * @return entity that matches the value, or null if not found
     */
    public <METADATA_TYPE,
            ENTITY_TYPE extends METADATA_TYPE> ENTITY_TYPE findSingle(Class<ENTITY_TYPE> entity,
                                                                      @Nullable GenericDaoCallback<ENTITY_TYPE> genericDaoCallback) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);

        if (genericDaoCallback != null) {
            genericDaoCallback.callback(criteriaQuery, root);
        }

        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * Returns a list of entities that matches a list of values for a specified property.
     *
     * @param entity the class of entity to return
     * @param singularAttribute the metadata field for the property to query
     * @param values list of values to query
     * @param genericDaoCallback optional callback to add fetches to the specified {@link Root}
     * @param <VALUE_TYPE> the type of the value in the query, e.g. String
     * @param <METADATA_TYPE> the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                       there is inheritance
     * @param <ENTITY_TYPE> the type of the entity to return
     * @return list of entities that match the value, or empty list if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> List<ENTITY_TYPE> findListByList(
            Class<ENTITY_TYPE> entity,
            final SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute,
            List<VALUE_TYPE> values,
            @Nullable GenericDaoCallback<ENTITY_TYPE> genericDaoCallback) {

        List<ENTITY_TYPE> resultList = new ArrayList<ENTITY_TYPE>();
        if(values.isEmpty()) {
            return resultList;
        }

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        final Root<ENTITY_TYPE> root = criteriaQuery.from(entity);

        if (genericDaoCallback != null) {
            genericDaoCallback.callback(criteriaQuery, root);
        }

        return JPASplitter.runCriteriaQuery(
                values,
                new CriteriaInClauseCreator<VALUE_TYPE>() {
                    @Override
                    public Query createCriteriaInQuery(Collection<VALUE_TYPE> parameterList) {
                        criteriaQuery.where(root.get(singularAttribute).in(parameterList));
                        return getEntityManager().createQuery(criteriaQuery);
                    }
                }
        );
    }

    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> List<ENTITY_TYPE> findListByList(
            Class<ENTITY_TYPE> entity, SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, List<VALUE_TYPE> values) {
        return findListByList(entity, singularAttribute, values, null);
    }

    /**
     * Returns a list of entities that matches a specified value for a specified property.
     *
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
            Class<ENTITY_TYPE> entity, final SingularAttribute<METADATA_TYPE, VALUE_TYPE> singularAttribute, VALUE_TYPE value) {
        if (value == null) {
            // Need to special case null value to handle it correctly.
            return findAll(entity, new GenericDaoCallback<ENTITY_TYPE>() {
                @Override
                public void callback(CriteriaQuery<ENTITY_TYPE> criteriaQuery, Root<ENTITY_TYPE> root) {
                    criteriaQuery.where(getCriteriaBuilder().isNull(root.get(singularAttribute)));
                }
            });
        }
        return findListByList(entity, singularAttribute, Collections.singletonList(value));
    }

    /**
     * Looks up an entity by its JPA id.
     *
     * @param entity the class of the entity to return
     * @param id the entity's JPA id
     * @param <ENTITY_TYPE> the name of the entity class
     * @return a single entity, or null if not found
     */
    public <ENTITY_TYPE> ENTITY_TYPE findById(Class<ENTITY_TYPE> entity, Long id) {
        return getEntityManager().find(entity, id);
    }

    /**
     * Returns a list of entities that matches wildcarded string ('% string %') for a specified property.
     *
     * @param entity             the class of entity to return
     * @param value              the value to query
     * @param singularAttributes one or more metadata fields for the property to query
     * @param <VALUE_TYPE>       the type of the value in the query, e.g. String
     * @param <METADATA_TYPE>    the type on which the property is defined, this can be different from the ENTITY_TYPE if
     *                           there is inheritance
     * @param <ENTITY_TYPE>      the type of the entity to return
     *
     * @return list of entities that match the value, or empty list if not found
     */
    public <VALUE_TYPE, METADATA_TYPE, ENTITY_TYPE extends METADATA_TYPE> List<ENTITY_TYPE> findListWithWildcard(
            Class<ENTITY_TYPE> entity, String value, boolean ignoreCase,
            SingularAttribute<METADATA_TYPE, VALUE_TYPE>... singularAttributes) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ENTITY_TYPE> criteriaQuery = criteriaBuilder.createQuery(entity);
        Root<ENTITY_TYPE> root = criteriaQuery.from(entity);
        Predicate[] predicates = new Predicate[singularAttributes.length];
        if (ignoreCase) {
            value = value.toLowerCase();
        }
        for (int i = 0; i < singularAttributes.length; i++) {
            Expression<String> expression = root.get(singularAttributes[i]).as(String.class);
            if (ignoreCase) {
                expression = criteriaBuilder.lower(expression);
            }
            predicates[i] = criteriaBuilder.like(expression, '%' + value + '%');
        }
        criteriaQuery.where(criteriaBuilder.or(predicates));
        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    protected Predicate createOrTerms(CriteriaBuilder cb, Path fieldExpression, List<?> values) {
        Predicate[] listOfOrTerms = new Predicate[values.size()];
        int i=0;
        for (Object value : values) {
            listOfOrTerms[i++] = cb.equal(fieldExpression, value);
        }

        return cb.or(listOfOrTerms);
    }
}
