package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Matthews
 *         Date: 3/11/13
 *         Time: 3:27 PM
 */
@Test(groups = TestGroups.STANDARD)
public class SquidConnectorTest extends Arquillian {

    @Inject
    SquidConnector connector;

    private Date   runDate;
    private String flowcellBarcode;
    private String runBarcode;
    private String runDirectory;

    @BeforeMethod
    public void setUp() throws Exception {

        if (connector == null) {
            return;
        }

        runDate = new Date();

        flowcellBarcode = "testcaseFlowcell" + runDate.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        runBarcode = "Run" + dateFormat.format(runDate);
        final String runName = "testRunName" + runDate.getTime();
        runDirectory = File.createTempFile(runName, "txt").getAbsolutePath();

    }

    @AfterMethod
    public void tearDown() {

        if (connector == null) {
            return;
        }

    }

    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testRunCreationConnection() throws Exception {

        SolexaRunBean connectorData =
                new SolexaRunBean(flowcellBarcode, runBarcode, runDate, "Superman", runDirectory, null);

        SquidConnector.SquidResponse response = connector.createRun(connectorData);

        Assert.assertEquals(response.getCode(), Response.Status.CREATED.getStatusCode());

    }

}
