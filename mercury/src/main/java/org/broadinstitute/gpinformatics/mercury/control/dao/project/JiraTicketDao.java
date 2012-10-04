package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access Object for JIRA tickets
 */
public class JiraTicketDao extends GenericDao{
    public List<JiraTicket> fetchAll(int first, int max) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<JiraTicket> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(JiraTicket.class);
        TypedQuery<JiraTicket> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(first);
        typedQuery.setMaxResults(max);
        return typedQuery.getResultList();
    }

    public JiraTicket fetchByName(String ticketName) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<JiraTicket> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(JiraTicket.class);
        Root<JiraTicket> root = criteriaQuery.from(JiraTicket.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(JiraTicket_.ticketName), ticketName));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
}
