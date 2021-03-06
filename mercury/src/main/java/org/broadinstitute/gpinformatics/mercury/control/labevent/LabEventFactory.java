package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsObjectFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StripTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Creates Lab Event entities from BettaLIMS JAXB beans.  Implements Serializable because it's used by a Stateful
 * session bean.
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "serial", "CloneableClassWithoutClone",
        "ClassExtendsConcreteCollection", "OverlyComplexClass", "ClassWithTooManyMethods", "ClassWithTooManyFields"})
@Dependent
public class LabEventFactory implements Serializable {

    /**
     * Section for all wells in a 96 well plate
     */
    public static final String SECTION_ALL_96 = "ALL96";
    /**
     * Physical type for rack of tubes
     */
    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    /**
     * Physical type for Eppendorf plate with 96 wells
     */
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";
    /**
     * Physical type for a rack that holds 12 strip tubes
     */
    public static final String PHYS_TYPE_STRIP_TUBE_RACK_OF_12 = "StripTubeRackOf12";
    /**
     * Physical type for a strip tube
     */
    public static final String PHYS_TYPE_STRIP_TUBE = "StripTube";
    /**
     * Physical type for an 8-lane flowcell
     */
    public static final String PHYS_TYPE_FLOWCELL = "Flowcell";
    /**
     * Physical type for a MiSeq reagent block.
     */
    public static final String PHYS_TYPE_REAGENT_BLOCK = "MiseqReagentKit";
    /**
     * Pattern that groups non-zero trailing digits
     */
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0+(?!$)");
    /**
     * For test messages, the message router uses the bettaLIMSMessage.mode attribute
     */
    public static final String MODE_MERCURY = "Mercury";
    /**
     * Used when backfilling messages from GAP, avoids forwarding them to GAP.
     */
    public static final String MODE_BACKFILL = "Backfill";
    /**
     * Whether to create sources that are not found in the database.  Handling out of order messages requires this
     * to be true, but when running in parallel with Squid / BettaLIMS (which can't handle out of order messages)
     * we set this to false so Mercury fails in the same way as Squid / BettaLIMS.
     */
    private static final boolean CREATE_SOURCES = false;
    public static final String ACTIVITY_USER_ID = "seqsystem";

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private StripTubeDao stripTubeDao;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private GenericReagentDao genericReagentDao;

    @Inject
    private EventHandlerSelector eventHandlerSelector;

    @Inject
    private BSPRestSender bspRestSender;

    @Inject
    private BSPSetVolumeConcentration bspSetVolumeConcentration;

    @Inject
    private MercurySampleDao mercurySampleDao;

    private static final Log logger = LogFactory.getLog(LabEventFactory.class);

    public LabEventFactory(){}

    public LabEventFactory(BSPUserList userList, BSPSetVolumeConcentration bspSetVolumeConcentration) {
        bspUserList = userList;
        this.bspSetVolumeConcentration = bspSetVolumeConcentration;
    }

    /**
     * This method builds the reagent to flowcell bettalims Jaxb Message
     * @param reagentKitBarcode barcode of the source reagent kit
     * @param flowcellBarcode barcode of the target flowcell
     * @param username username of the submitter
     * @param stationName station name that this stransfer is initiated from
     * @return an newly created JAXB object representing the reagent to flowcell transfer message
     */
    public PlateCherryPickEvent getReagentToFlowcellEventDBFree(String reagentKitBarcode, String flowcellBarcode,
                                                                String username, String stationName) {
        PlateCherryPickEvent transferEvent = new PlateCherryPickEvent();
        transferEvent.setEventType(LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER.getName());
        transferEvent.setStart(new Date());
        transferEvent.setDisambiguator(1L);
        transferEvent.setOperator(username);
        transferEvent.setStation(stationName);

        // Yes, yes, miSeq flowcell has one lane.
        for (VesselPosition vesselPosition : IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry()
                .getVesselPositions()) {
            CherryPickSourceType cherryPickSource = BettaLimsObjectFactory.createCherryPickSourceType(reagentKitBarcode,
                    MiSeqReagentKit.LOADING_WELL.name(), flowcellBarcode, vesselPosition.name());
            transferEvent.getSource().add(cherryPickSource);
        }

        PlateType reagentKitType = BettaLimsObjectFactory.createPlateType(reagentKitBarcode,
                StaticPlate.PlateType.MiSeqReagentKit.getAutomationName(), MiSeqReagentKit.LOADING_WELL.name(), null);
        transferEvent.getSourcePlate().add(reagentKitType);

        PlateType flowcell = BettaLimsObjectFactory
                .createPlateType(flowcellBarcode, IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getAutomationName(),
                        SBSSection.ALL96.getSectionName(),
                        null);
        transferEvent.getPlate().add(flowcell);
        return transferEvent;
    }

    private LabEventRefDataFetcher labEventRefDataFetcher = new LabEventRefDataFetcher() {

        @Override
        public BspUser getOperator(String userId) {
            BSPUserList testList = bspUserList;
            if (bspUserList == null) {
                testList = ServiceAccessUtility.getBean(BSPUserList.class);
            }
            return testList.getByUsername(userId);
        }

        @Override
        public BspUser getOperator(Long bspUserId) {
            BSPUserList testList = bspUserList;
            if (bspUserList == null) {
                testList = ServiceAccessUtility.getBean(BSPUserList.class);
            }
            return testList.getById(bspUserId);
        }

        @Override
        public LabBatch getLabBatch(String labBatchName) {
            return labBatchDao.findByName(labBatchName);
        }
    };

    /**
     * This class is used in a map, to detect duplicate events from a deck.
     */
    private static class UniqueEvent {
        /**
         * where the event happened
         */
        private final String eventLocation;
        /**
         * when the event happened
         */
        private final Date eventDate;
        /**
         * Needed if the deck is sending two transfers in the same message
         */
        private final Long disambiguator;

        private UniqueEvent(String eventLocation, Date eventDate, Long disambiguator) {
            this.eventLocation = eventLocation;
            this.eventDate = eventDate;
            this.disambiguator = disambiguator;
        }

        public String getEventLocation() {
            return eventLocation;
        }

        public Date getEventDate() {
            return eventDate;
        }

        public Long getDisambiguator() {
            return disambiguator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UniqueEvent other = (UniqueEvent) o;

            if (disambiguator != null ? !disambiguator.equals(other.getDisambiguator())
                    : other.getDisambiguator() != null) {
                return false;
            }
            if (eventDate != null ? !eventDate.equals(other.getEventDate()) : other.getEventDate() != null) {
                return false;
            }
            if (eventLocation != null ? !eventLocation.equals(other.getEventLocation())
                    : other.getEventLocation() != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = eventLocation != null ? eventLocation.hashCode() : 0;
            result = 31 * result + (eventDate != null ? eventDate.hashCode() : 0);
            result = 31 * result + (disambiguator != null ? disambiguator.hashCode() : 0);
            return result;
        }
    }

    private MercurySample extractSample(Collection<SampleInstanceV2> sampleInstances) {
        MercurySample mercurySample = null;
        for (SampleInstanceV2 sampleInstance : sampleInstances) {
            mercurySample = sampleInstance.getRootOrEarliestMercurySample();
            if (mercurySample != null) {
                break;
            }
        }
        return mercurySample;
    }

    /**
     * Builds one or more lab event entities from a JAXB message bean that contains one or more event beans
     *
     * @param bettaLIMSMessage JAXB bean
     *
     * @return list of entities
     */
    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<>();
        Set<UniqueEvent> uniqueEvents = new HashSet<>();

        // Have to persist and flush inside each loop, because the first event may create
        // vessels that are referenced by the second event, e.g. PreSelectionPool
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateCherryPickEvent);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateCherryPickEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateTransferEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateTransferEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent :
                bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptaclePlateTransferEvent);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, receptaclePlateTransferEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptacleEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, receptacleEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        StationSetupEvent stationSetupEvent = bettaLIMSMessage.getStationSetupEvent();
        if (stationSetupEvent != null) {
            LabEvent labEvent = buildFromBettaLims(stationSetupEvent);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, stationSetupEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : bettaLIMSMessage.getReceptacleTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptacleTransferEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, receptacleTransferEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }

        if (!labEvents.isEmpty()) {
            LabEvent labEvent = labEvents.get(0);
            LabEventType.ForwardMessage forwardMessage = labEvent.getLabEventType().getForwardMessage();
            switch (forwardMessage) {
                case BSP:
                case BSP_APPLY_SM_IDS:
                    BettaLIMSMessage bspBettaLIMSMessage = bspRestSender.bspBettaLIMSMessage(bettaLIMSMessage, labEvents);
                    if (bspBettaLIMSMessage != null) {
                        TransferReturn transferReturn = bspRestSender.postToBsp(bspBettaLIMSMessage,
                                BSPRestSender.BSP_TRANSFER_REST_URL);

                        if (forwardMessage == LabEventType.ForwardMessage.BSP_APPLY_SM_IDS) {
                            // Assign SM-IDs to destinations
                            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                                    transferReturn.getMapBarcodeToSmId().values());
                            for (LabEvent event : labEvents) {
                                for (LabVessel labVessel : event.getTargetLabVessels()) {
                                    VesselContainer<?> containerRole = labVessel.getContainerRole();
                                    if (containerRole != null) {
                                        for (LabVessel vessel : containerRole.getContainedVessels()) {
                                            String smId = transferReturn.getMapBarcodeToSmId().get(vessel.getLabel());
                                            if (smId != null) {
                                                MercurySample mercurySample = mapIdToMercurySample.get(smId);
                                                if (mercurySample == null) {
                                                    mercurySample = new MercurySample(smId,MercurySample.MetadataSource.BSP);
                                                }
                                                vessel.addSample(mercurySample);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    break;
                case NONE:
                    break;
                default:
                    throw new RuntimeException("Unexpected forwardMessage " + forwardMessage.name());
            }
        }
        return labEvents;
    }

    // todo jmt decide whether to delete from archiving
    @Nullable
    public static String determineForwardToGap(LabEvent labEvent, LabVessel labVessel,
            ProductEjb productEjb, AttributeArchetypeDao attributeArchetypeDao) {
        String forwardToGap = null;
        for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
            ProductOrderSample productOrderSample =
                    sampleInstanceV2.getProductOrderSampleForSingleBucket();
            if (productOrderSample != null) {
                Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(
                        productOrderSample.getProductOrder(), labEvent.getEventDate());
                if (chipFamilyAndName.getLeft() != null && chipFamilyAndName.getRight() != null) {
                    GenotypingChip chip = attributeArchetypeDao.findGenotypingChip(
                            chipFamilyAndName.getLeft(), chipFamilyAndName.getRight());
                    forwardToGap = chip.getAttributeMap().get("forward_to_gap");
                    if (forwardToGap != null) {
                        break;
                    }
                }
            }
        }
        return forwardToGap;
    }

    /**
     * Modify disambiguators of other events in the same message, if necessary, and persist an event
     *
     * @param uniqueEvents    events in a message
     * @param labEvent        event to be persisted
     * @param persistEntities true if this method should persist and flush the entities
     */
    private void persistLabEvent(Set<UniqueEvent> uniqueEvents, LabEvent labEvent, boolean persistEntities) {
        // The deck-side scripts don't always set the disambiguator correctly, so modify it, to make it unique
        // within this message, if necessary
        while (!uniqueEvents.add(new UniqueEvent(labEvent.getEventLocation(), labEvent.getEventDate(),
                labEvent.getDisambiguator()))) {
            labEvent.setDisambiguator(labEvent.getDisambiguator() + 1L);
        }
        labEvent.computeLabBatches();
        if (persistEntities) {
            labEventDao.persist(labEvent);
            labEventDao.flush();
        }
    }

    /**
     * Builds a lab event entity from a JAXB cherry pick event bean
     *
     * @param plateCherryPickEvent JAXB event bean
     *
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateCherryPickEvent plateCherryPickEvent) {

        LabEvent labEvent;
        if (plateCherryPickEvent.getPlate().get(0).getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = buildMapBarcodeToTubeFormation(
                    plateCherryPickEvent.getSourcePositionMap());
            Map<String, BarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
            for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                mapBarcodeToSourceTube.putAll(findTubesByBarcodes(positionMapType));
            }
            Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
            for (PlateType sourcePlateType : plateCherryPickEvent.getSourcePlate()) {
                mapBarcodeToSourceRackOfTubes
                        .put(sourcePlateType.getBarcode(), rackOfTubesDao.findByBarcode(sourcePlateType.getBarcode()));
            }

            Map<String, StripTube> mapBarcodeToTargetStripTube = new HashMap<>();
            labEvent = buildCherryPickRackToStripTubeDbFree(plateCherryPickEvent, mapBarcodeToSourceTubeFormation,
                    mapBarcodeToSourceTube, null, mapBarcodeToTargetStripTube, mapBarcodeToSourceRackOfTubes);
        } else if (plateCherryPickEvent.getPlate().get(0).getPhysType().equals(PHYS_TYPE_REAGENT_BLOCK)) {
            // todo jmt fix copy/paste
            Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = buildMapBarcodeToTubeFormation(
                    plateCherryPickEvent.getSourcePositionMap());
            Map<String, BarcodedTube> mapBarcodeToSourceTube = new HashMap<>();
            for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                mapBarcodeToSourceTube.putAll(findTubesByBarcodes(positionMapType));
            }
            Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<>();
            for (PlateType sourcePlateType : plateCherryPickEvent.getSourcePlate()) {
                mapBarcodeToSourceRackOfTubes
                        .put(sourcePlateType.getBarcode(), rackOfTubesDao.findByBarcode(sourcePlateType.getBarcode()));
            }

            labEvent = buildCherryPickRackToReagentKitDbFree(plateCherryPickEvent, mapBarcodeToSourceTubeFormation,
                    mapBarcodeToSourceRackOfTubes, mapBarcodeToSourceTube);
        } else {
            List<String> barcodes = new ArrayList<>();
            for (PlateType plateType : plateCherryPickEvent.getSourcePlate()) {
                barcodes.add(plateType.getBarcode());
            }
            List<PositionMapType> extractSourcePositionMaps = extractPositionMapBarcodes(
                    plateCherryPickEvent.getSourcePlate(), plateCherryPickEvent.getSourcePositionMap());
            extractBarcodes(barcodes, extractSourcePositionMaps);

            for (PlateType plateType : plateCherryPickEvent.getPlate()) {
                barcodes.add(plateType.getBarcode());
            }

            List<PositionMapType> extractPositionMaps = extractPositionMapBarcodes(
                    plateCherryPickEvent.getPlate(), plateCherryPickEvent.getPositionMap());
            extractBarcodes(barcodes, extractPositionMaps);

            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            trySampleIds(barcodes, mapBarcodeToVessel, mercurySampleDao);
            findTubeFormations(plateCherryPickEvent.getSourcePositionMap(), mapBarcodeToVessel);
            findTubeFormations(plateCherryPickEvent.getPositionMap(), mapBarcodeToVessel);

            labEvent = buildFromBettaLims(plateCherryPickEvent, mapBarcodeToVessel);
        }

        labEvent.setStationEventType(plateCherryPickEvent);
        return labEvent;
    }

    /**
     * Find position maps from plates that may have receptacle barcodes that will need to be extracted
     * to make a DAO call.
     *
     * @param plateTypes        list of plates to which to check if position map will need to be extracted
     * @param positionMapTypes  list of possible positionMaps that may need barcodes extracted
     */
    private List<PositionMapType> extractPositionMapBarcodes(List<PlateType> plateTypes,
                                                             List<PositionMapType> positionMapTypes) {
        List<PositionMapType> extractPositionMaps = new ArrayList<>();
        for (PlateType plateType: plateTypes) {
            if (expectBarcodedReceptacleTypes(plateType)) {
                for (PositionMapType positionMapType: positionMapTypes) {
                    if (positionMapType.getBarcode().equals(plateType.getBarcode())) {
                        extractPositionMaps.add(positionMapType);
                        break;
                    }
                }
            }
        }
        return extractPositionMaps;
    }

    /**
     * Extract the barcodes from position maps, in order to make a DAO call
     *
     * @param barcodes         array to which to add extracted barcodes
     * @param positionMapTypes from liquid handling deck
     */
    private void extractBarcodes(List<String> barcodes, List<PositionMapType> positionMapTypes) {
        for (PositionMapType positionMapType : positionMapTypes) {
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                barcodes.add(receptacleType.getBarcode());
                VesselPosition vesselPosition = VesselPosition.getByName(receptacleType.getPosition());
                if (vesselPosition == null) {
                    throw new RuntimeException("Failed to find position " + receptacleType.getPosition());
                }
            }
        }
    }

    /**
     * Extract the well barcodes from position maps, in order to make a DAO call
     *
     * @param plateBarcode barcode of static plate - prefix for well barcodes
     * @param wellBarcodes array to which to add extracted well barcodes
     * @param receptacles  well list from liquid handling deck
     */
    private void extractPlateWells(String plateBarcode, List<String> wellBarcodes, List<ReceptacleType> receptacles) {
        if (receptacles == null) {
            return;
        }
        for (ReceptacleType receptacleType : receptacles) {
            VesselPosition vesselPosition = VesselPosition.getByName(receptacleType.getPosition());
            if (vesselPosition == null) {
                throw new RuntimeException("Failed to find position " + receptacleType.getPosition());
            }
            wellBarcodes.add(plateBarcode + vesselPosition.name());
        }
    }


    /**
     * DAO-free building of entities from JAXB cherry pick event
     *
     * @param plateCherryPickEvent from liquid handling deck
     * @param mapBarcodeToVessel   existing entities fetched from database
     *
     * @return entity representing transfer
     */
    @DaoFree
    public LabEvent buildFromBettaLims(PlateCherryPickEvent plateCherryPickEvent,
                                       Map<String, LabVessel> mapBarcodeToVessel) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        boolean createSourcesForEvent = LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources();

        Map<String, TubeFormation> mapBarcodeToTubeFormation =
                buildPlates(mapBarcodeToVessel, plateCherryPickEvent.getSourcePlate(),
                        plateCherryPickEvent.getSourcePositionMap(), createSourcesForEvent, true, labEvent);

        mapBarcodeToTubeFormation.putAll(buildPlates(mapBarcodeToVessel, plateCherryPickEvent.getPlate(),
                plateCherryPickEvent.getPositionMap(), createSourcesForEvent, false, labEvent));

        // Map of destination containers to well position and volume being removed for the lab event.
        Map<String, Map<String, BigDecimal>> mapOfDestContainerBarcodeToPositionMap = new HashMap<>();
        // If the event is set to remove destination volume from the source, create a map of destination containers to position and amount removing.
        if (labEvent.getLabEventType().removeDestVolFromSource()) {
            // For each destination we need to be able to get the volume we're removing.
            for (PositionMapType positionMapType : plateCherryPickEvent.getPositionMap()) {
                Map<String, BigDecimal> mapOfDestPosToVol = new HashMap<>();
                for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                    mapOfDestPosToVol.put(receptacleType.getPosition(), receptacleType.getVolume());
                }
                mapOfDestContainerBarcodeToPositionMap.put(positionMapType.getBarcode(), mapOfDestPosToVol);
            }
        }
        Set<String> found = new HashSet<>();
        boolean allowSourceVolRemovalUpdate = true;
        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            if(!found.add(cherryPickSourceType.getDestinationBarcode()+cherryPickSourceType.getDestinationWell())) {
                allowSourceVolRemovalUpdate = false;
                break;
            }
        }

        // Set used to track whether a source has had it's volume updated in a cherry pick event (initial vol set)
        Set<String> sourceUpdated = new HashSet<>();

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String destinationRackBarcode = cherryPickSourceType.getDestinationBarcode();
            // If the message doesn't include the destination rack barcode, assume it's the first one
            if (destinationRackBarcode == null) {
                destinationRackBarcode = plateCherryPickEvent.getPlate().get(0).getBarcode();
            }

            LabVessel sourceContainer = mapBarcodeToTubeFormation.get(cherryPickSourceType.getBarcode());
            LabVessel ancillarySourceVessel = null;
            if (sourceContainer == null) {
                sourceContainer = mapBarcodeToVessel.get(cherryPickSourceType.getBarcode());
                if (sourceContainer == null) {
                    throw new RuntimeException("Failed to find source " + cherryPickSourceType.getBarcode());
                }
            } else {
                ancillarySourceVessel = mapBarcodeToVessel.get(cherryPickSourceType.getBarcode());
            }
            LabVessel destinationContainer = mapBarcodeToTubeFormation.get(destinationRackBarcode);
            LabVessel ancillaryTargetVessel = null;
            if (destinationContainer == null) {
                destinationContainer = mapBarcodeToVessel.get(destinationRackBarcode);
                if (destinationContainer == null) {
                    throw new RuntimeException("Failed to find destination " + destinationRackBarcode);
                }
            } else {
                ancillaryTargetVessel = mapBarcodeToVessel.get(destinationRackBarcode);
            }
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    sourceContainer.getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    ancillarySourceVessel,
                    destinationContainer.getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getDestinationWell()),
                    ancillaryTargetVessel,
                    labEvent));

            PositionMapType sourceContainerPositionMapType = findContainerPositionMapType(cherryPickSourceType.getBarcode(), plateCherryPickEvent.getSourcePositionMap());
            ReceptacleType sourceReceptacleType = findReceptacleType(cherryPickSourceType.getWell(), sourceContainerPositionMapType);
            LabVessel sourceVessel = sourceContainer.getContainerRole().getVesselAtPosition(VesselPosition.getByName(cherryPickSourceType.getWell()));

            // If this is an event where we are allowing destination volume to be removed from the source, we may need to
            // update the source volume in cherry pick before doing the transfer.
            if (labEvent.getLabEventType().removeDestVolFromSource()) {
                if(sourceReceptacleType != null && sourceReceptacleType.getVolume() != null) {
                    // We only want to update the source vessel volume to the vol from the message one time per entire set of cherry picks.
                    if(sourceUpdated.add(sourceReceptacleType.getBarcode())) {
                        sourceVessel.setVolume(sourceReceptacleType.getVolume());
                    }
                }
            }

            // If the event is removing destination volume from the sources and there are no multiple sources to one
            // destination, proceed to find the source barcoded tube and remove destination volume.
            if (labEvent.getLabEventType().removeDestVolFromSource() && allowSourceVolRemovalUpdate) {
                String destinationWell = cherryPickSourceType.getDestinationWell();

                // Get the destination volume amount to remove as noted in the PositionMap of the lab event message.
                BigDecimal destVolToRemove = mapOfDestContainerBarcodeToPositionMap.get(destinationRackBarcode).get(destinationWell);

                // Check to see if the destination map had a volume to remove.
                if (destVolToRemove != null && destVolToRemove.doubleValue() > 0.0) {
                    // Get the source BarcodedTube so we can update the source vessel volume directly.
                    BigDecimal currentSourceVolume = sourceVessel.getVolume() == null ? BigDecimal.ZERO : sourceVessel.getVolume();
                    BigDecimal finalSourceVolume = currentSourceVolume.subtract(destVolToRemove);
                    // Subtract the current source vessel volume from the destination volume amount noted in the message.
                    sourceVessel.setVolume((finalSourceVolume.doubleValue() < 0.0) ? BigDecimal.ZERO : finalSourceVolume);
                }
            }

            // Check whether there's a flag set for depleting sources.
            Boolean depleteSource = labEvent.getLabEventType().depleteSources() ||
                                    getSourceMetadataManipulationType(sourceReceptacleType, LabEventType.SourceHandling.DEPLETE);

            if (depleteSource) {
                sourceVessel.setVolume(BigDecimal.ZERO);
            }
        }
        return labEvent;
    }

    /**
     * Given a {@link PositionMapType} find the {@link ReceptacleType} for the tube matching the position given.
     *
     * @param position         Position to look for
     * @param positionMapType  PositionMapType that holds the information
     * @return {@link PositionMapType} of the desired receptacle or null if not found.
     */
    private ReceptacleType findReceptacleType(String position, PositionMapType positionMapType) {
        if (positionMapType != null) {
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                VesselPosition mapPosition = VesselPosition.getByName(receptacleType.getPosition());
                VesselPosition cherryPickPosition = VesselPosition.getByName(position);
                if (mapPosition == cherryPickPosition) {
                    return receptacleType;
                }
            }
        }
        return null;
    }

    /**
     * Given a container barcode and a list of {@link PositionMapType} objects, find the {@link PositionMapType} for the container.
     * @param containerBarcode Barcode of the container
     * @param positionMapTypes List of position maps from a message.
     * @return
     */
    private PositionMapType findContainerPositionMapType(String containerBarcode, List<PositionMapType> positionMapTypes) {
        for (PositionMapType positionMapType : positionMapTypes) {
            if (positionMapType.getBarcode().equalsIgnoreCase(containerBarcode)) {
                return positionMapType;
            }
        }
        return null;
    }

    /**
     * Wrapper around {@link #buildPlates(Map, List, List, boolean, boolean, LabEvent)},
     * for single plates and position maps.
     */
    @DaoFree
    private Map<String, TubeFormation> buildPlate(Map<String, LabVessel> mapBarcodeToVessel,
                                                  PlateType plateJaxb, PositionMapType positionMap,
                                                  boolean createSourcesForEvent, boolean source, LabEvent labEvent) {
        List<PlateType> plateTypeList = new ArrayList<>();
        if (plateJaxb != null) {
            plateTypeList.add(plateJaxb);
        }
        List<PositionMapType> positionMapTypeList = new ArrayList<>();
        if (positionMap != null) {
            positionMapTypeList.add(positionMap);
        }
        return buildPlates(mapBarcodeToVessel, plateTypeList, positionMapTypeList, createSourcesForEvent, source,
                labEvent);
    }

    /**
     * Builds plate / rack entities from JAXB fields.
     *
     * @param mapBarcodeToVessel Existing entities fetched from the database.  New entities are added to this map.
     * @param platesJaxb         list of plates / racks
     * @param positionMaps       list of tubes / positions
     * @param create             true if this method should create entities not found in mapBarcodeToVessel
     * @param areSourceTubes     true if the plate is a areSourceTubes in an event
     *
     * @return map from rack barcode to tube formation (in mapBarcodeToVessel, tube formations are mapped by their digest)
     */
    @DaoFree
    private Map<String, TubeFormation> buildPlates(Map<String, LabVessel> mapBarcodeToVessel,
                                                   List<PlateType> platesJaxb, List<PositionMapType> positionMaps,
                                                   boolean create, boolean areSourceTubes, LabEvent labEvent) {
        Map<String, TubeFormation> mapBarcodeToTubeFormation = new HashMap<>();
        for (PlateType plateType : platesJaxb) {
            String plateBarcode = plateType.getBarcode();

            // Find the map associated with the plate
            PositionMapType positionMapType = null;
            boolean positionsFound = false;
            for (PositionMapType theMap : positionMaps) {
                if (theMap.getBarcode().equals(plateBarcode)) {
                    positionsFound = true;
                    positionMapType = theMap;
                    break;
                }
            }

            if (plateType.getPhysType().equals(PHYS_TYPE_TUBE_RACK) ||
                    RackOfTubes.RackType.getByName(plateType.getPhysType()) != null) {
                if (positionsFound) {
                    String digest = makeDigest(positionMapType, mapBarcodeToVessel);
                    TubeFormation tubeFormation = OrmUtil.proxySafeCast(mapBarcodeToVessel.get(digest),
                            TubeFormation.class);
                    RackOfTubes rackOfTubes = OrmUtil.proxySafeCast(
                            mapBarcodeToVessel.get(plateBarcode), RackOfTubes.class);
                    boolean rackOfTubesWasNull = rackOfTubes == null;

                    Map<String, BarcodedTube> mapBarcodeToTube = new HashMap<>();
                    for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                        mapBarcodeToTube.put(receptacleType.getBarcode(),
                                OrmUtil.proxySafeCast(mapBarcodeToVessel.get(receptacleType.getBarcode()),
                                        BarcodedTube.class)
                        );
                    }
                    if (tubeFormation == null) {
                        tubeFormation = buildRackDaoFree(mapBarcodeToTube, rackOfTubes, plateType,
                                positionMapType, areSourceTubes, create, labEvent);
                        mapBarcodeToVessel.put(tubeFormation.getLabel(), tubeFormation);
                        if (rackOfTubesWasNull) {
                            rackOfTubes = tubeFormation.getRacksOfTubes().iterator().next();
                            mapBarcodeToVessel.put(rackOfTubes.getLabel(), rackOfTubes);
                        }
                    } else {
                        if (rackOfTubes == null) {
                            RackOfTubes.RackType rackType = getRackType(plateType);
                            rackOfTubes = new RackOfTubes(plateBarcode, rackType);
                            mapBarcodeToVessel.put(rackOfTubes.getLabel(), rackOfTubes);
                            tubeFormation.addRackOfTubes(rackOfTubes);
                        }
                        setTubeQuantities(mapBarcodeToTube, positionMapType, labEvent, areSourceTubes);
                    }
                    mapBarcodeToTubeFormation.put(plateBarcode, tubeFormation);
                } else {
                    // Position map for rack of tubes is mandatory
                    throw new RuntimeException("Failed to find positionMap for " + plateBarcode);
                }
            } else {
                LabVessel labVessel = mapBarcodeToVessel.get(plateBarcode);

                if (labVessel == null) {
                    IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.getTypeForPhysTypeAndBarcode(
                            plateType.getPhysType(), plateBarcode);
                    if (flowcellType != null) {
                        labVessel = new IlluminaFlowcell(flowcellType, plateBarcode);
                    }

                    if (labVessel == null) {
                        if (areSourceTubes && !create) {
                            throw new RuntimeException("Failed to find plate " + plateBarcode);
                        }
                        labVessel = new StaticPlate(plateBarcode,
                                StaticPlate.PlateType.getByAutomationName(plateType.getPhysType()));
                    }
                }

                // Handle wells if positionMap contains entries
                if (positionsFound && OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                    StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                    Map<String, PlateWell> mapBarcodeToWell = new HashMap<>();
                    staticPlate.getContainerRole().getContainedVessels().stream().forEach((well) -> mapBarcodeToWell.put(well.getLabel(), well));

                    // Build wells only if receptacles explicitly provided in message
                    for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                        VesselPosition position = VesselPosition.getByName(receptacleType.getPosition());
                        String wellBarcode = plateBarcode + position;
                        if (mapBarcodeToWell.get(wellBarcode) == null) {
                            if (position == null) {
                                throw new RuntimeException("Failed to find position " + receptacleType.getPosition());
                            }
                            PlateWell well = new PlateWell(staticPlate, position);
                            staticPlate.getContainerRole().addContainedVessel(well, position);
                            mapBarcodeToWell.put(wellBarcode, well);
                            mapBarcodeToVessel.put(wellBarcode, well);
                        }
                    }
                    setWellQuantities(mapBarcodeToWell, positionMapType);
                }
                mapBarcodeToVessel.put(plateBarcode, labVessel);
            }
        }
        return mapBarcodeToTubeFormation;
    }

    /**
     * Makes a TubeFormation digest from a position map.
     * @param positionMapType from message
     * @param mapBarcodeToVessel previously fetched tubes (allows use of Mercury Sample key or vessel barcode)
     */
    private String makeDigest(PositionMapType positionMapType, Map<String, LabVessel> mapBarcodeToVessel) {
        List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            String barcode = receptacleType.getBarcode();
            // A ReceptacleType with no barcode (position only) is used to set volumes on plate wells, but there is
            // no digest for this.
            if (barcode == null) {
                return null;
            }
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);
            positionBarcodeList.add(new ImmutablePair<>(
                    VesselPosition.getByName(receptacleType.getPosition()),
                    labVessel ==  null ? barcode : labVessel.getLabel()));
        }
        return TubeFormation.makeDigest(positionBarcodeList);
    }

    /**
     * From a list of positionMaps, create a map from barcode to rack
     *
     * @param positionMap list of positionMaps
     *
     * @return map from barcode to rack
     */
    private Map<String, TubeFormation> buildMapBarcodeToTubeFormation(List<PositionMapType> positionMap) {
        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = new HashMap<>();
        for (PositionMapType positionMapType : positionMap) {
            TubeFormation tubeFormation = fetchTubeFormation(positionMapType);
            mapBarcodeToSourceTubeFormation.put(positionMapType.getBarcode(), tubeFormation);
        }
        return mapBarcodeToSourceTubeFormation;
    }

    /**
     * Fetch an existing tube formation from the database
     *
     * @param positionMapType tube barcodes and positions
     *
     * @return database rack
     */
    private TubeFormation fetchTubeFormation(PositionMapType positionMapType) {
        return tubeFormationDao.findByDigest(makeDigest(positionMapType, Collections.emptyMap()));
    }

    /**
     * Build an entity to represent a cherry pick (random access) transfer between a source rack and a target strip
     * tube. Builds source and target LabVessels on-the-fly.
     *
     * @param plateCherryPickEvent          JAXB cherry-pick event, either deserialized from XML or created from BettaLimsMessageFactory
     * @param mapBarcodeToSourceTubeFormation
     *                                      map from source rack barcode to TubeFormation entities; newly created TubeFormations will be added to this map
     * @param mapBarcodeToSourceTube        map from source tube barcode to BarcodedTube entities; newly created BarcodedTubes will be added to this this map
     * @param mapBarcodeToTargetTubeFormation
     *                                      unused
     * @param mapBarcodeToTargetStripTube   map from target strip tube barcode to StripTube entities; newly created StripTubes will NOT be added to this map
     * @param mapBarcodeToSourceRackOfTubes map from target rack barcode to RackOfTubes entities; newly created RackOfTubes will NOT be added to this map
     *
     * @return a new LabEvent entity
     */
    @DaoFree
    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                         Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                                         Map<String, BarcodedTube> mapBarcodeToSourceTube,
                                                         Map<String, TubeFormation> mapBarcodeToTargetTubeFormation,
                                                         Map<String, StripTube> mapBarcodeToTargetStripTube,
                                                         Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube,
                mapBarcodeToSourceRackOfTubes, labEvent);

/*
        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToTargetTubeFormation.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                // todo jmt do we care about the strip tube holder?
                TubeFormation targetRack = new TubeFormation(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getContainerRole());
            }
        }
*/

        Map<String, StripTube> mapPositionToStripTube = new HashMap<>();
        for (ReceptacleType receptacleType : plateCherryPickEvent.getPositionMap().get(0).getReceptacle()) {
            if (!receptacleType.getReceptacleType().equals(PHYS_TYPE_STRIP_TUBE)) {
                throw new RuntimeException("Expected physType " + PHYS_TYPE_STRIP_TUBE + ", but received " +
                                           receptacleType.getReceptacleType());
            }
            StripTube stripTube = mapBarcodeToTargetStripTube.get(receptacleType.getBarcode());
            if (stripTube == null) {
                stripTube = new StripTube(receptacleType.getBarcode());
            }
            mapPositionToStripTube.put(receptacleType.getPosition(), stripTube);
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String position = LEADING_ZERO_PATTERN.matcher(cherryPickSourceType.getDestinationWell().substring(1))
                    .replaceFirst("");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceTubeFormation.get(cherryPickSourceType.getBarcode()).getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapBarcodeToSourceRackOfTubes.get(cherryPickSourceType.getBarcode()),
                    mapPositionToStripTube.get(position).getContainerRole(),
                    VesselPosition.getByName("TUBE" + Integer.toString(
                            cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1)),
                    null,
                    labEvent));
        }
        return labEvent;
    }


    /**
     * Create a PlateCherryPickEvent for transferring from a rack of tubes to a plate.
     *
     * @param plateCherryPickEvent          starting event for the transfer.
     * @param mapBarcodeToSourceTubeFormation
     *                                      map from source rack barcode to TubeFormation entities;
     *                                      newly created TubeFormations will be added to this map
     * @param mapBarcodeToSourceRackOfTubes map from target rack barcode to RackOfTubes entities;
     *                                      newly created RackOfTubes will NOT be added to this map
     * @param mapBarcodeToSourceTube        map from source tube barcode to BarcodedTube entities;
     *                                      newly created BarcodedTubes will be added to this this map
     *
     * @return the LabEvent object
     */
    @DaoFree
    private LabEvent buildCherryPickRackToReagentKitDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                          Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                                          Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes,
                                                          Map<String, BarcodedTube> mapBarcodeToSourceTube
    ) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube,
                mapBarcodeToSourceRackOfTubes, labEvent);

        for (Map.Entry<String, RackOfTubes> rackOfTubesSet : mapBarcodeToSourceRackOfTubes.entrySet()) {
            RackOfTubes rack = rackOfTubesSet.getValue();
            for (Map.Entry<String, TubeFormation> stringVesselContainerEntry : mapBarcodeToSourceTubeFormation
                    .entrySet()) {
                if (stringVesselContainerEntry.getValue() == null) {
                    TubeFormation targetRack =
                            buildRackDaoFree(mapBarcodeToSourceTube, rack, plateCherryPickEvent.getPlate().get(0),
                                    plateCherryPickEvent.getPositionMap().get(0), false,
                                    LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources(),
                                    labEvent);
                    stringVesselContainerEntry.setValue(targetRack);
                }
            }
        }

        // todo jmt why is this done twice?
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube,
                mapBarcodeToSourceRackOfTubes, labEvent);
        MiSeqReagentKit reagentKit = null;
        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            // todo jmt take barcode from plate element, not source element
            String reagentKitBarcode = cherryPickSourceType.getDestinationBarcode();
            if (reagentKit == null) {
                reagentKit = new MiSeqReagentKit(reagentKitBarcode);
            } else {
                if (reagentKitBarcode.equals(reagentKit.getLabel())) {
                    // todo jmt why limit this?
                    throw new RuntimeException("Can only transfer to one ReagentKit ");
                }
            }

            CherryPickTransfer cherryPickTransfer = new CherryPickTransfer(
                    mapBarcodeToSourceTubeFormation.get(cherryPickSourceType.getBarcode()).getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapBarcodeToSourceRackOfTubes.get(cherryPickSourceType.getBarcode()),
                    reagentKit.getContainerRole(),
                    MiSeqReagentKit.LOADING_WELL,
                    null,
                    labEvent);
            labEvent.getCherryPickTransfers().add(cherryPickTransfer);
        }
        return labEvent;
    }

    /**
     * if a source rack is not already in the map, add it
     *
     * @param plateCherryPickEvent    JAXB
     * @param mapBarcodeToSourceTubeFormation
     *                                map
     * @param mapBarcodeToSourceTube  needed to build the rack
     * @param mapBarcodeToRackOfTubes
     */
    private void addSourceTubeFormationsToMap(PlateCherryPickEvent plateCherryPickEvent,
                                              Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                              Map<String, BarcodedTube> mapBarcodeToSourceTube,
                                              Map<String, RackOfTubes> mapBarcodeToRackOfTubes,
                                              LabEvent labEvent) {
        for (PlateType sourceRackJaxb : plateCherryPickEvent.getSourcePlate()) {
            TubeFormation sourceTubeFormationEntity = mapBarcodeToSourceTubeFormation.get(sourceRackJaxb.getBarcode());
            if (sourceTubeFormationEntity == null) {
                for (PositionMapType sourcePositionMap : plateCherryPickEvent.getSourcePositionMap()) {
                    if (sourcePositionMap.getBarcode().equals(sourceRackJaxb.getBarcode())) {
                        sourceTubeFormationEntity = buildRackDaoFree(mapBarcodeToSourceTube,
                                mapBarcodeToRackOfTubes.get(sourceRackJaxb.getBarcode()), sourceRackJaxb,
                                sourcePositionMap,
                                true, LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources(),
                                labEvent);
                        break;
                    }
                }
            }
            // todo jmt how can this be null at this point?
            if (sourceTubeFormationEntity == null) {
                throw new RuntimeException("Failed to find source position map for " + sourceRackJaxb.getBarcode());
            }
            mapBarcodeToSourceTubeFormation.put(sourceRackJaxb.getBarcode(), sourceTubeFormationEntity);
        }
    }

    /**
     * Builds a lab event entity from a JAXB plate event (reagent addition) bean
     *
     * @param plateEventType JAXB event bean
     *
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        LabEvent labEvent;
        PlateType plateType = plateEventType.getPlate();
        if (plateType.getPhysType().equals(PHYS_TYPE_TUBE_RACK) ||
                RackOfTubes.RackType.getByName(plateType.getPhysType()) != null) {

            RackOfTubes rackOfTubes = rackOfTubesDao.findByBarcode(plateEventType.getPlate().getBarcode());
            Map<String, BarcodedTube> mapBarcodeToVessel = findTubesByBarcodes(plateEventType.getPositionMap());
            //noinspection unchecked
            TubeFormation tubeFormation = tubeFormationDao.findByDigest(makeDigest(plateEventType.getPositionMap(),
                    (Map<String, LabVessel>) (Map<?, ?>)mapBarcodeToVessel));
            //noinspection unchecked
            trySampleIds(plateEventType.getPositionMap().getReceptacle().stream().map(ReceptacleType::getBarcode).
                    collect(Collectors.toList()), (Map<String, LabVessel>) (Map<?, ?>)mapBarcodeToVessel, mercurySampleDao);
            labEvent = buildFromBettaLimsRackEventDbFree(plateEventType, tubeFormation, mapBarcodeToVessel, rackOfTubes);
        } else {
            PlateType plate = plateEventType.getPlate();
            if (plate == null) {
                // todo jmt why isn't this error caught in JAXB?
                throw new RuntimeException("No plate element in plateEvent");
            }
            StaticPlate staticPlate = staticPlateDao.findByBarcode(plate.getBarcode());
            labEvent = buildFromBettaLimsPlateEventDbFree(plateEventType, staticPlate);
        }
        labEvent.setStationEventType(plateEventType);
        return labEvent;
    }

    /**
     * Builds a lab event entity from a JAXB plate transfer event bean, (plate|rack) -> (plate|rack) or
     * strip tube -> flowcell
     *
     * @param plateTransferEvent JAXB from liquid handling deck
     *
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        if (plateTransferEvent.getSourcePlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE) &&
                plateTransferEvent.getPlate().getPhysType().equals(PHYS_TYPE_FLOWCELL)) {
            // todo jmt create if null
            StripTube stripTube = stripTubeDao.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
            IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(
                    plateTransferEvent.getPlate().getBarcode());
            LabEvent labEvent = buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, stripTube, illuminaFlowcell);
            return labEvent;
        }
        List<String> barcodes = new ArrayList<>();
        // Source and destination plate barcodes
        barcodes.add(plateTransferEvent.getSourcePlate().getBarcode());
        barcodes.add(plateTransferEvent.getPlate().getBarcode());

        // Source
        if (plateTransferEvent.getSourcePositionMap() != null) {
            if (expectBarcodedReceptacleTypes(plateTransferEvent.getSourcePlate())) {
                extractBarcodes(barcodes, Collections.singletonList(plateTransferEvent.getSourcePositionMap()));
            } else if (shouldCreatePlateWells(plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap().getReceptacle())) {
                extractPlateWells(plateTransferEvent.getSourcePlate().getBarcode(), barcodes, plateTransferEvent.getSourcePositionMap().getReceptacle());
            }
        }
        // Destination
        if (plateTransferEvent.getPositionMap() != null) {
            if (expectBarcodedReceptacleTypes(plateTransferEvent.getPlate())) {
                extractBarcodes(barcodes, Collections.singletonList(plateTransferEvent.getPositionMap()));
            } else if (shouldCreatePlateWells(plateTransferEvent.getPlate(), plateTransferEvent.getPositionMap().getReceptacle())) {
                extractPlateWells(plateTransferEvent.getPlate().getBarcode(), barcodes, plateTransferEvent.getPositionMap().getReceptacle());
            }
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        trySampleIds(barcodes, mapBarcodeToVessel, mercurySampleDao);

        if (plateTransferEvent.getSourcePositionMap() != null &&
                expectBarcodedReceptacleTypes(plateTransferEvent.getSourcePlate())) {
            findTubeFormations(Collections.singletonList(plateTransferEvent.getSourcePositionMap()), mapBarcodeToVessel);
        }
        if (plateTransferEvent.getPositionMap() != null &&
                expectBarcodedReceptacleTypes(plateTransferEvent.getPlate())) {
            findTubeFormations(Collections.singletonList(plateTransferEvent.getPositionMap()), mapBarcodeToVessel);
        }

        LabEvent labEvent = buildFromBettaLims(plateTransferEvent, mapBarcodeToVessel);
        labEvent.setStationEventType(plateTransferEvent);
        return labEvent;
    }

    /**
     * Adds TubeFormation entities to vessel map.
     * @param positionMaps from message
     * @param mapBarcodeToVessel previously fetched tubes (allows use of Mercury Sample key or vessel barcode),
     *                           TubeFormations are added
     */
    private void findTubeFormations(List<PositionMapType> positionMaps, Map<String, LabVessel> mapBarcodeToVessel) {
        for (PositionMapType positionMap : positionMaps) {
            String digest = makeDigest(positionMap, mapBarcodeToVessel);
            if (digest != null) {
                mapBarcodeToVessel.put(digest, tubeFormationDao.findByDigest(digest));
            }
        }
    }

    /**
     * In extractions, it is sometimes easier to scan the SM-ID than the manufacturer barcode.  If a barcode starts
     * with SM-, and it wasn't found when fetched by label, try fetching by sample ID.
     * @param barcodes           barcodes from the message
     * @param mapBarcodeToVessel vessels already fetched by manufacturer barcode, added to if fetch by SM-ID is
     *                           successful
     * @param mercurySampleDao   used to fetch
     */
    public static void trySampleIds(List<String> barcodes, Map<String, LabVessel> mapBarcodeToVessel,
            MercurySampleDao mercurySampleDao) {
        List<String> sampleIds = new ArrayList<>();
        for (String barcode : barcodes) {
            if (barcode.startsWith("SM-")) {
                if (mapBarcodeToVessel.get(barcode) == null) {
                    sampleIds.add(barcode);
                }
            }
        }
        if (!sampleIds.isEmpty()) {
            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);
            for (Map.Entry<String, MercurySample> sampleIdSampleEntry : mapIdToMercurySample.entrySet()) {
                MercurySample mercurySample = sampleIdSampleEntry.getValue();
                if (mercurySample != null) {
                    Set<LabVessel> labVessel = mercurySample.getLabVessel();
                    if (labVessel.size() == 1) {
                        mapBarcodeToVessel.put(sampleIdSampleEntry.getKey(), labVessel.iterator().next());
                    }
                }
            }
        }
    }

    /**
     * Dao-free method to build a lab event entity from a rack or plate transfer to a rack or plate
     *
     * @param plateTransferEvent from liquid handling deck
     * @param mapBarcodeToVessel entities fetched from database
     *
     * @return entity
     */
    @DaoFree
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent,
                                       Map<String, LabVessel> mapBarcodeToVessel) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        boolean createSourcesForEvent = LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources();

        Map<String, TubeFormation> mapBarcodeToTubeFormation = buildPlate(mapBarcodeToVessel,
                plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap(), createSourcesForEvent,
                true, labEvent);

        mapBarcodeToTubeFormation.putAll(buildPlate(mapBarcodeToVessel, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap(), createSourcesForEvent, false, labEvent));

        LabVessel sourceContainer = mapBarcodeToTubeFormation.get(plateTransferEvent.getSourcePlate().getBarcode());
        LabVessel ancillarySourceLabVessel = null;
        if (sourceContainer == null) {
            sourceContainer = mapBarcodeToVessel.get(plateTransferEvent.getSourcePlate().getBarcode());
            if (sourceContainer == null) {
                throw new RuntimeException("Failed to find source " + plateTransferEvent.getSourcePlate().getBarcode());
            }
            if (sourceContainer.getContainerRole() == null) {
                throw new RuntimeException(plateTransferEvent.getSourcePlate().getBarcode() + " is not a container");
            }
        } else {
            ancillarySourceLabVessel = mapBarcodeToVessel.get(plateTransferEvent.getSourcePlate().getBarcode());
        }

        LabVessel destinationContainer = mapBarcodeToTubeFormation.get(plateTransferEvent.getPlate().getBarcode());
        LabVessel ancillaryDestinationLabVessel = null;
        if (destinationContainer == null) {
            destinationContainer = mapBarcodeToVessel.get(plateTransferEvent.getPlate().getBarcode());
            if (destinationContainer == null) {
                throw new RuntimeException("Failed to find destination " + plateTransferEvent.getPlate().getBarcode());
            }
            if (destinationContainer.getContainerRole() == null) {
                throw new RuntimeException(plateTransferEvent.getPlate().getBarcode() + " is not a container");
            }
        } else {
            ancillaryDestinationLabVessel = mapBarcodeToVessel.get(plateTransferEvent.getPlate().getBarcode());
        }

        if (sourceContainer.getLabel().equals(destinationContainer.getLabel())) {
            throw new RuntimeException("Source and destination barcodes are the same " + sourceContainer.getLabel());
        }
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceContainer.getContainerRole(),
                SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                ancillarySourceLabVessel,
                destinationContainer.getContainerRole(),
                SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()),
                ancillaryDestinationLabVessel,
                labEvent));
        return labEvent;
    }

    /**
     * Build a rack entity
     *
     * @param mapBarcodeToTubes source tubes
     * @param rackOfTubes       The rack entity based on the plate element in the message
     * @param plate             JAXB rack
     * @param positionMap       JAXB list of tube barcodes
     * @param areSourceTubes    Whether the mapBarcodeToTubes passed are sources
     * @param createSources     Whether the sources can be created if missing
     * @param labEvent          The LabEvent that the rack is for.
     *
     * @return entity
     */
    @DaoFree
    private TubeFormation buildRackDaoFree(Map<String, BarcodedTube> mapBarcodeToTubes, RackOfTubes rackOfTubes,
                                           PlateType plate, PositionMapType positionMap, boolean areSourceTubes,
                                           boolean createSources, LabEvent labEvent) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new EnumMap<>(VesselPosition.class);
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if (barcodedTube == null) {
                if (areSourceTubes && !(CREATE_SOURCES || createSources)) {
                    throw new RuntimeException("Failed to find tube " + receptacleType.getBarcode());
                }
                BarcodedTube.BarcodedTubeType tubeType =
                        BarcodedTube.BarcodedTubeType.getByAutomationName(receptacleType.getReceptacleType());
                if (tubeType == null) {
                    tubeType = BarcodedTube.BarcodedTubeType.MatrixTube;
                }
                barcodedTube = new BarcodedTube(receptacleType.getBarcode(), tubeType);
                mapBarcodeToTubes.put(receptacleType.getBarcode(), barcodedTube);
            }
            mapPositionToTube.put(VesselPosition.getByName(receptacleType.getPosition()), barcodedTube);
        }
        setTubeQuantities(mapBarcodeToTubes, positionMap, labEvent, areSourceTubes);
        RackOfTubes.RackType rackType = getRackType(plate);
        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, rackType);
        if (rackOfTubes == null) {
            rackOfTubes = new RackOfTubes(plate.getBarcode(), rackType);
        }
        tubeFormation.addRackOfTubes(rackOfTubes);
        return tubeFormation;
    }

    private RackOfTubes.RackType getRackType(PlateType plate) {
        RackOfTubes.RackType rackType = RackOfTubes.RackType.getByName(plate.getPhysType());
        if(rackType == null){
            rackType = RackOfTubes.RackType.Matrix96;
        }
        return rackType;
    }

    private boolean expectBarcodedReceptacleTypes(PlateType plate) {
        return plate.getPhysType().equals(PHYS_TYPE_TUBE_RACK)
               || RackOfTubes.RackType.getByName(plate.getPhysType()) != null;
    }

    /**
     * If a plate event message occurs on a static plate with positionMap receptacles,
     *   explicit vessels for each well should be created
     */
    private boolean shouldCreatePlateWells(PlateType plateType, List<ReceptacleType> receptacles) {
        return receptacles != null && (
                plateType.getPhysType().equals(PHYS_TYPE_EPPENDORF_96)
                        || StaticPlate.PlateType.getByAutomationName(plateType.getPhysType()) != null);
    }

    /**
     * Set volume, concentration, receptacleWeight etc.
     *
     * @param mapBarcodeToTubes map from tube barcode to tube
     * @param positionMap       JAXB quantities from deck
     * @param areSourceTubes    Whether the tubes are source samples.
     */
    @DaoFree
    private void setTubeQuantities(Map<String, BarcodedTube> mapBarcodeToTubes, PositionMapType positionMap,
                                   LabEvent labEvent, Boolean areSourceTubes) {
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if (barcodedTube != null) {
                setTubeQuantities(receptacleType, barcodedTube, labEvent, areSourceTubes);
            }
        }
    }

    private void setTubeQuantities(ReceptacleType receptacleType, BarcodedTube barcodedTube, LabEvent labEvent,
                                   Boolean areSourceTubes) {
        // Allow update if the tubes are destination tubes or the lab event is NOT removing destination volume amount from sources.
        if (!areSourceTubes || (!labEvent.getLabEventType().removeDestVolFromSource())) {
            if (receptacleType.getVolume() != null) {
                barcodedTube.setVolume(receptacleType.getVolume());
            }
            if (receptacleType.getConcentration() != null) {
                barcodedTube.setConcentration(receptacleType.getConcentration());
            }
            if (receptacleType.getReceptacleWeight() != null) {
                barcodedTube.setReceptacleWeight(receptacleType.getReceptacleWeight());
            }
        }
        if (labEvent.getLabEventType().getVolumeConcUpdate() == LabEventType.VolumeConcUpdate.BSP_AND_MERCURY) {
            MercurySample mercurySample = extractSample(barcodedTube.getSampleInstancesV2());
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                // Check to see if the event has a metadata flag to apply to all daughters.
                // At least one of the values must be set or the tube(s) must be sources that are set to be depleted in
                // order to incur the cost of calling BSP.

                // Check the source receptacle type and event metadata to see if it has source specific manipulation set.
                Boolean terminateDepleted = labEvent.getLabEventType().depleteSources() ||
                                            getSourceMetadataManipulationType(receptacleType, LabEventType.SourceHandling.TERMINATE_DEPLETED);
                Boolean depleteSource = labEvent.getLabEventType().depleteSources() ||
                                        getSourceMetadataManipulationType(receptacleType, LabEventType.SourceHandling.DEPLETE);

                if ((receptacleType.getVolume() != null || receptacleType.getConcentration() != null ||
                        receptacleType.getReceptacleWeight() != null) || (areSourceTubes && (terminateDepleted || depleteSource))){
                    // If this is setting quantities for sources, check to see if we need to deplete the
                    // sources or possibly flag for termination on depletion.
                    if (areSourceTubes) {
                        // If the flag is set to deplete sources, then we need to set the volume to zero.
                        if (depleteSource) {
                            receptacleType.setVolume(BigDecimal.ZERO);
                            // If we're depleting the source, update Mercury source volume as well.
                            barcodedTube.setVolume(BigDecimal.ZERO);
                        }

                        // If the lab event type has a flag set for 'TERMINATE_DEPLETED' then check to see if the
                        // individual source sample is set for terminating on depleted.
                        if (labEvent.getLabEventType().terminateDepletedSources()) {
                            terminateDepleted = true;   // Default to true, then check for the individual metadata flag
                        }
                    }

                    BSPSetVolumeConcentration.TerminateAction terminateAction =
                            terminateDepleted ? BSPSetVolumeConcentration.TerminateAction.TERMINATE_DEPLETED :
                                    BSPSetVolumeConcentration.TerminateAction.LEAVE_CURRENT_STATE;
                    String result = bspSetVolumeConcentration.setVolumeAndConcentration(receptacleType.getBarcode(),
                            receptacleType.getVolume(), receptacleType.getConcentration(),
                            receptacleType.getReceptacleWeight(),
                            terminateAction);
                    if (!result.equals(BSPSetVolumeConcentration.RESULT_OK)) {
                        logger.error(result);
                    }
                }
            }
        }
    }

    /**
     * Given a {@link ReceptacleType} look through it's {@link MetadataType} to see if it has the
     * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.SourceHandling} passed into the method.
     *
     * @param receptacleType ReceptacleType object representing a tube/well position in a transfer.
     * @param sourceHandling SourceHandling object that determines what type of action we're looking for.
     * @return Returns false if the passed SourceHandling isn't found in the ReceptacleType metadata or the boolean value of the
     * metadatatype returned.
     */
    private Boolean getSourceMetadataManipulationType(ReceptacleType receptacleType,
                                                      LabEventType.SourceHandling sourceHandling) {
        if (receptacleType != null) {
            for (MetadataType metadataType : receptacleType.getMetadata()) {
                // If the individual tube has the metadata flag set, then use the value set.
                if (metadataType.getName().compareToIgnoreCase(sourceHandling.getDisplayName()) == 0) {
                    return Boolean.valueOf(metadataType.getValue());
                }
            }
        }
        return false;
    }

    /**
     * When plate wells are created from a bettalims message, copy over any associated volume and concentration data
     *
     * @param mapBarcodeToWell map from well barcode to well
     * @param positionMap       JAXB quantities from deck
     */
    @DaoFree
    private void setWellQuantities(Map<String, PlateWell> mapBarcodeToWell, PositionMapType positionMap) {
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            PlateWell plateWell = mapBarcodeToWell.get(positionMap.getBarcode() + VesselPosition.getByName(receptacleType.getPosition()));
            if (plateWell != null) {
                if (receptacleType.getVolume() != null) {
                    plateWell.setVolume(receptacleType.getVolume());
                }
                if (receptacleType.getConcentration() != null) {
                    plateWell.setConcentration(receptacleType.getConcentration());
                }
            }
        }
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateEventDbFree(PlateEventType plateEvent, StaticPlate staticPlate) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        Map<String, PlateWell> mapBarcodeToWell = new HashMap<>();

        if (staticPlate == null) {
            if (CREATE_SOURCES) {
                staticPlate = new StaticPlate(plateEvent.getPlate().getBarcode(), StaticPlate.PlateType.getByAutomationName(
                        plateEvent.getPlate().getPhysType()));
            } else {
                throw new RuntimeException("Failed to find plate " + plateEvent.getPlate().getBarcode());
            }
        } else {
            staticPlate.getContainerRole().getContainedVessels().stream().forEach((well) -> mapBarcodeToWell.put(well.getLabel(), well));
        }

        PositionMapType positionMapType = plateEvent.getPositionMap();
        if (positionMapType != null && positionMapType.getReceptacle() != null) {
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                VesselPosition position = VesselPosition.getByName(receptacleType.getPosition());
                if (position == null) {
                    throw new RuntimeException("Failed to find position " + receptacleType.getPosition());
                }
                String wellBarcode = staticPlate.getLabel() + position;
                if (mapBarcodeToWell.get(wellBarcode) == null) {
                    PlateWell well = new PlateWell(staticPlate, position);
                    staticPlate.getContainerRole().addContainedVessel(well, position);
                    mapBarcodeToWell.put(wellBarcode, well);
                }
            }
            setWellQuantities(mapBarcodeToWell, positionMapType);
        }

        staticPlate.addInPlaceEvent(labEvent);
        return labEvent;
    }

    // todo jmt make this database free?
    private void addReagents(LabEvent labEvent, List<ReagentType> reagentTypes) {
        LabEventType.ManualTransferDetails manualTransferDetails = labEvent.getLabEventType().getManualTransferDetails();

        for (ReagentType reagentType : reagentTypes) {
            GenericReagent genericReagent = null;
            // This is null only in database free tests
            if (genericReagentDao != null) {
                genericReagent = genericReagentDao.findByReagentNameLotExpiration(
                        reagentType.getKitType(), reagentType.getBarcode(), reagentType.getExpiration());
            }
            if (genericReagent == null) {
                genericReagent = new GenericReagent(reagentType.getKitType(), reagentType.getBarcode(),
                        reagentType.getExpiration());
            }
            Set<Metadata> metadataSet = new HashSet<>();
            for (MetadataType metadataType: reagentType.getMetadata()) {
                Metadata.Key metadataKey = Metadata.Key.fromDisplayName(metadataType.getName());
                if(metadataKey != null) {
                    Metadata metadata = new Metadata(metadataKey, metadataType.getValue());
                    metadataSet.add(metadata);
                } else {
                    throw new RuntimeException("Failed to find metadata " + metadataType.getName());
                }
            }

            labEvent.addReagentMetadata(genericReagent, metadataSet);
        }
    }

    private void addMetadatas( LabEvent labEvent, List<MetadataType> metadataTypes) {
        for (MetadataType metadataType: metadataTypes){
            LabEventMetadata.LabEventMetadataType labEventMetadataType =
                    LabEventMetadata.LabEventMetadataType.getByName(metadataType.getName());
            if (labEventMetadataType != null) { //Throw runtime exception for unknown metadata?
                LabEventMetadata labEventMetadata =
                        new LabEventMetadata(labEventMetadataType, metadataType.getValue());
                labEvent.addMetadata(labEventMetadata);
            } else {
                throw new RuntimeException("Failed to find metadata " + metadataType.getName());
            }
        }
    }

    @DaoFree
    public LabEvent buildFromBettaLimsRackEventDbFree(PlateEventType plateEvent, @Nullable TubeFormation tubeFormation,
                                                      Map<String, BarcodedTube> mapBarcodeToTubes,
                                                      @Nullable RackOfTubes rackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if (tubeFormation == null) {
            tubeFormation =
                    buildRackDaoFree(mapBarcodeToTubes, rackOfTubes, plateEvent.getPlate(), plateEvent.getPositionMap(),
                            true, LabEventType.getByName(plateEvent.getEventType()).isCreateSources(), labEvent);
        } else {
            setTubeQuantities(mapBarcodeToTubes, plateEvent.getPositionMap(), labEvent, true);
        }
        tubeFormation.addInPlaceEvent(labEvent);
        return labEvent;
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateToPlateDbFree(PlateTransferEventType plateTransferEvent,
                StripTube sourceStripTube, IlluminaFlowcell targetFlowcell) {
        if (sourceStripTube == null) {
            throw new RuntimeException("Failed to find StripTube " + plateTransferEvent.getSourcePlate().getBarcode());
        }
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        String barcode = plateTransferEvent.getPlate().getBarcode();
        IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.getTypeForPhysTypeAndBarcode(
                plateTransferEvent.getPlate().getPhysType(), barcode);

        if (targetFlowcell == null) {
            targetFlowcell = new IlluminaFlowcell(flowcellType, barcode);
        }

        SBSSection targetSection;
        SBSSection sourceSection = SBSSection.STRIP_TUBE8;
        switch (flowcellType.getVesselGeometry()) {
        case FLOWCELL1x1:
            targetSection = SBSSection.LANE1;
            break;
        case FLOWCELL1x2:
            sourceSection = SBSSection.STRIP_TUBE2;
            targetSection = SBSSection.ALL2;
            break;
        case FLOWCELL1x8:
        default:
            targetSection = SBSSection.FLOWCELL8;
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getContainerRole(), sourceSection, null,
                targetFlowcell.getContainerRole(), targetSection, null, labEvent));
        return labEvent;
    }

    @DaoFree
    public LabEvent buildVesselToSectionDbFree(ReceptaclePlateTransferEvent receptaclePlateTransferEvent,
                                               BarcodedTube sourceTube,
                                               @Nullable VesselContainerEmbedder targetVessel,
                                               String targetSection) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent, labEventRefDataFetcher);
        if (sourceTube == null) {
            throw new RuntimeException(
                    "Source tube not found for " + receptaclePlateTransferEvent.getSourceReceptacle().getBarcode());
        }
        if (targetVessel == null) {
            String physType = receptaclePlateTransferEvent.getDestinationPlate().getPhysType();
            String destinationBarcode = receptaclePlateTransferEvent.getDestinationPlate().getBarcode();
            if (StaticPlate.PlateType.getByAutomationName(physType) != null) {
                targetVessel = new StaticPlate(destinationBarcode,
                        StaticPlate.PlateType.getByAutomationName(physType));
            } else {
                IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.getTypeForPhysTypeAndBarcode(
                        physType, destinationBarcode);
                if (flowcellType != null) {
                    targetVessel = new IlluminaFlowcell(flowcellType, destinationBarcode);
                } else if (physType.equals(PHYS_TYPE_FLOWCELL)) {
                    // Guard against the possibility that automation scripts send us bare "Flowcell" types.
                    targetVessel = new IlluminaFlowcell(destinationBarcode);
                } else {
                    throw new RuntimeException("Unexpected physical type: " + physType);
                }
            }
        }
        labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(sourceTube,
                SBSSection.getBySectionName(targetSection), targetVessel.getContainerRole(), null, labEvent));
        return labEvent;
    }

    private Map<String, BarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return barcodedTubeDao.findByBarcodes(barcodes);
    }

    /**
     * Build an entity to represent a transfer from a tube to an entire section of a plate
     *
     * @param receptaclePlateTransferEvent JAXB
     *
     * @return entity
     */
    public LabEvent buildFromBettaLims(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        VesselContainerEmbedder destination = (VesselContainerEmbedder) labVesselDao.findByIdentifier(
                receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
        LabEvent labEvent = buildVesselToSectionDbFree(receptaclePlateTransferEvent,
                barcodedTubeDao.findByBarcode(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode()),
                destination, receptaclePlateTransferEvent.getDestinationPlate().getSection());
        labEvent.setStationEventType(receptaclePlateTransferEvent);
        return labEvent;
    }

    public LabEvent buildFromBettaLims(ReceptacleEventType receptacleEventType) {
        LabEvent labEvent = buildReceptacleEventDbFree(receptacleEventType, labVesselDao.findByIdentifier(
                receptacleEventType.getReceptacle().getBarcode()));
        labEvent.setStationEventType(receptacleEventType);
        return labEvent;
    }

    private LabEvent buildFromBettaLims(StationSetupEvent stationSetupEvent) {
        LabEvent labEvent = constructReferenceData(stationSetupEvent, labEventRefDataFetcher);
        labEvent.setStationEventType(stationSetupEvent);
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for an in-place event on a tube.
     *
     * @param receptacleEventType JAXB
     * @param labVessel           from database
     *
     * @return lab event entity
     */
    @DaoFree
    public LabEvent buildReceptacleEventDbFree(ReceptacleEventType receptacleEventType, LabVessel labVessel) {
        LabEvent labEvent = constructReferenceData(receptacleEventType, labEventRefDataFetcher);
        if (labVessel == null) {
            throw new RuntimeException("Source tube not found for " + receptacleEventType.getReceptacle().getBarcode());
        }
        labVessel.addInPlaceEvent(labEvent);
        return labEvent;
    }

    public LabEvent buildFromBettaLims(ReceptacleTransferEventType receptacleTransferEventType) {
        List<String> barcodes = new ArrayList<>();
        barcodes.add(receptacleTransferEventType.getSourceReceptacle().getBarcode());
        barcodes.add(receptacleTransferEventType.getReceptacle().getBarcode());
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        LabEvent labEvent = buildReceptacleTransferEventDbFree(receptacleTransferEventType, mapBarcodeToVessel);
        labEvent.setStationEventType(receptacleTransferEventType);
        return labEvent;
    }

    @DaoFree
    public LabEvent buildReceptacleTransferEventDbFree(ReceptacleTransferEventType receptacleTransferEventType,
            Map<String, LabVessel> mapBarcodeToVessel) {
        LabEvent labEvent = constructReferenceData(receptacleTransferEventType, labEventRefDataFetcher);
        LabVessel sourceLabVessel = mapBarcodeToVessel.get(receptacleTransferEventType.getSourceReceptacle().getBarcode());
        if (sourceLabVessel == null) {
            throw new RuntimeException("Source tube not found for " +
                    receptacleTransferEventType.getSourceReceptacle().getBarcode());
        }
        LabVessel targetLabVessel = mapBarcodeToVessel.get(receptacleTransferEventType.getReceptacle().getBarcode());
        if (targetLabVessel == null) {
            BarcodedTube.BarcodedTubeType tubeType = BarcodedTube.BarcodedTubeType.getByAutomationName(
                    receptacleTransferEventType.getReceptacle().getReceptacleType());
            if (tubeType == null) {
                tubeType = BarcodedTube.BarcodedTubeType.MatrixTube;
            }
            targetLabVessel = new BarcodedTube(receptacleTransferEventType.getReceptacle().getBarcode(), tubeType);
        }

        setTubeQuantities(receptacleTransferEventType.getSourceReceptacle(),
                OrmUtil.proxySafeCast(sourceLabVessel, BarcodedTube.class), labEvent, true);
        setTubeQuantities(receptacleTransferEventType.getReceptacle(),
                OrmUtil.proxySafeCast(targetLabVessel, BarcodedTube.class), labEvent, false);
        labEvent.getVesselToVesselTransfers().add(new VesselToVesselTransfer(sourceLabVessel, targetLabVessel, labEvent));
        return labEvent;
    }

    private LabEvent constructReferenceData(StationEventType stationEventType,
                                           LabEventRefDataFetcher labEventRefDataFetcher) {

        LabEventType labEventType = LabEventType.getByName(stationEventType.getEventType());
        if (labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }

        Long operator;
        if (stationEventType instanceof StationSetupEvent && stationEventType.getOperator() == null) {
            operator = labEventRefDataFetcher.getOperator(ACTIVITY_USER_ID).getUserId();
        } else {
            BspUser bspUser = labEventRefDataFetcher.getOperator(stationEventType.getOperator());
            if (bspUser == null) {
                throw new RuntimeException("Failed to find operator " + stationEventType.getOperator());
            }
            operator = bspUser.getUserId();
        }

        Long disambiguator = stationEventType.getDisambiguator();
        if (disambiguator == null) {
            disambiguator = 1L;
        }
        LabEvent genericLabEvent = new LabEvent(labEventType, stationEventType.getStart(),
                stationEventType.getStation(), disambiguator, operator, stationEventType.getProgram());
        genericLabEvent.setWorkflowQualifier(stationEventType.getWorkflowQualifier());
        if (stationEventType.getBatchId() != null) {
            LabBatch labBatch = labEventRefDataFetcher.getLabBatch(stationEventType.getBatchId());
            if (labBatch == null) {
                throw new RuntimeException("Failed to find lab batch " + stationEventType.getBatchId());
            }
            genericLabEvent.setLabBatch(labBatch);
            labBatch.getLabEvents().add(genericLabEvent);
        }
        addReagents(genericLabEvent, stationEventType.getReagent());
        addMetadatas(genericLabEvent, stationEventType.getMetadata());
        return genericLabEvent;
    }

    public void setLabEventRefDataFetcher(LabEventRefDataFetcher labEventRefDataFetcher) {
        this.labEventRefDataFetcher = labEventRefDataFetcher;
    }

    /**
     * Based on a collection of {@link LabVessel}s to be processed, this method will generate the appropriate event
     * to associate with each Vessel
     *
     * @param entryCollection
     * @param operator        representation of the user that submitted the request
     * @param batchIn         LabBatch to which the created events will be associate
     * @param eventLocation
     * @param programName
     * @param eventType
     *
     * @return A collection of the created events for the submitted lab vessels
     */
    public Collection<LabEvent> buildFromBatchRequests(@Nonnull Collection<BucketEntry> entryCollection,
                                                       String operator, LabBatch batchIn, @Nonnull String eventLocation,
                                                       @Nonnull String programName, @Nonnull LabEventType eventType,
                                                       Date date, long disambiguatorOffset) {

        long workCounter = 1 + disambiguatorOffset;

        List<LabEvent> fullEventList = new LinkedList<>();

        Set<UniqueEvent> uniqueEvents = new HashSet<>();

        for (BucketEntry mapEntry : entryCollection) {
            List<LabEvent> events = new LinkedList<>();
            LabEvent currEvent = createFromBatchItems(mapEntry.getProductOrder().getBusinessKey(), mapEntry.getLabVessel(),
                    workCounter++, operator, eventType, eventLocation, programName, date);
            if (null != batchIn) {
                currEvent.setLabBatch(batchIn);
            }

            persistLabEvent(uniqueEvents, currEvent, false);
            events.add(currEvent);
            fullEventList.addAll(events);
        }
        return fullEventList;
    }

    /**
     * Actually does the work to create an event for a given {@link LabVessel}.  Will associate the related Product '
     * Order ID to the event for reference
     *
     */
    private LabEvent createFromBatchItems(@Nonnull String pdoKey, @Nonnull LabVessel batchItem,
                                         @Nonnull Long disambiguator, String operator, @Nonnull LabEventType eventType,
                                         @Nonnull String eventLocation, @Nonnull String programName,
                                          @Nonnull Date date) {

        Long operatorInfo = labEventRefDataFetcher.getOperator(operator).getUserId();

        LabEvent bucketMoveEvent =
                new LabEvent(eventType, date, eventLocation, disambiguator, operatorInfo, programName);

        //TODO add to container.
        batchItem.addInPlaceEvent(bucketMoveEvent);

        //TODO If LabVessel has a batch waiting to be associated with an event, add it here

        return bucketMoveEvent;
    }

    public void setEventHandlerSelector(EventHandlerSelector eventHandlerSelector) {
        this.eventHandlerSelector = eventHandlerSelector;
    }

    public EventHandlerSelector getEventHandlerSelector() {
        return eventHandlerSelector;
    }

    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public void setTubeFormationDao(TubeFormationDao tubeFormationDao) {
        this.tubeFormationDao = tubeFormationDao;
    }

    public void setRackOfTubesDao(RackOfTubesDao rackOfTubesDao) {
        this.rackOfTubesDao = rackOfTubesDao;
    }

    public void setBarcodedTubeDao(BarcodedTubeDao barcodedTubeDao) {
        this.barcodedTubeDao = barcodedTubeDao;
    }

    public void setBspRestSender(BSPRestSender bspRestSender) {
        this.bspRestSender = bspRestSender;
    }
}
