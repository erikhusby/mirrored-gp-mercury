package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 3/11/13
 *         Time: 3:27 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SquidConnectorTest extends ContainerTest {

    @Inject
    SquidConnector connector;

    private Date             runDate;
    private SimpleDateFormat format;
    private String           flowcellBarcode;
    private boolean          result;
    private String           runBarcode;
    private String           runFileDirectory;
    public static final int SUCCESS = 201;

    @BeforeMethod
    public void setUp() {

        runDate = new Date();

        format = new SimpleDateFormat("yyyyMMdd");

        flowcellBarcode = "testcaseFlowcell" + runDate.getTime();

        runBarcode = "Run" + format.format(runDate);
        final String runName = "testRunName" + runDate.getTime();
        String baseDirectory = System.getProperty("java.io.tmpdir","/tmp");
        runFileDirectory = baseDirectory;// + File.separator + "bin" + File.separator + "testRoot" + File.separator + "finalPath" + runDate.getTime() + File.separator + runName;
        File runFile = new File(runFileDirectory);
        result = runFile.mkdirs();

    }

    @AfterMethod
    public void tearDown() {

    }

    public void testRunCreationConnection() {

        SolexaRunBean connectorData =
                new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "Superman", runFileDirectory, null);


        SquidConnector.SquidResponse response = connector.createRun(connectorData);

        Assert.assertEquals(response.getCode(), SUCCESS);

    }

}
