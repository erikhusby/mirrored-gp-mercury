package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.test.LabEventTest;
import org.broadinstitute.sequel.control.dao.project.JiraTicketDao;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

/**
 * Validate that the traverser finds events correctly
 */
public class TransferTraverserTest extends ContainerTest{
    @Inject
    private JiraTicketDao jiraTicketDao;

    @Test(enabled = false)
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
        Project project = jiraTicket.getProjects().iterator().next();
        ProjectPlan projectPlan = project.getProjectPlans().iterator().next();
        StartingSample startingSample = projectPlan.getStartingSamples().iterator().next();
        SampleSheet sampleSheet = startingSample.getSampleSheets().iterator().next();
        LabVessel labVessel = sampleSheet.getLabVessels().iterator().next();
        VesselContainer<?> vesselContainer = labVessel.getContainers().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        vesselContainer.evaluateCriteria(vesselContainer.getPositionOfVessel(labVessel),
                transferTraverserCriteria, VesselContainer.TraversalDirection.Descendants,
                null, 0);
        System.out.println(jiraTicket.getTicketName() + ": " + transferTraverserCriteria.getLabEventNames());
    }
}
