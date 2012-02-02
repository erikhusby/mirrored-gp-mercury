package org.broadinstitute.sequel.factory;

import org.broadinstitute.sequel.GenericLabEvent;
import org.broadinstitute.sequel.GenericReagent;
import org.broadinstitute.sequel.LabEvent;
import org.broadinstitute.sequel.LabEventMessage;
import org.broadinstitute.sequel.LabEventType;
import org.broadinstitute.sequel.MolecularState;
import org.broadinstitute.sequel.PersonDAO;
import org.broadinstitute.sequel.RackOfTubes;
import org.broadinstitute.sequel.SampleSheet;
import org.broadinstitute.sequel.StaticPlate;
import org.broadinstitute.sequel.TwoDBarcodedTube;
import org.broadinstitute.sequel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReagentType;
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
    }
    
    TwoDBarcodedTubeDAO twoDBarcodedTubeDao;
    PersonDAO personDAO;

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
        LabEvent labEvent = constructReferenceData(plateTransferEvent);

        // todo jmt separate out the database fetch stages
        // todo jmt there's currently no difference between a plate and a rack, consider combining them
        if(plateTransferEvent.getSourcePositionMap() != null ) {
            RackOfTubes sourceRack = constructRack(plateTransferEvent.getSourcePlate(),
                    plateTransferEvent.getSourcePositionMap());
            labEvent.addSourceLabVessel(sourceRack);
        } else {
            StaticPlate sourcePlate = constructPlate(plateTransferEvent.getSourcePlate());
            labEvent.addSourceLabVessel(sourcePlate);
        }

        if(plateTransferEvent.getPositionMap() != null ) {
            RackOfTubes targetRack = constructRack(plateTransferEvent.getPlate(), plateTransferEvent.getPositionMap());
            labEvent.addTargetLabVessel(targetRack);
        } else {
            StaticPlate targetPlate = constructPlate(plateTransferEvent.getPlate());
            labEvent.addTargetLabVessel(targetPlate);
        }

        for (ReagentType reagentType : plateTransferEvent.getReagent()) {
            // todo jmt how to choose specific reagent subclasses?
            labEvent.addReagent(new GenericReagent(reagentType.getKitType(), reagentType.getBarcode()));
        }

        return labEvent;
    }

    private StaticPlate constructPlate(PlateType plateType) {
        return new StaticPlate(plateType.getBarcode());
    }

    private RackOfTubes constructRack(PlateType plateType, PositionMapType positionMapType) {
        RackOfTubes rackOfTubes = new RackOfTubes(plateType.getBarcode());
        List<String> barcodes = new ArrayList<String>();
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            barcodes.add(receptacleType.getBarcode());
        }
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDao.findByBarcodes(barcodes);
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTube.get(receptacleType.getBarcode());
            if(twoDBarcodedTube == null) {

            }
            SampleSheet sampleSheet = null;
            rackOfTubes.addContainedVessel(/*receptacleType.getPosition(),*/
                    new TwoDBarcodedTube(receptacleType.getBarcode(), sampleSheet));
        }
        return rackOfTubes;
    }

    // plate to rack
    // rack to plate
    // rack to rack
    // plate event
    // rack event
    // create rack or plate for source
    // create rack or plate for destination

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
}
