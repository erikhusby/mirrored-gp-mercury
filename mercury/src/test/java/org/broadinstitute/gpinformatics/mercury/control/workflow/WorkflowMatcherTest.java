package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test that events can be matched to a workflow.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowMatcherTest {
    @Test
    public void testExtraction() {
        // Create samples
        BarcodedTube t1 = new BarcodedTube("T1", BarcodedTube.BarcodedTubeType.MatrixTube);
        BarcodedTube t2 = new BarcodedTube("T2", BarcodedTube.BarcodedTubeType.MatrixTube);

        // Create new batch with workflow
        Set<LabVessel> starterVessels = new HashSet<>();
        starterVessels.add(t1);
        starterVessels.add(t2);
        LabBatch labBatch = new LabBatch("ESET-1", starterVessels, LabBatch.LabBatchType.WORKFLOW);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        // Add batch event
        LabEvent prepReagents = new LabEvent(LabEventType.PREP, gregorianCalendar.getTime(), LabEvent.UI_EVENT_LOCATION,
                1L, 101L, LabEvent.UI_PROGRAM_NAME);
        prepReagents.setWorkflowQualifier("Reagents");
        prepReagents.setLabBatch(labBatch);
        gregorianCalendar.add(Calendar.SECOND, 1);

        LabEvent disinfect = new LabEvent(LabEventType.PREP, gregorianCalendar.getTime(), LabEvent.UI_EVENT_LOCATION,
                1L, 101L, LabEvent.UI_PROGRAM_NAME);
        disinfect.setWorkflowQualifier("Disinfect");
        disinfect.setLabBatch(labBatch);
        gregorianCalendar.add(Calendar.SECOND, 1);

        // Add transfer with reagents
        LabEvent bloodToMicro = new LabEvent(LabEventType.EXTRACT_BLOOD_TO_MICRO, gregorianCalendar.getTime(),
                LabEvent.UI_EVENT_LOCATION, 1L, 101L, LabEvent.UI_PROGRAM_NAME);
        BarcodedTube m1 = new BarcodedTube("M1", BarcodedTube.BarcodedTubeType.EppendoffFliptop15);
        bloodToMicro.getVesselToVesselTransfers().add(new VesselToVesselTransfer(t1, m1, bloodToMicro));
        bloodToMicro.addReagent(new GenericReagent("R1", "1234", null));
        gregorianCalendar.add(Calendar.SECOND, 1);

        // Add vessel event
        LabEvent addEthanol = new LabEvent(LabEventType.ADD_REAGENT, gregorianCalendar.getTime(),
                LabEvent.UI_EVENT_LOCATION, 1L, 101L, LabEvent.UI_PROGRAM_NAME);
        addEthanol.setWorkflowQualifier("Ethanol");
        addEthanol.addReagent(new GenericReagent("100% Ethanol", "00001234", new Date()));
        m1.addInPlaceEvent(addEthanol);
        gregorianCalendar.add(Calendar.SECOND, 1);

        // Add an event that isn't in the workflow
        LabEvent addUnmatchedReagent = new LabEvent(LabEventType.ADD_REAGENT, gregorianCalendar.getTime(),
                LabEvent.UI_EVENT_LOCATION, 1L, 101L, LabEvent.UI_PROGRAM_NAME);
        addUnmatchedReagent.addReagent(new GenericReagent("Unmatched", "00001234", new Date()));
        m1.addInPlaceEvent(addUnmatchedReagent);

        WorkflowMatcher workflowMatcher = new WorkflowMatcher();
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();

        List<WorkflowMatcher.WorkflowEvent> workflowEvents = workflowMatcher.match(
                workflowConfig.getWorkflowVersionByName("Clinical Whole Blood Extraction", new Date()),
                labBatch);
        Assert.assertEquals(workflowEvents.size(), 25);

        Assert.assertEquals(workflowEvents.get(0).getLabEvents().size(), 1);
        LabEvent labEvent = workflowEvents.get(0).getLabEvents().get(0);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.PREP);
        Assert.assertEquals(labEvent.getWorkflowQualifier(), "Reagents");

        Assert.assertEquals(workflowEvents.get(1).getLabEvents().size(), 1);
        LabEvent labEvent1 = workflowEvents.get(1).getLabEvents().get(0);
        Assert.assertEquals(labEvent1.getLabEventType(), LabEventType.PREP);
        Assert.assertEquals(labEvent1.getWorkflowQualifier(), "Disinfect");

        Assert.assertEquals(workflowEvents.get(3).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(3).getLabEvents().get(0).getLabEventType(),
                LabEventType.EXTRACT_BLOOD_TO_MICRO);

        Assert.assertEquals(workflowEvents.get(7).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(7).getLabEvents().get(0).getLabEventType(),
                LabEventType.ADD_REAGENT);

        Assert.assertEquals(workflowEvents.get(24).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(24).getLabEvents().get(0).getLabEventType(),
                LabEventType.ADD_REAGENT);

        // Move suggested reagents from LabEventType to workflow?

        // Need to render per-sample transfer links for steps that haven't happened yet?  Not per-sample, because
        // they must be scanned.  Could just render link unconditionally.

        // todo for batch events: checkbox or button, or both
        // todo LabEventType additions: batch event? vs vessel event vs transfer
        // todo manual transfer page: support vessel event (reagent addition); event type parameter
        // todo Enter LCSET and / or search.
        // todo list of samples at top of page?

        // If there are multiple events for a step, should the step be repeated, or should the events be normalized?
        // It's possible (likely?) that the dates overlap, unless the focus is on a single sample, this argues for
        // repeating step information.

        // Create LCSET
        // Visit workflow page
        // Link to manual transfer page
        // Return to workflow page
    }
}
