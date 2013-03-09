package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test run registration web service
 */
public class SolexaRunResourceTest extends ContainerTest {

    @Inject
    IlluminaSequencingRunDao runDao;

    @Inject
    IlluminaFlowcellDao flowcellDao;

    @Test(enabled = true, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testCreateRun(@ArquillianResource URL baseUrl) {
        final Date runDate = new Date();

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

        final String flowcellBarcode = "testcaseFlowcell" + runDate.getTime();

        IlluminaFlowcell newFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.TWO_LANE,
                                                                   flowcellBarcode);

        flowcellDao.persist(newFlowcell);
        flowcellDao.flush();
        flowcellDao.clear();

        final String runBarcode = "Run" + format.format(runDate);
        final String runName = "testRunName"+runDate.getTime();
        String baseDirectory =System.getProperty("JBOSS_HOME", "./");
        final String runFileDirectory = baseDirectory+File.separator + "bin"  +File.separator +
                                                "testRoot" + File.separator + "finalPath"+runDate.getTime() +
                                                File.separator+runName;
        File runFile = new File(runFileDirectory);
        boolean result = runFile.mkdirs();
        Assert.assertTrue(result);

//        try {

            String response = Client.create().resource(baseUrl.toExternalForm() + "rest/solexarun")
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
