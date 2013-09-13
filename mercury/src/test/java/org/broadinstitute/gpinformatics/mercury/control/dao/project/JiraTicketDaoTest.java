package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test persist and fetch
 */
public class JiraTicketDaoTest extends ContainerTest{

    @Inject
    private JiraTicketDao jiraTicketDao;

    @Inject
    private JiraService jiraService;


    @Inject
    private UserTransaction utx;

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFetchByName() {
        String ticketName = "UT-" + UUID.randomUUID();
        JiraTicket jiraTicket = new JiraTicket(jiraService, ticketName);
        jiraTicketDao.persist(jiraTicket);
        jiraTicketDao.flush();
        jiraTicketDao.clear();

        JiraTicket jiraTicketFetched = jiraTicketDao.fetchByName(ticketName);
        Assert.assertNotNull(jiraTicketFetched, "No ticket");
        Assert.assertEquals(jiraTicket.getTicketName(),  ticketName, "Wrong name");
    }
}
