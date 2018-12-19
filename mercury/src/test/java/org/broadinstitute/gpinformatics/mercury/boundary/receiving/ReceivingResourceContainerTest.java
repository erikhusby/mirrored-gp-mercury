package org.broadinstitute.gpinformatics.mercury.boundary.receiving;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ReceiveSamplesEjb;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitInfo;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Database test for the Receiving Resource
 */
@Test(groups = TestGroups.STANDARD, singleThreaded = true)
public class ReceivingResourceContainerTest extends Arquillian {

    @Inject
    private ReceivingResource receivingResource;

    @Inject
    private BSPUserList bspUserList;

    private ReceiveSamplesEjb mockReceiveSamplesEjb;

    private String unknownTubeBarcode = "FailTubeBarcode01231124";
    private String unknownSampleId = "SM-ImUnknown1234";
    private String knownTerminatedSample = "SM-I7Y5W";
    private String knownShippedSample = "SM-IENNB";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod
    public void setUp() {
        if (receivingResource != null) {
            mockReceiveSamplesEjb = mock(ReceiveSamplesEjb.class);
            receivingResource.setReceiveSamplesEjb(mockReceiveSamplesEjb);

            bspUserList = mock(BSPUserList.class);
            BspUser bspUser = new BspUser();
            bspUser.setUsername("jowalsh");
            when(bspUserList.getByUsername("jowalsh")).thenReturn(bspUser);
        }
    }

    @Test
    public void testReceiveWithUnknownsFails() {
        List<String> tubeIdentifiers = Arrays.asList(unknownSampleId, unknownTubeBarcode);
        Response response = receivingResource.receiveBySamplesAndDDP(tubeIdentifiers, "jowalsh");

        Assert.assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        String errMsg = (String) response.getEntity();
        Assert.assertThat(errMsg, containsString(String.format("Unknown tube barcodes: %s", unknownTubeBarcode)));
        Assert.assertThat(errMsg, containsString(String.format("Unknown sample IDs: %s", unknownSampleId)));
    }

    @Test
    public void testReceiveWithNoneInReceivableStateWarns() {
        List<String> tubeIdentifiers = Collections.singletonList(knownTerminatedSample);
        Response response = receivingResource.receiveBySamplesAndDDP(tubeIdentifiers, "jowalsh");

        Assert.assertThat(response.getStatus(), is(Response.Status.NOT_MODIFIED.getStatusCode()));
        String errMsg = (String) response.getEntity();
        Assert.assertThat(errMsg, containsString("None of the samples are in state ready to be 'Received'"));
    }

    @Test
    public void testShippedSampleAttemptsToReceive() throws Exception {
        SampleKitReceiptResponse kitReceiptResponse = new SampleKitReceiptResponse();
        when(mockReceiveSamplesEjb.receiveSamples(anyMapOf(String.class, SampleKitInfo.class),
                (List<String>) Mockito.argThat(Matchers.contains(knownShippedSample)), any(), any()))
                .thenReturn(kitReceiptResponse);

        List<String> tubeIdentifiers = Collections.singletonList(knownShippedSample);
        Response response = receivingResource.receiveBySamplesAndDDP(tubeIdentifiers, "jowalsh");

        Assert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }
}