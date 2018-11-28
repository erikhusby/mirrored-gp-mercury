package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.gap.GAPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Forwards Arrays messages to GAP.
 */
@Dependent
public class GapHandler {

    @Inject
    private GAPRestClient gapRestClient;

    public void postToGap(BettaLIMSMessage message) {
        // When backfilling messages from GAP to Mercury, avoid sending them to GAP
        if (message.getMode() == null || !message.getMode().equals(LabEventFactory.MODE_BACKFILL)) {
            // Posts message to BSP using the specified REST url.
            String urlString = gapRestClient.getUrl("bettalims");
            WebResource webResource = gapRestClient.getWebResource(urlString);
            // todo jmt reduce copy / paste
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, message);

            // This is called in context of bettalims message handling which handles errors via RuntimeException.
            if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new RuntimeException("POST to " + urlString + " returned: " + response.getEntity(String.class));
            }
        }
    }
}
