package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;

/**
 * Validate that the traverser finds events correctly
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class TransferTraverserTest extends StubbyContainerTest {

    public TransferTraverserTest(){}

    @Inject
    private JiraTicketDao jiraTicketDao;

    @Test(enabled = false, groups = STUBBY)
    public void testLcSetPaths() {
        boolean finished = false;
        int first = 0;
        int max = 50;
        while (!finished) {
            List<JiraTicket> jiraTickets = jiraTicketDao.fetchAll(first, max);
            first += max;
            if (jiraTickets.isEmpty()) {
                finished = true;
            } else {
                for (JiraTicket jiraTicket : jiraTickets) {
                    printJiraTicket(jiraTicket);
                }
            }
            jiraTicketDao.clear();
        }
        // todo jmt asserts
    }

    @Test(enabled = false)
    public void testSingleLcSet() {
        JiraTicket jiraTicket = jiraTicketDao.fetchByName("LCSET-600");
        printJiraTicket(jiraTicket);
    }

    private void printJiraTicket(JiraTicket jiraTicket) {
        LabBatch labBatch = jiraTicket.getLabBatch();
        LabVessel labVessel = labBatch.getStartingBatchLabVessels().iterator().next();

        // either the starter is a bsp sample, in which case we need to get the aliquot
//        if (OrmUtil.proxySafeIsInstance(starter,Starter.class)) {
//            labVessel = starter.getSampleInstances().iterator().next().getSingleProjectPlan().getAliquotForStarter(starter);
//        }
        // or the start is itself a lab vessel for something like topoffs or rework
//        else {
//            labVessel = OrmUtil.proxySafeCast(starter,LabVessel.class);
//        }
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        labVessel.evaluateCriteria(transferTraverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        System.out.println(jiraTicket.getTicketName() + ": " + transferTraverserCriteria.getLabEventNames());
    }
}
