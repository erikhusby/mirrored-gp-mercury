package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

/**
 * Data Access Object for JIRA tickets
 */
@Stateful
@RequestScoped
public class JiraTicketDao extends GenericDao{

    public List<JiraTicket> fetchAll(int first, int max) {
        CriteriaQuery<JiraTicket> criteriaQuery =
                getEntityManager().getCriteriaBuilder().createQuery(JiraTicket.class);
        TypedQuery<JiraTicket> typedQuery = getEntityManager().createQuery(criteriaQuery);
        typedQuery.setFirstResult(first);
        typedQuery.setMaxResults(max);
        return typedQuery.getResultList();
    }

    public JiraTicket fetchByName(String ticketName) {
        return findSingle(JiraTicket.class, JiraTicket_.ticketName, ticketName);
    }
}
