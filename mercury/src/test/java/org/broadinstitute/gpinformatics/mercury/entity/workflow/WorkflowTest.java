package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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
        ArrayList<WorkflowStepDef> workflowStepDefs = new ArrayList<WorkflowStepDef>();
        WorkflowProcessDef sampleReceipt = new WorkflowProcessDef("Sample Receipt");
        new WorkflowProcessDef("Extraction");
        new WorkflowProcessDef("Finger Printing");
        new WorkflowProcessDef("Samples Pico / Plating");
        WorkflowProcessDef preLc = new WorkflowProcessDef("Pre-Library Construction");
        preLc.addStep(new WorkflowBucketDef("Pre-LC Bucket"));

        WorkflowProcessDef libraryConstruction = new WorkflowProcessDef("Library Construction");
        libraryConstruction.addStep(new WorkflowStepDef("EndRepair").addLabEvent(LabEventType.END_REPAIR));
        libraryConstruction.addStep(new WorkflowStepDef("EndRepairCleanup").addLabEvent(LabEventType.END_REPAIR_CLEANUP));

        new WorkflowProcessDef("Post-Library Construction");
        new WorkflowProcessDef("QTP");
        new WorkflowProcessDef("HiSeq");
    }
}
