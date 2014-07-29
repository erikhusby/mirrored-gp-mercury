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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles bettalims messages that need to be passed to a BSP REST service for processing.
 */
public class SamplesDaughterPlateHandler {
    public static final String BSP_TRANSFER_REST_URL = "plate/transfer";
    private static final Log logger = LogFactory.getLog(SamplesDaughterPlateHandler.class);
    private ObjectFactory factory = new ObjectFactory();

    @Inject
    private BSPRestClient bspRestClient;

    public void postToBsp(StationEventType stationEvent, String bspRestUrl) {

        // Creates a bettalims message to pass to BSP.
        BettaLIMSMessage message = factory.createBettaLIMSMessage();
        if (OrmUtil.proxySafeIsInstance(stationEvent, PlateTransferEventType.class)) {
            PlateTransferEventType transferEvent = OrmUtil.proxySafeCast(stationEvent, PlateTransferEventType.class);
            message.getPlateTransferEvent().add(transferEvent);
        } else if (OrmUtil.proxySafeIsInstance(stationEvent, PlateCherryPickEvent.class)) {
            PlateCherryPickEvent event = OrmUtil.proxySafeCast(stationEvent, PlateCherryPickEvent.class);
            message.getPlateCherryPickEvent().add(event);
        }

        // Posts message to BSP using the specified REST url.
        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, message);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("POST to " + urlString + " returned: " + response.getEntity(String.class));
        }

    }
}
