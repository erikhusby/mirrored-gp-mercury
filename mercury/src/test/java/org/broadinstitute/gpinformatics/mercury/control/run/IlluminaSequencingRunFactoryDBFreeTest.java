package org.broadinstitute.gpinformatics.mercury.control.run;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Date;
import java.util.Random;

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

    @BeforeMethod
    public void setUp() {



        testMachine = "Superman";
        runBarcode = "123_TestFlow";

        flowcellTestBarcode = "flowTestBcode123";
        IlluminaFlowcell testFlowcell =
                new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE, flowcellTestBarcode);

        IlluminaFlowcellDao mockDao = EasyMock.createMock(IlluminaFlowcellDao.class);
        EasyMock.expect(mockDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(testFlowcell);
        EasyMock.replay(mockDao);

        runFactory = new IlluminaSequencingRunFactory(mockDao);

        testRunName = "run" + (new Date()).getTime();
        runFileDirectory = "testRoot" + File.separator + "finalPath" + File.separator + testRunName;

        File runFile = new File(runFileDirectory);
        boolean fileSuccess = true;
        if (!runFile.exists()) {
            fileSuccess = runFile.mkdirs();
        }
        if (!fileSuccess) {
            Assert.fail("Unable to setup test run directory for testing sequencingRunFactoryTest");
        }

    }

    @AfterMethod
    public void tearDown() {
        File cleanupFile = new File(runFileDirectory);

        cleanupFile.delete();
    }

    public void testIlluminaFactoryBuild() {

        SolexaRunBean testRunBean =
                new SolexaRunBean(flowcellTestBarcode, runBarcode, new Date(), testMachine, runFileDirectory, null);

        IlluminaSequencingRun testRun = runFactory.build(testRunBean);

        Assert.assertEquals(testMachine,testRun.getMachineName());

        Assert.assertNotNull(testRun.getSampleCartridge());

        Assert.assertEquals(flowcellTestBarcode, testRun.getSampleCartridge().getLabel());

        Assert.assertEquals(runBarcode, testRun.getRunBarcode());

        Assert.assertEquals(testRunName, testRun.getRunName());

        Assert.assertNotNull(testRun.getRunLocation());

        Assert.assertEquals(runFileDirectory, testRun.getRunLocation().getDataLocation());

        Assert.assertFalse(testRun.getRunLocation().isArchived());

        Assert.assertFalse(testRun.isTestRun());

        Assert.assertNotNull(testRun.getRunDate());

    }

}
