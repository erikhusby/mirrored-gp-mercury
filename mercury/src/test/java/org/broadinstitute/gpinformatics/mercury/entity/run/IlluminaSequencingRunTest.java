package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 3/7/13
 *         Time: 3:08 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class IlluminaSequencingRunTest {

    private Date runDate;
    private String flowcellBarcode;
    private String runName;
    private String runBarcode;
    private IlluminaFlowcell testFlowcell;
    private String runPath;
    private String fullRunPath;
    private OutputDataLocation dataLocation;
    private String machineName;
    private IlluminaSequencingRun testRun;

    public void testConstruction() {

        Assert.assertNotNull(testRun.getRunLocation());
        Assert.assertEquals(fullRunPath, testRun.getRunLocation().getDataLocation());
        Assert.assertFalse(testRun.getRunLocation().isArchived());

        Assert.assertNotNull(testRun.getRunName());
        Assert.assertEquals(runName, testRun.getRunName());

        Assert.assertNotNull(testRun.getSampleCartridge());

        Assert.assertEquals(testFlowcell.getLabel(), testRun.getSampleCartridge().getCartridgeBarcode());

        Assert.assertNull(testRun.getOperator());

        Assert.assertEquals(machineName, testRun.getMachineName());

        Assert.assertEquals(runBarcode, testRun.getRunBarcode());

        Assert.assertFalse(testRun.isTestRun());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        runDate = new Date();
        flowcellBarcode = "flowBcode" + runDate.getTime();
        runName = "runTest" + runDate.getTime();
        runBarcode = "runBcode" + runDate.getTime();
        testFlowcell = new IlluminaFlowcell(flowcellBarcode, IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell);
        runPath = "/start/of/run/";
        fullRunPath = runPath + runName;
        dataLocation = new OutputDataLocation(fullRunPath);
        machineName = "Superman";
        testRun = new IlluminaSequencingRun(testFlowcell, runName, runBarcode, machineName, null, false, runDate,
                dataLocation);
    }
}
