package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Test persist and fetch
 */
public class JiraTicketDaoTest extends ContainerTest{

    @Inject
    JiraTicketDao jiraTicketDao;

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFetchByName() {
        String ticketName = "UT-" + UUID.randomUUID();
        JiraTicket jiraTicket = new JiraTicket( ticketName, ticketName);
        jiraTicketDao.persist(jiraTicket);
        jiraTicketDao.flush();
        jiraTicketDao.clear();

        JiraTicket jiraTicketFetched = jiraTicketDao.fetchByName(ticketName);
        Assert.assertNotNull("No ticket", jiraTicketFetched);
        Assert.assertEquals("Wrong name", jiraTicket.getTicketName(),  ticketName);

        jiraTicketDao.remove(jiraTicketFetched);
    }
}
