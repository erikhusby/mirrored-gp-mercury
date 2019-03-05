package org.broadinstitute.gpinformatics.infrastructure.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.barcode.generated.DecodeResponse;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
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
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData("file",
                file,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
//        fileDataBodyPart.setContentDisposition(
//                FormDataContentDisposition.name("file")
//                        .fileName(file.getName()).build());

//        final MultiPart multiPart = new FormDataMultiPart().bodyPart(fileDataBodyPart);
        mdo.addFormData("eventClass", eventClass, MediaType.MULTIPART_FORM_DATA_TYPE);

        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) { };
        Response response = getWebResource(url)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String responseEntity = response.readEntity(String.class);
            log.error("Error when calling decode server: " + responseEntity);
            throw new RuntimeException("POST to " + url + " returned: " + responseEntity);
        } else {
            String json = response.readEntity(String.class);
            ObjectMapper mapper = new ObjectMapper();
            response.close();
            return mapper.readValue(json, DecodeResponse.class);
        }
    }
}
