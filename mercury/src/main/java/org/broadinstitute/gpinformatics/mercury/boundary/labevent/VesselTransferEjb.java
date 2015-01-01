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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;

/**
 * This class has methods for transferring stuff from one Vessel to another One.
 */
@Stateful
@RequestScoped
public class VesselTransferEjb {
    @Inject
    private LabEventDao labEventDao;
    @Inject
    private MiSeqReagentKitDao miSeqReagentKitDao;
    @Inject
    private LabEventHandler labEventHandler;
    @Inject
    private LabEventFactory labEventFactory;

    public VesselTransferEjb() {
    }

    public VesselTransferEjb(LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                             LabEventDao labEventDao) {
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.labEventDao = labEventDao;
    }

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

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (Map.Entry<String, VesselPosition> item : denatureBarcodeMap.entrySet()) {
            String tubeBarcode = item.getKey();
            BarcodedTube sourceTube = new BarcodedTube(tubeBarcode);
            VesselPosition vesselPosition = item.getValue();
            mapPositionToTube.put(vesselPosition, sourceTube);
        }

        if (denatureRackBarcode == null) {
            denatureRackBarcode = "DenatureRack" + reagentKitBarcode;
        }

        PlateType sourceTubeRack = BettaLimsObjectFactory.createPlateType(
                denatureRackBarcode, LabEventFactory.PHYS_TYPE_TUBE_RACK, LabEventFactory.SECTION_ALL_96, null);

        // source plate: denature rack
        transferEvent.getSourcePlate().add(sourceTubeRack);
        List<ReceptacleType> sourceReceptacles = new ArrayList<>();

        for (Map.Entry<String, VesselPosition> entry : denatureBarcodeMap.entrySet()) {
            String sourceBarcode = entry.getKey();
            String sourcePositionName = entry.getValue().name();
            ReceptacleType sourceReceptacle = BettaLimsObjectFactory.createReceptacleType(
                    sourceBarcode, "tube", sourcePositionName);
            sourceReceptacles.add(sourceReceptacle);
            CherryPickSourceType cherryPickSource = BettaLimsObjectFactory.createCherryPickSourceType(
                    denatureRackBarcode,
                    sourcePositionName,
                    reagentKitBarcode,
                    MiSeqReagentKit.LOADING_WELL.name());
            transferEvent.getSource().add(cherryPickSource);
        }
        // where on rack is tube?
        transferEvent.getSourcePositionMap().add(BettaLimsObjectFactory.createPositionMapType(
                denatureRackBarcode, sourceReceptacles));
        // target plate
        PlateType reagentKitType = BettaLimsObjectFactory.createPlateType(reagentKitBarcode,
                LabEventFactory.PHYS_TYPE_REAGENT_BLOCK, MiSeqReagentKit.LOADING_WELL.name(), null);
        transferEvent.getPlate().add(reagentKitType);
        bettaLIMSMessage.getPlateCherryPickEvent().add(transferEvent);
        return bettaLIMSMessage;
    }


    /**
     * Transfer contents of MiSeq Reagent Kit to a MiSeq Flowcell.  THis method builds a reagent kit to flowcell
     * transfer message and utilizes the LabEvent Factory to build and persist a labEvent for that transfer.
     *
     * @param reagentKitBarcode barcode of the reagent kit
     * @param flowcellBarcode   flowcell barcode
     * @param username          user performing action.
     * @param stationName       where the transfer occurred (UI, robot, etc)
     *
     * @return fully persisted labEvent
     */
    public LabEvent reagentKitToFlowcell(@Nonnull String reagentKitBarcode, @Nonnull String flowcellBarcode,
                                         @Nonnull String username, @Nonnull String stationName) {

        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        PlateCherryPickEvent transferEvent =
                labEventFactory
                        .getReagentToFlowcellEventDBFree(reagentKitBarcode, flowcellBarcode, username, stationName);
        bettaLIMSMessage.getPlateCherryPickEvent().add(transferEvent);

        LabEvent labEvent = labEventFactory.buildFromBettaLims(bettaLIMSMessage).iterator().next();
        labEventHandler.processEvent(labEvent);
        labEventDao.persist(labEvent);
        return labEvent;
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
        flowcell.setPhysType(HiSeq2500Flowcell.getAutomationName());
        flowcell.setSection(SBSSection.ALL2.getSectionName());
        event.setDestinationPlate(flowcell);

        return event;
    }
}
