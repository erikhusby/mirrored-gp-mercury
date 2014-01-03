package org.broadinstitute.gpinformatics.athena.boundary.orders;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.AUTO_BUILD;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PDOSampleBillingStatusResourceTest extends RestServiceContainerTest {


    @Deployment
    public static WebArchive buildMercuryWar() {
       return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }


    /*
    List<PDOSamplePair> pdoSamplesList = new ArrayList<>();
        pdoSamplesList.add(new PDOSamplePair("PDO-123", "SM-456"));
        PDOSamplePairs pdoSamplePairs = new PDOSamplePairs();
        pdoSamplePairs.setPdoSamplePairs(pdoSamplesList);

        JSONJAXBContext context = new JSONJAXBContext(PDOSamplePairs.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();
        marshaller.marshallToJSON(pdoSamplePairs, writer);
        System.out.println(writer.toString());
        System.out.println("Done");
     */


    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testPDOSampleBilling(@ArquillianResource URL baseUrl) throws Exception {
        // todo arz check the base url for 0.0.0.0 or localhost
        List<PDOSamplePair> pdoSamplesList = new ArrayList<>();
        pdoSamplesList.add(new PDOSamplePair("PDO-123", "SM-456",null));
        PDOSamplePairs pdoSamplePairs = new PDOSamplePairs();
        pdoSamplePairs.setPdoSamplePairs(pdoSamplesList);

        // todo arz figure out why 0.0.0.0 barfs
        URL fixedUrl = new URL(baseUrl.getProtocol(),"127.0.0.1",baseUrl.getPort(),baseUrl.getFile());


        // todo arz put in assertions
        PDOSamplePairs samplePairs = makeWebResource(fixedUrl,"pdoSampleBillingStatus")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .entity(pdoSamplePairs)
                .post(PDOSamplePairs.class);
        System.out.println("got it");
    }


}
