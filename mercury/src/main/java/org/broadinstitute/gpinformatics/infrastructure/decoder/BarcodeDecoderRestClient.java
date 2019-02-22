package org.broadinstitute.gpinformatics.infrastructure.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.barcode.generated.DecodeResponse;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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

    public WebTarget getWebResource(String urlString) {
        return getJerseyClient().target(urlString);
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
                .request(MediaType.MULTIPART_FORM_DATA_TYPE).accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(multiPart, multiPart.getMediaType()),  ClientResponse.class);
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String responseEntity = response.readEntity(String.class);
            log.error("Error when calling decode server: " + responseEntity);
            throw new RuntimeException("POST to " + url + " returned: " + responseEntity);
        } else {
            String json = response.readEntity(String.class);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, DecodeResponse.class);
        }
    }
}
