package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Queries for the research project.
 */
@Stateful
@RequestScoped
public class ResearchProjectDAO extends GenericDao {

    @SuppressWarnings("unchecked")
    public List<ResearchProject> findResearchProjectsByOwner(long username) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.createdBy), username));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @SuppressWarnings("unchecked")
    public ResearchProject findResearchProjectsByName(String name) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.title), name));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<ResearchProject> findAllResearchProjects() {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public ResearchProject findById(Long rpId) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.id), rpId));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

}
