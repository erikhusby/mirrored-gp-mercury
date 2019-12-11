package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.EntityLoggingFilter;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
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

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testStoreAndRetrieve(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(true);

        String rsidsUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/rsids";
        RsIdsBean rsIdsBean = client.target(rsidsUrl).request(MediaType.APPLICATION_JSON_TYPE).get(RsIdsBean.class);

        String postUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE;
        List<FingerprintCallsBean> calls = new ArrayList<>();
        for (String rsid : rsIdsBean.getRsids()) {
            calls.add(new FingerprintCallsBean(rsid, "AA", "99.99"));
        }

        String aliquotLsid = LibraryBean.MERCURY_LSID_PREFIX + System.currentTimeMillis();
        FingerprintBean fingerprintBean = new FingerprintBean("", "P", aliquotLsid,
                "FLUIDIGM", "HG19", "FluidigmFPv5", new Date(), "M", calls);
        String response = client.target(postUrl).request(MediaType.APPLICATION_JSON_TYPE).
                post(Entity.json(fingerprintBean), String.class);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        FingerprintsBean fingerprintsBean = client.target(getUrl).queryParam("lsids", aliquotLsid).
                request(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        // todo jmt asserts
        fingerprintsBean.getFingerprints();
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testRetrieveBackfill(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(true);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        String queryLsid = "broadinstitute.org:bsp.prod.sample:GOHM6";
        FingerprintsBean fingerprintsBean = client.target(getUrl).
                queryParam("lsids", queryLsid).
                request(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);

        Assert.assertEquals(fingerprintsBean.getFingerprints().size(), 2);
        FingerprintBean fingerprintBean = fingerprintsBean.getFingerprints().get(1);
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
            FingerprintsBean fingerprintsBean = client.target(getUrl).
                    queryParam("lsids", "broadinstitute.org:bsp.prod.sample:GOHM6").
                    request(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("401 Unauthorized"));
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testCrsp(@ArquillianResource URL baseUrl) throws MalformedURLException {
        Client client = getClient(true);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + WS_BASE + "/query";
        String queryLsid = "org.broadinstitute:crsp:G947T";
        FingerprintsBean fingerprintsBean = client.target(getUrl).
                queryParam("lsids", queryLsid).
                request(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        Assert.assertNotNull(fingerprintsBean);
    }

    private Client getClient(boolean basicAuth) {
        Client client = JaxRsUtils.getClientBuilderAcceptCertificate().build();

        client.register(new EntityLoggingFilter());
        if (basicAuth) {
            client.register(new BasicAuthentication("thompson", "password"));
        }
        return client;
    }
}