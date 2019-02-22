package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RegisterNonBroadTubesBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitReceivedBean;
import org.glassfish.jersey.client.ClientResponse;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;

/**
 * Grabs Sample Kit Info from BSP via a BSP REST service.
 */
@Dependent
public class BSPRestService implements Serializable {
    public static final String BSP_SAMPLE_KIT_INFO = "sampleKit/getSampleKitDetails";
    public static final String BSP_CONTAINER_SAMPLE_INFO = "container/getSampleInfo";
    public static final String BSP_RECEIVE_NON_BROAD_SAMPLE = "sampleKit/receiveNonBroadTubes";

    private static final Log logger = LogFactory.getLog(BSPRestService.class);

    @Inject
    private BSPRestClient bspRestClient;

    public SampleKitInfo getSampleKitDetails(String kitId) {

        String urlString = bspRestClient.getUrl(BSP_SAMPLE_KIT_INFO);
        WebTarget webResource = bspRestClient.getWebResource(urlString).queryParam("kit_id", kitId);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.request(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.readEntity(String.class));
        }

        return response.readEntity(SampleKitInfo.class);
    }

    public GetSampleDetails.SampleDetails getSampleInfoForContainer(String containerId) {
        String urlString = bspRestClient.getUrl(BSP_CONTAINER_SAMPLE_INFO);
        WebTarget webResource = bspRestClient.getWebResource(urlString).queryParam("containerBarcode", containerId);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.request(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.readEntity(String.class));
        }

        return response.readEntity(GetSampleDetails.SampleDetails.class);
    }

    /**
     * Receives non broad samples within BSP, validate that they can be received and receives the samples within Mercury.
     *
     * @param sampleToCollaborator Map of BSP Sample IDs to Collaborator Sample ID which are to be received.
     * @param username Username of the operator
     * @return SampleKitReceiptResponse returned from BSP.
     */
    public SampleKitReceivedBean receiveNonBroadSamples(Map<String, String> sampleToCollaborator, String username) {
        RegisterNonBroadTubesBean registerNonBroadTubesBean = new RegisterNonBroadTubesBean();
        for (Map.Entry<String, String> entry: sampleToCollaborator.entrySet()) {
            RegisterNonBroadTubesBean.RegisterNonBroadTubeBean registerNonBroadTubeBean =
                    new RegisterNonBroadTubesBean.RegisterNonBroadTubeBean();
            registerNonBroadTubeBean.setSampleKey(entry.getKey());
            registerNonBroadTubeBean.setCollaboratorSampleId(entry.getValue());
            registerNonBroadTubesBean.getRegisterTubeBeans().add(registerNonBroadTubeBean);
        }
        registerNonBroadTubesBean.setUsername(username);

        String urlString = bspRestClient.getUrl(BSP_RECEIVE_NON_BROAD_SAMPLE);

        WebTarget webResource = bspRestClient.getWebResource(urlString);

        ClientResponse response = webResource.request(MediaType.APPLICATION_XML)
                .post(Entity.xml(registerNonBroadTubesBean), ClientResponse.class);

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            SampleKitReceivedBean receiptResponse = new SampleKitReceivedBean(false);
            logger.warn("POST to " + urlString + " returned: " + response.readEntity(String.class));
            return receiptResponse;
        }

        return response.readEntity(SampleKitReceivedBean.class);
    }

    public void setBspRestClient(BSPRestClient bspRestClient) {
        this.bspRestClient = bspRestClient;
    }
}
