package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * unit test of Event Etl doing a lookup of WorkflowConfigDenorm.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowConfigLookupDbFreeTest {

    // 3 successive days starting 1-jan-2013 00:00:00 EST
    static final long[] MSEC_DATES = {1357016400000L, 1357016400000L + 86400000L, 1357016400000L + 2L * 86400000L};

    private WorkflowConfigLookup wfConfigLookup;
    private final WorkflowConfig workflowConfig = buildWorkflowConfig();
    private Collection<WorkflowConfigDenorm> workflowConfigDenorms;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        wfConfigLookup = new WorkflowConfigLookup();
        wfConfigLookup.setWorkflowConfig(workflowConfig);

        workflowConfigDenorms = wfConfigLookup.getDenormConfigs();
        assertTrue(workflowConfigDenorms.size() >= 4);
    }

    @Test
    public void testLookupWorkflowForEvent() {

        HashSet<LabVessel> starters = new HashSet<>();
        LabBatch labBatch1 = new LabBatch("Product 1", starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch1.setWorkflowName("Workflow 1");

        LabBatch labBatch2 = new LabBatch("Product 2", starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch2.setWorkflowName("Workflow 1");

        LabBatch labBatch3 = new LabBatch("Product 3", starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch3.setWorkflowName("Workflow 2");

        // Does lookups based on product and date.  Checks the denorm cache for hits.
        assertNull(wfConfigLookup.lookupWorkflowConfig(
                "No such event", labBatch1.getWorkflowName(), new Date(MSEC_DATES[0] + 1000L)));
        assertNull(wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch1.getWorkflowName(), new Date(MSEC_DATES[0] - 1000L)));

        long day1 = wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch1.getWorkflowName(), new Date(MSEC_DATES[0] + 1000L)).getWorkflowConfigDenormId();
        assertEquals(wfConfigLookup.cacheHit, 0);

        long day2 = wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch1.getWorkflowName(), new Date(MSEC_DATES[1] + 1000L)).getWorkflowConfigDenormId();
        assertNotEquals(day2, day1);
        assertEquals(wfConfigLookup.cacheHit, 0);

        long day3 = wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch1.getWorkflowName(), new Date(MSEC_DATES[2] + 1000L)).getWorkflowConfigDenormId();
        assertNotEquals(day3, day2);
        assertNotEquals(day3, day1);
        assertEquals(wfConfigLookup.cacheHit, 0);

        long day3again = wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch2.getWorkflowName(), new Date(MSEC_DATES[2] + 1000L)).getWorkflowConfigDenormId();
        assertEquals(day3again, day3);
        assertEquals(wfConfigLookup.cacheHit, 1);

        long w2day3 = wfConfigLookup.lookupWorkflowConfig(
                "SageCleanup", labBatch3.getWorkflowName(), new Date(MSEC_DATES[2] + 1000L)).getWorkflowConfigDenormId();
        assertNotEquals(w2day3, day3);
        assertNotEquals(w2day3, day2);
        assertNotEquals(w2day3, day1);
        assertEquals(wfConfigLookup.cacheHit, 1);

        long day3again2 = wfConfigLookup.lookupWorkflowConfig(
                "GSWash1", labBatch2.getWorkflowName(), new Date(MSEC_DATES[2] + 1000L)).getWorkflowConfigDenormId();
        assertEquals(day3again2, day3);
        assertEquals(wfConfigLookup.cacheHit, 2);
    }


    static WorkflowConfig buildWorkflowConfig() {

        // defining workflows
        List<WorkflowProcessDef> workflowList = new ArrayList<>();

        WorkflowProcessDef w1 = new WorkflowProcessDef("Process 1");

        // these steps valid day 0 onward
        WorkflowProcessDefVersion w1v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        w1v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        w1v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        w1v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_3));
        w1.addWorkflowProcessDefVersion(w1v1);

        // these steps valid day 1 onward
        WorkflowProcessDefVersion w1v2 = new WorkflowProcessDefVersion("2.0", new Date(MSEC_DATES[1]));
        w1v2.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_4));
        w1v2.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_5));
        w1v2.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_6));
        w1.addWorkflowProcessDefVersion(w1v2);

        // these steps valid day 2 onward
        WorkflowProcessDefVersion w1v3 = new WorkflowProcessDefVersion("3.0", new Date(MSEC_DATES[2]));
        w1v3.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        w1v3.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        w1.addWorkflowProcessDefVersion(w1v3);
        workflowList.add(w1);

        WorkflowProcessDef w2 = new WorkflowProcessDef("Process 2");
        WorkflowProcessDefVersion w2v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        // these steps valid day 0 and later
        w2v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.SAGE_LOADING));
        w2v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.SAGE_LOADED));
        w2v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.SAGE_UNLOADING));
        w2v1.addStep(new WorkflowStepDef("Step 4").addLabEvent(LabEventType.SAGE_CLEANUP));
        w2.addWorkflowProcessDefVersion(w2v1);
        workflowList.add(w2);

        // defining products
        List<ProductWorkflowDef> pList = new ArrayList<>();

        ProductWorkflowDef p1 = new ProductWorkflowDef("Workflow 1");
        // only valid day 0
        ProductWorkflowDefVersion p1v1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        p1v1.addWorkflowProcessDef(w1);
        p1.addProductWorkflowDefVersion(p1v1);
        // valid day 1 and later
        ProductWorkflowDefVersion p1v2 = new ProductWorkflowDefVersion("2.0", new Date(MSEC_DATES[1]));
        p1v2.addWorkflowProcessDef(w1);
        p1.addProductWorkflowDefVersion(p1v2);
        pList.add(p1);

        ProductWorkflowDef p2 = new ProductWorkflowDef("Workflow 2");
        // valid day 0 and later
        ProductWorkflowDefVersion p2v1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        p2v1.addWorkflowProcessDef(w2);
        p2.addProductWorkflowDefVersion(p2v1);
        pList.add(p2);

        return new WorkflowConfig(workflowList, pList);
    }
}
