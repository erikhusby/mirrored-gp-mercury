package org.broadinstitute.gpinformatics.athena.control.dao;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

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
public class ResearchProjectDao extends GenericDao {

    @SuppressWarnings("unchecked")
    public List<ResearchProject> findResearchProjectsByOwner(long username) {
        EntityManager entityManager = getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.createdBy), username));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @SuppressWarnings("unchecked")
    public ResearchProject findByTitle(String title) {
        EntityManager entityManager = getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.title), title));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<ResearchProject> findAllResearchProjects() {
        EntityManager entityManager = getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        criteriaQuery.from(ResearchProject.class);
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public ResearchProject findByJiraTicketKey(String jiraTicketKey) {
        EntityManager entityManager = getEntityManager();
        CriteriaQuery<ResearchProject> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ResearchProject.class);
        Root<ResearchProject> root = criteriaQuery.from(ResearchProject.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ResearchProject_.jiraTicketKey), jiraTicketKey));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

}
