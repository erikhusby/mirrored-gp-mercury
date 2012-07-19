package org.broadinstitute.sequel.boundary.zims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchServiceProducer;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.jmx.ZimsCacheControl;
import org.broadinstitute.sequel.infrastructure.thrift.*;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.broadinstitute.sequel.integration.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EmptyReadsIlluminaRunResourceTest extends ContainerTest {

    @Inject
    IlluminaRunResource runLaneResource;

    private TZamboniRun zamboniRun;

    public static final String RUN_NAME = "120718_M00158_0008_AFC000000000-A13AJ";

    private final String WEBSERVICE_URL = "rest/IlluminaRun/query";

    public static final String HUMAN = "Human";

    public static final String BSP_HUMAN = "Homo : Homo sapiens";

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildSequelWar() {
        return DeploymentBuilder.buildSequelWar(Deployment.PROD);
    }

    @Test(groups = EXTERNAL_INTEGRATION,dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER,enabled = true)
    @RunAsClient
    public void test_empty_reads(@ArquillianResource URL baseUrl) throws Exception {
        String url = baseUrl.toExternalForm() + WEBSERVICE_URL;

        DefaultClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);


        ZimsIlluminaRun run = Client.create(clientConfig).resource(url)
                .queryParam("runName", RUN_NAME)
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertFalse(run.getReads().isEmpty());
        run = Client.create(clientConfig).resource(url)
                .queryParam("runName", "120717_SL-MAE_0107_AMS2006894-00300")
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertFalse(run.getReads().isEmpty());
        run = Client.create(clientConfig).resource(url)
                .queryParam("runName", "120716_SL-MAJ_0009_AFC000000000-A125E")
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertFalse(run.getReads().isEmpty());
        String rawJson = Client.create(clientConfig).resource(url)
                .queryParam("runName", "120717_SL-MAE_0107_AMS2006894-00300")
                .accept(MediaType.APPLICATION_JSON).get(String.class);
        run = Client.create(clientConfig).resource(url)
                .queryParam("runName", "120717_SL-MAE_0107_AMS2006894-00300")
                .accept(MediaType.APPLICATION_JSON).get(ZimsIlluminaRun.class);
        assertFalse(run.getReads().isEmpty());

        System.out.println(rawJson);
    }

}
