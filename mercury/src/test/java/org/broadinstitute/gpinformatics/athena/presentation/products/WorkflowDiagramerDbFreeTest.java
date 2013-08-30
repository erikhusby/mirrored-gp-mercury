package org.broadinstitute.gpinformatics.athena.presentation.products;

import com.cenqua.clover.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
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
import sun.swing.StringUIClientPropertyKey;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowDiagramerDbFreeTest {
    // 3 successive days starting 1-jan-2013 00:00:00 EST
    private static final long ONE_DAY = 86400000L;
    private static final long[] MSEC_DATES = {1357016400000L, 1357016400000L + ONE_DAY, 1357016400000L + 2L * ONE_DAY};

    private final WorkflowDiagramer diagramer = new WorkflowDiagramer();
    private final WorkflowLoader workflowLoader = createMock(WorkflowLoader.class);
    private WorkflowConfig workflowConfig;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        // Defines workflows.
        workflowConfig = new WorkflowConfig();
        ProductWorkflowDef exEx = new ProductWorkflowDef("Exome Express");
        ProductWorkflowDef wholeGenome = new ProductWorkflowDef("Whole Genome");

        // Exome Express 1.0 is only valid day 0
        ProductWorkflowDefVersion exExV1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        // Exome Express 2.0 is valid day 1 and later
        ProductWorkflowDefVersion exExV2 = new ProductWorkflowDefVersion("2.0", new Date(MSEC_DATES[1]));
        // Whole Genome 1.0 is valid day 0 and later
        ProductWorkflowDefVersion wholeGenomeV1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));

        // Defines processes.
        List<WorkflowProcessDef> processDefs = new ArrayList<>();
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

        // Puts all into WorkflowConfig.
        processDefs.add(process1);
        workflowConfig.getWorkflowProcessDefs().addAll(processDefs);
        processDefs.clear();
        processDefs.add(process2);
        workflowConfig.getWorkflowProcessDefs().addAll(processDefs);
        processDefs.clear();

        exExV1.addWorkflowProcessDef(process1);

        exExV2.addWorkflowProcessDef(process1);
        exExV2.addWorkflowProcessDef(process2);

        wholeGenomeV1.addWorkflowProcessDef(process2);

        exEx.addProductWorkflowDefVersion(exExV1);
        exEx.addProductWorkflowDefVersion(exExV2);
        wholeGenome.addProductWorkflowDefVersion(wholeGenomeV1);

        List<ProductWorkflowDef> workflowDefs = new ArrayList<>();
        workflowDefs.add(exEx);
        workflowDefs.add(wholeGenome);
        workflowConfig.getProductWorkflowDefs().addAll(workflowDefs);

        // Mock the loader.
        reset(workflowLoader);
        expect(workflowLoader.load()).andReturn(workflowConfig).anyTimes();
        replay(workflowLoader);
        diagramer.setWorkflowLoader(workflowLoader);
    }

    @Test
    public void testDates() {
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            if (workflowDef.getName().equals("Exome Express")) {
                List<Date> dates = workflowDef.getEffectiveDates();
                Assert.assertEquals(dates.size(), 3);
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[0])));
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[1])));
                Assert.assertTrue(dates.contains(new Date(MSEC_DATES[2])));
            } else if (workflowDef.getName().equals("Whole Genome")) {
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
            String filename = workflowDef.getWorkflowImageFileName(testDate);
            Assert.assertTrue(filename.contains(String.valueOf(MSEC_DATES[0])));
            Assert.assertFalse(filename.contains(" "));
        }
    }

    @Test
    public void testGraph() throws Exception {
        List<WorkflowDiagramer.WorkflowGraph> graphs = diagramer.createGraphs();
        Assert.assertEquals(graphs.size(), 4);
        boolean found1 = false;
        boolean found2 = false;
        for (WorkflowDiagramer.WorkflowGraph graph : graphs) {
            Assert.assertFalse(StringUtils.isEmpty(graph.getDiagramFileName()));
            switch (graph.getWorkflowName()) {
            case "Exome Express" :
                switch (graph.getWorkflowVersion()) {
                case "1.0" :
                    Assert.assertEquals(graph.getEffectiveDate(), new Date(MSEC_DATES[0]));
                    Assert.assertEquals(graph.getNodes().size(), 4);
                    break;
                case "2.0" :
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
                case "3.0" :
                    Assert.assertEquals(graph.getEffectiveDate(), new Date(MSEC_DATES[2]));
                    Assert.assertEquals(graph.getNodes().size(), 4);
                    break;
                default :
                    Assert.fail();
                    break;
                }
                break;
            case "Whole Genome" :
                switch (graph.getWorkflowVersion()) {
                case "1.0" :
                    Assert.assertEquals(graph.getEffectiveDate(), new Date(MSEC_DATES[0]));
                    Assert.assertEquals(graph.getNodes().size(), 5);
                    break;
                default :
                    Assert.fail();
                    break;
                }
            }
        }
        Assert.assertTrue(found1);
        Assert.assertTrue(found2);
    }

    @Test
    public void testDotFiles() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir") + "/test" + System.currentTimeMillis());
        Assert.assertTrue(tmpDir.mkdirs());

        List<WorkflowDiagramer.WorkflowGraph> graphs = diagramer.createGraphs();
        diagramer.writeDotFiles(tmpDir, graphs);

        File[] files = tmpDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(WorkflowDiagramer.DOT_EXTENSION);
            }
        });
        Assert.assertEquals(files.length, 4);
        FileUtils.deltree(tmpDir);
    }
}
