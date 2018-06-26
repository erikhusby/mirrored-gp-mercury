package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;

/**
 * Test persist and fetch
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class JiraTicketDaoTest extends StubbyContainerTest {

    public JiraTicketDaoTest(){}

    @Inject
    private JiraTicketDao jiraTicketDao;

    @Inject
    private JiraService jiraService;


    @Inject
    private UserTransaction utx;

    @BeforeMethod(groups = STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test(groups = TestGroups.STUBBY)
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
