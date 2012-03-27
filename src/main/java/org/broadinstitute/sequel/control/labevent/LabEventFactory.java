package org.broadinstitute.sequel.control.labevent;

import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;
import org.broadinstitute.sequel.bettalims.jaxb.StationEventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates Lab Events
 */
public class LabEventFactory {

    private static final Map<String, LabEventType> mapMessageNameToEventType = new HashMap<String, LabEventType>();
    static {
        mapMessageNameToEventType.put("ShearingTransfer", new LabEventType(false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA));
        mapMessageNameToEventType.put("PostShearingTransferCleanup", new LabEventType(false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA));
        mapMessageNameToEventType.put("IndexedAdapterLigation", new LabEventType(true, false,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA));
        mapMessageNameToEventType.put("PondRegistration", new LabEventType(false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA));
        mapMessageNameToEventType.put("PreSelectionPool", new LabEventType(false, true,
                MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA));
    }
    
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDao;
    private StaticPlateDAO staticPlateDAO;
    private PersonDAO personDAO;

    public List<LabEvent> buildFromBettaLims(BettaLIMSMessage bettaLIMSMessage) {
        List<LabEvent> labEvents = new ArrayList<LabEvent>();
        bettaLIMSMessage.getMode();
        labEvents.add(buildFromBettaLims(bettaLIMSMessage.getPlateCherryPickEvent()));
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            labEvents.add(buildFromBettaLims(plateEventType));
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            labEvents.add(buildFromBettaLims(plateTransferEventType));
        }
        labEvents.add(buildFromBettaLims(bettaLIMSMessage.getReceptaclePlateTransferEvent()));
        return labEvents;
    }

    public LabEvent buildFromBettaLims(PlateEventType plateEventType) {
        LabEvent labEvent = constructReferenceData(plateEventType);
        return labEvent;
    }

    public LabEvent buildFromBettaLims(PlateTransferEventType plateTransferEvent) {
        Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes = null;
        if(plateTransferEvent.getSourcePositionMap() != null) {
            mapBarcodeToSourceTubes = findTubesByBarcodes(plateTransferEvent.getSourcePositionMap());
        }
        // todo jmt hash the tube positions, fetch any existing tube formation
        StaticPlate targetPlate = this.staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        
        return buildFromBettaLimsRackToPlateDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
    }

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
                rackOfTubes, SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate, SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }

    private RackOfTubes buildRack(Map<String, TwoDBarcodedTube> mapBarcodeToTubes, PlateType plate, PositionMapType positionMap) {
        RackOfTubes rackOfTubes = new RackOfTubes(plate.getBarcode());
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTubes.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode(), null);
                mapBarcodeToTubes.put(receptacleType.getBarcode(), twoDBarcodedTube);
            }
            rackOfTubes.addContainedVessel(twoDBarcodedTube, receptacleType.getPosition());
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
                sourceRack, SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetRackOfTubes, SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                sourcePlate, SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                rackOfTubes, SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
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
                sourcePlate, SBSSection.valueOf(plateTransferEvent.getSourcePlate().getSection()),
                targetPlate, SBSSection.valueOf(plateTransferEvent.getPlate().getSection())));
        return labEvent;
    }


    private Map<String, TwoDBarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return this.twoDBarcodedTubeDao.findByBarcodes(barcodes);
    }

    public LabEvent buildFromBettaLims(PlateCherryPickEvent plateCherryPickEvent) {
        LabEvent labEvent = constructReferenceData(plateCherryPickEvent);
        plateCherryPickEvent.getReagent();
        plateCherryPickEvent.getSourcePositionMap();
        plateCherryPickEvent.getPositionMap();
        return labEvent;
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
        
        LabEventType labEventType = mapMessageNameToEventType.get(stationEventType.getEventType());
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
