package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.StringWriter;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a factory for BettaLIMSMessage JAXB objects.  It is intended to facilitate building messages in test cases.
 */
@SuppressWarnings("FeatureEnvy")
public class BettaLimsMessageTestFactory {
    public static final int NUMBER_OF_RACK_COLUMNS = 12;
    public static final String HISEQ_SEQUENCING_STATION_MACHINE_NAME = "SL-HBU";
    public static final String MISEQ_SEQUENCING_STATION_MACHINE_NAME = "SL-MAA";

    private long time = System.currentTimeMillis();
    /** True if the mode element in the messages should be set to Mercury. This causes all messages to bypass
     * routing logic and be processed by Mercury. */
    private final boolean mercuryMode;

    public BettaLimsMessageTestFactory(boolean mercuryMode) {
        this.mercuryMode = mercuryMode;
    }

    public static String marshal(BettaLIMSMessage blmJaxbObject) {
        try {
            JAXBContext jc = JAXBContext.newInstance(BettaLIMSMessage.class);

            Marshaller marsh = jc.createMarshaller();

            StringWriter writer = new StringWriter();

            marsh.marshal(blmJaxbObject, writer);

            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds one or more station events to a list of bettalims messages.  Advances the time, to avoid unique
     * constraint violations.
     * @param messageList list of bettalims messages, typically sent to the BettaLIMS web service, in a loop
     * @param stationEventTypes one or more station events
     * @return the created BettaLIMS message
     */
    public BettaLIMSMessage addMessage(List<BettaLIMSMessage> messageList, StationEventType... stationEventTypes) {
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        if (mercuryMode) {
            bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        }
        for (StationEventType stationEventType : stationEventTypes) {
            if (stationEventType instanceof PlateTransferEventType) {
                bettaLIMSMessage.getPlateTransferEvent().add((PlateTransferEventType) stationEventType);
            } else if (stationEventType instanceof PlateCherryPickEvent) {
                bettaLIMSMessage.getPlateCherryPickEvent().add((PlateCherryPickEvent) stationEventType);
            } else if (stationEventType instanceof PlateEventType) {
                bettaLIMSMessage.getPlateEvent().add((PlateEventType) stationEventType);
            } else if (stationEventType instanceof ReceptaclePlateTransferEvent) {
                bettaLIMSMessage.getReceptaclePlateTransferEvent().add((ReceptaclePlateTransferEvent) stationEventType);
            } else if (stationEventType instanceof ReceptacleEventType) {
                bettaLIMSMessage.getReceptacleEvent().add((ReceptacleEventType) stationEventType);
            } else {
                throw new RuntimeException("Unknown station event type " + stationEventType);
            }
        }
        messageList.add(bettaLIMSMessage);
        advanceTime();
        return bettaLIMSMessage;
    }

    /**
     * LabEvent has a unique constraint that includes a timestamp, so database test code must advance the timestamp
     * after each message.  The timestamp does not need to be advanced for each event within a message, because
     * each event has a disambiguator.
     */
    public void advanceTime() {
        // The Mercury constraint has millisecond resolution, but the Squid equivalent has second resolution, so to
        // allow cross system testing we advance by 1 second.
        time += 1000L;
    }

    public enum WellNameType {
        /** long form of well name, e.g. A01 */
        LONG, /**
         * long form of well name, e.g. A01
         */
        SHORT 
    }

    public String buildWellName(int positionNumber, WellNameType wellNameType) {
        @SuppressWarnings("NumericCastThatLosesPrecision")
        char row = (char) ('A' + (positionNumber / NUMBER_OF_RACK_COLUMNS));
        int column = positionNumber % NUMBER_OF_RACK_COLUMNS;
        if (column == 0) {
            column = NUMBER_OF_RACK_COLUMNS;
            row--;
        }
        String columnString = String.valueOf(column);
        if (wellNameType == WellNameType.LONG) {
            if (columnString.length() == 1) {
                columnString = "0" + columnString;
            }
        }
        return row + columnString;
    }

    public PlateTransferEventType buildRackToPlate(String eventType, String rackBarcode, List<String> tubeBarcodes,
            String plateBarcode) {
        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        setStationEventData(eventType, plateTransferEvent);

        plateTransferEvent.setSourcePositionMap(buildPositionMap(rackBarcode, tubeBarcodes));
        plateTransferEvent.setSourcePlate(buildRack(rackBarcode));

        plateTransferEvent.setPlate(buildPlate(plateBarcode));

        return plateTransferEvent;
    }

    public PlateTransferEventType buildRackToPlate(String eventType, String rackBarcode, List<String> tubeBarcodes,
                                                   String plateBarcode, String plateType, String sourceSection,
                                                   String targetSection) {
        PlateTransferEventType event = buildRackToPlate(eventType, rackBarcode, tubeBarcodes, plateBarcode);
        event.getSourcePlate().setSection(sourceSection);
        event.getPlate().setSection(targetSection);
        event.getPlate().setPhysType(plateType);
        return event;
    }

    public PlateTransferEventType buildPlateToRack(String eventType, String plateBarcode, String rackBarcode,
            List<String> tubeBarcodes) {
        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        setStationEventData(eventType, plateTransferEvent);

        plateTransferEvent.setSourcePlate(buildPlate(plateBarcode));

        plateTransferEvent.setPositionMap(buildPositionMap(rackBarcode, tubeBarcodes));
        plateTransferEvent.setPlate(buildRack(rackBarcode));

        return plateTransferEvent;
    }

    public PlateTransferEventType buildRackToRack(String eventType, String sourceRackBarcode, List<String> sourceTubeBarcodes,
            String targetRackBarcode, List<String> targetTubeBarcodes) {
        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        setStationEventData(eventType, plateTransferEvent);

        plateTransferEvent.setSourcePositionMap(buildPositionMap(sourceRackBarcode, sourceTubeBarcodes));
        plateTransferEvent.setSourcePlate(buildRack(sourceRackBarcode));

        plateTransferEvent.setPositionMap(buildPositionMap(targetRackBarcode, targetTubeBarcodes));
        plateTransferEvent.setPlate(buildRack(targetRackBarcode));

        return plateTransferEvent;
    }

    public ReceptaclePlateTransferEvent buildTubeToPlate(String eventType, String sourceTubeBarcode,
            String targetPlateBarcode, String physType, String section, String receptacleType) {
        ReceptaclePlateTransferEvent receptaclePlateTransferEvent = new ReceptaclePlateTransferEvent();
        setStationEventData(eventType, receptaclePlateTransferEvent);
        if(physType.equals(LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE)) {
            receptaclePlateTransferEvent.setStation(HISEQ_SEQUENCING_STATION_MACHINE_NAME);
        }

        ReceptacleType sourceReceptacle = new ReceptacleType();
        sourceReceptacle.setBarcode(sourceTubeBarcode);
        sourceReceptacle.setReceptacleType(receptacleType);
        receptaclePlateTransferEvent.setSourceReceptacle(sourceReceptacle);

        PlateType plate = new PlateType();
        plate.setBarcode(targetPlateBarcode);
        plate.setPhysType(physType);
        plate.setSection(section);
        receptaclePlateTransferEvent.setDestinationPlate(plate);

        return receptaclePlateTransferEvent;
    }

    public PlateEventType buildPlateEvent(String eventType, String plateBarcode) {
        PlateEventType plateEventType = new PlateEventType();
        setStationEventData(eventType, plateEventType);

        plateEventType.setPlate(buildPlate(plateBarcode));

        return plateEventType;
    }

    public PlateEventType buildRackEvent(String eventType, String rackBarcode, List<String> tubeBarcodes) {
        PlateEventType rackEvent = new PlateEventType();
        setStationEventData(eventType, rackEvent);

        rackEvent.setPlate(buildRack(rackBarcode));
        rackEvent.setPositionMap(buildPositionMap(rackBarcode, tubeBarcodes));

        return rackEvent;
    }

    public ReceptacleEventType buildReceptacleEvent(String eventType, String tubeBarcode, String tubeType) {
        ReceptacleEventType receptacleEventType = new ReceptacleEventType();
        setStationEventData(eventType, receptacleEventType);
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setBarcode(tubeBarcode);
        receptacleType.setReceptacleType(tubeType);
        receptacleEventType.setReceptacle(receptacleType);
        return receptacleEventType;
    }


    /**
     * Create a PlateCherryPickEvent for transferring from a rack of tubes to a ReagentKit.
     *
     * @param eventType                     Event type of transfer
     * @param rackOfTubes                   List of RackOfTubes entities in the rack;
     *                                      newly created RackOfTubes will NOT be added to this map
     * @param reagentKitBarcode             barcode of the reagentKit.
     *
     * @return the LabEvent object
     */
    public Collection<PlateCherryPickEvent> buildCherryPickToReagentKit(String eventType,
                                                            RackOfTubes rackOfTubes,
                                                            String reagentKitBarcode) {
        PlateCherryPickEvent stationEvent = new PlateCherryPickEvent();

        setStationEventData(eventType, stationEvent);
        Map<String, VesselPosition> denatureBarcodeMap=new HashMap<>();
        VesselTransferEjb transferBean = new VesselTransferEjb();

        for (TubeFormation tubeFormation : rackOfTubes.getTubeFormations()) {
            for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPosition : tubeFormation.getContainerRole().getMapPositionToVessel().entrySet()) {
                denatureBarcodeMap.put(vesselPosition.getValue().getLabel(), vesselPosition.getKey());
            }
        }

        return transferBean.denatureToReagentKitTransfer(rackOfTubes.getLabel(), denatureBarcodeMap, reagentKitBarcode,
                stationEvent.getOperation(), stationEvent.getStation()).getPlateCherryPickEvent();
    }

    public PlateCherryPickEvent buildCherryPick(String eventType, List<String> sourceRackBarcodes,
            List<List<String>> sourceTubeBarcodes, List<String> targetRackBarcodes, List<List<String>> targetTubeBarcodes,
            List<CherryPick> cherryPicks) {
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        setStationEventData(eventType, plateCherryPickEvent);

        for (String sourceRackBarcode : sourceRackBarcodes) {
            plateCherryPickEvent.getSourcePlate().add(buildRack(sourceRackBarcode));
        }
        for (int i = 0, sourceTubeBarcodesSize = sourceTubeBarcodes.size(); i < sourceTubeBarcodesSize; i++) {
            List<String> sourceTubeBarcode = sourceTubeBarcodes.get(i);
            plateCherryPickEvent.getSourcePositionMap()
                    .add(buildPositionMap(sourceRackBarcodes.get(i), sourceTubeBarcode));
        }

        for (String targetRackBarcode : targetRackBarcodes) {
            plateCherryPickEvent.getPlate().add(buildRack(targetRackBarcode));
        }

        for (int i = 0, targetTubeBarcodesSize = targetTubeBarcodes.size(); i < targetTubeBarcodesSize; i++) {
            List<String> targetTubeBarcode = targetTubeBarcodes.get(i);
            plateCherryPickEvent.getPositionMap().add(buildPositionMap(targetRackBarcodes.get(i), targetTubeBarcode));
        }

        for (CherryPick cherryPick : cherryPicks) {
            CherryPickSourceType cherryPickSource = new CherryPickSourceType();
            cherryPickSource.setBarcode(cherryPick.getSourceRackBarcode());
            cherryPickSource.setWell(cherryPick.getSourceWell());
            cherryPickSource.setDestinationBarcode(cherryPick.getDestinationRackBarcode());
            cherryPickSource.setDestinationWell(cherryPick.getDestinationWell());
            plateCherryPickEvent.getSource().add(cherryPickSource);
        }

        return plateCherryPickEvent;
    }

    public PlateCherryPickEvent buildCherryPickToStripTube(String eventType, List<String> sourceRackBarcodes,
            List<List<String>> sourceTubeBarcodes, String targetRackBarcode, List<String> targetStripTubeBarcodes,
            List<CherryPick> cherryPicks) {
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        setStationEventData(eventType, plateCherryPickEvent);

        for (String sourceRackBarcode : sourceRackBarcodes) {
            plateCherryPickEvent.getSourcePlate().add(buildRack(sourceRackBarcode));
        }
        for (int i = 0, sourceTubeBarcodesSize = sourceTubeBarcodes.size(); i < sourceTubeBarcodesSize; i++) {
            List<String> sourceTubeBarcode = sourceTubeBarcodes.get(i);
            plateCherryPickEvent.getSourcePositionMap().add(
                    buildPositionMap(sourceRackBarcodes.get(i), sourceTubeBarcode));
        }

        PlateType targetRack = new PlateType();
        targetRack.setBarcode(targetRackBarcode);
        targetRack.setPhysType(LabEventFactory.PHYS_TYPE_STRIP_TUBE_RACK_OF_12);
        targetRack.setSection(LabEventFactory.SECTION_ALL_96);
        plateCherryPickEvent.getPlate().add(targetRack);

        PositionMapType targetPositionMap = new PositionMapType();
        targetPositionMap.setBarcode(targetRack.getBarcode());
        int rackPosition = 1;
        for (String barcode : targetStripTubeBarcodes) {
            ReceptacleType receptacleType = new ReceptacleType();
            receptacleType.setReceptacleType(LabEventFactory.PHYS_TYPE_STRIP_TUBE);
            receptacleType.setBarcode(barcode);
            receptacleType.setPosition(Integer.toString(rackPosition));
            targetPositionMap.getReceptacle().add(receptacleType);
            rackPosition++;
        }
        plateCherryPickEvent.getPositionMap().add(targetPositionMap);

        for (CherryPick cherryPick : cherryPicks) {
            CherryPickSourceType cherryPickSource = new CherryPickSourceType();
            cherryPickSource.setBarcode(cherryPick.getSourceRackBarcode());
            cherryPickSource.setWell(cherryPick.getSourceWell());
            cherryPickSource.setDestinationBarcode(cherryPick.getDestinationRackBarcode());
            cherryPickSource.setDestinationWell(cherryPick.getDestinationWell());
            plateCherryPickEvent.getSource().add(cherryPickSource);
        }

        return plateCherryPickEvent;
    }

    public PlateTransferEventType buildPlateToPlate(String eventType, String sourcePlateBarcode, String targetPlateBarcode) {
        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        setStationEventData(eventType, plateTransferEvent);

        plateTransferEvent.setSourcePlate(buildPlate(sourcePlateBarcode));

        plateTransferEvent.setPlate(buildPlate(targetPlateBarcode));

        return plateTransferEvent;
    }

    public PlateTransferEventType buildStripTubeToFlowcell(String eventType, String stripTubeBarcode, String flowcellBarcode) {
        PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
        setStationEventData(eventType, plateTransferEvent);
        plateTransferEvent.setStation(HISEQ_SEQUENCING_STATION_MACHINE_NAME);

        PlateType stripTube = new PlateType();
        stripTube.setBarcode(stripTubeBarcode);
        stripTube.setPhysType(LabEventFactory.PHYS_TYPE_STRIP_TUBE);
        stripTube.setSection(LabEventFactory.SECTION_ALL_96);
        plateTransferEvent.setSourcePlate(stripTube);

        PlateType flowcell = new PlateType();
        flowcell.setBarcode(flowcellBarcode);
        flowcell.setPhysType(LabEventFactory.PHYS_TYPE_FLOWCELL);
        flowcell.setSection(LabEventFactory.SECTION_ALL_96);
        plateTransferEvent.setPlate(flowcell);

        return plateTransferEvent;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ReceptaclePlateTransferEvent buildDenatureTubeToFlowcell(String eventType, String denatureTubeBarcode,
                                                                    String flowcellBarcode, String flowcellType,
                                                                    String flowcellSection) {
        ReceptaclePlateTransferEvent event = new ReceptaclePlateTransferEvent();
        setStationEventData(eventType, event);

        ReceptacleType denatureTube = new ReceptacleType();
        denatureTube.setBarcode(denatureTubeBarcode);
        denatureTube.setReceptacleType("tube");
        event.setSourceReceptacle(denatureTube);

        PlateType flowcell = new PlateType();
        flowcell.setBarcode(flowcellBarcode);
        flowcell.setPhysType(flowcellType);
        flowcell.setSection(flowcellSection);
        event.setDestinationPlate(flowcell);

        return event;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlateEventType buildFlowcellEvent(String eventType, String flowcellBarcode) {

        PlateEventType flowcellEvent = new PlateEventType();
        setStationEventData(eventType, flowcellEvent);

        PlateType flowcell = new PlateType();
        flowcell.setBarcode(flowcellBarcode);
        flowcell.setPhysType(LabEventFactory.PHYS_TYPE_FLOWCELL);
        flowcell.setSection(LabEventFactory.SECTION_ALL_96);
        flowcellEvent.setPlate(flowcell);

        return flowcellEvent;
    }

    private void setStationEventData(String eventType, StationEventType plateTransferEvent) {
        plateTransferEvent.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(time);
        try {
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        plateTransferEvent.setDisambiguator(1L);
        plateTransferEvent.setOperator("pdunlea");
        plateTransferEvent.setStation("ZAN");
        plateTransferEvent.setProgram("Bravo");
    }

    private PlateType buildRack(String rackBarcode) {
        PlateType rack = new PlateType();
        rack.setBarcode(rackBarcode);
        rack.setPhysType(LabEventFactory.PHYS_TYPE_TUBE_RACK);
        rack.setSection(LabEventFactory.SECTION_ALL_96);
        return rack;
    }

    private PositionMapType buildPositionMap(String rackBarcode, List<String> tubeBarcodes) {
        PositionMapType positionMap = new PositionMapType();
        int rackPosition = 1;
        for (String barcode : tubeBarcodes) {
            addReceptacleToPositionMap(rackPosition, positionMap, barcode);
            rackPosition++;
        }
        positionMap.setBarcode(rackBarcode);
        return positionMap;
    }

    private PlateType buildPlate(String plateBarcode) {
        PlateType plate = new PlateType();
        plate.setBarcode(plateBarcode);
        plate.setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        plate.setSection(LabEventFactory.SECTION_ALL_96);
        return plate;
    }

    private void addReceptacleToPositionMap(int rackPosition, PositionMapType targetPositionMap, String barcode) {
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setBarcode(barcode);
        receptacleType.setPosition(buildWellName(rackPosition, WellNameType.SHORT));
        receptacleType.setReceptacleType("tube");
        receptacleType.setConcentration(12.2);
        receptacleType.setVolume(8.3);
        targetPositionMap.getReceptacle().add(receptacleType);
    }

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
}
