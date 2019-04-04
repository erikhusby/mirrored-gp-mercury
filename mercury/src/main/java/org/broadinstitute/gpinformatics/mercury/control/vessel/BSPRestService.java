package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ExternalSamplesRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.IdNames;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RegisterNonBroadTubesBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitReceivedBean;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
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
    public static final String BSP_GET_LABELS = "receptacle/getLabelFormats";
    public static final String BSP_GET_CHILD_RECEPTACLES = "receptacle/getChildReceptacleTypesForBarcode";
    public static final String BSP_RECEIVE_EXT_TUBES = "sampleKit/receiveExternalTubes";
    public static final String BSP_RECEIVE_EXT_MATRIX_TUBES = "sampleKit/receiveExternalMatrixTubes";

    private static final Log logger = LogFactory.getLog(BSPRestService.class);

    @Inject
    private BSPRestClient bspRestClient;

    public SampleKitInfo getSampleKitDetails(String kitId) {

        String urlString = bspRestClient.getUrl(BSP_SAMPLE_KIT_INFO);
        MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
        parameters.add("kit_id", kitId);
        WebResource webResource = bspRestClient.getWebResource(urlString).queryParams(parameters);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.getEntity(String.class));
        }

        return response.getEntity(SampleKitInfo.class);
    }

    public GetSampleDetails.SampleDetails getSampleInfoForContainer(String containerId) {
        String urlString = bspRestClient.getUrl(BSP_CONTAINER_SAMPLE_INFO);
        MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
        parameters.add("containerBarcode", containerId);
        WebResource webResource = bspRestClient.getWebResource(urlString).queryParams(parameters);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.getEntity(String.class));
        }

        return response.getEntity(GetSampleDetails.SampleDetails.class);
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

        WebResource webResource = bspRestClient.getWebResource(urlString);

        ClientResponse response = webResource.accept(MediaType.APPLICATION_XML)
                .post(ClientResponse.class, registerNonBroadTubesBean);

        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            SampleKitReceivedBean receiptResponse = new SampleKitReceivedBean(false);
            logger.warn("POST to " + urlString + " returned: " + response.getEntity(String.class));
            return receiptResponse;
        }

        return response.getEntity(SampleKitReceivedBean.class);
    }

    public IdNames getLabelFormats(String receptacleName) {
        String urlString = bspRestClient.getUrl(BSP_GET_LABELS);
        MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
        parameters.add("receptacleName", receptacleName);
        WebResource webResource = bspRestClient.getWebResource(urlString).queryParams(parameters);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.getEntity(String.class));
        }

        return response.getEntity(IdNames.class);
    }

    public IdNames getChildReceptacleTypesForBarcode(String receptacleBarcode) {
        String urlString = bspRestClient.getUrl(BSP_GET_CHILD_RECEPTACLES);
        MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
        parameters.add("receptacleBarcode", receptacleBarcode);
        WebResource webResource = bspRestClient.getWebResource(urlString).queryParams(parameters);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).get(ClientResponse.class);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("GET to " + urlString + " returned: " + response.getEntity(String.class));
        }

        return response.getEntity(IdNames.class);
    }

    public SampleKitReceivedBean receiveExternalTubes(ExternalSamplesRequest sampleContents) {
        String urlString = bspRestClient.getUrl(BSP_RECEIVE_EXT_TUBES);

        WebResource webResource = bspRestClient.getWebResource(urlString);

        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, sampleContents);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException(response.getEntity(String.class));
        }

        return response.getEntity(SampleKitReceivedBean.class);
    }

    public SampleKitReceivedBean receiveExternalMatrixTubes(ExternalSamplesRequest sampleContents, InputStream inputStream,
                                                            String filename) {
        String urlString = bspRestClient.getUrl(BSP_RECEIVE_EXT_MATRIX_TUBES);

        WebResource webResource = bspRestClient.getWebResource(urlString);

        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            formDataMultiPart.field("externalSamplesRequest", sampleContents, MediaType.APPLICATION_XML_TYPE);
            formDataMultiPart.field("spreadsheetFilename", filename);
            MultiPart multiPart = formDataMultiPart.bodyPart(
                    new FormDataBodyPart("spreadsheet", inputStream,
                            MediaType.APPLICATION_OCTET_STREAM_TYPE));
            ClientResponse response =
                    webResource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, multiPart);
            if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new RuntimeException(response.getEntity(String.class));
            }

            return response.getEntity(SampleKitReceivedBean.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBspRestClient(BSPRestClient bspRestClient) {
        this.bspRestClient = bspRestClient;
    }
}
