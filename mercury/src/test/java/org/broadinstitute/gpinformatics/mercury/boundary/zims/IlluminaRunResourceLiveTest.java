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
    public void testZimsOverHttp(@ArquillianResource URL baseUrl) throws Exception {
        String url = baseUrl.toExternalForm() + IlluminaRunResourceTest.WEBSERVICE_URL;
        DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        ZimsIlluminaRun run = Client.create(clientConfig).resource(url)
                .queryParam("runName", "140108_SL-HDJ_0272_BFCH7A84ADXX")
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);

        Assert.assertEquals(run.getLanes().size(), 2, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 20.0);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 5, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "0154850237_Illumina_P5-Wizap_P7-Jatod");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "12/17/2013 12:50");
    }
}
