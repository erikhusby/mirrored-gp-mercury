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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleInfo;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.TransferReturn;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Passes info to BSP via a BSP REST service.
 */
@Dependent
public class BSPRestSender implements Serializable {
    public static final String BSP_TRANSFER_REST_URL = "plate/transfer";
    public static final String BSP_CREATE_DISSASSOC_PLATE_URL = "plate/createDisassociatedPlate";
    public static final String BSP_CONTAINER_URL = "container/getSampleInfo";
    public static final String BSP_UPLOAD_QUANT_URL = "quant/upload";
    public static final String BSP_UPDATE_TUBE_QUANTS_URL = "quant/updateByTubeBarcode";
    public static final String BSP_KIT_REST_URL = "kit";
    public static final String BSP_CONTAINER_UPDATE_LAYOUT = "container/updateLayout";

    private static final Log logger = LogFactory.getLog(BSPRestSender.class);

    public static final Format PLATE_NAME_DATE_FORMAT = FastDateFormat.getInstance("MMddHHmm");
    private static final String PDO_NOT_NEEDED = "PDO Not Needed";

    @Inject
    private BSPRestClient bspRestClient;

    public TransferReturn postToBsp(BettaLIMSMessage message, String bspRestUrl) {

        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebTarget webTarget = bspRestClient.getWebResource(urlString);

        // Posts message to BSP using the specified REST url.
        Response response = webTarget.request().post(Entity.xml(message));

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String entity = response.readEntity(String.class);
            response.close();
            throw new RuntimeException("POST to " + urlString + " returned: " + entity);
        }
        TransferReturn transferReturn = response.readEntity(TransferReturn.class);
        response.close();
        return transferReturn;
    }

    /**
     * Posts tube quants to BSP REST service at the specified url.
     * Expects to be called from a UI gesture, which handles errors via MessageCollection.
     */
    public void postToBsp(TubeQuants tubeQuants, String bspRestUrl, MessageCollection messageCollection) {
        String urlString = bspRestClient.getUrl(bspRestUrl);
        WebTarget webTarget = bspRestClient.getWebResource(urlString);

        Response response = webTarget.request().post(Entity.json(tubeQuants));
        String responseString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            messageCollection.addError("Failed to post to Bsp: " + responseString);
        } else {
            messageCollection.addInfo(responseString);
        }
        response.close();
    }

    private PlateCherryPickEvent plateTransferToCherryPick(PlateTransferEventType plateTransferEventType,
            List<LabEvent> labEvents) {
        // Convert the section transfer to a cherry pick, because this gives us control over transferring
        // individual positions (rather than an entire section)

        // Clone plates and position maps
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        plateCherryPickEvent.setEventType(plateTransferEventType.getEventType());
        plateCherryPickEvent.setOperator(plateTransferEventType.getOperator());
        plateCherryPickEvent.getMetadata().addAll(plateTransferEventType.getMetadata());
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
        String messagePdo = null;

        for (int i = 0; i < wells.size(); i++) {
            VesselPosition sourceVesselPosition = wells.get(i);
            VesselPosition destVesselPosition = destSection.getWells().get(i);

            // Blood Biopsy extraction creates a psuedo-pool of 2-3 samples, so we have to handle multiple SampleInstances.
            Set<SampleInstanceV2> sampleInstances = sourceLabVessel.getContainerRole().getSampleInstancesAtPositionV2(
                    sourceVesselPosition);
            Set<MercurySample.MetadataSource> metadataSources = new HashSet<>();
            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                // NA12878 samples, that fill up partial fingerprint plates to 48 wells, are reagents
                if (!sampleInstance.isReagentOnly()) {
                    MercurySample rootMercurySample = sampleInstance.getRootOrEarliestMercurySample();
                    // if root is null, assume it's an old BSP sample that was received before Mercury existed
                    metadataSources.add(rootMercurySample == null ? MercurySample.MetadataSource.BSP :
                            rootMercurySample.getMetadataSource());
                }
            }

            if (metadataSources.size() > 1) {
                throw new RuntimeException("Expected 1 metadata source, found " + metadataSources.size());
            } else if (metadataSources.size() == 1) {
                MercurySample.MetadataSource metadataSource = metadataSources.iterator().next();
                if (metadataSource == MercurySample.MetadataSource.BSP) {
                    atLeastOneTransfer = true;
                    // add source element
                    CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
                    cherryPickSourceType.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
                    cherryPickSourceType.setWell(sourceVesselPosition.name());
                    cherryPickSourceType.setDestinationBarcode(plateTransferEventType.getPlate().getBarcode());
                    cherryPickSourceType.setDestinationWell(destVesselPosition.name());
                    plateCherryPickEvent.getSource().add(cherryPickSourceType);
                    if (targetEvent.getLabEventType().getAddMetadataToBsp() == LabEventType.AddMetadataToBsp.PDO) {
                        // Set PDO.  If it's a psuedo-pool, assume all are in the same PDO
                        SampleInstanceV2 sampleInstanceV2 = sampleInstances.iterator().next();
                        List<ProductOrderSample> allProductOrderSamples = sampleInstanceV2.getAllProductOrderSamples();
                        // todo jmt if multiple, pick most recent?
                        String pdoJiraId = allProductOrderSamples.isEmpty() ? PDO_NOT_NEEDED :
                                allProductOrderSamples.iterator().next().getProductOrder().getBusinessKey();
                        MetadataType metadataType = new MetadataType();
                        metadataType.setName("PDO");
                        metadataType.setValue(pdoJiraId);
                        if (messagePdo == null) {
                            messagePdo = pdoJiraId;
                        }
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
        if (atLeastOneTransfer &&
                targetEvent.getLabEventType().getAddMetadataToBsp() == LabEventType.AddMetadataToBsp.PDO) {
            MetadataType metadataType = new MetadataType();
            metadataType.setName("PLATE_NAME");
            String plateNamePrefix = messagePdo.equals(PDO_NOT_NEEDED) ?
                    plateTransferEventType.getSourcePlate().getBarcode() : messagePdo;
            metadataType.setValue(plateNamePrefix + "_" + PLATE_NAME_DATE_FORMAT.format(new Date()));
            plateCherryPickEvent.getMetadata().add(metadataType);
        }


        if (plateCherryPickEvent.getSourcePositionMap() != null && !plateCherryPickEvent.getSourcePositionMap().isEmpty()) {
            plateCherryPickEvent.getSourcePositionMap().get(0).getReceptacle().removeAll(removeSources);
        }
        if (plateCherryPickEvent.getPositionMap() != null && !plateCherryPickEvent.getPositionMap().isEmpty()) {
            plateCherryPickEvent.getPositionMap().get(0).getReceptacle().removeAll(removeDests);
        }

        return atLeastOneTransfer ? plateCherryPickEvent : null;
    }

    /**
     * Check whether the event expects to handle source samples specifically. If yes, then check to see if the metadata
     * needs to be added to the sourcePositionMap or to the sourcePlateType element in the message to BSP.
     *
     * @param targetEventType    The lab event type instance from the message
     * @param sourcePlateType    PlateType instance from the message
     * @param sourcePositionMaps The source position maps from the message
     */
    private void addSourceHandlingException(LabEventType targetEventType, List<PlateType> sourcePlateType, List<PositionMapType> sourcePositionMaps) {

        // Check to see if 'DEPLETE' or 'TERMINATE_DEPLETED' flag is set on the lab event type.
        if (targetEventType.depleteSources() || targetEventType.terminateDepletedSources()) {

            // If there are any sourcePositionMap objects, then add the metadata to each receptacleType entree in the maps.
            if (sourcePositionMaps != null && !sourcePositionMaps.isEmpty()) {
                for (PositionMapType positionMapType : sourcePositionMaps) {
                    for (ReceptacleType sourceReceptacleType : positionMapType.getReceptacle()) {

                        addSourceHandlingException(targetEventType, sourceReceptacleType);
                    }
                }
                // If no sourcePositionMap, then we need to add the special handling flag to the event's sourcePlateType element.
            } else if (sourcePlateType != null) {

                for (PlateType plateType : sourcePlateType) {

                    // If the source container is a plate and no position map is provided for it, we need to set the source handling
                    // metadata tag on the PlateType element.
                    if (StaticPlate.PlateType.getByAutomationName(plateType.getPhysType()) != null) {

                        MetadataType metadataType = new MetadataType();
                        metadataType.setValue(Boolean.TRUE.toString());

                        if (targetEventType.depleteSources()) {
                            metadataType.setName(LabEventType.SourceHandling.DEPLETE.getDisplayName());
                        } else {
                            metadataType.setName(LabEventType.SourceHandling.TERMINATE_DEPLETED.getDisplayName());
                        }
                        plateType.getMetadata().add(metadataType);
                        // Check to see if there is a sourcePositionMap where each source could be set with a metadataType.
                    }
                }
            }
        }
    }

    /**
     * Check whether the event type expects to handle sources in a special way and do so if necessary.
     *
     * @param targetEventType      LabEventType of the lab event
     * @param sourceReceptacleType Source
     */
    private void addSourceHandlingException(LabEventType targetEventType, ReceptacleType sourceReceptacleType) {
        // Check to see if 'DEPLETE' or 'TERMINATE_DEPLETED' flag is set on the lab event type.
        if (targetEventType.depleteSources() || targetEventType.terminateDepletedSources()) {

            MetadataType metadataType = new MetadataType();
            // Always add the 'Terminate Depleted' enum display name as BSP will handle termination if the volume is zero.
            metadataType.setName(LabEventType.SourceHandling.TERMINATE_DEPLETED.getDisplayName());
            metadataType.setValue(Boolean.TRUE.toString());
            sourceReceptacleType.getMetadata().add(metadataType);

            // If we are set to deplete the sources then we need to ensure it's volume is zero.
            if (targetEventType.depleteSources()) {
                sourceReceptacleType.setVolume(BigDecimal.ZERO);
            }
        }
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
            LabEventType labEventType = LabEventType.getByName(plateCherryPickEvent.getEventType());
            if(labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP  ||
                    labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP_APPLY_SM_IDS) {

                addSourceHandlingException(labEventType, plateCherryPickEvent.getSourcePlate(), plateCherryPickEvent.getSourcePositionMap());
                // todo jmt method to filter out clinical samples
                copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
                atLeastOneEvent = true;
            }
        }
        for (PlateTransferEventType plateTransferEventType : message.getPlateTransferEvent()) {
            LabEventType labEventType = LabEventType.getByName(plateTransferEventType.getEventType());
            if(labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP ||
                    labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP_APPLY_SM_IDS) {
                // If there is a sourcePositionMap create a list to handle.
                List<PositionMapType> sourcePositionMapList = plateTransferEventType.getSourcePositionMap() != null ?
                        Collections.singletonList(plateTransferEventType.getSourcePositionMap()) : null;

                List<PlateType> sourcePlateType = plateTransferEventType.getSourcePlate() != null ?
                        Collections.singletonList(plateTransferEventType.getSourcePlate()) : null;
                addSourceHandlingException(labEventType, sourcePlateType, sourcePositionMapList);

                if (labEventType.getTranslateBspMessage() == LabEventType.TranslateBspMessage.SECTION_TO_CHERRY) {
                    PlateCherryPickEvent plateCherryPickEvent = plateTransferToCherryPick(plateTransferEventType,
                            labEvents);
                    if (plateCherryPickEvent != null) {
                        copy.getPlateCherryPickEvent().add(plateCherryPickEvent);
                        atLeastOneEvent = true;
                    }
                } else {
                    List<LabEvent> labEventList = labEvents.stream().
                            filter(le -> Objects.equals(le.getStationEventType(), plateTransferEventType)).
                            collect(Collectors.toList());
                    boolean mercury = false;
                    for (LabEvent labEvent : labEventList) {
                        for (LabVessel labVessel : labEvent.getSourceLabVessels()) {
                            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                                if (sampleInstanceV2.getRootOrEarliestMercurySample().getMetadataSource() ==
                                        MercurySample.MetadataSource.MERCURY) {
                                    // Don't support mixed MERCURY and BSP (engineer must change to
                                    // TranslateBspMessage.SECTION_TO_CHERRY if necessary).
                                    mercury = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!mercury) {
                        copy.getPlateTransferEvent().add(plateTransferEventType);
                        atLeastOneEvent = true;
                    }
                }
            }
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : message.getReceptacleTransferEvent()) {
            LabEventType labEventType = LabEventType.getByName(receptacleTransferEventType.getEventType());
            if(labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP ||
                    labEventType.getForwardMessage() == LabEventType.ForwardMessage.BSP_APPLY_SM_IDS) {
                // todo jmt method to filter out clinical samples
                addSourceHandlingException(labEventType, receptacleTransferEventType.getSourceReceptacle());
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
        WebTarget webTarget = bspRestClient.getWebResource(urlString).
                queryParam("username", bspUsername).
                queryParam("filename", filename);

        Response response = webTarget.request().post(Entity.entity(new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                IOUtils.copy(inputStream, outputStream);
            }
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // Handles errors by throwing RuntimeException.
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String msg = "POST to " + urlString + " returned: " + response.readEntity(String.class);
            logger.error(msg);
            response.close();
            throw new RuntimeException(msg);
        }
        response.close();
    }

    /**
     * Register a new Plate in BSP
     * @param receptacleType - Receptacle type to create
     * @param wellType - Types off wells to be created for plate
     * @return - Container ID of the newly created plate
     */
    public String createDisassociatedPlate(String receptacleType, String wellType) {
        String urlString = bspRestClient.getUrl(BSP_CREATE_DISSASSOC_PLATE_URL);
        WebTarget webTarget = bspRestClient.getWebResource(urlString);

        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("receptacleType", receptacleType);
        formData.add("wellType", wellType);

        // Posts message to BSP using the specified REST url.
        Response response = webTarget.request().post(Entity.form(formData));

        // This is called in context of bettalims message handling which handles errors via RuntimeException.
        String entity = response.readEntity(String.class);
        response.close();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("POST to " + urlString + " returned: " + entity);
        } else {
            return entity;
        }
    }

    public GetSampleInfo.SampleInfos  getSampleInfo(String containerBarcode) {
        String urlString = bspRestClient.getUrl(BSP_CONTAINER_URL);
        WebTarget webTarget = bspRestClient.getWebResource(urlString).queryParam("containerBarcode", containerBarcode);
        Response response = webTarget.request().get();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            logger.warn("Failed to find container info for " + containerBarcode);
            return null;
        } else {
            GetSampleInfo.SampleInfos sampleInfos = response.readEntity(GetSampleInfo.SampleInfos.class);
            response.close();
            return sampleInfos;
        }
    }

    public static class TubeQuants {
        private String username;
        private List<String> tubeBarcodes;
        private List<String> quants;
        private List<String> volumes;
        private String runDate;

        public TubeQuants(String username, List<String> tubeBarcodes, List<String> quants, List<String> volumes,
                String runDate) {
            this.username = username;
            this.tubeBarcodes = tubeBarcodes;
            this.quants = quants;
            this.volumes = volumes;
            this.runDate = runDate;
        }

        public String getUsername() {
            return username;
        }

        public List<String> getTubeBarcodes() {
            return tubeBarcodes;
        }

        public List<String> getQuants() {
            return quants;
        }

        public List<String> getVolumes() {
            return volumes;
        }

        public String getRunDate() {
            return runDate;
        }
    }

}
