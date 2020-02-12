package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
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

        LabEvent bucketExtractions = new LabEvent(LabEventType.DNA_EXTRACTION_BUCKET, gregorianCalendar.getTime(), LabEvent.UI_EVENT_LOCATION,
                1L, 101L, LabEvent.UI_PROGRAM_NAME);
                //todo: set workflowQualifier, if applicable
//        bucketExtractions.setWorkflowQualifier("??");
        bucketExtractions.setLabBatch(labBatch);
        gregorianCalendar.add(Calendar.SECOND, 1);

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
        BarcodedTube m1 = new BarcodedTube("M1", BarcodedTube.BarcodedTubeType.EppendorfFliptop15);
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
        WorkflowConfig workflowConfig = workflowLoader.getWorkflowConfig();

        List<WorkflowMatcher.WorkflowEvent> workflowEvents = workflowMatcher.match(
                workflowConfig.getWorkflowVersionByName(Workflow.CLINICAL_WHOLE_BLOOD_EXTRACTION, new Date()),
                labBatch);
        Assert.assertEquals(workflowEvents.size(), 26);

        Assert.assertEquals(workflowEvents.get(1).getLabEvents().size(), 1);
        LabEvent labEvent = workflowEvents.get(1).getLabEvents().get(0);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.PREP);
        Assert.assertEquals(labEvent.getWorkflowQualifier(), "Reagents");

        Assert.assertEquals(workflowEvents.get(2).getLabEvents().size(), 1);
        LabEvent labEvent1 = workflowEvents.get(2).getLabEvents().get(0);
        Assert.assertEquals(labEvent1.getLabEventType(), LabEventType.PREP);
        Assert.assertEquals(labEvent1.getWorkflowQualifier(), "Disinfect");

        Assert.assertEquals(workflowEvents.get(4).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(4).getLabEvents().get(0).getLabEventType(),
                LabEventType.EXTRACT_BLOOD_TO_MICRO);

        Assert.assertEquals(workflowEvents.get(8).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(8).getLabEvents().get(0).getLabEventType(),
                LabEventType.ADD_REAGENT);

        Assert.assertEquals(workflowEvents.get(25).getLabEvents().size(), 1);
        Assert.assertEquals(workflowEvents.get(25).getLabEvents().get(0).getLabEventType(),
                LabEventType.ADD_REAGENT);

        // todo list of samples at top of page?
    }
}
