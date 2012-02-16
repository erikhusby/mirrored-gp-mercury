package org.broadinstitute.sequel.factory;

import org.broadinstitute.sequel.GenericLabEvent;
import org.broadinstitute.sequel.LabEvent;
import org.broadinstitute.sequel.LabEventType;
import org.broadinstitute.sequel.MolecularState;
import org.broadinstitute.sequel.PersonDAO;
import org.broadinstitute.sequel.RackOfTubes;
import org.broadinstitute.sequel.StaticPlate;
import org.broadinstitute.sequel.StaticPlateDAO;
import org.broadinstitute.sequel.TwoDBarcodedTube;
import org.broadinstitute.sequel.TwoDBarcodedTubeDAO;
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
        StaticPlate targetPlate = staticPlateDAO.findByBarcode(plateTransferEvent.getPlate().getBarcode());
        
        return buildFromBettaLimsDbFree(plateTransferEvent, mapBarcodeToSourceTubes, targetPlate);
    }

    public LabEvent buildFromBettaLimsDbFree(
            PlateTransferEventType plateTransferEvent,
            Map<String, TwoDBarcodedTube> mapBarcodeToSourceTubes,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        RackOfTubes rackOfTubes = new RackOfTubes(plateTransferEvent.getSourcePlate().getBarcode());
        for (ReceptacleType receptacleType : plateTransferEvent.getSourcePositionMap().getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToSourceTubes.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(receptacleType.getBarcode(), null);
            }
            rackOfTubes.addContainedVessel(twoDBarcodedTube, receptacleType.getPosition());
        }
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(rackOfTubes);
        labEvent.addTargetLabVessel(targetPlate);
        return labEvent;
    }

    public LabEvent buildFromBettaLimsDbFree(
            PlateTransferEventType plateTransferEvent,
            StaticPlate sourcePlate,
            StaticPlate targetPlate) {
        LabEvent labEvent = constructReferenceData(plateTransferEvent);
        if(targetPlate == null) {
            targetPlate = new StaticPlate(plateTransferEvent.getPlate().getBarcode());
        }

        labEvent.addSourceLabVessel(sourcePlate);
        labEvent.addTargetLabVessel(targetPlate);
        return labEvent;
    }


    private Map<String, TwoDBarcodedTube> findTubesByBarcodes(PositionMapType positionMap) {
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMap.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        return twoDBarcodedTubeDao.findByBarcodes(barcodes);
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
                stationEventType.getStation(), personDAO.findByName(stationEventType.getOperator()));
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
