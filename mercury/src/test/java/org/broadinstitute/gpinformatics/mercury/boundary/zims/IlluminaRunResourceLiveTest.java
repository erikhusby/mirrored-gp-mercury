package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test against a full deployment of the pipeline API (no mocks).
 */
public class IlluminaRunResourceLiveTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, groups = EXTERNAL_INTEGRATION)
    @RunAsClient
    public void testMercury(@ArquillianResource URL baseUrl) throws Exception {
        ZimsIlluminaRun run = getZimsIlluminaRun(baseUrl, "140108_SL-HDJ_0272_BFCH7A84ADXX");

        Assert.assertEquals(run.getLanes().size(), 2, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 20.0);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 5, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "0154850236_Illumina_P5-Yahih_P7-Zepon");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "12/17/2013 12:50");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, groups = EXTERNAL_INTEGRATION)
    @RunAsClient
    public void testThrift(@ArquillianResource URL baseUrl) throws Exception {
        ZimsIlluminaRun run = getZimsIlluminaRun(baseUrl, "120910_SL-HBL_0218_BFCD15B6ACXX");

        Assert.assertEquals(run.getLanes().size(), 8, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 14.5);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 94, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "Pond-176198");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "08/22/2012 12:40");
    }

    private ZimsIlluminaRun getZimsIlluminaRun(URL baseUrl, String runName) {
        String url = baseUrl.toExternalForm() + IlluminaRunResourceTest.WEBSERVICE_URL;
        DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        return Client.create(clientConfig).resource(url)
                .queryParam("runName", runName)
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
    }
}
