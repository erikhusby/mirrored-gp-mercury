package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test Designation web service.
 */
@Test(groups = TestGroups.STUBBY)
public class DesignationResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testBasics(@ArquillianResource URL baseUrl) throws MalformedURLException {
        DesignationBean designationBean = new DesignationBean();
        designationBean.setTubeBarcode("AB51462527");
        designationBean.setNumLanes(4);
        designationBean.setUserId("thompson");
        designationBean.setPoolTestFlowcell("H7LMCALXX");
        designationBean.setPoolTestFlowcellLane(8);
        designationBean.setTargetSize(2745);
        designationBean.setTargetCoverage(30);
        designationBean.setLaneYield(135);
        designationBean.setSeqPenalty(new BigDecimal("1.25"));
        WebResource resource = makeWebResource(baseUrl, null);
        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE).entity(designationBean).
                post(ClientResponse.class);
        Assert.assertEquals(response.getStatus(), ClientResponse.Status.OK.getStatusCode());
    }

    @Override
    protected String getResourcePath() {
        return "designation";
    }
}