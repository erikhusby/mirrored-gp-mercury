package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BasePlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StripTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDao;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    //TODO SGM  remove inject here
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
    private LabBatchDAO labBatchDAO;

    @Inject
    private GenericReagentDao genericReagentDao;

    //TODO SGM Remove default constructor
    public LabEventFactory() {
    }

    //TODO SGM Make inject constructor.  Replace test cases with this and a BSPUserList initialized with test people.
    public LabEventFactory(BSPUserList userList) {
        this.bspUserList = userList;
    }

    public interface LabEventRefDataFetcher {
        BspUser getOperator(String userId);

        BspUser getOperator(Long bspUserId);

        LabBatch getLabBatch(String labBatchName);
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
            return labBatchDAO.findByName(labBatchName);
        }
    };

    public static class CherryPick {
        private final String sourceRackBarcode;
        private final String sourceWell;
        private final String destinationRackBarcode;
        private final String destinationWell;

        public CherryPick(String sourceRackBarcode, String sourceWell, String destinationRackBarcode, String destinationWell) {
            this.sourceRackBarcode = sourceRackBarcode;
            this.sourceWell = sourceWell;
            this.destinationRackBarcode = destinationRackBarcode;
            this.destinationWell = destinationWell;
        }

        public String getSourceRackBarcode() {
            return sourceRackBarcode;
        }

        public String getSourceWell() {
            return sourceWell;
        }

        public String getDestinationRackBarcode() {
            return destinationRackBarcode;
        }

        public String getDestinationWell() {
            return destinationWell;
        }
    }


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
     * Builds one or more lab event entities from a JAXB message bean that contains one or more event beans
     *
     * @param bettaLIMSMessage JAXB bean
     * @return list of entities
     */
    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<LabEvent>();
        Set<UniqueEvent> uniqueEvents = new HashSet<UniqueEvent>();

        // Have to persist and flush inside each loop, because the first event may create
        // vessels that are referenced by the second event, e.g. PreSelectionPool
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateCherryPickEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateTransferEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent :
                bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptaclePlateTransferEvent);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }
        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptacleEventType);
            persistLabEvent(uniqueEvents, labEvent, true);
            labEvents.add(labEvent);
        }

        return labEvents;
    }

    /**
     * Modify disambiguators of other events in the same message, if necessary, and persist an event
     *
     * @param uniqueEvents    events in a message
     * @param labEvent        event to be persisted
     * @param persistEntities
     */
    private void persistLabEvent(Set<UniqueEvent> uniqueEvents, LabEvent labEvent, boolean persistEntities) {
        // The deck-side scripts don't always set the disambiguator correctly, so modify it, to make it unique
        // within this message, if necessary
        while (!uniqueEvents.add(new UniqueEvent(labEvent.getEventLocation(), labEvent.getEventDate(),
                labEvent.getDisambiguator()))) {
            labEvent.setDisambiguator(labEvent.getDisambiguator() + 1);
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
     * @return entity
     */
    public LabEvent buildFromBettaLims(final PlateCherryPickEvent plateCherryPickEvent) {
        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = buildMapBarcodeToTubeFormation(
                plateCherryPickEvent.getSourcePositionMap());

        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<String, TwoDBarcodedTube>();
        for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
            mapBarcodeToSourceTube.putAll(findTubesByBarcodes(positionMapType));
        }
        Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes = new HashMap<String, RackOfTubes>();
        for (PlateType sourcePlateType : plateCherryPickEvent.getSourcePlate()) {
            mapBarcodeToSourceRackOfTubes.put(sourcePlateType.getBarcode(), rackOfTubesDao.findByBarcode(sourcePlateType.getBarcode()));
        }


        LabEvent labEvent;
        if (plateCherryPickEvent.getPlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            Map<String, StripTube> mapBarcodeToTargetStripTube = new HashMap<String, StripTube>();
            labEvent = buildCherryPickRackToStripTubeDbFree(plateCherryPickEvent, mapBarcodeToSourceTubeFormation,
                    mapBarcodeToSourceTube, null, mapBarcodeToTargetStripTube, mapBarcodeToSourceRackOfTubes);
        } else {
            Map<String, TubeFormation> mapBarcodeToTargetTubeFormation = buildMapBarcodeToTubeFormation(
                    new ArrayList<PositionMapType>() {{
                        add(plateCherryPickEvent.getPositionMap());
                    }});

            labEvent = buildCherryPickRackToRackDbFree(plateCherryPickEvent, mapBarcodeToSourceTubeFormation,
                    mapBarcodeToSourceRackOfTubes, mapBarcodeToSourceTube, mapBarcodeToTargetTubeFormation,
                    findTubesByBarcodes(plateCherryPickEvent.getPositionMap()),
                    rackOfTubesDao.findByBarcode(plateCherryPickEvent.getPlate().getBarcode()));
        }
        addReagents(labEvent, plateCherryPickEvent);
        return labEvent;
    }

    /**
     * From a list of positionMaps, create a map from barcode to rack
     *
     * @param positionMap list of positionMaps
     * @return map from barcode to rack
     */
    private Map<String, TubeFormation> buildMapBarcodeToTubeFormation(List<PositionMapType> positionMap) {
        Map<String, TubeFormation> mapBarcodeToSourceTubeFormation = new HashMap<String, TubeFormation>();
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
     * @return database rack
     */
    private TubeFormation fetchTubeFormation(PositionMapType positionMapType) {
        List<Map.Entry<VesselPosition, String>> positionBarcodeList =
                new ArrayList<Map.Entry<VesselPosition, String>>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            positionBarcodeList.add(new AbstractMap.SimpleEntry<VesselPosition, String>(VesselPosition.getByName(
                    receptacleType.getPosition()), receptacleType.getBarcode()));
        }
        return tubeFormationDao.findByDigest(TubeFormation.makeDigest(positionBarcodeList));
    }

    /**
     * Build an entity to represent a cherry pick (random access) transfer from one or more source racks to a target rack
     *
     * @param plateCherryPickEvent   JAXB
     * @param mapBarcodeToSourceTubeFormation
     *                               entities
     * @param mapBarcodeToSourceTube entities
     * @param mapBarcodeToTargetTubeFormation
     *                               entities
     * @param mapBarcodeToTargetTube entities
     * @param targetRackOfTubes      entity
     * @return entity
     */
    @DaoFree
    public LabEvent buildCherryPickRackToRackDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                    Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                                    Map<String, RackOfTubes> mapBarcodeToSourceRackOfTubes,
                                                    Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
                                                    Map<String, TubeFormation> mapBarcodeToTargetTubeFormation,
                                                    Map<String, TwoDBarcodedTube> mapBarcodeToTargetTube,
                                                    @Nullable RackOfTubes targetRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceTubeFormationsToMap(plateCherryPickEvent, mapBarcodeToSourceTubeFormation, mapBarcodeToSourceTube, mapBarcodeToSourceRackOfTubes);

        for (Map.Entry<String, TubeFormation> stringVesselContainerEntry : mapBarcodeToTargetTubeFormation.entrySet()) {
            if (stringVesselContainerEntry.getValue() == null) {
                TubeFormation targetRack = buildRackDaoFree(mapBarcodeToTargetTube, targetRackOfTubes, plateCherryPickEvent.getPlate(),
                        plateCherryPickEvent.getPositionMap(), false, LabEventType.getByName(plateCherryPickEvent.getEventType()).isCreateSources());
                stringVesselContainerEntry.setValue(targetRack);
            }
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String destinationRackBarcode = cherryPickSourceType.getDestinationBarcode();
            if (destinationRackBarcode == null) {
                destinationRackBarcode = plateCherryPickEvent.getPlate().getBarcode();
            }
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceTubeFormation.get(cherryPickSourceType.getBarcode()).getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapBarcodeToTargetTubeFormation.get(destinationRackBarcode).getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getDestinationWell()),
                    labEvent));
        }
        return labEvent;
    }

    /**
     * Build an entity to represent a cherry pick (random access) transfer between a source rack and a target strip
     * tube. Builds source and target LabVessels on-the-fly.
     *
     * @param plateCherryPickEvent          JAXB cherry-pick event, either deserialized from XML or created from BettalimsMessageFactory
     * @param mapBarcodeToSourceTubeFormation
     *                                      map from source rack barcode to TubeFormation entities; newly created TubeFormations will be added to this map
     * @param mapBarcodeToSourceTube        map from source tube barcode to TwoDBarcodedTube entities; newly created TwoDBarcodedTubes will be added to this this map
     * @param mapBarcodeToTargetTubeFormation
     *                                      unused
     * @param mapBarcodeToTargetStripTube   map from target strip tube barcode to StripTube entities; newly created StripTubes will NOT be added to this map
     * @param mapBarcodeToSourceRackOfTubes map from target rack barcode to RackOfTubes entities; newly created RackOfTubes will NOT be added to this map
     * @return a new LabEvent entity
     */
    @DaoFree
    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                         Map<String, TubeFormation> mapBarcodeToSourceTubeFormation,
                                                         Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
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

        Map<String, StripTube> mapPositionToStripTube = new HashMap<String, StripTube>();
        for (ReceptacleType receptacleType : plateCherryPickEvent.getPositionMap().getReceptacle()) {
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
            String position = LEADING_ZERO_PATTERN.matcher(cherryPickSourceType.getDestinationWell().substring(1)).replaceFirst("");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceTubeFormation.get(cherryPickSourceType.getBarcode()).getContainerRole(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapPositionToStripTube.get(position).getContainerRole(),
                    VesselPosition.getByName("TUBE" + Integer.toString(
                            cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1)),
                    labEvent));
        }
        return labEvent;
    }


    /**
     * Create a PlateCherryPickEvent for transferring from a rack of tubes to a plate.
     *
     * @param plateCherryPickEvent starting event for the transfer.
     * @param sourceRackBarcode    barcode of the Rack holding the tubes.
     * @param sourceTubeBarcodes   mapping of tube barcodes to their position on the rack.
     * @param targetPlateBarcode   barcode of the final plate.
     * @param plateType            type of plate you are transferring to.
     * @param cherryPicks          list of cherryPicks.
     *
     * @return the event object
     */
    @DaoFree
    public PlateCherryPickEvent buildCherryPickRackToPlateDbFree(PlateCherryPickEvent plateCherryPickEvent,
                                                                 String sourceRackBarcode,
                                                                 Map<String, VesselPosition> sourceTubeBarcodes,
                                                                 String targetPlateBarcode, String plateType,
                                                                 List<LabEventFactory.CherryPick> cherryPicks) {
        // create a position map based on sourceTubeBarcodes
        for (Map.Entry<String, VesselPosition> sourceBarcodeEntry : sourceTubeBarcodes.entrySet()) {
            PositionMapType positionMap = new PositionMapType();
            ReceptacleType receptacleType = new ReceptacleType();
            receptacleType.setBarcode(sourceBarcodeEntry.getKey());
            receptacleType.setReceptacleType("tube");
            receptacleType.setPosition(sourceBarcodeEntry.getValue().name());
            positionMap.getReceptacle().add(receptacleType);
            positionMap.setBarcode(sourceRackBarcode);
        }
        // The source TubeRack
        PlateType sourcePlate = new PlateType();
        sourcePlate.setBarcode(sourceRackBarcode);
        sourcePlate.setPhysType("TubeRack");
        sourcePlate.setSection(LabEventFactory.SECTION_ALL_96);

        // The plate receiving the transfer
        PlateType targetPlate = new PlateType();
        targetPlate.setBarcode(targetPlateBarcode);
        targetPlate.setPhysType(plateType);
        targetPlate.setSection(LabEventFactory.SECTION_ALL_96);
        plateCherryPickEvent.setPlate(targetPlate);

        PositionMapType sourcePositionMap = new PositionMapType();

        for (LabEventFactory.CherryPick cherryPick : cherryPicks) {
            ReceptacleType sourceReceptacleType = new ReceptacleType();
            sourceReceptacleType.setBarcode(cherryPick.getSourceRackBarcode());
            sourceReceptacleType.setPosition(cherryPick.getSourceWell());
            sourceReceptacleType.setReceptacleType("tube");

            sourcePositionMap.getReceptacle().add(sourceReceptacleType);
            sourcePositionMap.setBarcode(sourceRackBarcode);
        }
        plateCherryPickEvent.getSourcePlate().add(sourcePlate);
        plateCherryPickEvent.setPlate(targetPlate);
        plateCherryPickEvent.getSourcePositionMap().add(sourcePositionMap);

        return plateCherryPickEvent;
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
                                              Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube, Map<String, RackOfTubes> mapBarcodeToRackOfTubes) {
        for (PlateType sourceRackJaxb : plateCherryPickEvent.getSourcePlate()) {
            TubeFormation sourceTubeFormationEntity = mapBarcodeToSourceTubeFormation.get(sourceRackJaxb.getBarcode());
            if (sourceTubeFormationEntity == null) {
                for (PositionMapType sourcePositionMap : plateCherryPickEvent.getSourcePositionMap()) {
                    if (sourcePositionMap.getBarcode().equals(sourceRackJaxb.getBarcode())) {
                        sourceTubeFormationEntity = buildRackDaoFree(mapBarcodeToSourceTube,
                                mapBarcodeToRackOfTubes.get(sourceRackJaxb.getBarcode()), sourceRackJaxb, sourcePositionMap,
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
            StaticPlate staticPlate = staticPlateDAO.findByBarcode(plate.getBarcode());
            labEvent = buildFromBettaLimsPlateEventDbFree(plateEventType, staticPlate);
        } else {
            TubeFormation tubeFormation = fetchTubeFormation(plateEventType.getPositionMap());
            labEvent = buildFromBettaLimsRackEventDbFree(plateEventType, tubeFormation, findTubesByBarcodes(
                    plateEventType.getPositionMap()), rackOfTubesDao.findByBarcode(plateEventType.getPlate().getBarcode()));
        }
        addReagents(labEvent, plateEventType);
        return labEvent;
    }

    /**
     * Builds a lab event entity from a JAXB plate transfer event bean, (plate|rack) -> (plate|rack) or
     * strip tube -> flowcell
     *
     * @param plateTransferEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes = null;
        StaticPlate sourcePlate = null;
        TubeFormation sourceTubeFormation = null;
        RackOfTubes sourceRackOfTubes = null;
        if (plateTransferEvent.getSourcePositionMap() == null) {
            if (plateTransferEvent.getSourcePlate().getPhysType().equals(
                    PHYS_TYPE_STRIP_TUBE) && plateTransferEvent.getPlate().getPhysType().equals(
                    PHYS_TYPE_FLOWCELL)) {
                // todo jmt create if null
                StripTube stripTube = stripTubeDao.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
                IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(
                        plateTransferEvent.getPlate().getBarcode());
                LabEvent labEvent = buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, stripTube, illuminaFlowcell);
                addReagents(labEvent, plateTransferEvent);
                return labEvent;
            }
            sourcePlate = staticPlateDAO.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
            if (sourcePlate == null) {
                if (LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources() || CREATE_SOURCES) {
                    sourcePlate = new StaticPlate(plateTransferEvent.getSourcePlate().getBarcode(),
                            StaticPlate.PlateType.getByDisplayName(
                                    plateTransferEvent.getSourcePlate().getPhysType()));
                } else {
                    throw new RuntimeException("Failed to find source plate " + plateTransferEvent.getSourcePlate().getBarcode());
                }
            }
        } else {
            sourceTubeFormation = fetchTubeFormation(plateTransferEvent.getSourcePositionMap());
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
            sourceRackOfTubes = rackOfTubesDao.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
            if (sourceTubeFormation == null) {
                sourceTubeFormation = buildRackDaoFree(mapBarcodeToSourceTubes, sourceRackOfTubes, plateTransferEvent.getSourcePlate(),
                        plateTransferEvent.getSourcePositionMap(), true, LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());
            } else {
                if (sourceRackOfTubes == null) {
                    sourceRackOfTubes = new RackOfTubes(plateTransferEvent.getSourcePlate().getBarcode(), RackOfTubes.RackType.Matrix96);
                }
                sourceTubeFormation.addRackOfTubes(sourceRackOfTubes);

            }
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = null;
        StaticPlate targetPlate = null;
        TubeFormation targetTubeFormation = null;
        RackOfTubes targetRackOfTubes = null;
        if (plateTransferEvent.getPositionMap() == null) {
            targetPlate = staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        } else {
            targetTubeFormation = fetchTubeFormation(plateTransferEvent.getPositionMap());
            mapBarcodeToTargetTubes = findTubesByBarcodes(plateTransferEvent.getPositionMap());
            targetRackOfTubes = rackOfTubesDao.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        }

        LabEvent labEvent;
        if (plateTransferEvent.getSourcePositionMap() == null) {
            // plate to ...
            if (plateTransferEvent.getPositionMap() == null) {
                // plate
                labEvent = buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, sourcePlate, targetPlate);
            } else {
                // rack
                labEvent = buildFromBettaLimsPlateToRackDbFree(plateTransferEvent, sourcePlate, mapBarcodeToTargetTubes, targetRackOfTubes);
            }
        } else {
            // rack to ...
            if (plateTransferEvent.getPositionMap() == null) {
                // plate
                if (sourceTubeFormation == null) {
                    labEvent = buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, sourceRackOfTubes,
                            targetPlate);
                } else {
                    labEvent = buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, sourceTubeFormation, targetPlate);
                }
            } else {
                // rack
                if (targetTubeFormation == null) {
                    labEvent = buildFromBettaLimsRackToRackDbFree(plateTransferEvent, sourceTubeFormation,
                            mapBarcodeToTargetTubes, targetRackOfTubes);
                } else {
                    labEvent = buildFromBettaLimsRackToRackDbFree(plateTransferEvent, sourceTubeFormation,
                            targetTubeFormation);
                }
            }
        }
        addReagents(labEvent, plateTransferEvent);
        return labEvent;
    }

    // todo jmt combine following two methods?

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to plate
     *
     * @param plateTransferEvent      JAXB plate transfer event
     * @param mapBarcodeToSourceTubes existing source tubes (in new rack)
     * @param targetPlate             existing plate, or null for new plate
     * @return entity
     */
    @DaoFree
    public LabEvent buildFromBettaLimsRackToPlateDbFree(PlateTransferEventType plateTransferEvent,
                                                        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
                                                        @Nullable RackOfTubes sourceRackOfTubes,
                                                        @Nullable StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        TubeFormation tubeFormation = buildRackDaoFree(mapBarcodeToSourceTubes, sourceRackOfTubes, plateTransferEvent.getSourcePlate(),
                plateTransferEvent.getSourcePositionMap(), true, LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());
        if (targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(
                            plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                tubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to plate
     *
     * @param plateTransferEvent JAXB plate transfer event
     * @param tubeFormation      existing source rack
     * @param targetPlate        existing plate, or null for new plate
     * @return entity
     */
    @DaoFree
    public LabEvent buildFromBettaLimsRackToPlateDbFree(PlateTransferEventType plateTransferEvent,
                                                        TubeFormation tubeFormation, @Nullable StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if (targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(
                            plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                tubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Build a rack entity
     *
     * @param mapBarcodeToTubes source tubes
     * @param plate             JAXB rack
     * @param positionMap       JAXB list of tube barcodes
     * @return entity
     */
    private TubeFormation buildRackDaoFree(Map<String, TwoDBarcodedTube> mapBarcodeToTubes, RackOfTubes rackOfTubes,
                                           PlateType plate, PositionMapType positionMap, boolean source, boolean createSources) {
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if (twoDBarcodedTube == null) {
                if (source && !(CREATE_SOURCES || createSources)) {
                    throw new RuntimeException("Failed to find tube " + receptacleType.getBarcode());
                }
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode());
                mapBarcodeToTubes.put(receptacleType.getBarcode(), twoDBarcodedTube);
            }
            mapPositionToTube.put(VesselPosition.getByName(receptacleType.getPosition()), twoDBarcodedTube);
        }
        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        if (rackOfTubes == null) {
            rackOfTubes = new RackOfTubes(plate.getBarcode(), RackOfTubes.RackType.Matrix96);
        }
        tubeFormation.addRackOfTubes(rackOfTubes);
        return tubeFormation;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     *
     * @param plateTransferEvent      JAXB
     * @param sourceTubeFormation     from database
     * @param mapBarcodeToTargetTubes each entry may be null, if it isn't in the database
     * @param targetRackOfTubes
     * @return entity
     */
    // todo jmt combine following four methods?
    @DaoFree
    public LabEvent buildFromBettaLimsRackToRackDbFree(PlateTransferEventType plateTransferEvent,
                                                       TubeFormation sourceTubeFormation,
                                                       Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes, RackOfTubes targetRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);

        TubeFormation targetTubeFormation = buildRackDaoFree(mapBarcodeToTargetTubes, targetRackOfTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap(), false, LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     *
     * @param plateTransferEvent      JAXB
     * @param mapBarcodeToSourceTubes each entry may be null, if it isn't in the database
     * @param mapBarcodeToTargetTubes each entry may be null, if it isn't in the database
     * @return entity
     */
    // todo jmt revisit uses of this
    @DaoFree
    public LabEvent buildFromBettaLimsRackToRackDbFree(PlateTransferEventType plateTransferEvent,
                                                       Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
                                                       @Nullable RackOfTubes sourceRackOfTubes,
                                                       Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes,
                                                       @Nullable RackOfTubes targetRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        TubeFormation sourceTubeFormation = buildRackDaoFree(mapBarcodeToSourceTubes, sourceRackOfTubes,
                plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap(), true,
                LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());

        TubeFormation targetTubeFormation = buildRackDaoFree(mapBarcodeToTargetTubes, targetRackOfTubes,
                plateTransferEvent.getPlate(), plateTransferEvent.getPositionMap(), false,
                LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     *
     * @param plateTransferEvent      JAXB
     * @param mapBarcodeToSourceTubes each entry may be null, if it isn't in the database
     * @param targetTubeFormation     from database
     * @return entity
     */
    // todo jmt revisit uses of this
    @DaoFree
    public LabEvent buildFromBettaLimsRackToRackDbFree(PlateTransferEventType plateTransferEvent,
                                                       Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
                                                       @Nullable RackOfTubes rackOfTubes,
                                                       TubeFormation targetTubeFormation) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        TubeFormation sourceTubeFormation = buildRackDaoFree(mapBarcodeToSourceTubes, rackOfTubes,
                plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap(), true,
                LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     *
     * @param plateTransferEvent  JAXB
     * @param sourceTubeFormation from database
     * @param targetTubeFormation from database
     * @return entity
     */
    @DaoFree
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            TubeFormation sourceTubeFormation,
            TubeFormation targetTubeFormation) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetTubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateToRackDbFree(PlateTransferEventType plateTransferEvent,
                                                        StaticPlate sourcePlate,
                                                        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes, @Nullable RackOfTubes targetRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        TubeFormation tubeFormation = buildRackDaoFree(mapBarcodeToTargetTubes, targetRackOfTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap(), false, LabEventType.getByName(plateTransferEvent.getEventType()).isCreateSources());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                tubeFormation.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateEventDbFree(PlateEventType plateEvent, StaticPlate plate) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if (plate == null) {
            if (CREATE_SOURCES) {
                plate = new StaticPlate(plateEvent.getPlate().getBarcode(), StaticPlate.PlateType.getByDisplayName(
                        plateEvent.getPlate().getPhysType()));
            } else {
                throw new RuntimeException("Failed to find plate " + plateEvent.getPlate().getBarcode());
            }
        }

        plate.addInPlaceEvent(labEvent);
        return labEvent;
    }

    // todo jmt make this database free?
    private void addReagents(LabEvent labEvent, BasePlateEventType basePlateEventType) {
        for (ReagentType reagentType : basePlateEventType.getReagent()) {
            GenericReagent genericReagent = genericReagentDao.findByReagentNameAndLot(
                    reagentType.getKitType(), reagentType.getBarcode());
            if (genericReagent == null) {
                genericReagent = new GenericReagent(reagentType.getKitType(), reagentType.getBarcode());
            }
            labEvent.addReagent(genericReagent);
        }
    }

    @DaoFree
    public LabEvent buildFromBettaLimsRackEventDbFree(PlateEventType plateEvent, @Nullable TubeFormation tubeFormation,
                                                      Map<String, TwoDBarcodedTube> mapBarcodeToTubes, @Nullable RackOfTubes rackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if (tubeFormation == null) {
            tubeFormation = buildRackDaoFree(mapBarcodeToTubes, rackOfTubes, plateEvent.getPlate(), plateEvent.getPositionMap(),
                    true, LabEventType.getByName(plateEvent.getEventType()).isCreateSources());
        }

        tubeFormation.addInPlaceEvent(labEvent);
        return labEvent;
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateToPlateDbFree(PlateTransferEventType plateTransferEvent,
                                                         StaticPlate sourcePlate, @Nullable StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if (targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(
                            plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getContainerRole(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    @DaoFree
    public LabEvent buildFromBettaLimsPlateToPlateDbFree(PlateTransferEventType plateTransferEvent,
                                                         StripTube sourceStripTube,
                                                         IlluminaFlowcell targetFlowcell) {
        if (sourceStripTube == null) {
            throw new RuntimeException("Failed to find StripTube " + plateTransferEvent.getSourcePlate().getBarcode());
        }
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if (targetFlowcell == null) {
            // todo jmt what about MiSeq?
            // todo jmt how to populate run configuration?
            targetFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell,
                    plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getContainerRole(), SBSSection.STRIP_TUBE8,
                targetFlowcell.getContainerRole(), SBSSection.FLOWCELL8, labEvent));
        return labEvent;
    }

    @DaoFree
    public LabEvent buildVesselToSectionDbFree(ReceptaclePlateTransferEvent receptaclePlateTransferEvent,
                                               TwoDBarcodedTube sourceTube,
                                               @Nullable VesselContainerEmbedder targetVessel,
                                               String targetSection) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent, labEventRefDataFetcher);
        if (sourceTube == null) {
            throw new RuntimeException("Source tube not found for " + receptaclePlateTransferEvent.getSourceReceptacle().getBarcode());
        }
        if (targetVessel == null) {
            String physType = receptaclePlateTransferEvent.getDestinationPlate().getPhysType();
            if (StaticPlate.PlateType.getByAutomationName(physType) != null) {
                targetVessel = new StaticPlate(receptaclePlateTransferEvent.getDestinationPlate().getBarcode(),
                        StaticPlate.PlateType.getByAutomationName(physType));
            } else if (IlluminaFlowcell.FlowcellType.getByAutomationName(physType) != null) {
                targetVessel = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.getByAutomationName(physType),
                        receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
            } else if (physType.equals(PHYS_TYPE_FLOWCELL)) {
                // Guard against the possibility that automation scripts send us bare "Flowcell" types.
                // Assume it to mean an 8-lane HiSeq flowcell.
                targetVessel = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell,
                        receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
            } else {
                throw new RuntimeException("Unexpected physical type: " + physType);
            }
        }
        labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(sourceTube,
                SBSSection.getBySectionName(targetSection), targetVessel.getContainerRole(), labEvent));
        return labEvent;
    }

    private Map<String, TwoDBarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return this.twoDBarcodedTubeDao.findByBarcodes(barcodes);
    }

    /**
     * Build an entity to represent a transfer from a tube to an entire section of a plate
     *
     * @param receptaclePlateTransferEvent JAXB
     * @return entity
     */
    public LabEvent buildFromBettaLims(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        return buildVesselToSectionDbFree(receptaclePlateTransferEvent,
                twoDBarcodedTubeDao.findByBarcode(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode()),
                staticPlateDAO.findByBarcode(receptaclePlateTransferEvent.getDestinationPlate().getBarcode()),
                receptaclePlateTransferEvent.getDestinationPlate().getSection());
    }

    public LabEvent buildFromBettaLims(ReceptacleEventType receptacleEventType) {
        return buildReceptacleEventDbFree(receptacleEventType, labVesselDao.findByIdentifier(
                receptacleEventType.getReceptacle().getBarcode()));
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for an in-place event on a tube.
     *
     * @param receptacleEventType JAXB
     * @param labVessel           from database
     * @return lab event entity
     */
    @DaoFree
    public LabEvent buildReceptacleEventDbFree(ReceptacleEventType receptacleEventType, LabVessel labVessel) {
        LabEvent labEvent = constructReferenceData(receptacleEventType, labEventRefDataFetcher);
        if (labVessel == null) {
            throw new RuntimeException("Source tube not found for " + receptacleEventType.getReceptacle().getBarcode());
        }
        labEvent.setInPlaceLabVessel(labVessel);
        return labEvent;
    }

    public LabEvent constructReferenceData(StationEventType stationEventType,
                                           LabEventRefDataFetcher labEventRefDataFetcher) {
        //        stationEventType.getComment();
        //        stationEventType.getEnd();
        //        stationEventType.getProgram();

        LabEventType labEventType = LabEventType.getByName(stationEventType.getEventType());
        if (labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }

        BspUser bspUser = labEventRefDataFetcher.getOperator(stationEventType.getOperator());
        if (bspUser == null) {
            throw new RuntimeException("Failed to find operator " + stationEventType.getOperator());
        }
        Long operator = bspUser.getUserId();

        Long disambiguator = stationEventType.getDisambiguator();
        if (disambiguator == null) {
            disambiguator = 1L;
        }
        LabEvent genericLabEvent = new LabEvent(labEventType, stationEventType.getStart().toGregorianCalendar().getTime(),
                stationEventType.getStation(), disambiguator, operator);
        if (stationEventType.getBatchId() != null) {
            LabBatch labBatch = labEventRefDataFetcher.getLabBatch(stationEventType.getBatchId());
            if (labBatch == null) {
                throw new RuntimeException("Failed to find lab batch " + stationEventType.getBatchId());
            }
            genericLabEvent.setLabBatch(labBatch);
            labBatch.getLabEvents().add(genericLabEvent);
        }
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
     * @param eventType
     * @return A collection of the created events for the submitted lab vessels
     */
    public Collection<LabEvent> buildFromBatchRequests(@Nonnull Collection<BucketEntry> entryCollection,
                                                       String operator, LabBatch batchIn, @Nonnull String eventLocation,
                                                       @Nonnull LabEventType eventType) {

        long workCounter = 1L;

        List<LabEvent> fullEventList = new LinkedList<LabEvent>();

        Set<UniqueEvent> uniqueEvents = new HashSet<UniqueEvent>();

        for (BucketEntry mapEntry : entryCollection) {
            List<LabEvent> events = new LinkedList<LabEvent>();
            LabEvent currEvent = createFromBatchItems(mapEntry.getPoBusinessKey(), mapEntry.getLabVessel(),
                    workCounter++, operator, eventType, eventLocation);
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
     * @param pdoKey
     * @param batchItem
     * @param disambiguator
     * @param operator
     * @param eventType
     * @param eventLocation
     * @return
     */
    public LabEvent createFromBatchItems(@Nonnull String pdoKey, @Nonnull LabVessel batchItem,
                                         @Nonnull Long disambiguator, String operator,
                                         @Nonnull LabEventType eventType, @Nonnull String eventLocation) {

        Long operatorInfo = labEventRefDataFetcher.getOperator(operator).getUserId();

        LabEvent bucketMoveEvent = new LabEvent(eventType, new Date(), eventLocation, disambiguator, operatorInfo);

        bucketMoveEvent.setProductOrderId(pdoKey);

        //TODO SGM: add to container.
        batchItem.addInPlaceEvent(bucketMoveEvent);

        //TODO SGM: If LabVessel has a batch waiting to be associated with an event, add it here

        return bucketMoveEvent;
    }


}
