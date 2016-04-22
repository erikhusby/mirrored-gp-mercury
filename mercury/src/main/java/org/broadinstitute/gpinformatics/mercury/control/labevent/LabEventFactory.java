package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StripTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.GapHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.SamplesDaughterPlateHandler;
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
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Serializable;
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


/**
 * Creates Lab Event entities from BettaLIMS JAXB beans.  Implements Serializable because it's used by a Stateful
 * session bean.
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "serial", "CloneableClassWithoutClone",
        "ClassExtendsConcreteCollection", "OverlyComplexClass", "ClassWithTooManyMethods", "ClassWithTooManyFields"})
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
    private SamplesDaughterPlateHandler samplesDaughterPlateHandler;

    @Inject
    private GapHandler gapHandler;

    private BSPSetVolumeConcentration bspSetVolumeConcentration;

    private static final Log logger = LogFactory.getLog(LabEventFactory.class);

    @Inject
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

    /**
     * Determines whether volume / concentration etc. should be updated in BSP.
     */
    public boolean isUpdateVolConcInBsp(LabEvent labEvent) {
        LabEventType labEventType = labEvent.getLabEventType();
        LabVessel labVessel = labEvent.getInPlaceLabVessel();
        if (labVessel == null) {
            labVessel = labEvent.getSourceLabVessels().iterator().next();
        }
        if (labEventType.getVolumeConcUpdate() == LabEventType.VolumeConcUpdate.BSP_AND_MERCURY) {
            MercurySample mercurySample = null;
            if (labVessel.getContainerRole() != null) {
                for (LabVessel testVessel : labVessel.getContainerRole().getContainedVessels()) {
                    mercurySample = extractSample(testVessel.getSampleInstancesV2());
                    if (mercurySample != null) {
                        break;
                    }
                }
            } else {
                mercurySample = extractSample(labVessel.getSampleInstancesV2());
            }
            // If no mercury samples found, assumes samples are derived from old BSP samples that haven't
            // been exported to Mercury.  This is OK provided they are processed by Squid
            // CRSP/Buick samples are expected to be already accessioned and therefore have mercury samples.
            return (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP);
        }
        return false;
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
        Set<ReceptacleType> updateReceptacles = new HashSet<>();

        // Have to persist and flush inside each loop, because the first event may create
        // vessels that are referenced by the second event, e.g. PreSelectionPool
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateCherryPickEvent);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateCherryPickEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
            if (isUpdateVolConcInBsp(labEvent)) {
                for (PositionMapType positionMapType : plateCherryPickEvent.getPositionMap()) {
                    updateReceptacles.addAll(positionMapType.getReceptacle());
                }
            }
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
            if (isUpdateVolConcInBsp(labEvent)) {
                if (plateEventType.getPositionMap() != null) {
                    updateReceptacles.addAll(plateEventType.getPositionMap().getReceptacle());
                }
            }
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateTransferEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, plateTransferEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
            if (isUpdateVolConcInBsp(labEvent)) {
                if (plateTransferEventType.getPositionMap() != null) {
                    updateReceptacles.addAll(plateTransferEventType.getPositionMap().getReceptacle());
                }
            }
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent :
                bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptaclePlateTransferEvent);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, receptaclePlateTransferEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
            if (isUpdateVolConcInBsp(labEvent)) {
                for (PositionMapType positionMapType : receptaclePlateTransferEvent.getDestinationPositionMap()) {
                    updateReceptacles.addAll(positionMapType.getReceptacle());
                }
            }
        }
        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptacleEventType);
            eventHandlerSelector.applyEventSpecificHandling(labEvent, receptacleEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
            if (isUpdateVolConcInBsp(labEvent)) {
                updateReceptacles.add(receptacleEventType.getReceptacle());
            }
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
            if (isUpdateVolConcInBsp(labEvent)) {
                updateReceptacles.add(receptacleTransferEventType.getReceptacle());
            }
        }

        if (!labEvents.isEmpty()) {
            LabEventType.ForwardMessage forwardMessage = labEvents.get(0).getLabEventType().getForwardMessage();
            switch (forwardMessage) {
                case BSP:
                    samplesDaughterPlateHandler.postToBsp(bettaLIMSMessage,
                            SamplesDaughterPlateHandler.BSP_TRANSFER_REST_URL);
                    break;
                case GAP:
                    gapHandler.postToGap(bettaLIMSMessage);
                    break;
                case NONE:
                    break;
                default:
                    throw new RuntimeException("Unexpected forwardMessage " + forwardMessage.name());
            }
        }

        updateVolumeConcentration(updateReceptacles.toArray(new ReceptacleType[updateReceptacles.size()]));
        return labEvents;
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

            List<PositionMapType> extractPositionMaps = extractPositionMapBarcodes(
                    plateCherryPickEvent.getPlate(), plateCherryPickEvent.getPositionMap());
            extractBarcodes(barcodes, extractPositionMaps);

            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            labEvent = buildFromBettaLims(plateCherryPickEvent, mapBarcodeToVessel);
        }

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
            List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                barcodes.add(receptacleType.getBarcode());
                VesselPosition vesselPosition = VesselPosition.getByName(receptacleType.getPosition());
                if (vesselPosition == null) {
                    throw new RuntimeException("Failed to find position " + receptacleType.getPosition());
                }
                positionBarcodeList.add(new ImmutablePair<>(vesselPosition, receptacleType.getBarcode()));
            }
            barcodes.add(TubeFormation.makeDigest(positionBarcodeList));
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
                        plateCherryPickEvent.getSourcePositionMap(), createSourcesForEvent, true);

        mapBarcodeToTubeFormation.putAll(buildPlates(mapBarcodeToVessel, plateCherryPickEvent.getPlate(),
                plateCherryPickEvent.getPositionMap(), createSourcesForEvent, false));

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
        }
        return labEvent;
    }

    /**
     * Wrapper around {@link #buildPlates(java.util.Map, java.util.List, java.util.List, boolean, boolean)}, for
     * single plates and position maps
     */
    @DaoFree
    private Map<String, TubeFormation> buildPlate(Map<String, LabVessel> mapBarcodeToVessel,
                                                  PlateType plateJaxb, PositionMapType positionMap,
                                                  boolean createSourcesForEvent, boolean source) {
        List<PlateType> plateTypeList = new ArrayList<>();
        if (plateJaxb != null) {
            plateTypeList.add(plateJaxb);
        }
        List<PositionMapType> positionMapTypeList = new ArrayList<>();
        if (positionMap != null) {
            positionMapTypeList.add(positionMap);
        }
        return buildPlates(mapBarcodeToVessel, plateTypeList,
                positionMapTypeList, createSourcesForEvent, source);
    }

    /**
     * Builds plate / rack entities from JAXB fields.
     *
     * @param mapBarcodeToVessel Existing entities fetched from the database.  New entities are added to this map.
     * @param platesJaxb         list of plates / racks
     * @param positionMaps       list of tubes / positions
     * @param create             true if this method should create entities not found in mapBarcodeToVessel
     * @param source             true if the plate is a source in an event
     *
     * @return map from rack barcode to tube formation (in mapBarcodeToVessel, tube formations are mapped by their digest)
     */
    @DaoFree
    private Map<String, TubeFormation> buildPlates(Map<String, LabVessel> mapBarcodeToVessel,
                                                   List<PlateType> platesJaxb, List<PositionMapType> positionMaps,
                                                   boolean create, boolean source) {
        Map<String, TubeFormation> mapBarcodeToTubeFormation = new HashMap<>();
        for (PlateType plateType : platesJaxb) {
            if (plateType.getPhysType().equals(PHYS_TYPE_TUBE_RACK) ||
                RackOfTubes.RackType.getByName(plateType.getPhysType()) != null) {
                List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
                boolean found = false;
                for (PositionMapType positionMapType : positionMaps) {
                    if (positionMapType.getBarcode().equals(plateType.getBarcode())) {
                        found = true;
                        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                            positionBarcodeList.add(new ImmutablePair<>(
                                    VesselPosition.getByName(receptacleType.getPosition()),
                                    receptacleType.getBarcode()));
                        }
                        String digest = TubeFormation.makeDigest(positionBarcodeList);
                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(mapBarcodeToVessel.get(digest),
                                TubeFormation.class);
                        RackOfTubes rackOfTubes = OrmUtil.proxySafeCast(
                                mapBarcodeToVessel.get(plateType.getBarcode()), RackOfTubes.class);
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
                                    positionMapType, source, create);
                            mapBarcodeToVessel.put(tubeFormation.getLabel(), tubeFormation);
                            if (rackOfTubesWasNull) {
                                rackOfTubes = tubeFormation.getRacksOfTubes().iterator().next();
                                mapBarcodeToVessel.put(rackOfTubes.getLabel(), rackOfTubes);
                            }
                        } else {
                            if (rackOfTubes == null) {
                                RackOfTubes.RackType rackType = getRackType(plateType);
                                rackOfTubes = new RackOfTubes(plateType.getBarcode(), rackType);
                                mapBarcodeToVessel.put(rackOfTubes.getLabel(), rackOfTubes);
                                tubeFormation.addRackOfTubes(rackOfTubes);
                            }
                            setTubeQuantities(mapBarcodeToTube, positionMapType);
                        }
                        mapBarcodeToTubeFormation.put(plateType.getBarcode(), tubeFormation);
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Failed to find positionMap for " + plateType.getBarcode());
                }
            } else {
                LabVessel labVessel = mapBarcodeToVessel.get(plateType.getBarcode());
                if (labVessel == null) {
                    IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.getTypeForPhysTypeAndBarcode(
                            plateType.getPhysType(), plateType.getBarcode());
                    if (flowcellType != null) {
                        labVessel = new IlluminaFlowcell(flowcellType, plateType.getBarcode());
                    }

                    if (labVessel == null) {
                        if (source && !create) {
                            throw new RuntimeException("Failed to find plate " + plateType.getBarcode());
                        }
                        labVessel = new StaticPlate(plateType.getBarcode(),
                                StaticPlate.PlateType.getByAutomationName(plateType.getPhysType()));
                    }
                    mapBarcodeToVessel.put(plateType.getBarcode(), labVessel);
                }
            }
        }
        return mapBarcodeToTubeFormation;
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
        List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            positionBarcodeList.add(new ImmutablePair<>(VesselPosition.getByName(
                    receptacleType.getPosition()), receptacleType.getBarcode()));
        }
        return tubeFormationDao.findByDigest(TubeFormation.makeDigest(positionBarcodeList));
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
                mapBarcodeToSourceRackOfTubes);

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
    public LabEvent buildCherryPickRackToReagentKitDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                          Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                                          Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes,
                                                          Map<String, BarcodedTube> mapBarcodeToSourceTube
    ) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube,
                mapBarcodeToSourceRackOfTubes);

        for (Map.Entry<String, RackOfTubes> rackOfTubesSet : mapBarcodeToSourceRackOfTubes.entrySet()) {
            RackOfTubes rack = rackOfTubesSet.getValue();
            for (Map.Entry<String, TubeFormation> stringVesselContainerEntry : mapBarcodeToSourceTubeFormation
                    .entrySet()) {
                if (stringVesselContainerEntry.getValue() == null) {
                    TubeFormation targetRack =
                            buildRackDaoFree(mapBarcodeToSourceTube, rack, plateCherryPickEvent.getPlate().get(0),
                                    plateCherryPickEvent.getPositionMap().get(0), false,
                                    LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources());
                    stringVesselContainerEntry.setValue(targetRack);
                }
            }
        }

        // todo jmt why is this done twice?
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube,
                mapBarcodeToSourceRackOfTubes);
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
                                              Map<String, RackOfTubes> mapBarcodeToRackOfTubes) {
        for (PlateType sourceRackJaxb : plateCherryPickEvent.getSourcePlate()) {
            TubeFormation sourceTubeFormationEntity = mapBarcodeToSourceTubeFormation.get(sourceRackJaxb.getBarcode());
            if (sourceTubeFormationEntity == null) {
                for (PositionMapType sourcePositionMap : plateCherryPickEvent.getSourcePositionMap()) {
                    if (sourcePositionMap.getBarcode().equals(sourceRackJaxb.getBarcode())) {
                        sourceTubeFormationEntity = buildRackDaoFree(mapBarcodeToSourceTube,
                                mapBarcodeToRackOfTubes.get(sourceRackJaxb.getBarcode()), sourceRackJaxb,
                                sourcePositionMap,
                                true, LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources());
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
        if (plateEventType.getPositionMap() == null) {
            PlateType plate = plateEventType.getPlate();
            if (plate == null) {
                // todo jmt why isn't this error caught in JAXB?
                throw new RuntimeException("No plate element in plateEvent");
            }
            StaticPlate staticPlate = staticPlateDao.findByBarcode(plate.getBarcode());
            labEvent = buildFromBettaLimsPlateEventDbFree(plateEventType, staticPlate);
        } else {
            TubeFormation tubeFormation = fetchTubeFormation(plateEventType.getPositionMap());
            labEvent = buildFromBettaLimsRackEventDbFree(plateEventType, tubeFormation, findTubesByBarcodes(
                    plateEventType.getPositionMap()),
                    rackOfTubesDao.findByBarcode(plateEventType.getPlate().getBarcode()));
        }
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
        barcodes.add(plateTransferEvent.getSourcePlate().getBarcode());
        if (plateTransferEvent.getSourcePositionMap() != null &&
            expectBarcodedReceptacleTypes(plateTransferEvent.getSourcePlate())) {
            extractBarcodes(barcodes, Collections.singletonList(plateTransferEvent.getSourcePositionMap()));
        }
        barcodes.add(plateTransferEvent.getPlate().getBarcode());
        if (plateTransferEvent.getPositionMap() != null &&
            expectBarcodedReceptacleTypes(plateTransferEvent.getPlate())) {
            extractBarcodes(barcodes, Collections.singletonList(plateTransferEvent.getPositionMap()));
        }
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        LabEvent labEvent = buildFromBettaLims(plateTransferEvent, mapBarcodeToVessel);
        return labEvent;
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
                true);

        mapBarcodeToTubeFormation.putAll(buildPlate(mapBarcodeToVessel, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap(), createSourcesForEvent, false));

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
     * @param plate             JAXB rack
     * @param positionMap       JAXB list of tube barcodes
     *
     * @return entity
     */
    @DaoFree
    private TubeFormation buildRackDaoFree(Map<String, BarcodedTube> mapBarcodeToTubes, RackOfTubes rackOfTubes,
                                           PlateType plate, PositionMapType positionMap, boolean source,
                                           boolean createSources) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new EnumMap<>(VesselPosition.class);
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if (barcodedTube == null) {
                if (source && !(CREATE_SOURCES || createSources)) {
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
        setTubeQuantities(mapBarcodeToTubes, positionMap);
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
     * Set volume, concentration, receptacleWeight etc.
     * @param mapBarcodeToTubes map from tube barcode to tube
     * @param positionMap JAXB quantities from deck
     */
    @DaoFree
    public void setTubeQuantities(Map<String, BarcodedTube> mapBarcodeToTubes, PositionMapType positionMap) {
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if (barcodedTube != null) {
                setTubeQuantities(receptacleType, barcodedTube);
            }
        }
    }

    private void setTubeQuantities(ReceptacleType receptacleType, BarcodedTube barcodedTube) {
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

    @DaoFree
    public LabEvent buildFromBettaLimsPlateEventDbFree(PlateEventType plateEvent, StaticPlate plate) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if (plate == null) {
            if (CREATE_SOURCES) {
                plate = new StaticPlate(plateEvent.getPlate().getBarcode(), StaticPlate.PlateType.getByAutomationName(
                        plateEvent.getPlate().getPhysType()));
            } else {
                throw new RuntimeException("Failed to find plate " + plateEvent.getPlate().getBarcode());
            }
        }

        plate.addInPlaceEvent(labEvent);
        return labEvent;
    }

    // todo jmt make this database free?
    private void addReagents(LabEvent labEvent, List<ReagentType> reagentTypes) {
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
        for(MetadataType metadataType: metadataTypes){
            LabEventMetadata.LabEventMetadataType labEventMetadataType =
                    LabEventMetadata.LabEventMetadataType.getByName(metadataType.getName());
            if(labEventMetadataType != null) { //Throw runtime exception for unknown metadata?
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
                            true, LabEventType.getByName(plateEvent.getEventType()).isCreateSources());
        } else {
            setTubeQuantities(mapBarcodeToTubes, plateEvent.getPositionMap());
        }
        tubeFormation.addInPlaceEvent(labEvent);
        return labEvent;
    }

    /**
     * Pull volume and concentration values from the receptacleType and update bsp samples.
     *
     * @param receptacleTypes One or more receptacleTypes from a BettaLimsMessage.
     */
    private void updateVolumeConcentration(ReceptacleType... receptacleTypes) {
        for (ReceptacleType receptacleType : receptacleTypes) {
            // At least one of the values must be set in order to incur the cost of calling BSP.
            if (receptacleType.getVolume() != null || receptacleType.getConcentration() != null ||
                    receptacleType.getReceptacleWeight() != null) {
                String result = bspSetVolumeConcentration.setVolumeAndConcentration(receptacleType.getBarcode(),
                        receptacleType.getVolume(), receptacleType.getConcentration(),
                        receptacleType.getReceptacleWeight());
                if (!result.equals(BSPSetVolumeConcentration.RESULT_OK)) {
                    logger.error(result);
                }
            }
        }
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
        switch (flowcellType.getVesselGeometry()) {
        case FLOWCELL1x1:
            targetSection = SBSSection.LANE1;
            break;
        case FLOWCELL1x2:
            targetSection = SBSSection.ALL2;
            break;
        case FLOWCELL1x8:
        default:
            targetSection = SBSSection.FLOWCELL8;
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getContainerRole(), SBSSection.STRIP_TUBE8, null,
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
        return labEvent;
    }

    public LabEvent buildFromBettaLims(ReceptacleEventType receptacleEventType) {
        return buildReceptacleEventDbFree(receptacleEventType, labVesselDao.findByIdentifier(
                receptacleEventType.getReceptacle().getBarcode()));
    }

    private LabEvent buildFromBettaLims(StationSetupEvent stationSetupEvent) {
        return constructReferenceData(stationSetupEvent, labEventRefDataFetcher);
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
        return buildReceptacleTransferEventDbFree(receptacleTransferEventType, mapBarcodeToVessel);
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
                OrmUtil.proxySafeCast(sourceLabVessel, BarcodedTube.class));
        setTubeQuantities(receptacleTransferEventType.getReceptacle(),
                OrmUtil.proxySafeCast(targetLabVessel, BarcodedTube.class));
        labEvent.getVesselToVesselTransfers().add(new VesselToVesselTransfer(sourceLabVessel, targetLabVessel, labEvent));
        return labEvent;
    }

    public LabEvent constructReferenceData(StationEventType stationEventType,
                                           LabEventRefDataFetcher labEventRefDataFetcher) {

        LabEventType labEventType = LabEventType.getByName(stationEventType.getEventType());
        if (labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }

        Long operator;
        if (stationEventType instanceof StationSetupEvent) {
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
                                                       @Nonnull String programName, @Nonnull LabEventType eventType) {

        long workCounter = 1L;

        List<LabEvent> fullEventList = new LinkedList<>();

        Set<UniqueEvent> uniqueEvents = new HashSet<>();

        for (BucketEntry mapEntry : entryCollection) {
            List<LabEvent> events = new LinkedList<>();
            LabEvent currEvent = createFromBatchItems(mapEntry.getProductOrder().getBusinessKey(), mapEntry.getLabVessel(),
                    workCounter++, operator, eventType, eventLocation, programName);
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
    public LabEvent createFromBatchItems(@Nonnull String pdoKey, @Nonnull LabVessel batchItem,
                                         @Nonnull Long disambiguator, String operator, @Nonnull LabEventType eventType,
                                         @Nonnull String eventLocation, @Nonnull String programName) {

        Long operatorInfo = labEventRefDataFetcher.getOperator(operator).getUserId();

        LabEvent bucketMoveEvent =
                new LabEvent(eventType, new Date(), eventLocation, disambiguator, operatorInfo, programName);

        //TODO SGM: add to container.
        batchItem.addInPlaceEvent(bucketMoveEvent);

        //TODO SGM: If LabVessel has a batch waiting to be associated with an event, add it here

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
}
