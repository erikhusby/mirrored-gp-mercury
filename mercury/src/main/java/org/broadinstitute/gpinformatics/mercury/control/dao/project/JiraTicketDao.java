package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for JIRA tickets
 */
@Stateful
@RequestScoped
public class JiraTicketDao extends GenericDao {

    public List<JiraTicket> fetchAll(int first, int max) {
        return findAll(JiraTicket.class, first, max);
    }

    public JiraTicket fetchByName(String ticketName) {
        return findSingle(JiraTicket.class, JiraTicket_.ticketName, ticketName);
    }
}
