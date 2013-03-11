package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test run registration web service
 */
@Test(groups = EXTERNAL_INTEGRATION)
public class SolexaRunResourceTest extends ContainerTest {

    @Inject
    IlluminaSequencingRunDao runDao;

    @Inject
    IlluminaFlowcellDao flowcellDao;

    @Inject
    UserTransaction utx;
    private Date runDate;
    private SimpleDateFormat format;
    private String flowcellBarcode;
    private IlluminaFlowcell newFlowcell;
    private boolean result;
    private String runBarcode;
    private String runFileDirectory;

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {


        if (utx == null) {
            return;
        }
        utx.begin();

        runDate = new Date();

        format = new SimpleDateFormat("yyyyMMdd");

        flowcellBarcode = "testcaseFlowcell" + runDate.getTime();

        newFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                flowcellBarcode);

        flowcellDao.persist(newFlowcell);
        flowcellDao.flush();
        flowcellDao.clear();

        utx.commit();

        runBarcode = "Run" + format.format(runDate);
        final String runName = "testRunName" + runDate.getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir");
        runFileDirectory = baseDirectory + File.separator + "bin" + File.separator +
                "testRoot" + File.separator + "finalPath" + runDate.getTime() +
                File.separator + runName;
        File runFile = new File(runFileDirectory);
        result = runFile.mkdirs();
    }

    @AfterMethod(groups = EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.begin();

        newFlowcell = flowcellDao.findByBarcode(flowcellBarcode);
        flowcellDao.remove(newFlowcell);

        utx.commit();
    }

    @Test(enabled = true, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testCreateRun() {


        Assert.assertTrue(result);

//        try {

        String response = Client.create().resource(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/solexarun")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "SL-HAL",
                        runFileDirectory, null)).post(String.class);
        System.out.println(response);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        IlluminaFlowcell createdFlowcell = flowcellDao.findByBarcode(flowcellBarcode);

    }
}
