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

import com.rits.cloning.Cloner;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
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
        // Convert the section transfer to a cherry pick, because this gives us control over transferring
        // individual positions (rather than an entire section)

        // Clone plates and position maps
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        plateCherryPickEvent.setEventType(plateTransferEventType.getEventType());
        plateCherryPickEvent.setOperator(plateTransferEventType.getOperator());
        Cloner cloner = new Cloner();
        plateCherryPickEvent.getSourcePlate().add(cloner.deepClone(plateTransferEventType.getSourcePlate()));
        if (plateTransferEventType.getSourcePositionMap() != null) {
            plateCherryPickEvent.getSourcePositionMap().add(cloner.deepClone(plateTransferEventType.getSourcePositionMap()));
        }
        plateCherryPickEvent.getPlate().add(cloner.deepClone(plateTransferEventType.getPlate()));
        if (plateTransferEventType.getPositionMap() != null) {
            plateCherryPickEvent.getPositionMap().add(cloner.deepClone(plateTransferEventType.getPositionMap()));
        }

        // Find the corresponding event entity
        LabEvent targetEvent = null;
        for (LabEvent labEvent : labEvents) {
            if (Objects.equals(labEvent.getStationEventType(), plateTransferEventType)) {
                targetEvent = labEvent;
                break;
            }
        }
        assert targetEvent != null;

        // Add source elements; remove tubes that don't have a BSP chain of custody
        LabVessel sourceLabVessel = targetEvent.getSourceLabVessels().iterator().next();
        SBSSection sourceSection = SBSSection.getBySectionName(plateTransferEventType.getSourcePlate().getSection());
        SBSSection destSection = SBSSection.getBySectionName(plateTransferEventType.getPlate().getSection());
        List<VesselPosition> wells = sourceSection.getWells();
        Map<VesselPosition, ReceptacleType> mapSourcePosToReceptacle = buildMapPosToReceptacle(plateCherryPickEvent.getSourcePositionMap());
        Map<VesselPosition, ReceptacleType> mapDestPosToReceptacle = buildMapPosToReceptacle(plateCherryPickEvent.getPositionMap());
        List<ReceptacleType> removeSources = new ArrayList<>();
        List<ReceptacleType> removeDests = new ArrayList<>();
        boolean atLeastOneTransfer = false;

        for (int i = 0; i < wells.size(); i++) {
            VesselPosition sourceVesselPosition = wells.get(i);
            VesselPosition destVesselPosition = destSection.getWells().get(i);
            Set<SampleInstanceV2> sampleInstances = sourceLabVessel.getContainerRole().getSampleInstancesAtPositionV2(
                    sourceVesselPosition);
            if (sampleInstances.size() > 1) {
                throw new RuntimeException("Expected 1 sample, found " + sampleInstances.size());
            } else if (sampleInstances.size() == 1) {
                SampleInstanceV2 sampleInstanceV2 = sampleInstances.iterator().next();
                Set<MercurySample> rootMercurySamples = sampleInstanceV2.getRootMercurySamples();
                if (!sampleInstanceV2.isReagentOnly() &&
                        rootMercurySamples.iterator().next().getMetadataSource() == MercurySample.MetadataSource.BSP) {
                    atLeastOneTransfer = true;
                    // add source element
                    CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
                    cherryPickSourceType.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
                    cherryPickSourceType.setWell(sourceVesselPosition.name());
                    cherryPickSourceType.setDestinationBarcode(plateTransferEventType.getPlate().getBarcode());
                    cherryPickSourceType.setDestinationWell(destVesselPosition.name());
                    plateCherryPickEvent.getSource().add(cherryPickSourceType);
                    // Set PDO
                    // todo jmt make this conditional on a flag in LabEventType
                    List<ProductOrderSample> allProductOrderSamples = sampleInstanceV2.getAllProductOrderSamples();
                    if (!allProductOrderSamples.isEmpty()) {
                        MetadataType metadataType = new MetadataType();
                        metadataType.setName("PDO");
                        // todo jmt if multiple, pick most recent?
                        metadataType.setValue(allProductOrderSamples.iterator().next().getProductOrder().getBusinessKey());
                        ReceptacleType receptacleType = mapDestPosToReceptacle.get(destVesselPosition);
                        if (receptacleType != null) {
                            receptacleType.getMetadata().add(metadataType);
                        }
                    }
                } else {
                    // Queue up tubes to remove from position maps
                    ReceptacleType sourceReceptacleType = mapSourcePosToReceptacle.get(sourceVesselPosition);
                    if (sourceReceptacleType != null) {
                        removeSources.add(sourceReceptacleType);
                    }
                    ReceptacleType destReceptacleType = mapDestPosToReceptacle.get(destVesselPosition);
                    if (destReceptacleType != null) {
                        removeDests.add(destReceptacleType);
                    }
                }
            }
        }

        if (plateCherryPickEvent.getSourcePositionMap() != null && !plateCherryPickEvent.getSourcePositionMap().isEmpty()) {
            plateCherryPickEvent.getSourcePositionMap().get(0).getReceptacle().removeAll(removeSources);
        }
        if (plateCherryPickEvent.getPositionMap() != null && !plateCherryPickEvent.getPositionMap().isEmpty()) {
            plateCherryPickEvent.getPositionMap().get(0).getReceptacle().removeAll(removeDests);
        }

        return atLeastOneTransfer ? plateCherryPickEvent : null;
    }

    private Map<VesselPosition, ReceptacleType> buildMapPosToReceptacle(List<PositionMapType> positionMaps) {
        Map<VesselPosition, ReceptacleType> mapPosToReceptacle = new HashMap<>();
        if (positionMaps != null && !positionMaps.isEmpty()) {
            List<ReceptacleType> receptacles = positionMaps.get(0).getReceptacle();
            for (ReceptacleType receptacle : receptacles) {
                mapPosToReceptacle.put(VesselPosition.getByName(receptacle.getPosition()), receptacle);
            }
        }
        return mapPosToReceptacle;
    }

    public BettaLIMSMessage bspBettaLIMSMessage(BettaLIMSMessage message, List<LabEvent> labEvents) {
        // Forward only the events that are for BSP, e.g. Blood Biopsy extraction from blood to plasma and buffy coat
        // is two events in one message, but only one is configured to forward to BSP.
        BettaLIMSMessage copy = new BettaLIMSMessage();
        boolean atLeastOneEvent = false;
        for (PlateCherryPickEvent plateCherryPickEvent : message.getPlateCherryPickEvent()) {
            if(LabEventType.getByName(plateCherryPickEvent.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                // todo jmt method to filter out clinical samples
                copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
                atLeastOneEvent = true;
            }
        }
        for (PlateTransferEventType plateTransferEventType : message.getPlateTransferEvent()) {
            if(LabEventType.getByName(plateTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                PlateCherryPickEvent plateCherryPickEvent = plateTransfer(plateTransferEventType, labEvents);
                if (plateCherryPickEvent != null) {
                    copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
                    atLeastOneEvent = true;
                }
            }
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : message.getReceptacleTransferEvent()) {
            if(LabEventType.getByName(receptacleTransferEventType.getEventType()).getForwardMessage() ==
                    LabEventType.ForwardMessage.BSP) {
                // todo jmt method to filter out clinical samples
                copy.getReceptacleTransferEvent().add(receptacleTransferEventType);
                atLeastOneEvent = true;
            }
        }
        return atLeastOneEvent ? copy : null;
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
