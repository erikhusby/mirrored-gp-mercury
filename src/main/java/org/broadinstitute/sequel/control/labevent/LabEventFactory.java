package org.broadinstitute.sequel.control.labevent;

import org.broadinstitute.sequel.bettalims.jaxb.CherryPickSourceType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
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
import org.broadinstitute.sequel.entity.vessel.VesselContainer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates Lab Event entities from BettaLIMS JAXB beans
 */
@SuppressWarnings({"FeatureEnvy", "OverlyCoupledClass"})
public class LabEventFactory {

    public static final String SECTION_ALL_96 = "ALL96";

    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";
    public static final String PHYS_TYPE_STRIP_TUBE_RACK_OF_12 = "StripTubeRackOf12";
    public static final String PHYS_TYPE_STRIP_TUBE = "StripTube";
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0+(?!$)");

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDao;
    @Inject
    private StaticPlateDAO staticPlateDAO;
    @Inject
    private PersonDAO personDAO;

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
    public LabEvent buildFromBettaLims(PlateCherryPickEvent plateCherryPickEvent) {
        if(plateCherryPickEvent.getPlate().getPhysType().equals(PHYS_TYPE_STRIP_TUBE_RACK_OF_12)) {
            return buildCherryPickRackToStripTubeDbFree(null, null, null, null, null);
        }
        return buildCherryPickRackToRackDbFree(null, null, null, null, null);
    }

    public LabEvent buildCherryPickRackToRackDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, VesselContainer<?>> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, VesselContainer<?>> mapBarcodeToTargetRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToTargetTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                RackOfTubes targetRack = new RackOfTubes(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getVesselContainer());
            }
            labEvent.addTargetLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()),
                    cherryPickSourceType.getWell(),
                    mapBarcodeToTargetRack.get(cherryPickSourceType.getDestinationBarcode()),
                    cherryPickSourceType.getDestinationWell()));
        }
        return labEvent;
    }

    public LabEvent buildCherryPickRackToStripTubeDbFree(PlateCherryPickEvent plateCherryPickEvent,
            Map<String, VesselContainer<?>> mapBarcodeToSourceRack,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTube,
            Map<String, VesselContainer<?>> mapBarcodeToTargetRack,
            Map<String, StripTube> mapBarcodeToTargetStripTube) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToSourceRack.entrySet()) {
            labEvent.addSourceLabVessel(stringVesselContainerEntry.getValue().getEmbedder());
        }

        for (Map.Entry<String, VesselContainer<?>> stringVesselContainerEntry : mapBarcodeToTargetRack.entrySet()) {
            if(stringVesselContainerEntry.getValue() == null) {
                // todo jmt do we care about the strip tube holder?
                RackOfTubes targetRack = new RackOfTubes(stringVesselContainerEntry.getKey());
                stringVesselContainerEntry.setValue(targetRack.getVesselContainer());
            }
        }

        Map<String, StripTube> mapPositionToStripTube = new HashMap<String, StripTube>();
        for (ReceptacleType receptacleType : plateCherryPickEvent.getPositionMap().getReceptacle()) {
            if(!receptacleType.getReceptacleType().equals(PHYS_TYPE_STRIP_TUBE)) {
                throw new RuntimeException("Expected physType " + PHYS_TYPE_STRIP_TUBE + ", but received " + receptacleType.getReceptacleType());
            }
            StripTube stripTube = new StripTube(receptacleType.getBarcode());
            labEvent.addTargetLabVessel(stripTube);
            mapPositionToStripTube.put(receptacleType.getPosition(), stripTube);
        }

        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
            String position = LEADING_ZERO_PATTERN.matcher(cherryPickSourceType.getDestinationWell().substring(1)).replaceFirst("");
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    mapBarcodeToSourceRack.get(cherryPickSourceType.getBarcode()),
                    cherryPickSourceType.getWell(),
                    mapPositionToStripTube.get(position).getVesselContainer(),
                    Integer.toString(cherryPickSourceType.getDestinationWell().charAt(0) - 'A' + 1)));
        }
        return labEvent;
    }

    /**
    * Builds a lab event entity from a JAXB plate event bean
    * @param plateEventType JAXB event bean
    * @return entity
    */
    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        LabEvent labEvent = constructReferenceData(plateEventType);
        return labEvent;
    }

    /**
     * Builds a lab event entity from a JAXB plate transfer event bean
     * @param plateTransferEvent JAXB event bean
     * @return entity
     */
    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes = null;
        Map<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = null;
        StaticPlate sourcePlate = null;
        StaticPlate targetPlate = null;
        if(plateTransferEvent.getSourcePositionMap() == null) {
            sourcePlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getSourcePlate().getBarcode());
        } else {
            // todo jmt hash the tube positions, fetch any existing tube formation
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
        }
        if(plateTransferEvent.getPositionMap() == null) {
            targetPlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        } else {
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
                return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
            } else {
                // rack
                return buildFromBettaLimsRackToRackDbFree(plateTransferEvent,
                        buildRack(mapBarcodeToSourceTubes, plateTransferEvent.getSourcePlate(), plateTransferEvent.getSourcePositionMap()),
                        mapBarcodeToTargetTubes);
            }
        }
    }

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
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
        return rackOfTubes;
    }

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
                targetRackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                targetRack.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                rackOfTubes.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                targetPlate.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                targetFlowcell.getVesselContainer(), SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
        LabEvent labEvent = constructReferenceData(receptaclePlateTransferEvent);
        receptaclePlateTransferEvent.getSourceReceptacle();
        return labEvent;
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

    public void setTwoDBarcodedTubeDao(TwoDBarcodedTubeDAO twoDBarcodedTubeDao) {
        this.twoDBarcodedTubeDao = twoDBarcodedTubeDao;
    }

    public void setStaticPlateDAO(StaticPlateDAO staticPlateDAO) {
        this.staticPlateDAO = staticPlateDAO;
    }

    public void setPersonDAO(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }
}
