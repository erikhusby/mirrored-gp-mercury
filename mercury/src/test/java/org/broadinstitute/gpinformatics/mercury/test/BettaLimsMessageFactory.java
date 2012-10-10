package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This class is a factory for BettaLIMSMessage JAXB objects.  It is intended to facilitate building messages in test cases.
 */
public class BettaLimsMessageFactory {
    public static final int NUMBER_OF_RACK_COLUMNS = 12;

    private long time = System.currentTimeMillis();

    /**
     * LabEvent has a unique constraint that includes a timestamp, so database test code must advance the timestamp
     * after each message.  The timestamp does not need to be advanced for each event within a message, because
     * each event has a disambiguator.
     */
    public void advanceTime() {
        time++;
    }

    public String buildWellName(int positionNumber) {
        @SuppressWarnings("NumericCastThatLosesPrecision")
        char row = (char) ('A' + (positionNumber / NUMBER_OF_RACK_COLUMNS));
        int column = positionNumber % NUMBER_OF_RACK_COLUMNS;
        if(column == 0) {
            column = NUMBER_OF_RACK_COLUMNS;
            row--;
        }
        String columnString = String.valueOf(column);
        return row + columnString;
    }

    public PlateTransferEventType buildRackToPlate(String eventType, String rackBarcode, Collection<String> tubeBarcodes,
            String plateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

            plateTransferEvent.setSourcePositionMap(buildPositionMap(rackBarcode, tubeBarcodes));
            plateTransferEvent.setSourcePlate(buildRack(rackBarcode));

            plateTransferEvent.setPlate(buildPlate(plateBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateTransferEventType buildPlateToRack(String eventType, String plateBarcode, String rackBarcode,
            List<String> tubeBarcodes) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

            plateTransferEvent.setSourcePlate(buildPlate(plateBarcode));

            plateTransferEvent.setPositionMap(buildPositionMap(rackBarcode, tubeBarcodes));
            plateTransferEvent.setPlate(buildRack(rackBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateTransferEventType buildRackToRack(String eventType, String sourceRackBarcode, List<String> sourceTubeBarcodes,
            String targetRackBarcode, List<String> targetTubeBarcodes) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

            plateTransferEvent.setSourcePositionMap(buildPositionMap(sourceRackBarcode, sourceTubeBarcodes));
            plateTransferEvent.setSourcePlate(buildRack(sourceRackBarcode));

            plateTransferEvent.setPositionMap(buildPositionMap(targetRackBarcode, targetTubeBarcodes));
            plateTransferEvent.setPlate(buildRack(targetRackBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public ReceptaclePlateTransferEvent buildTubeToPlate(String eventType, String sourceTubeBarcode, String targetPlateBarcode) {
        try {
            ReceptaclePlateTransferEvent receptaclePlateTransferEvent = new ReceptaclePlateTransferEvent();
            setStationEventData(eventType, receptaclePlateTransferEvent);

            ReceptacleType sourceReceptacle = new ReceptacleType();
            sourceReceptacle.setBarcode(sourceTubeBarcode);
            receptaclePlateTransferEvent.setSourceReceptacle(sourceReceptacle);
            receptaclePlateTransferEvent.setDestinationPlate(buildPlate(targetPlateBarcode));

            return receptaclePlateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateEventType buildPlateEvent(String eventType, String plateBarcode) {
        try {
            PlateEventType plateEventType = new PlateEventType();
            setStationEventData(eventType, plateEventType);

            plateEventType.setPlate(buildPlate(plateBarcode));

            return plateEventType;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateEventType buildRackEvent(String eventType, String rackBarcode, List<String> tubeBarcodes) {
        try {
            PlateEventType rackEvent = new PlateEventType();
            setStationEventData(eventType, rackEvent);

            rackEvent.setPlate(buildRack(rackBarcode));
            rackEvent.setPositionMap(buildPositionMap(rackBarcode, tubeBarcodes));

            return rackEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CherryPick {
        private final String sourceRackBarcode;
        private final String sourceWell;
        private final String destinationRackBarcode;
        private final String destinationWell;

        CherryPick(String sourceRackBarcode, String sourceWell, String destinationRackBarcode, String destinationWell) {
            this.sourceRackBarcode = sourceRackBarcode;
            this.sourceWell = sourceWell;
            this.destinationRackBarcode = destinationRackBarcode;
            this.destinationWell = destinationWell;
        }

        public String getSourceRackBarcode() {
            return this.sourceRackBarcode;
        }

        public String getSourceWell() {
            return this.sourceWell;
        }

        public String getDestinationRackBarcode() {
            return this.destinationRackBarcode;
        }

        public String getDestinationWell() {
            return this.destinationWell;
        }
    }

    public PlateCherryPickEvent buildCherryPick(String eventType, List<String> sourceRackBarcodes,
            List<List<String>> sourceTubeBarcodes, String targetRackBarcode, List<String> targetTubeBarcodes,
            List<CherryPick> cherryPicks) {
        try {
            PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
            setStationEventData(eventType, plateCherryPickEvent);

            for (String sourceRackBarcode : sourceRackBarcodes) {
                plateCherryPickEvent.getSourcePlate().add(buildRack(sourceRackBarcode));
            }
            for (int i = 0, sourceTubeBarcodesSize = sourceTubeBarcodes.size(); i < sourceTubeBarcodesSize; i++) {
                List<String> sourceTubeBarcode = sourceTubeBarcodes.get(i);
                plateCherryPickEvent.getSourcePositionMap().add(buildPositionMap(sourceRackBarcodes.get(i), sourceTubeBarcode));
            }

            plateCherryPickEvent.setPlate(buildPlate(targetRackBarcode));
            plateCherryPickEvent.setPositionMap(buildPositionMap(targetRackBarcode, targetTubeBarcodes));

            for (CherryPick cherryPick : cherryPicks) {
                CherryPickSourceType cherryPickSource = new CherryPickSourceType();
                cherryPickSource.setBarcode(cherryPick.getSourceRackBarcode());
                cherryPickSource.setWell(cherryPick.getSourceWell());
                cherryPickSource.setDestinationBarcode(cherryPick.getDestinationRackBarcode());
                cherryPickSource.setDestinationWell(cherryPick.getDestinationWell());
                plateCherryPickEvent.getSource().add(cherryPickSource);
            }

            return plateCherryPickEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateCherryPickEvent buildCherryPickToStripTube(String eventType, List<String> sourceRackBarcodes,
            List<List<String>> sourceTubeBarcodes, String targetRackBarcode, List<String> targetStripTubeBarcodes,
            List<CherryPick> cherryPicks) {
        try {
            PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
            setStationEventData(eventType, plateCherryPickEvent);

            for (String sourceRackBarcode : sourceRackBarcodes) {
                plateCherryPickEvent.getSourcePlate().add(buildRack(sourceRackBarcode));
            }
            for (int i = 0, sourceTubeBarcodesSize = sourceTubeBarcodes.size(); i < sourceTubeBarcodesSize; i++) {
                List<String> sourceTubeBarcode = sourceTubeBarcodes.get(i);
                plateCherryPickEvent.getSourcePositionMap().add(buildPositionMap(sourceRackBarcodes.get(i), sourceTubeBarcode));
            }

            PlateType targetRack = new PlateType();
            targetRack.setBarcode(targetRackBarcode);
            targetRack.setPhysType(LabEventFactory.PHYS_TYPE_STRIP_TUBE_RACK_OF_12);
            targetRack.setSection(LabEventFactory.SECTION_ALL_96);
            plateCherryPickEvent.setPlate(targetRack);

            PositionMapType targetPositionMap = new PositionMapType();
            int rackPosition = 1;
            for (String barcode : targetStripTubeBarcodes) {
                ReceptacleType receptacleType = new ReceptacleType();
                receptacleType.setReceptacleType(LabEventFactory.PHYS_TYPE_STRIP_TUBE);
                receptacleType.setBarcode(barcode);
                receptacleType.setPosition(Integer.toString(rackPosition));
                targetPositionMap.getReceptacle().add(receptacleType);
                rackPosition++;
            }
            plateCherryPickEvent.setPositionMap(targetPositionMap);

            for (CherryPick cherryPick : cherryPicks) {
                CherryPickSourceType cherryPickSource = new CherryPickSourceType();
                cherryPickSource.setBarcode(cherryPick.getSourceRackBarcode());
                cherryPickSource.setWell(cherryPick.getSourceWell());
                cherryPickSource.setDestinationBarcode(cherryPick.getDestinationRackBarcode());
                cherryPickSource.setDestinationWell(cherryPick.getDestinationWell());
                plateCherryPickEvent.getSource().add(cherryPickSource);
            }

            return plateCherryPickEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateTransferEventType buildPlateToPlate(String eventType, String sourcePlateBarcode, String targetPlateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

            plateTransferEvent.setSourcePlate(buildPlate(sourcePlateBarcode));

            plateTransferEvent.setPlate(buildPlate(targetPlateBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public PlateTransferEventType buildStripTubeToFlowcell(String eventType, String stripTubeBarcode, String flowcellBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

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
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setStationEventData(String eventType, StationEventType plateTransferEvent) throws DatatypeConfigurationException {
        plateTransferEvent.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(time);
        plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        plateTransferEvent.setDisambiguator(1L);
        plateTransferEvent.setOperator("jowalsh");
        plateTransferEvent.setStation("ZAN");
    }

    private PlateType buildRack(String rackBarcode) {
        PlateType rack = new PlateType();
        rack.setBarcode(rackBarcode);
        rack.setPhysType(LabEventFactory.PHYS_TYPE_TUBE_RACK);
        rack.setSection(LabEventFactory.SECTION_ALL_96);
        return rack;
    }

    private PositionMapType buildPositionMap(String rackBarcode, Collection<String> tubeBarcodes) {
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
        receptacleType.setPosition(buildWellName(rackPosition));
        targetPositionMap.getReceptacle().add(receptacleType);
    }
}
