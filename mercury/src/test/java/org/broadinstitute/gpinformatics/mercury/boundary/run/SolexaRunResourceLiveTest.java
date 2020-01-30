package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResourceLiveTest;
import org.broadinstitute.gpinformatics.mercury.control.EntityLoggingFilter;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchDbTest;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

/**
 * Test SolexaRunResource with no stubs.
 */
@Test(groups = TestGroups.STANDARD)
public class SolexaRunResourceLiveTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(TEST);
    }

    @Test(groups = STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testSquidLanes(@ArquillianResource URL baseUrl) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(LabBatchDbTest.XML_DATE_FORMAT);
        String timeStamp = simpleDateFormat.format(new Date());

        String runName1 = "120907_SL-HBV_0191_BFCD15DDACXX";
        ZimsIlluminaRun zimsIlluminaRun = IlluminaRunResourceLiveTest.getZimsIlluminaRun(baseUrl,
                runName1);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8);
        for (ZimsIlluminaChamber zimsIlluminaChamber : zimsIlluminaRun.getLanes()) {
            Assert.assertTrue(zimsIlluminaChamber.getActualReadStructure().startsWith("STRUC2020-"),
                    "Unexpected value " + zimsIlluminaChamber.getActualReadStructure());
        }
    }
}
