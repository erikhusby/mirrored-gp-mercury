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

package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * This class has methods for transferring stuff from one Vessel to Another One.
 */
@Stateful
@RequestScoped
public class VesselTransferEjb {
    /**
     * Create a vessel transfer for denature to reagent kit.
     *
     * @param denatureRackBarcode The barcode of denature rack. If null a fake one will be created.
     * @param denatureBarcodeMap  A Map of denature tube barcodes and location on the denature rack.
     * @param reagentKitBarcode   The barcode of reagent kit that will receive the transfer.
     * @param username            Name of the user performing the action.
     * @param stationName         Where the transfer occurred (such as UI)
     *
     * @return The newly created event.
     */
    public BettaLIMSMessage denatureToReagentKitTransfer(@Nullable String denatureRackBarcode,
                                                         Map<String, VesselPosition> denatureBarcodeMap,
                                                         String reagentKitBarcode, String username,
                                                         String stationName) {

        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();

        PlateCherryPickEvent transferEvent = new PlateCherryPickEvent();
        transferEvent.setEventType(LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER.getName());
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            transferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        transferEvent.setDisambiguator(1L);
        transferEvent.setOperator(username);
        transferEvent.setStation(stationName);

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
        for (Map.Entry<String, VesselPosition> item : denatureBarcodeMap.entrySet()) {
            String tubeBarcode = item.getKey();
            TwoDBarcodedTube sourceTube = new TwoDBarcodedTube(tubeBarcode);
            VesselPosition vesselPosition = item.getValue();
            mapPositionToTube.put(vesselPosition, sourceTube);
        }

        if (denatureRackBarcode == null) {
            denatureRackBarcode = "DenatureRack" + reagentKitBarcode;
        }

        transferEvent.getSourcePlate().add(buildRackPlateType(denatureRackBarcode));
        PositionMapType positionMap = new PositionMapType();
        positionMap.setBarcode(denatureRackBarcode);

        for (Map.Entry<String, VesselPosition> entry : denatureBarcodeMap.entrySet()) {
            ReceptacleType receptacleType = new ReceptacleType();
            receptacleType.setBarcode(entry.getKey());
            receptacleType.setPosition(entry.getValue().toString());
            receptacleType.setReceptacleType("tube");

            positionMap.getReceptacle().add(receptacleType);

            CherryPickSourceType cherryPickSource = new CherryPickSourceType();
            cherryPickSource.setBarcode(denatureRackBarcode);
            cherryPickSource.setWell(entry.getValue().toString());
            cherryPickSource.setDestinationBarcode(reagentKitBarcode);
            cherryPickSource.setDestinationWell(MiSeqReagentKit.LOADING_WELL.toString());
            transferEvent.getSource().add(cherryPickSource);
        }
        transferEvent.getSourcePositionMap().add(positionMap);
        transferEvent.getPlate().add(buildReagentKitPlateType(reagentKitBarcode));
        bettaLIMSMessage.getPlateCherryPickEvent().add(transferEvent);
        return bettaLIMSMessage;
    }

    public ReceptaclePlateTransferEvent buildDenatureTubeToFlowcell(String eventType, String denatureTubeBarcode,
                                                                    String flowcellBarcode, String username,
                                                                    String eventStationName) {
        ReceptaclePlateTransferEvent event = new ReceptaclePlateTransferEvent();
        event.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            event.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        event.setDisambiguator(1L);
        event.setOperator(username);
        event.setStation(eventStationName);

        ReceptacleType denatureTube = new ReceptacleType();
        denatureTube.setBarcode(denatureTubeBarcode);
        denatureTube.setReceptacleType("tube");
        event.setSourceReceptacle(denatureTube);

        PlateType flowcell = new PlateType();
        flowcell.setBarcode(flowcellBarcode);
        flowcell.setPhysType(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell.getAutomationName());
        flowcell.setSection(SBSSection.ALL2.getSectionName());
        event.setDestinationPlate(flowcell);

        return event;
    }

    /**
     * Create and populate a new MiSeqReagentKit PlateType.
     * @param reagentKitBarcode miSeqReagentKit barcode.
     * @return Returns a new fully populated PlateType.
     */
    private PlateType buildReagentKitPlateType(String reagentKitBarcode) {
        PlateType kit = new PlateType();
        kit.setBarcode(reagentKitBarcode);
        kit.setPhysType(LabEventFactory.PHYS_TYPE_REAGENT_BLOCK);
        kit.setSection(MiSeqReagentKit.LOADING_WELL.name());
        return kit;
    }

    /**
     * Create and populate anew Rack PlateType
     * @param rackBarcode The barcode of the Rack.
     * @return Returns a new fully populated PlateType.
     */
    private PlateType buildRackPlateType(String rackBarcode) {
        PlateType rack = new PlateType();
        rack.setBarcode(rackBarcode);
        rack.setPhysType(LabEventFactory.PHYS_TYPE_TUBE_RACK);
        rack.setSection(LabEventFactory.SECTION_ALL_96);
        return rack;
    }
}
