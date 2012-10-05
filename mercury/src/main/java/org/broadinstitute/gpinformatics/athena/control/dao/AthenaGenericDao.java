package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

/**
 * Superclass for Data Access Objects.  Managed beans can't be parameterized types, so this DAO can't be typesafe.
 */
@Stateful
@RequestScoped
public class AthenaGenericDao {
    @Inject
    private AthenaThreadEntityManager AthenaThreadEntityManager;

    public AthenaThreadEntityManager getAthenaThreadEntityManager() {
        return AthenaThreadEntityManager;
    }

    protected EntityManager em() {
        return getAthenaThreadEntityManager().getEntityManager();
    }


    protected CriteriaBuilder getCriteriaBuilder() {
        return em().getCriteriaBuilder();
    }

    public void flush() {
        this.AthenaThreadEntityManager.getEntityManager().flush();
    }

    public void clear() {
        this.AthenaThreadEntityManager.getEntityManager().clear();
    }

    public void persist(Object entity) {
        this.AthenaThreadEntityManager.getEntityManager().persist(entity);
    }

    public void persistAll(List<?> entities) {
        EntityManager entityManager = AthenaThreadEntityManager.getEntityManager();
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    public void delete(ResearchProject researchProject) {
        EntityManager entityManager = AthenaThreadEntityManager.getEntityManager();
        entityManager.remove(researchProject);
    }
}
