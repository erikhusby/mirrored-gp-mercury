package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 3/7/13
 *         Time: 1:46 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class IlluminaSequencingRunFactoryDBFreeTest {

    IlluminaSequencingRunFactory runFactory;
    private String flowcellTestBarcode;
    private String testRunName;
    private String runFileDirectory;
    private String testMachine;
    private String runBarcode;
    private IlluminaFlowcell testFlowcell;
    private Date runDate;

    @BeforeMethod
    public void setUp() {


        testMachine = "Superman";

        flowcellTestBarcode = "flowTestBcode123";
        runDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        runBarcode = flowcellTestBarcode + format.format(runDate);
        testFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, flowcellTestBarcode);

        runFactory = new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));

        testRunName = "run" + (new Date()).getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir");

        runFileDirectory = baseDirectory + File.separator + "testRoot" + File.separator + "finalPath" + File.separator + testRunName;

    }

    @AfterMethod
    public void tearDown() {
        File cleanupFile = new File(runFileDirectory);

        cleanupFile.delete();
    }

    public void testIlluminaFactoryBuild() {

        SolexaRunBean testRunBean =
                new SolexaRunBean(flowcellTestBarcode, runBarcode, runDate, testMachine, runFileDirectory, null);

        IlluminaSequencingRun testRun = runFactory.build(testRunBean, testFlowcell);

        Assert.assertNotNull(testRun);

        Assert.assertEquals(testMachine, testRun.getMachineName());

        Assert.assertNotNull(testRun.getSampleCartridge());

        Assert.assertEquals(flowcellTestBarcode, testRun.getSampleCartridge().getLabel());

        Assert.assertEquals(runBarcode, testRun.getRunBarcode());

        Assert.assertEquals(testRunName, testRun.getRunName());

        Assert.assertNotNull(testRun.getRunDirectory());

        Assert.assertEquals(runFileDirectory, testRun.getRunDirectory());

        Assert.assertFalse(testRun.isTestRun());

        Assert.assertNotNull(testRun.getRunDate());

    }

}
