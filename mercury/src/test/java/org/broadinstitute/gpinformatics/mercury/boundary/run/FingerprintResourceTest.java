package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the Fingerprint web service.
 */
@Test(groups = TestGroups.STANDARD)
public class FingerprintResourceTest extends Arquillian {

    private static final String WS_BASE = "rest/external/fingerprint";
    @Inject
    private FingerprintResource fingerprintResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testStoreAndRetrieve(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(true);

        String rsidsUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/rsids";
        RsIdsBean rsIdsBean = client.resource(rsidsUrl).type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON).get(RsIdsBean.class);

        String postUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE;
        List<FingerprintCallsBean> calls = new ArrayList<>();
        for (String rsid : rsIdsBean.getRsids()) {
            calls.add(new FingerprintCallsBean(rsid, "AA", "99.99"));
        }

        String aliquotLsid = "org.broad:" + System.currentTimeMillis();
        FingerprintBean fingerprintBean = new FingerprintBean("", "P", aliquotLsid,
                "FLUIDIGM", "HG19", "FluidigmFPv5", new Date(), "M", calls);
        String response = client.resource(postUrl).type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON).entity(fingerprintBean).post(String.class);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        FingerprintsBean fingerprintsBean = client.resource(getUrl).queryParam("lsids", aliquotLsid).
                accept(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        // todo jmt asserts
        fingerprintsBean.getFingerprints();
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testRetrieveBackfill(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(true);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        String queryLsid = "broadinstitute.org:bsp.prod.sample:GOHM6";
        FingerprintsBean fingerprintsBean = client.resource(getUrl).
                queryParam("lsids", queryLsid).
                accept(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);

        Assert.assertEquals(fingerprintsBean.getFingerprints().size(), 1);
        FingerprintBean fingerprintBean = fingerprintsBean.getFingerprints().get(0);
        Assert.assertEquals(fingerprintBean.getQueriedLsid(), queryLsid);
        Assert.assertEquals(fingerprintBean.getAliquotLsid(), "broadinstitute.org:bsp.prod.sample:GP3T6");
        Assert.assertEquals(fingerprintBean.getCalls().size(), 96);
    }

    /**
     * DO NOT DISABLE OR QUARANTINE THIS TEST.  It verifies that authentication is required to access the fingerprint
     * web service, which is visible to the outside world.
     */
    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testNoAuth(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(false);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        boolean exception = false;
        try {
            FingerprintsBean fingerprintsBean = client.resource(getUrl).
                    queryParam("lsids", "broadinstitute.org:bsp.prod.sample:GOHM6").
                    accept(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("401 Unauthorized"));
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    private Client getClient(boolean basicAuth) {
        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));
        if (basicAuth) {
            client.addFilter(new HTTPBasicAuthFilter("thompson", "password"));
        }
        return client;
    }
}