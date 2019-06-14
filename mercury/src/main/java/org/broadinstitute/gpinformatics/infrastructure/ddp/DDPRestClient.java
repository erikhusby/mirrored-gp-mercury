package org.broadinstitute.gpinformatics.infrastructure.ddp;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.DDPBarcodeRegistration;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.DDPKitInfo;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This contains common code used by all clients of DDP rest services.
 */
@Dependent
public class DDPRestClient extends AbstractJaxRsClientService {
    private Logger log = Logger.getLogger(DDPConfig.class);

    public static final String DDP_KIT_INFO = "Kits/";
    public static final String DDP_KITS_REGISTERED = "KitsRegistered?";

    @Inject
    private DDPConfig ddpConfig;

    public DDPRestClient() {
    }

    public DDPRestClient(DDPConfig ddpConfig) {
        this.ddpConfig = ddpConfig;
    }

    public Optional<DDPKitInfo> getKitInfo(String kitBarcode) {
        String urlString = getUrl(DDP_KIT_INFO + kitBarcode);

        WebTarget webResource = getWebResource(urlString);
        Response response = webResource.request(MediaType.APPLICATION_JSON).get();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            log.warn("GET to " + urlString + " returned: " + response.readEntity(String.class));
            response.close();
            return Optional.empty();
        }

        DDPKitInfo ddpKitInfo = response.readEntity(DDPKitInfo.class);
        ddpKitInfo.setManufacturerBarcode(kitBarcode);
        response.close();
        return Optional.of(ddpKitInfo);
    }

    public Map<String, Boolean> areKitsRegistered(List<String> kitBarcodes) {
        String queryParams = makeQueryString("barcode", kitBarcodes);

        String urlString = getUrl(DDP_KITS_REGISTERED + queryParams);

        WebTarget webResource = getWebResource(urlString);

        Response response = webResource.request(MediaType.APPLICATION_JSON).get();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            log.warn("GET to " + urlString + " returned: " + response.readEntity(String.class));
            response.close();
            return null;
        }

        List<DDPBarcodeRegistration> ddpKitInfo = response.readEntity(
                new GenericType<List<DDPBarcodeRegistration>>() {});

        Map<String, Boolean> mapBarcodeToDsmFlag = new HashMap<>();
        for (String barcode: kitBarcodes) {
            boolean found = false;
            for (DDPBarcodeRegistration ddpBarcodeRegistration: ddpKitInfo) {
                if (barcode.equals(ddpBarcodeRegistration.getBarcode())) {
                    mapBarcodeToDsmFlag.put(barcode, ddpBarcodeRegistration.getDsmKit());
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException("Failed to find barcode " + barcode);
            }
        }
        response.close();
        return mapBarcodeToDsmFlag;
    }

    public String getUrl(String urlSuffix) {
        return ddpConfig.getUrl(urlSuffix);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyAuthorizationToken(client, ddpConfig);
    }

    public WebTarget getWebResource(String urlString) {
        return getJaxRsClient().target(urlString);
    }
}
