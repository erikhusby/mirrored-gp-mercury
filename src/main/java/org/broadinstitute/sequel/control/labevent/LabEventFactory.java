package org.broadinstitute.sequel.control.labevent;

import org.broadinstitute.sequel.bettalims.jaxb.CherryPickSourceType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.sequel.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.dao.vessel.StripTubeDao;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.sequel.entity.run.IlluminaFlowcell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.StripTube;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;
import org.broadinstitute.sequel.bettalims.jaxb.StationEventType;

import javax.inject.Inject;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates Lab Event entities from BettaLIMS JAXB beans
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass", "serial", "CloneableClassWithoutClone", "ClassExtendsConcreteCollection"})
public class LabEventFactory {

    public static final String SECTION_ALL_96 = "ALL96";

    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";
    public static final String PHYS_TYPE_STRIP_TUBE_RACK_OF_12 = "StripTubeRackOf12";
    public static final String PHYS_TYPE_STRIP_TUBE = "StripTube";
    public static final String PHYS_TYPE_FLOWCELL = "Flowcell";
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

    /**
     * Builds one or more lab event entities from a JAXB message bean that contains one or more event beans
     * @param bettaLIMSMessage JAXB bean
     * @return list of entities
     */
    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<LabEvent>();
        bettaLIMSMessage.getMode();
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            labEvents.add(buildFromBettaLims(plateCherryPickEvent));
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            labEvents.add(buildFromBettaLims(plateEventType));
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            labEvents.add(buildFromBettaLims(plateTransferEventType));
        }
        if (bettaLIMSMessage.getReceptaclePlateTransferEvent() != null) {
            labEvents.add(buildFromBettaLims(bettaLIMSMessage.getReceptaclePlateTransferEvent()));
        }
        return labEvents;
    }

    /**
     * Builds a lab event entity from a JAXB cherry pick event bean
     * @param plateCherryPickEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(final PlateCherryPickEvent plateCherryPickEvent) {
        Map<String, RackOfTubes> mapBarcodeToSourceRack = buildMapBarcodeToRack(plateCherryPickEvent.getSourcePositionMap());
        if(plateCherryPickEvent.getPlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            Map<String, StripTube> mapBarcodeToTargetStripTube = new HashMap<String, StripTube>();
            // todo jmt decide about the two null parameters
            return buildCherryPickRackToStripTubeDbFree(plateCherryPickEvent, mapBarcodeToSourceRack, null, null,
                    mapBarcodeToTargetStripTube);
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube = new HashMap<String, TwoDBarcodedTube>();
        for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
            mapBarcodeToSourceTube.putAll(findTubesByBarcodes(positionMapType));
        }

        Map<String, RackOfTubes> mapBarcodeToTargetRack = buildMapBarcodeToRack(
                new ArrayList<PositionMapType>(){{add(plateCherryPickEvent.getPositionMap());}});

        return buildCherryPickRackToRackDbFree(plateCherryPickEvent, mapBarcodeToSourceRack,
                mapBarcodeToSourceTube, mapBarcodeToTargetRack, findTubesByBarcodes(plateCherryPickEvent.getPositionMap()));
    }

    private Map<String, RackOfTubes> buildMapBarcodeToRack(List<PositionMapType> positionMap) {
        Map<String, RackOfTubes> mapBarcodeToSourceRack = new HashMap<String, RackOfTubes>();
        for (PositionMapType positionMapType : positionMap) {
            RackOfTubes rackOfTubes = fetchRack(positionMapType);
            mapBarcodeToSourceRack.put(positionMapType.getBarcode(), rackOfTubes);
        }
        return mapBarcodeToSourceRack;
    }

    private RackOfTubes fetchRack(PositionMapType positionMapType) {
        List<Map.Entry<String, String>> positionBarcodeList  = new ArrayList<Map.Entry<String, String>>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            positionBarcodeList.add(new AbstractMap.SimpleEntry<String, String>(
                    receptacleType.getPosition(), receptacleType.getBarcode()));
        }
        String digest = RackOfTubes.makeDigest(positionBarcodeList);
        List<RackOfTubes> racksOfTubes = rackOfTubesDao.findByDigest(digest);
        RackOfTubes rackOfTubes = null;
        // todo jmt handle digest collision
        if(!racksOfTubes.isEmpty()) {
            rackOfTubes = racksOfTubes.get(0);
        }
        return rackOfTubes;
    }

    public LabEvent buildCherryPickRackToRackDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, RackOfTubes> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, RackOfTubes> mapBarcodeToTargetRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, RackOfTubes> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue());
        }

        for (Map.Entry<String, RackOfTubes> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                RackOfTubes targetRack = buildRack(mapBarcodeToTargetTube, plateCherryPickEvent.getPlate(),
                        plateCherryPickEvent.getPositionMap());
                stringVesselContainerEntry.setValue(targetRack);
            }
            labEvent.addTargetLabVessel(stringVesselContainerEntry.getValue());
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()).getVesselContainer(),
                    cherryPickSourceType.getWell(),
                    mapBarcodeToTargetRack.get(cherryPickSourceType.getDestinationBarcode()).getVesselContainer(),
                    cherryPickSourceType.getDestinationWell(), labEvent));
        }
        return labEvent;
    }

    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, RackOfTubes> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, RackOfTubes> mapBarcodeToTargetRack,
            Map<String, StripTube> mapBarcodeToTargetStripTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, RackOfTubes> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue());
        }

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
                throw new RuntimeException("Expected physType " + PHYS_TYPE_STRIP_TUBE + ", but received " + receptacleType.getReceptacleType());
            }
            StripTube stripTube = mapBarcodeToTargetStripTube.get(receptacleType.getBarcode());
            if(stripTube == null) {
                stripTube = new StripTube(receptacleType.getBarcode());
            }
            labEvent.addTargetLabVessel(stripTube);
            mapPositionToStripTube.put(receptacleType.getPosition(), stripTube);
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String position = LEADING_ZERO_PATTERN.matcher(cherryPickSourceType.getDestinationWell().substring(1)).replaceFirst("");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()).getVesselContainer(),
                    cherryPickSourceType.getWell(),
                    mapPositionToStripTube.get(position).getVesselContainer(),
                    Integer.toString(cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1), labEvent));
        }
        return labEvent;
    }

    /**
    * Builds a lab event entity from a JAXB plate event bean
    * @param plateEventType JAXB event bean
    * @return entity
    */
    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        StaticPlate staticPlate = staticPlateDAO.findByBarcode(plateEventType.getPlate().getBarcode());
        return buildFromBettaLimsPlateEventDbFree(plateEventType, staticPlate);
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
                StripTube stripTube = stripTubeDao.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
                IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(plateTransferEvent.getPlate().getBarcode());
                return buildFromBettaLimsPlateToPlateDbFree(plateTransferEvent, stripTube, illuminaFlowcell);
            }
            sourcePlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
        } else {
            sourceRackOfTubes = fetchRack(plateTransferEvent.getSourcePositionMap());
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
            if(sourceRackOfTubes == null) {
                sourceRackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap());
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
                if(targetRackOfTubes == null) {
                    return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
                } else {
                    return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, targetRackOfTubes, targetPlate);
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
     * lab event entity
     * @param plateTransferEvent JAXB plate transfer event
     * @param mapBarcodeToSourceTubes existing source tubes (in new rack)
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap());
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(rackOfTubes);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    /**
     * Database free (i.e. entities have already been fetched from the database, or constructed in tests) building of
     * lab event entity
     * @param plateTransferEvent JAXB plate transfer event
     * @param rackOfTubes existing source rack
     * @param targetPlate existing plate, or null for new plate
     * @return entity
     */
    public LabEvent buildFromBettaLimsRackToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes rackOfTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(rackOfTubes);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
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
        RackOfTubes rackOfTubes = new RackOfTubes(plate.getBarcode());
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode(), null);
                mapBarcodeToTubes.put(receptacleType.getBarcode(), twoDBarcodedTube);
            }
            rackOfTubes.getVesselContainer().addContainedVessel(twoDBarcodedTube, receptacleType.getPosition());
        }
        rackOfTubes.makeDigest();
        return rackOfTubes;
    }

    // todo jmt combine following two methods?
    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);

        RackOfTubes targetRackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.addSourceLabVessel(sourceRack);
        labEvent.addTargetLabVessel(targetRackOfTubes);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsRackToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            RackOfTubes sourceRack,
            RackOfTubes targetRack) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        labEvent.addSourceLabVessel(sourceRack);
        labEvent.addTargetLabVessel(targetRack);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToRackDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        RackOfTubes rackOfTubes = buildRack(mapBarcodeToTargetTubes, plateTransferEvent.getPlate(),
                plateTransferEvent.getPositionMap());

        labEvent.addSourceLabVessel(sourcePlate);
        labEvent.addTargetLabVessel(rackOfTubes);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateEventDbFree(
            PlateEventType plateEvent,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateEvent.getPlate().getBarcode());
        }

        labEvent.addTargetLabVessel(targetPlate);
        return labEvent;
    }
    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(sourcePlate);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourcePlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildFromBettaLimsPlateToPlateDbFree(
            PlateTransferEventType plateTransferEvent,
            StripTube sourceStripTube,
            IlluminaFlowcell targetFlowcell) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetFlowcell == null) {
            // todo jmt what about MiSeq?
            // todo jmt how to populate run configuration?
            targetFlowcell = new IlluminaFlowcell(IlluminaFlowcell.FLOWCELL_TYPE.EIGHT_LANE,
                    plateTransferEvent.getPlate().getBarcode(), null);
        }

        labEvent.addSourceLabVessel(sourceStripTube);
        labEvent.addTargetLabVessel(targetFlowcell);
        labEvent.getSectionTransfers().add(new SectionTransfer(
                sourceStripTube.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetFlowcell.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection()), labEvent));
        return labEvent;
    }

    public LabEvent buildVesselToSectionDbFree(ReceptaclePlateTransferEvent receptaclePlateTransferEvent,
            TwoDBarcodedTube sourceTube, StaticPlate targetPlate, String targetSection) {
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
        }
        labEvent.addSourceLabVessel(sourceTube);
        labEvent.addTargetLabVessel(targetPlate);
        labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(sourceTube, targetSection,
                targetPlate.getVesselContainer()));
        return labEvent;
    }

    private Map<String, TwoDBarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return this.twoDBarcodedTubeDao.findByBarcodes(barcodes);
    }

    public LabEvent buildFromBettaLims(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        return buildVesselToSectionDbFree(receptaclePlateTransferEvent,
                twoDBarcodedTubeDao.findByBarcode(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode()),
                staticPlateDAO.findByBarcode(receptaclePlateTransferEvent.getDestinationPlate().getBarcode()),
                SECTION_ALL_96);
    }
    
    public LabEvent constructReferenceData(StationEventType stationEventType) {
        stationEventType.getComment();
        stationEventType.getDisambiguator();
        stationEventType.getEnd();
        stationEventType.getProgram();
        
        LabEventType labEventType = LabEventType.getByName(stationEventType.getEventType());
        if(labEventType == null) {
            throw new RuntimeException("Unexpected event type " + stationEventType.getEventType());
        }
        return new GenericLabEvent(labEventType, stationEventType.getStart().toGregorianCalendar().getTime(),
                stationEventType.getStation(), this.personDAO.findByName(stationEventType.getOperator()));
    }

    public void setPersonDAO(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }
}
