package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Test product workflow, processes and steps
 */
public class WorkflowTest {
    @Test
    public void testMessageValidation() {
        // Quote
        // Send kits
        // BSP portal -> Athena Product Order
        // Athena Product Order -> Mercury
        // Receive samples in BSP
        // BSP samples -> Mercury
        // Web service to advance workflow without message
        // Associate starters with workflow
        // Validate first message
        // Perform first message
        // Validate second message
    }

    public void buildProcesses() {
        ArrayList<WorkflowStep> workflowSteps = new ArrayList<WorkflowStep>();
        WorkflowProcess sampleReceipt = new WorkflowProcess("Sample Receipt");
        new WorkflowProcess("Extraction");
        new WorkflowProcess("Finger Printing");
        new WorkflowProcess("Samples Pico / Plating");
        WorkflowProcess preLc = new WorkflowProcess("Pre-Library Construction");
        preLc.addStep(new WorkflowBucket("Pre-LC Bucket"));

        WorkflowProcess libraryConstruction = new WorkflowProcess("Library Construction");
        libraryConstruction.addStep(new WorkflowStep("EndRepair").addLabEvent(LabEventType.END_REPAIR));
        libraryConstruction.addStep(new WorkflowStep("EndRepairCleanup").addLabEvent(LabEventType.END_REPAIR_CLEANUP));

        new WorkflowProcess("Post-Library Construction");
        new WorkflowProcess("QTP");
        new WorkflowProcess("HiSeq");
    }
}
