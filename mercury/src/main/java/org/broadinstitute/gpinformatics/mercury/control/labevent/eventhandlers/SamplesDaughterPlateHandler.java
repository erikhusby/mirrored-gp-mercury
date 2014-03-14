/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ObjectFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles an automated daughter plate creation message by passing the bettalims message to BSP,
 * which does the aliquoting and transfer.
 */
public class SamplesDaughterPlateHandler extends AbstractEventHandler {
    private static final String PLATE_TRANSFER_URL_PART = "plate/transfer";
    private static final Log logger = LogFactory.getLog(SamplesDaughterPlateHandler.class);
    private ObjectFactory factory = new ObjectFactory();

    @Inject
    private BSPRestClient bspRestClient;

    // Since this is called in context of bettalims message handling which must continue regardless,
    // any error encountered can only be logged and not passed up from here.
    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        PlateTransferEventType transferEvent = OrmUtil.proxySafeCast(stationEvent, PlateTransferEventType.class);

        // Renames the event to an equivalent event, but one that Mercury will not subsequently
        // pass to BSP.  This breaks the loop that would otherwise exist when BSP passes the message
        // to Mercury as part of its processing.
        stationEvent.setEventType(LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName());

        // Recreates a bettalims message to pass to BSP.
        BettaLIMSMessage message = factory.createBettaLIMSMessage();
        message.getPlateTransferEvent().add(transferEvent);

        String urlString = bspRestClient.getUrl(PLATE_TRANSFER_URL_PART);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, message);

        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException(
                    "POST to " + urlString + " returned " + response.getClientResponseStatus().getReasonPhrase());

        }

    }
}
