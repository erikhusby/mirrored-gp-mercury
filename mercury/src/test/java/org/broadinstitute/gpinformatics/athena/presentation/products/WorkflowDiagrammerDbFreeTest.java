package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowDiagrammerDbFreeTest {
    // 3 successive days starting 1-jan-2013 00:00:00 EST
    private static final long ONE_DAY = 86400000L;
    private static final long[] MSEC_DATES = {1357016400000L, 1357016400000L + ONE_DAY, 1357016400000L + 2L * ONE_DAY};

    private final WorkflowDiagrammer diagrammer = new WorkflowDiagrammer();
    private WorkflowConfig workflowConfig;

    private static final String WORKFLOW_NAME_1 = "Partial Genome";
    private static final String WORKFLOW_NAME_2 = "Complete Genome";

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        // Defines workflows.
        ProductWorkflowDef workflow1 = new ProductWorkflowDef(WORKFLOW_NAME_1);
        ProductWorkflowDef workflow2 = new ProductWorkflowDef(WORKFLOW_NAME_2);

        // workflow1 1.0 is only valid day 0
        ProductWorkflowDefVersion workflow1V1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        // workflow1 2.0 is valid day 1 and later
        ProductWorkflowDefVersion workflow1V2 = new ProductWorkflowDefVersion("2.0", new Date(MSEC_DATES[1]));
        // Whole Genome 1.0 is valid day 0 and later
        ProductWorkflowDefVersion workflow2V1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));

        // Defines processes.
        WorkflowProcessDef process1 = new WorkflowProcessDef("Process 1");
        WorkflowProcessDef process2 = new WorkflowProcessDef("Process 2");

        // Process1 version definitions.
        // process1v1 valid day 0 onward
        WorkflowProcessDefVersion process1v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        process1v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        process1v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        process1v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_3));
        process1.addWorkflowProcessDefVersion(process1v1);

        // process1v2 valid day 1 onward
        WorkflowProcessDefVersion process1v2 = new WorkflowProcessDefVersion("2.0", new Date(MSEC_DATES[1]));
        process1v2.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_4));
        process1v2.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_5));
        process1v2.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_6));
        process1.addWorkflowProcessDefVersion(process1v2);

        // process1v3 valid day 2 onward
        WorkflowProcessDefVersion process1v3 = new WorkflowProcessDefVersion("3.0", new Date(MSEC_DATES[2]));
        process1v3.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        process1v3.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        process1.addWorkflowProcessDefVersion(process1v3);

        // Process2 version definitions.
        // process2v0 never valid -- too early
        WorkflowProcessDefVersion process2v0 = new WorkflowProcessDefVersion("0.0", new Date(MSEC_DATES[0] - ONE_DAY));
        process2v0.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.SAGE_LOADING));
        process2v0.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.SAGE_LOADED));
        process2v0.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.SAGE_UNLOADING));
        process2v0.addStep(new WorkflowStepDef("Step 4").addLabEvent(LabEventType.SAGE_CLEANUP));
        process2.addWorkflowProcessDefVersion(process2v0);

        // process2v1 valid day 0 and later
        WorkflowProcessDefVersion process2v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        process2v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.SAGE_LOADING));
        process2v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.SAGE_LOADED));
        process2v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.SAGE_UNLOADING));
        process2v1.addStep(new WorkflowStepDef("Step 4").addLabEvent(LabEventType.SAGE_CLEANUP));
        process2.addWorkflowProcessDefVersion(process2v1);

        // process2v2 valid day 0 and later BUT has identical version and steps with process2v1
        WorkflowProcessDefVersion process2v2 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[1]));
        process2v2.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.SAGE_LOADING));
        process2v2.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.SAGE_LOADED));
        process2v2.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.SAGE_UNLOADING));
        process2v2.addStep(new WorkflowStepDef("Step 4").addLabEvent(LabEventType.SAGE_CLEANUP));
        process2.addWorkflowProcessDefVersion(process2v2);

        workflow1V1.addWorkflowProcessDef(process1);

        workflow1V2.addWorkflowProcessDef(process1);
        workflow1V2.addWorkflowProcessDef(process2);

        workflow2V1.addWorkflowProcessDef(process2);

        workflow1.addProductWorkflowDefVersion(workflow1V1);
        workflow1.addProductWorkflowDefVersion(workflow1V2);
        workflow2.addProductWorkflowDefVersion(workflow2V1);

        List<ProductWorkflowDef> workflowDefs = new ArrayList<>();
        workflowDefs.add(workflow1);
        workflowDefs.add(workflow2);

        // Puts all into WorkflowConfig.
        workflowConfig = new WorkflowConfig(Arrays.asList(process1, process2), workflowDefs);

        diagrammer.setWorkflowConfig(workflowConfig);
    }

    @Test
    public void testWorkflowConfig() throws Exception {
        //Load and verify WorkflowConfig file.
        diagrammer.makeAllDiagramFiles();
    }

    @Test
    public void testDates() {
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            if (workflowDef.getName().equals(WORKFLOW_NAME_1)) {
                List<Date> dates = workflowDef.getEffectiveDates();
                Assert.assertEquals(dates.size(), 3);
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[0])));
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[1])));
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[2])));
            } else if (workflowDef.getName().equals(WORKFLOW_NAME_2)) {
                List<Date> dates = workflowDef.getEffectiveDates();
                Assert.assertEquals(dates.size(), 2);
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[0])));
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[1])));
            } else {
                Assert.fail("Unexpected workflowDef named " + workflowDef.getName());
            }
        }
    }

    @Test
    public void testFilename() {
        Date testDate = new Date(MSEC_DATES[0]);
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            String filename = WorkflowDiagrammer.getWorkflowImageFileName(workflowDef, testDate);
            Assert.assertTrue(filename.contains(String.valueOf(MSEC_DATES[0])));
            Assert.assertFalse(filename.contains(" "));
        }
    }


    @Test
    public void testGraph() throws Exception {
        List<WorkflowDiagrammer.Graph> graphs = diagrammer.createGraphs();
        Assert.assertEquals(graphs.size(), 5);
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        boolean found4 = false;
        for (WorkflowDiagrammer.Graph graph : graphs) {
            Assert.assertFalse(StringUtils.isEmpty(graph.getDiagramFileName()));
            switch (graph.getWorkflowName()) {
            case WORKFLOW_NAME_1:
                switch (graph.getWorkflowVersion()) {
                case "1.0":
                    Assert.assertEquals(graph.getEffectiveDate(), new Date(MSEC_DATES[0]));
                    Assert.assertEquals(graph.getNodes().size(), 4);
                    break;
                case "2.0":
                    if (graph.getEffectiveDate().equals(new Date(MSEC_DATES[1]))) {
                        found1 = true;
                        Assert.assertEquals(graph.getNodes().size(), 8);
                    } else if (graph.getEffectiveDate().equals(new Date(MSEC_DATES[2]))) {
                        found2 = true;
                        Assert.assertEquals(graph.getNodes().size(), 7);
                    } else {
                        Assert.fail();
                    }
                    break;
                case "3.0":
                    Assert.assertEquals(graph.getEffectiveDate(), new Date(MSEC_DATES[2]));
                    Assert.assertEquals(graph.getNodes().size(), 4);
                    break;
                default:
                    Assert.fail();
                    break;
                }
                break;
            case WORKFLOW_NAME_2:
                switch (graph.getWorkflowVersion()) {
                case "1.0":
                    if (graph.getEffectiveDate().equals(new Date(MSEC_DATES[0]))) {
                        found3 = true;
                        Assert.assertEquals(graph.getNodes().size(), 5);
                    } else if (graph.getEffectiveDate().equals(new Date(MSEC_DATES[1]))) {
                        found4 = true;
                        Assert.assertEquals(graph.getNodes().size(), 5);
                    } else {
                        Assert.fail();
                    }
                    break;
                default:
                    Assert.fail();
                    break;
                }
            }
        }
        Assert.assertTrue(found1);
        Assert.assertTrue(found2);
        Assert.assertTrue(found3);
        Assert.assertTrue(found4);
    }

    @Test
    public void testDotFiles() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir") + "/test" + System.currentTimeMillis());
        Assert.assertTrue(tmpDir.mkdirs());

        List<WorkflowDiagrammer.Graph> graphs = diagrammer.createGraphs();
        diagrammer.writeDotFiles(tmpDir, graphs);

        File[] files = tmpDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // Filter passes only the .dot files.
                return name.endsWith(WorkflowDiagrammer.DOT_EXTENSION);
            }
        });
        Assert.assertEquals(files.length, 5);

        for (File file : tmpDir.listFiles()) {
            file.delete();
        }
        tmpDir.delete();
    }
}
