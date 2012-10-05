package org.broadinstitute.gpinformatics.mercury.control.dao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
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

    public <S, T> T findSingle(Class<T> entity, SingularAttribute<T, S> singularAttribute, S value) {
        CriteriaQuery<T> criteriaQuery =
                getEntityManager().getCriteriaBuilder().createQuery(entity);
        Root<T> root = criteriaQuery.from(entity);
        criteriaQuery.where(getEntityManager().getCriteriaBuilder().equal(root.get(singularAttribute), value));
        return getEntityManager().createQuery(criteriaQuery).getSingleResult();
    }
}
