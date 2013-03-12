package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.OutputDataLocation;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 3/7/13
 *         Time: 3:06 PM
 */

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class IlluminaSequencingRunDaoTest extends ContainerTest {


    @Inject
    IlluminaSequencingRunDao runDao;

    @Inject
    IlluminaFlowcellDao flowcellDao;

    @Inject
    UserTransaction utx;


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


    @BeforeMethod
    public void setUp() throws Exception {


        if (utx == null) {
            return;
        }
        utx.begin();

        runDate = new Date();
        flowcellBarcode = "flowBcode" + runDate.getTime();


        IlluminaFlowcell initialFCell =
                new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, flowcellBarcode);

        flowcellDao.persist(initialFCell);
        flowcellDao.flush();
        flowcellDao.clear();

        testFlowcell = flowcellDao.findByBarcode(flowcellBarcode);
        Assert.assertNotNull(testFlowcell);

        runName = "runTest" + runDate.getTime();
        runBarcode = "runBcode" + runDate.getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir");

        runPath = baseDirectory + "/start/of/run/";
        fullRunPath = runPath + runName;
        dataLocation = new OutputDataLocation(fullRunPath);
        machineName = "Superman";
        IlluminaSequencingRun initialRun = new IlluminaSequencingRun(testFlowcell, runName, runBarcode, machineName, null, false, runDate,
                dataLocation);

        runDao.persist(initialRun);
        runDao.flush();
        runDao.clear();

        testRun = runDao.findByRunName(runName);
        testFlowcell = flowcellDao.findByBarcode(flowcellBarcode);
        Assert.assertNotNull(testFlowcell);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    public void testDBReceipt() {

        Assert.assertNotNull(testRun);

        Assert.assertFalse(testRun.isTestRun());

        Assert.assertEquals(runBarcode, testRun.getRunBarcode());
        Assert.assertEquals(runName, testRun.getRunName());

        Assert.assertEquals(fullRunPath, testRun.getRunLocation().getDataLocation());

        Assert.assertEquals(testFlowcell, testRun.getSampleCartridge());

        Assert.assertEquals(1, testFlowcell.getSequencingRuns().size());

        Assert.assertEquals(testRun.getRunName(), testFlowcell.getSequencingRuns().iterator().next().getRunName());

        Assert.assertFalse(testRun.getRunLocation().isArchived());

        Assert.assertEquals(machineName, testRun.getMachineName());

    }


}
