package org.broadinstitute.gpinformatics.mercury.control.dao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collections;
import java.util.List;

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

    public void persistAll(List<?> entities) {
        EntityManager entityManager = this.threadEntityManager.getEntityManager();
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    public void remove(Object entity) {
        getEntityManager().remove(entity);
    }

    public EntityManager getEntityManager() {
        return threadEntityManager.getEntityManager();
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
        criteriaQuery.where(root.get(singularAttribute).in(values));
        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }
}
