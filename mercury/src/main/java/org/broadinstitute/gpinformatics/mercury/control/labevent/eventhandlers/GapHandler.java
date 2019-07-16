package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.infrastructure.gap.GAPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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
            WebTarget webTarget = gapRestClient.getWebResource(urlString);
            // todo jmt reduce copy / paste
            Response response = webTarget.request().post(Entity.xml(message));

            // This is called in context of bettalims message handling which handles errors via RuntimeException.
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                String entity = response.readEntity(String.class);
                response.close();
                throw new RuntimeException("POST to " + urlString + " returned: " + entity);
            }
            response.close();
        }
    }
}
