package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class VesselResourceTest extends Arquillian {

    @Inject
    private VesselResource vesselResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    /**
     * Simple tube registration test with two known barcodes, this data should never become invalid.
     */
    @Test
    public void testTubeRegistration() {

        Map<String, String> matrixToSample = new HashMap<>();
        matrixToSample.put("0101584604", "SM-1TNRT");
        matrixToSample.put("0097401641", "SM-1TNRR");

        // This does not actually call the webservice but provides a MultiValuedMap that looks like what the
        // VesselResource should be receiving.
        MultivaluedMap<String, String> parameterMap = new MultivaluedMapImpl();
        for (String barcode : matrixToSample.keySet()) {
            parameterMap.add("barcodes", barcode);
        }

        // Check the HTTP Status.
        Response response = vesselResource.registerTubes(parameterMap);
        assertThat(response.getStatus(), is(equalTo(Response.Status.OK.getStatusCode())));

        // Make sure the returned entity is not null and of the expected type.
        Object responseEntity = response.getEntity();
        assertThat(responseEntity, is(notNullValue()));
        assertThat(responseEntity, is(instanceOf(RegisterTubesBean.class)));

        // Make sure the returned entity has the expected number of contained tube beans.
        RegisterTubesBean registerTubesBean = (RegisterTubesBean) responseEntity;
        assertThat(registerTubesBean.getRegisterTubeBeans(), hasSize(matrixToSample.size()));

        for (RegisterTubeBean tubeBean : registerTubesBean.getRegisterTubeBeans()) {
            String sampleBarcode = tubeBean.getSampleId();
            String matrixBarcode = tubeBean.getBarcode();

            // Make sure the returned tube beans have the expected sample barcodes.
            assertThat(sampleBarcode, is(notNullValue()));
            assertThat("map contains key", matrixToSample.containsKey(matrixBarcode));
            assertThat(matrixToSample.get(matrixBarcode), is(equalTo(sampleBarcode)));
        }
    }
}
