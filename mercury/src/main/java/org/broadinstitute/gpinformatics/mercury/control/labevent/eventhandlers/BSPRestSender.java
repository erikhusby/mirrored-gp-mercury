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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Passes info to BSP via a BSP REST service.
 */
public class BSPRestSender implements Serializable {
    public static final String BSP_TRANSFER_REST_URL = "plate/transfer";
    public static final String BSP_PLATE_EXISTS_URL = "plate/exists";
    public static final String BSP_UPLOAD_QUANT_URL = "quant/upload";
    public static final String BSP_KIT_REST_URL = "kit";
    public static final String BSP_CONTAINER_UPDATE_LAYOUT = "container/updateLayout";

    private static final Log logger = LogFactory.getLog(BSPRestSender.class);

    @Inject
    private BSPRestClient bspRestClient;

    public void postToBsp(BettaLIMSMessage message, String bspRestUrl) {

        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebResource webResource = bspRestClient.getWebResource(urlString);

        // Posts message to BSP using the specified REST url.
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, message);

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getClientResponseStatus().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("POST to " + urlString + " returned: " + response.getEntity(String.class));
        }

    }

    private PlateCherryPickEvent plateTransfer(PlateTransferEventType plateTransferEventType, List<LabEvent> labEvents) {
        // Convert the section transfer to a cherry pick, because BSP doesn't require a matching source rack for
        // cherry picks.
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        plateCherryPickEvent.setEventType(plateTransferEventType.getEventType());
        plateCherryPickEvent.setOperator(plateTransferEventType.getOperator());

        PlateType sourcePlate = new PlateType();
        sourcePlate.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
        sourcePlate.setSection("ALL96");
        sourcePlate.setPhysType(plateTransferEventType.getSourcePlate().getPhysType());
        plateCherryPickEvent.getSourcePlate().add(sourcePlate);

        PositionMapType sourcePosMap = new PositionMapType();
        sourcePosMap.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
        plateCherryPickEvent.getSourcePositionMap().add(sourcePosMap);

        PlateType destPlate = new PlateType();
        destPlate.setBarcode(plateTransferEventType.getPlate().getBarcode());
        destPlate.setSection("ALL96");
        destPlate.setPhysType(plateTransferEventType.getPlate().getPhysType());
        plateCherryPickEvent.getPlate().add(destPlate);

        PositionMapType destPosMap = new PositionMapType();
        destPosMap.setBarcode(plateTransferEventType.getPlate().getBarcode());
        plateCherryPickEvent.getPositionMap().add(destPosMap);

        LabEvent targetEvent = null;
        for (LabEvent labEvent : labEvents) {
            if (Objects.equals(labEvent.getStationEventType(), plateTransferEventType)) {
                targetEvent = labEvent;
                break;
            }
        }
        assert targetEvent != null;
        TubeFormation tubeFormation = (TubeFormation) targetEvent.getSourceLabVessels().iterator().next();

        // Allow random access to source tubes
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        for (LabVessel labVessel : tubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(labVessel.getLabel(), labVessel);
        }

        // Allow random access to destination tubes by position
        Map<String, ReceptacleType> mapPositionToReceptacle = new HashMap<>();
        for (ReceptacleType receptacleType : plateTransferEventType.getPositionMap().getReceptacle()) {
            mapPositionToReceptacle.put(receptacleType.getPosition(), receptacleType);
        }

        // Transfer tubes that are in BSP
        for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
            LabVessel labVessel = mapBarcodeToVessel.get(receptacleType.getBarcode());
            Set<SampleInstanceV2> sampleInstancesV2 = labVessel.getSampleInstancesV2();
            if (sampleInstancesV2.size() != 1) {
                throw new RuntimeException("Expected 1 sample, found " + sampleInstancesV2.size());
            }
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesV2.iterator().next();
            if (sampleInstanceV2.getRootMercurySamples().isEmpty() ||
                    sampleInstanceV2.getRootMercurySamples().iterator().next().getMetadataSource() ==
                            MercurySample.MetadataSource.BSP) {
                ReceptacleType sourceRecep = new ReceptacleType();
                sourceRecep.setBarcode(receptacleType.getBarcode());
                sourceRecep.setPosition(receptacleType.getPosition());
                sourceRecep.setVolume(receptacleType.getVolume());
                sourceRecep.setReceptacleType(receptacleType.getReceptacleType());
                sourcePosMap.getReceptacle().add(sourceRecep);

                ReceptacleType destRecep = new ReceptacleType();
                destRecep.setBarcode(mapPositionToReceptacle.get(receptacleType.getPosition()).getBarcode());
                destRecep.setPosition(receptacleType.getPosition());
                destRecep.setVolume(mapPositionToReceptacle.get(receptacleType.getPosition()).getVolume());
                destRecep.setReceptacleType(receptacleType.getReceptacleType());
                destPosMap.getReceptacle().add(destRecep);

                CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
                cherryPickSourceType.setBarcode(sourcePlate.getBarcode());
                cherryPickSourceType.setWell(receptacleType.getPosition());
                cherryPickSourceType.setDestinationBarcode(destPlate.getBarcode());
                cherryPickSourceType.setDestinationWell(receptacleType.getPosition());
                plateCherryPickEvent.getSource().add(cherryPickSourceType);
            }
        }
        return plateCherryPickEvent;
    }

    @NotNull
    public BettaLIMSMessage bspBettaLIMSMessage(BettaLIMSMessage message, List<LabEvent> labEvents) {
        // Forward only the events that are for BSP, e.g. Blood Biopsy extraction from blood to plasma and buffy coat
        // is two events in one message, but only one is configured to forward to BSP.
        BettaLIMSMessage copy = new BettaLIMSMessage();
        for (PlateCherryPickEvent plateCherryPickEvent : message.getPlateCherryPickEvent()) {
            if(LabEventType.getByName(plateCherryPickEvent.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                // todo jmt method to filter out clinical samples
                copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
            }
        }
        for (PlateTransferEventType plateTransferEventType : message.getPlateTransferEvent()) {
            if(LabEventType.getByName(plateTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                copy.getPlateCherryPickEvent().add(plateTransfer(plateTransferEventType, labEvents));
            }
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : message.getReceptacleTransferEvent()) {
            if(LabEventType.getByName(receptacleTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                // todo jmt method to filter out clinical samples
                copy.getReceptacleTransferEvent().add(receptacleTransferEventType);
            }
        }
        return copy;
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

}
