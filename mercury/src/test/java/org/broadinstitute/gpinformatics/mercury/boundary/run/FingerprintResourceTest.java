package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
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

    @Inject
    private FingerprintResource fingerprintResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testBasics(@ArquillianResource URL baseUrl) throws MalformedURLException {
        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        Client client = Client.create(clientConfig);
        client.addFilter(new LoggingFilter(System.out));

        String rsidsUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/fingerprint/rsids";
        RsIdsBean rsIdsBean = client.resource(rsidsUrl).type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON).get(RsIdsBean.class);

        String postUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/fingerprint";
        List<FingerprintCallsBean> calls = new ArrayList<>();
        for (String rsid : rsIdsBean.getRsids()) {
            calls.add(new FingerprintCallsBean(rsid, "AA", "99.7"));
        }

        String aliquotLsid = "org.broad:" + System.currentTimeMillis();
        FingerprintBean fingerprintBean = new FingerprintBean("", "P", aliquotLsid,
                "FLUIDIGM", "HG19", new Date(), "M", calls);
        String response = client.resource(postUrl).type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON).entity(fingerprintBean).post(String.class);

        String getUrl = RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/fingerprint/query";
        FingerprintsBean fingerprintsBean = client.resource(getUrl).queryParam("lsids", aliquotLsid).
                /*type(MediaType.APPLICATION_JSON_TYPE).*/accept(MediaType.APPLICATION_JSON).get(FingerprintsBean.class);
        fingerprintsBean.getFingerprints();
    }
}