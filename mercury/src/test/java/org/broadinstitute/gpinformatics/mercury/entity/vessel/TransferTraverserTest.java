package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Validate that the traverser finds events correctly
 */
public class TransferTraverserTest extends ContainerTest{
    @Inject
    private JiraTicketDao jiraTicketDao;

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
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
        Starter starter = labBatch.getStarters().iterator().next();
        LabVessel labVessel = null;

        // either the starter is a bsp sample, in which case we need to get the aliquot
        if (OrmUtil.proxySafeIsInstance(starter,Starter.class)) {
//            labVessel = starter.getSampleInstances().iterator().next().getSingleProjectPlan().getAliquotForStarter(starter);
        }
        // or the start is itself a lab vessel for something like topoffs or rework
        else {
            labVessel = OrmUtil.proxySafeCast(starter,LabVessel.class);
        }
        VesselContainer<?> vesselContainer = labVessel.getContainers().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        vesselContainer.evaluateCriteria(vesselContainer.getPositionOfVessel(labVessel),
                transferTraverserCriteria, VesselContainer.TraversalDirection.Descendants,
                null, 0);
        System.out.println(jiraTicket.getTicketName() + ": " + transferTraverserCriteria.getLabEventNames());
    }
}
