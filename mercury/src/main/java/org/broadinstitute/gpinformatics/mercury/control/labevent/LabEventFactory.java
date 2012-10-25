package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.person.PersonDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StripTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.inject.Inject;
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
 * Creates Lab Event entities from BettaLIMS JAXB beans
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "serial", "CloneableClassWithoutClone", "ClassExtendsConcreteCollection"})
public class LabEventFactory {

    /** Section for all wells in a 96 well plate */
    public static final String SECTION_ALL_96 = "ALL96";
    /** Physical type for rack of tubes */
    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    /** Physical type for Eppendorf plate with 96 wells */
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";
    /** Physical type for a rack that holds 12 strip tubes */
    public static final String PHYS_TYPE_STRIP_TUBE_RACK_OF_12 = "StripTubeRackOf12";
    /** Physical type for a strip tube*/
    public static final String PHYS_TYPE_STRIP_TUBE = "StripTube";
    /** Physical type for a flowcell */
    public static final String PHYS_TYPE_FLOWCELL = "Flowcell";
    /** Pattern that groups non-zero trailing digits */
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0+(?!$)");

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDao;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private PersonDAO personDAO;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private StripTubeDao stripTubeDao;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private LabBatchDAO labBatchDAO;

    public LabEventFactory() {}

    public LabEventFactory(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }

    public interface LabEventRefDataFetcher {
        Person getOperator(String userId);
        LabBatch getLabBatch(String labBatchName);
    }

    private LabEventRefDataFetcher labEventRefDataFetcher = new LabEventRefDataFetcher() {
        @Override
        public Person getOperator(String userId) {
            return personDAO.findByName(userId);
        }

        @Override
        public LabBatch getLabBatch(String labBatchName) {
            return labBatchDAO.findByName(labBatchName);
        }
    };

    /**
     * This class is used in a map, to detect duplicate events from a deck.
     */
    private static class UniqueEvent {
        /** where the event happened */
        private final String eventLocation;
        /** when the event happened */
        private final Date eventDate;
        /** Needed if the deck is sending two transfers in the same message */
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

            if (disambiguator != null ? !disambiguator.equals(other.getDisambiguator()) : other.getDisambiguator() != null) {
                return false;
            }
            if (eventDate != null ? !eventDate.equals(other.getEventDate()) : other.getEventDate() != null) {
                return false;
            }
            if (eventLocation != null ? !eventLocation.equals(other.getEventLocation()) : other.getEventLocation() != null) {
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
     * @param bettaLIMSMessage JAXB bean
     * @return list of entities
     */
    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<LabEvent>();
        bettaLIMSMessage.getMode();
        Set<UniqueEvent> uniqueEvents = new HashSet<UniqueEvent>();

        // Have to persist and flush inside each loop, because the first event may create
        // vessels that are referenced by the second event, e.g. PreSelectionPool
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateCherryPickEvent);
            persistLabEvent(uniqueEvents, labEvent);
            labEvents.add(labEvent);
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateEventType);
            persistLabEvent(uniqueEvents, labEvent);
            labEvents.add(labEvent);
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(plateTransferEventType);
            persistLabEvent(uniqueEvents, labEvent);
            labEvents.add(labEvent);
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            LabEvent labEvent = buildFromBettaLims(receptaclePlateTransferEvent);
            persistLabEvent(uniqueEvents, labEvent);
            labEvents.add(labEvent);
        }
        return labEvents;
    }

    /**
     * Modify disambiguators of other events in the same message, if necessary, and persist an event
     * @param uniqueEvents events in a message
     * @param labEvent     event to be persisted
     */
    private void persistLabEvent(Set<UniqueEvent> uniqueEvents, LabEvent labEvent) {
        // The deck-side scripts don't always set the disambiguator correctly, so modify it, to make it unique
        // within this message, if necessary
        while (!uniqueEvents.add(new UniqueEvent(labEvent.getEventLocation(), labEvent.getEventDate(), labEvent.getDisambiguator()))) {
            labEvent.setDisambiguator(labEvent.getDisambiguator() + 1);
        }
        labEventDao.persist(labEvent);
        labEventDao.flush();
    }

    /**
     * Builds a lab event entity from a JAXB cherry pick event bean
     * @param plateCherryPickEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(final PlateCherryPickEvent plateCherryPickEvent) {
        Map<String, RackOfTubes> mapBarcodeToSourceRack = buildMapBarcodeToRack(plateCherryPickEvent.getSourcePositionMap());

        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<String, TwoDBarcodedTube>();
        for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
            mapBarcodeToSourceTube.putAll(findTubesByBarcodes(positionMapType));
        }

        if(plateCherryPickEvent.getPlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            Map<String, StripTube> mapBarcodeToTargetStripTube = new HashMap<String, StripTube>();
            return buildCherryPickRackToStripTubeDbFree(plateCherryPickEvent, mapBarcodeToSourceRack, mapBarcodeToSourceTube,
                    null, mapBarcodeToTargetStripTube);
        }

        Map<String, RackOfTubes> mapBarcodeToTargetRack = buildMapBarcodeToRack(
                new ArrayList<PositionMapType>(){{add(plateCherryPickEvent.getPositionMap());}});

        return buildCherryPickRackToRackDbFree(plateCherryPickEvent, mapBarcodeToSourceRack,
                mapBarcodeToSourceTube, mapBarcodeToTargetRack, findTubesByBarcodes(plateCherryPickEvent.getPositionMap()));
    }

    /**
     * From a list of positionMaps, create a map from barcode to rack
     * @param positionMap list of positionMaps
     * @return map from barcode to rack
     */
    private Map<String, RackOfTubes> buildMapBarcodeToRack(List<PositionMapType> positionMap) {
        Map<String, RackOfTubes> mapBarcodeToSourceRack = new HashMap<String, RackOfTubes>();
        for (PositionMapType positionMapType : positionMap) {
            RackOfTubes rackOfTubes = fetchRack(positionMapType);
            mapBarcodeToSourceRack.put(positionMapType.getBarcode(), rackOfTubes);
        }
        return mapBarcodeToSourceRack;
    }

    /**
     * Fetch an existing tube formation from the database
     * @param positionMapType tube barcodes and positions
     * @return database rack
     */
    private RackOfTubes fetchRack(PositionMapType positionMapType) {
        List<Map.Entry<VesselPosition, String>> positionBarcodeList  = new ArrayList<Map.Entry<VesselPosition, String>>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            positionBarcodeList.add(new AbstractMap.SimpleEntry<VesselPosition, String>(
                    VesselPosition.getByName(receptacleType.getPosition()), receptacleType.getBarcode()));
        }
        String digest = RackOfTubes.makeDigest(positionBarcodeList);
        return rackOfTubesDao.findByDigest(digest);
    }

    /**
     * Build an entity to represent a cherry pick (random access) transfer from a source rack to a target rack
     * @param plateCherryPickEvent JAXB
     * @param mapBarcodeToSourceRack obvious
     * @param mapBarcodeToSourceTube obvious
     * @param mapBarcodeToTargetRack obvious
     * @param mapBarcodeToTargetTube obvious
     * @return event
     */
    public LabEvent buildCherryPickRackToRackDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, RackOfTubes> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, RackOfTubes> mapBarcodeToTargetRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceRackToMap(plateCherryPickEvent, mapBarcodeToSourceRack, mapBarcodeToSourceTube);

        for (Map.Entry<String, RackOfTubes> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                RackOfTubes targetRack = buildRack(mapBarcodeToTargetTube, plateCherryPickEvent.getPlate(),
                        plateCherryPickEvent.getPositionMap());
                stringVesselContainerEntry.setValue(targetRack);
            }
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String destinationRackBarcode = cherryPickSourceType.getDestinationBarcode();
            if(destinationRackBarcode == null) {
                destinationRackBarcode = plateCherryPickEvent.getPlate().getBarcode();
            }
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()).getVesselContainer(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapBarcodeToTargetRack.get(destinationRackBarcode).getVesselContainer(),
                    VesselPosition.getByName(cherryPickSourceType.getDestinationWell()),
                    labEvent));
        }
        return labEvent;
    }

    /**
     * Build an entity to represent a cherry pick (random access) transfer between a source rack and a target strip
     * tube
     * @param plateCherryPickEvent JAXB
     * @param mapBarcodeToSourceRack obvious
     * @param mapBarcodeToSourceTube obvious
     * @param mapBarcodeToTargetRack obvious
     * @param mapBarcodeToTargetStripTube obvious
     * @return entity
     */
    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, RackOfTubes> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, RackOfTubes> mapBarcodeToTargetRack,
            Map<String, StripTube> mapBarcodeToTargetStripTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent, labEventRefDataFetcher);
        addSourceRackToMap(plateCherryPickEvent, mapBarcodeToSourceRack, mapBarcodeToSourceTube);

/*
        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                // todo jmt do we care about the strip tube holder?
                RackOfTubes targetRack = new RackOfTubes(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getVesselContainer());
            }
        }
*/

        Map<String, StripTube> mapPositionToStripTube = new HashMap<String, StripTube>();
        for (ReceptacleType receptacleType : plateCherryPickEvent.getPositionMap().getReceptacle()) {
            if(!receptacleType.getReceptacleType().equals(PHYS_TYPE_STRIP_TUBE)) {
                throw new RuntimeException("Expected physType " + PHYS_TYPE_STRIP_TUBE + ", but received " +
                        receptacleType.getReceptacleType());
            }
            StripTube stripTube = mapBarcodeToTargetStripTube.get(receptacleType.getBarcode());
            if(stripTube == null) {
                stripTube = new StripTube(receptacleType.getBarcode());
            }
            mapPositionToStripTube.put(receptacleType.getPosition(), stripTube);
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String position = LEADING_ZERO_PATTERN.matcher(cherryPickSourceType.getDestinationWell().substring(1)).replaceFirst("");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()).getVesselContainer(),
                    VesselPosition.getByName(cherryPickSourceType.getWell()),
                    mapPositionToStripTube.get(position).getVesselContainer(),
                    VesselPosition.getByName("TUBE" + Integer.toString(cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1)),
                    labEvent));
        }
        return labEvent;
    }

    /**
     * if a source rack is not already in the map, add it
     * @param plateCherryPickEvent JAXB
     * @param mapBarcodeToSourceRack map
     * @param mapBarcodeToSourceTube needed to build the rack
     */
    private void addSourceRackToMap(PlateCherryPickEvent plateCherryPickEvent, Map<String, RackOfTubes> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube) {
        for (PlateType sourceRackJaxb : plateCherryPickEvent.getSourcePlate()) {
            RackOfTubes sourceRackEntity = mapBarcodeToSourceRack.get(sourceRackJaxb.getBarcode());
            if(sourceRackEntity == null) {
                for (PositionMapType sourcePositionMap : plateCherryPickEvent.getSourcePositionMap()) {
                    if(sourcePositionMap.getBarcode().equals(sourceRackJaxb.getBarcode())) {
                        sourceRackEntity = buildRack(mapBarcodeToSourceTube, sourceRackJaxb, sourcePositionMap);
                        break;
                    }
                }
            }
            if(sourceRackEntity == null) {
                throw new RuntimeException("Failed to find source position map for " + sourceRackJaxb.getBarcode());
            }
            mapBarcodeToSourceRack.put(sourceRackJaxb.getBarcode(), sourceRackEntity);
        }
    }

    /**
    * Builds a lab event entity from a JAXB plate event (reagent addition) bean
    * @param plateEventType JAXB event bean
    * @return entity
    */
    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        if(plateEventType.getPositionMap() == null) {
            PlateType plate = plateEventType.getPlate();
            if(plate == null) {
                // todo jmt why isn't this error caught in JAXB?
                throw new RuntimeException("No plate element in plateEvent");
            }
            StaticPlate staticPlate = staticPlateDAO.findByBarcode(plate.getBarcode());
            return buildFromBettaLimsPlateEventDbFree(plateEventType, staticPlate);
        } else {
            RackOfTubes rackOfTubes = fetchRack(plateEventType.getPositionMap());
            return buildFromBettaLimsRackEventDbFree(plateEventType, rackOfTubes, findTubesByBarcodes(
                    plateEventType.getPositionMap()));
        }
    }

    /**
     * Builds a lab event entity from a JAXB plate transfer event bean
     * @param plateTransferEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes = null;
        StaticPlate sourcePlate = null;
        RackOfTubes sourceRackOfTubes = null;
        if(plateTransferEvent.getSourcePositionMap() == null) {
            if(plateTransferEvent.getSourcePlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE) &&
                    plateTransferEvent.getPlate().getPhysType().equals(PHYS_TYPE_FLOWCELL)) {
                // todo jmt create if null
                StripTube stripTube = stripTubeDao.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
                IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(plateTransferEvent.getPlate().getBarcode());
                return buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, stripTube, illuminaFlowcell);
            }
            sourcePlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
            // todo jmt log this?
            if(sourcePlate == null) {
                sourcePlate = new StaticPlate(plateTransferEvent.getSourcePlate().getBarcode(),
                        StaticPlate.PlateType.getByDisplayName(plateTransferEvent.getSourcePlate().getPhysType()));
            }
        } else {
            sourceRackOfTubes = fetchRack(plateTransferEvent.getSourcePositionMap());
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
            if(sourceRackOfTubes == null) {
                sourceRackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(),
                        plateTransferEvent.getSourcePositionMap());
            }
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = null;
        StaticPlate targetPlate = null;
        RackOfTubes targetRackOfTubes = null;
        if(plateTransferEvent.getPositionMap() == null) {
            targetPlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        } else {
            targetRackOfTubes = fetchRack(plateTransferEvent.getPositionMap());
            mapBarcodeToTargetTubes = findTubesByBarcodes(plateTransferEvent.getPositionMap());
        }

        if(plateTransferEvent.getSourcePositionMap() == null){
            // plate to ...
            if(plateTransferEvent.getPositionMap() == null) {
                // plate
                return buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, sourcePlate, targetPlate);
            } else {
                // rack
                return buildFromBettaLimsPlateToRackDbFree(plateTransferEvent, sourcePlate, mapBarcodeToTargetTubes);
            }
        } else {
            // rack to ...
            if(plateTransferEvent.getPositionMap() == null) {
                // plate
                if(sourceRackOfTubes == null) {
                    return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
                } else {
                    return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, sourceRackOfTubes, targetPlate);
                }
            } else {
                // rack
                if(targetRackOfTubes == null) {
                    return buildFromBettaLimsRackToRackDbFree(plateTransferEvent,
                            sourceRackOfTubes,
                            mapBarcodeToTargetTubes);
                } else {
                    return buildFromBettaLimsRackToRackDbFree(plateTransferEvent,
                            sourceRackOfTubes,
                            targetRackOfTubes);
                }
            }
        }
    }

    // todo jmt combine following two methods?
    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to plate
     * @param plateTransferEvent JAXB plate transfer event
     * @param mapBarcodeToSourceTubes existing source tubes (in new rack)
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(),
                plateTransferEvent.getSourcePositionMap());
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to plate
     * @param plateTransferEvent JAXB plate transfer event
     * @param rackOfTubes existing source rack
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes rackOfTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Build a rack entity
     * @param mapBarcodeToTubes source tubes
     * @param plate JAXB rack
     * @param positionMap JAXB list of tube barcodes
     * @return entity
     */
    private RackOfTubes buildRack(Map<String, TwoDBarcodedTube> mapBarcodeToTubes, PlateType plate, PositionMapType positionMap) {
        // todo jmt fix label
        RackOfTubes rackOfTubes = new RackOfTubes(plate.getBarcode() + "_" + Long.toString(System.currentTimeMillis()), RackOfTubes.RackType.Matrix96);
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode());
                mapBarcodeToTubes.put(receptacleType.getBarcode(), twoDBarcodedTube);
            }
            rackOfTubes.getVesselContainer().addContainedVessel(twoDBarcodedTube, VesselPosition.getByName(receptacleType.getPosition()));
        }
        rackOfTubes.makeDigest();
        return rackOfTubes;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     * @param plateTransferEvent JAXB
     * @param sourceRack from database
     * @param mapBarcodeToTargetTubes each entry may be null, if it isn't in the database
     * @return entity
     */
    // todo jmt combine following four methods?
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);

        RackOfTubes targetRackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     * @param plateTransferEvent JAXB
     * @param mapBarcodeToSourceTubes each entry may be null, if it isn't in the database
     * @param mapBarcodeToTargetTubes each entry may be null, if it isn't in the database
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        RackOfTubes sourceRackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(),
                plateTransferEvent.getSourcePositionMap());

        RackOfTubes targetRackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     * @param plateTransferEvent JAXB
     * @param mapBarcodeToSourceTubes each entry may be null, if it isn't in the database
     * @param targetRackOfTubes from database
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            RackOfTubes targetRackOfTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        RackOfTubes sourceRackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(),
                plateTransferEvent.getSourcePositionMap());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity for transfer from rack to rack
     * @param plateTransferEvent JAXB
     * @param sourceRack from database
     * @param targetRack from database
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            RackOfTubes targetRack) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetRack.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                rackOfTubes.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateEventDbFree(
            PlateEventType plateEvent,
            StaticPlate plate) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if(plate == null) {
            plate = new StaticPlate(plateEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(plateEvent.getPlate().getPhysType()));
        }

        plate.addInPlaceEvent(labEvent);
        return labEvent;
    }

    public LabEvent buildFromBettaLimsRackEventDbFree(
            PlateEventType plateEvent,
            RackOfTubes rackOfTubes,
            Map<String, TwoDBarcodedTube> mapBarcodeToTubes) {
        LabEvent labEvent = constructReferenceData(plateEvent, labEventRefDataFetcher);
        if(rackOfTubes == null) {
            rackOfTubes = buildRack(mapBarcodeToTubes, plateEvent.getPlate(), plateEvent.getPositionMap());
        }

        rackOfTubes.addInPlaceEvent(labEvent);
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(plateTransferEvent.getPlate().getPhysType()));
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.getBySectionName(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StripTube sourceStripTube,
            IlluminaFlowcell targetFlowcell) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent, labEventRefDataFetcher);
        if(targetFlowcell == null) {
            // todo jmt what about MiSeq?
            // todo jmt how to populate run configuration?
            targetFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE,
                    plateTransferEvent.getPlate().getBarcode(), null);
        }

        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getVesselContainer(), SBSSection.STRIP_TUBE8,
                targetFlowcell.getVesselContainer(), SBSSection.FLOWCELL8, labEvent));
        return labEvent;
    }

    public LabEvent buildVesselToSectionDbFree(ReceptaclePlateTransferEvent receptaclePlateTransferEvent,
            TwoDBarcodedTube sourceTube, StaticPlate targetPlate, String targetSection) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent, labEventRefDataFetcher);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(receptaclePlateTransferEvent.getDestinationPlate().getBarcode(),
                    StaticPlate.PlateType.getByDisplayName(receptaclePlateTransferEvent.getDestinationPlate().getPhysType()));
        }
        labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(sourceTube,
                SBSSection.getBySectionName(targetSection), targetPlate.getVesselContainer(), labEvent));
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
     * @param receptaclePlateTransferEvent JAXB
     * @return entity
     */
    public LabEvent buildFromBettaLims(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        return buildVesselToSectionDbFree(receptaclePlateTransferEvent,
                twoDBarcodedTubeDao.findByBarcode(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode()),
                staticPlateDAO.findByBarcode(receptaclePlateTransferEvent.getDestinationPlate().getBarcode()),
                SECTION_ALL_96);
    }
    
    public LabEvent constructReferenceData(StationEventType stationEventType, LabEventRefDataFetcher labEventRefDataFetcher) {
//        stationEventType.getComment();
//        stationEventType.getEnd();
//        stationEventType.getProgram();
        
        LabEventType labEventType = LabEventType.getByName(stationEventType.getEventType());
        if(labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }
        Person operator = labEventRefDataFetcher.getOperator(stationEventType.getOperator());
        if(operator == null) {
            throw new RuntimeException("Failed to find operator " + stationEventType.getOperator());
        }
        Long disambiguator = stationEventType.getDisambiguator();
        if(disambiguator == null) {
            disambiguator = 1L;
        }
        GenericLabEvent genericLabEvent = new GenericLabEvent(labEventType, stationEventType.getStart().toGregorianCalendar().getTime(),
                stationEventType.getStation(), disambiguator, operator);
        if(stationEventType.getBatchId() != null) {
            LabBatch labBatch = labEventRefDataFetcher.getLabBatch(stationEventType.getBatchId());
            if(labBatch == null) {
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


    public Collection<GenericLabEvent> buildFromBatchRequestsDBFree ( Map<String, List<LabVessel>> pdoToVessels,
                                                                      Person actor, LabBatch batchIn ) {

        long workCounter = 0L;

        List<GenericLabEvent> fullEventList = new LinkedList<GenericLabEvent> ();

        for ( Map.Entry<String, List<LabVessel>> mapEntry : pdoToVessels.entrySet () ) {
            List<GenericLabEvent> events = new LinkedList<GenericLabEvent> ();
            for ( LabVessel currVessel : mapEntry.getValue () ) {
                GenericLabEvent currEvent = createFromBatchItems ( mapEntry.getKey (), currVessel, workCounter++, actor,
                                                                    LabEventType.BUCKET_EXIT );
                currEvent.setLabBatch(batchIn);
                events.add ( currEvent );
            }

            // TODO SGM Create Batch from list of vessels and the currently created Events
            // TODO SGM Submit Batch to Create Jira Ticket
            // TODO SGM link Batch Ticket to PDO ticket
            fullEventList.addAll(events);
        }

        return fullEventList;
    }

    public GenericLabEvent createFromBatchItems ( String pdoKey, LabVessel batchItem, Long ambiguator, Person actor,
                                           LabEventType eventType ) {
        GenericLabEvent bucketMoveEvent = new GenericLabEvent ( eventType, new Date (),
                                                         LabEvent.UI_EVENT_LOCATION, ambiguator,
                                                         actor );
        bucketMoveEvent.setInPlaceLabVessel ( batchItem );
        bucketMoveEvent.setProductOrderId ( pdoKey );

        return bucketMoveEvent;
    }


}
