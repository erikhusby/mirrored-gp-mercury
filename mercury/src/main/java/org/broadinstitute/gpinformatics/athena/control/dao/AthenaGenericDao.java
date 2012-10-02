package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Superclass for Data Access Objects.  Managed beans can't be parameterized types, so this DAO can't be typesafe.
 */
@Stateful
@RequestScoped
public class AthenaGenericDao {
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
        EntityManager entityManager = threadEntityManager.getEntityManager();
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    public void delete(ResearchProject researchProject) {
        EntityManager entityManager = threadEntityManager.getEntityManager();
        entityManager.remove(researchProject);
    }
}
