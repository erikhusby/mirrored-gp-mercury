package org.broadinstitute.gpinformatics.infrastructure.decoder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.barcode.generated.DecodeResponse;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.codehaus.jackson.map.ObjectMapper;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

/**
 * This contains common code used by all clients of Barcode Decoder rest (ie: non-broadcore) services.
 */
@Dependent
public class BarcodeDecoderRestClient extends AbstractJerseyClientService {
    private static final Log log = LogFactory.getLog(BarcodeDecoderRestClient.class);

    @Inject
    private BarcodeDecoderConfig barcodeDecoderConfig;

    public BarcodeDecoderRestClient() {
    }

    public BarcodeDecoderRestClient(BarcodeDecoderConfig barcodeDecoderConfig) {
        this.barcodeDecoderConfig = barcodeDecoderConfig;
    }

    public String getUrl(String urlSuffix) {
        return barcodeDecoderConfig.getUrl("decoder/" + urlSuffix);
    }


    @Override
    protected void customizeClient(Client client) {
    }

    public WebResource getWebResource(String urlString) {
        return getJerseyClient().resource(urlString);
    }

    public DecodeResponse analyzeImage(File file, String eventClass) throws IOException {
        String url = getUrl("image");
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
                file,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        fileDataBodyPart.setContentDisposition(
                FormDataContentDisposition.name("file")
                        .fileName(file.getName()).build());

        final MultiPart multiPart = new FormDataMultiPart().bodyPart(fileDataBodyPart);
        multiPart.bodyPart(new FormDataBodyPart("eventClass", eventClass));
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        ClientResponse response = getWebResource(url)
                .type(MediaType.MULTIPART_FORM_DATA_TYPE).accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, multiPart);
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String responseEntity = response.getEntity(String.class);
            log.error("Error when calling decode server: " + responseEntity);
            throw new RuntimeException("POST to " + url + " returned: " + responseEntity);
        } else {
            String json = response.getEntity(String.class);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, DecodeResponse.class);
        }
    }
}
