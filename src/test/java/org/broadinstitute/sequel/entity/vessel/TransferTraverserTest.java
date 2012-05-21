package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.LabEventTest;
import org.broadinstitute.sequel.control.dao.project.JiraTicketDao;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

/**
 * Validate that the traverser finds events correctly
 */
public class TransferTraverserTest extends ContainerTest{
    @Inject
    private JiraTicketDao jiraTicketDao;

    @Test
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
                    Project project = jiraTicket.getProjects().iterator().next();
                    ProjectPlan projectPlan = project.getProjectPlans().iterator().next();
                    BSPSample bspSample = projectPlan.getBspSamples().iterator().next();
                    SampleSheet sampleSheet = bspSample.getSampleSheets().iterator().next();
                    LabVessel labVessel = sampleSheet.getLabVessels().iterator().next();
                    VesselContainer<?> vesselContainer = labVessel.getContainers().iterator().next();
                    LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
                    vesselContainer.evaluateCriteria(vesselContainer.getPositionOfVessel(labVessel),
                            transferTraverserCriteria, VesselContainer.TraversalDirection.Descendants,
                            null, 0);
                    System.out.println(jiraTicket.getTicketName() + ": " + transferTraverserCriteria.getLabEventNames());
                }
            }
            jiraTicketDao.clear();
        }
        // todo jmt asserts
    }
}
