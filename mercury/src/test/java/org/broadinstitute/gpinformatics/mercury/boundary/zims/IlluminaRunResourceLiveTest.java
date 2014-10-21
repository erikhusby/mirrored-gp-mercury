package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.aerogear.arquillian.test.smarturl.SchemeName;
import org.jboss.aerogear.arquillian.test.smarturl.UriScheme;
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
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

/**
 * Test against a full deployment of the pipeline API (no mocks).
 */
@Test(groups = TestGroups.STANDARD)
public class IlluminaRunResourceLiveTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, groups = STANDARD)
    @RunAsClient
    public void testMercury(
            @ArquillianResource @UriScheme(name = SchemeName.HTTPS,
                    port = RestServiceContainerTest.DEFAULT_FORWARD_PORT) URL baseUrl) {
        ZimsIlluminaRun run = getZimsIlluminaRun(baseUrl, "130903_SL-HDG_0177_BFCH16FBADXX");

        Assert.assertEquals(run.getLanes().size(), 2, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 20.0);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 91, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "0148909054_Illumina_P5-Kizez_P7-Cakax");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "08/29/2013 11:51");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, groups = STANDARD)
    @RunAsClient
    public void testThrift(
            @ArquillianResource @UriScheme(name = SchemeName.HTTPS,
                    port = RestServiceContainerTest.DEFAULT_FORWARD_PORT) URL baseUrl) {
        ZimsIlluminaRun run = getZimsIlluminaRun(baseUrl, "120910_SL-HBL_0218_BFCD15B6ACXX");

        Assert.assertEquals(run.getLanes().size(), 8, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 14.5);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 94, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "Pond-176198");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "08/22/2012 12:40");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, groups = STANDARD)
    @RunAsClient
    public void testThriftNullConc(
            @ArquillianResource @UriScheme(name = SchemeName.HTTPS,
                    port = RestServiceContainerTest.DEFAULT_FORWARD_PORT) URL baseUrl) {
        ZimsIlluminaRun run = getZimsIlluminaRun(baseUrl, "120830_SL-MAK_0035_AFC000000000-A1ETN");

        Assert.assertEquals(run.getLanes().size(), 1, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = run.getLanes().iterator().next();
        // todo jmt the following should be null
        Assert.assertEquals(zimsIlluminaChamber.getLoadingConcentration(), 0.0);

        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), 1, "Wrong number of libraries");
        LibraryBean libraryBean = zimsIlluminaChamber.getLibraries().iterator().next();
        Assert.assertEquals(libraryBean.getLibrary(), "Solexa-119567");
        Assert.assertEquals(libraryBean.getLibraryCreationDate(), "08/30/2012 10:06");
    }

    public static ZimsIlluminaRun getZimsIlluminaRun(URL baseUrl, String runName) {
        WebResource.Builder builder = getBuilder(baseUrl, runName);
        return builder.get(ZimsIlluminaRun.class);
    }

    public static String getZimsIlluminaRunString(URL baseUrl, String runName) {
        WebResource.Builder builder = getBuilder(baseUrl, runName);
        return builder.get(String.class);
    }

    private static WebResource.Builder getBuilder(URL baseUrl, String runName) {
        String url = RestServiceContainerTest.convertPortToPresetPort(baseUrl) + IlluminaRunResourceTest.WEBSERVICE_URL;
        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        clientConfig.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, Boolean.TRUE);
        return Client.create(clientConfig).resource(url)
                .queryParam("runName", runName)
                .accept(MediaType.APPLICATION_JSON);
    }
}
