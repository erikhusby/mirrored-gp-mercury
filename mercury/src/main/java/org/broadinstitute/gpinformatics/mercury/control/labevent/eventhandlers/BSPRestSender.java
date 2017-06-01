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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Passes info to BSP via a BSP REST service.
 */
public class BSPRestSender implements Serializable {
    public static final String BSP_TRANSFER_REST_URL = "plate/transfer";
    public static final String BSP_PLATE_EXISTS_URL = "plate/exists";
    public static final String BSP_UPLOAD_QUANT_URL = "quant/upload";
    private static final Log logger = LogFactory.getLog(BSPRestSender.class);

    @Inject
    private BSPRestClient bspRestClient;

    public void postToBsp(BettaLIMSMessage message, String bspRestUrl) {

        // Forward only the events that are for BSP, e.g. Blood Biopsy extraction from blood to plasma and buffy coat
        // is two events in one message, but only one is configured to forward to BSP.
        BettaLIMSMessage copy = new BettaLIMSMessage();
        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebResource webResource = bspRestClient.getWebResource(urlString);
        for (PlateCherryPickEvent plateCherryPickEvent : message.getPlateCherryPickEvent()) {
            if(LabEventType.getByName(plateCherryPickEvent.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
            }
        }
        for (PlateTransferEventType plateTransferEventType : message.getPlateTransferEvent()) {
            if(LabEventType.getByName(plateTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                copy.getPlateTransferEvent().add(plateTransferEventType);
            }
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : message.getReceptacleTransferEvent()) {
            if(LabEventType.getByName(receptacleTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                copy.getReceptacleTransferEvent().add(receptacleTransferEventType);
            }
        }

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, copy);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("POST to " + urlString + " returned: " + response.getEntity(String.class));
        }

    }

    /**
     * Posts to BSP REST url.
     * @param bspUsername bsp username
     * @param filename filename of the input stream
     * @param inputStream the content
     * @param bspRestUrl the relative url of the BSP web service to post to
     */
    public void postToBsp(String bspUsername, String filename, InputStream inputStream, String bspRestUrl) {
        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebResource webResource = bspRestClient.getWebResource(urlString).
                queryParam("username", bspUsername).
                queryParam("filename", filename);

        ClientResponse response = webResource.post(ClientResponse.class, inputStream);

        // Handles errors by throwing RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String msg = "POST to " + urlString + " returned: " + response.getEntity(String.class);
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Determines if the lab vessel exists in BSP.
     */
    public boolean vesselExists(String barcode) {
        String urlString = bspRestClient.getUrl(BSP_PLATE_EXISTS_URL);
        WebResource webResource = bspRestClient.getWebResource(urlString).path(barcode);
        ClientResponse response = webResource.get(ClientResponse.class);
        switch (response.getClientResponseStatus()) {
        case OK:
            return true;
        case NOT_FOUND:
            return false;
        default:
            String msg = "GET " + urlString + " barcode '" + barcode + "' returned " +
                    response.getClientResponseStatus().getReasonPhrase() + ": " + response.getEntity(String.class);
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }
}
