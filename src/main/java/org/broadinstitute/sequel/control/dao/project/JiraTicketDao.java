package org.broadinstitute.sequel.control.dao.project;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.project.JiraTicket;

import javax.persistence.Query;
import java.util.List;

/**
 * Data Access Object for JIRA tickets
 */
public class JiraTicketDao extends GenericDao{
    public List<JiraTicket> fetchAll(int first, int max) {
        Query query = this.getThreadEntityManager().getEntityManager().createQuery("from JiraTicket j order by j.ticketName");
        query.setFirstResult(first);
        query.setMaxResults(max);
        //noinspection unchecked
        return query.getResultList();
    }
}
