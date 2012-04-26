package org.broadinstitute.sequel;

import org.broadinstitute.sequel.bettalims.jaxb.CherryPickSourceType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptaclePlateTransferEvent;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;
import org.broadinstitute.sequel.bettalims.jaxb.StationEventType;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A factory for BettaLIMSMessage JAXB beans
 */
public class BettaLimsMessageFactory {
    public static final int NUMBER_OF_RACK_COLUMNS = 12;

    String buildWellName(int positionNumber) {
        @SuppressWarnings("NumericCastThatLosesPrecision")
        char row = (char) ('A' + (positionNumber / NUMBER_OF_RACK_COLUMNS));
        int column = positionNumber % NUMBER_OF_RACK_COLUMNS;
        if(column == 0) {
            column = NUMBER_OF_RACK_COLUMNS;
            row--;
        }
        String columnString = String.valueOf(column);
        if(columnString.length() < 2) {
            columnString = '0' + columnString;
        }
        return row + columnString;
    }

    public PlateTransferEventType buildRackToPlate(String eventType, String rackBarcode, List<String> tubeBarcodes,
            String plateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            setStationEventData(eventType, plateTransferEvent);

            plateTransferEvent.setSourcePositionMap(buildPositionMap(tubeBarcodes));
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

            plateTransferEvent.setPositionMap(buildPositionMap(tubeBarcodes));
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

            plateTransferEvent.setSourcePositionMap(buildPositionMap(sourceTubeBarcodes));
            plateTransferEvent.setSourcePlate(buildRack(sourceRackBarcode));

            plateTransferEvent.setPositionMap(buildPositionMap(targetTubeBarcodes));
            plateTransferEvent.setPlate(buildPlate(targetRackBarcode));

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
            for (List<String> sourceTubeBarcode : sourceTubeBarcodes) {
                plateCherryPickEvent.getSourcePositionMap().add(buildPositionMap(sourceTubeBarcode));
            }

            plateCherryPickEvent.setPlate(buildPlate(targetRackBarcode));
            plateCherryPickEvent.setPositionMap(buildPositionMap(targetTubeBarcodes));

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
            for (List<String> sourceTubeBarcode : sourceTubeBarcodes) {
                plateCherryPickEvent.getSourcePositionMap().add(buildPositionMap(sourceTubeBarcode));
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

            plateTransferEvent.setSourcePlate(buildRack(sourcePlateBarcode));

            plateTransferEvent.setPlate(buildRack(targetPlateBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setStationEventData(String eventType, StationEventType plateTransferEvent) throws DatatypeConfigurationException {
        plateTransferEvent.setEventType(eventType);
        plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
    }

    private PlateType buildRack(String rackBarcode) {
        PlateType rack = new PlateType();
        rack.setBarcode(rackBarcode);
        rack.setPhysType(LabEventFactory.PHYS_TYPE_TUBE_RACK);
        rack.setSection(LabEventFactory.SECTION_ALL_96);
        return rack;
    }

    private PositionMapType buildPositionMap(List<String> tubeBarcodes) {
        PositionMapType positionMap = new PositionMapType();
        int rackPosition = 1;
        for (String barcode : tubeBarcodes) {
            addReceptacleToPositionMap(rackPosition, positionMap, barcode);
            rackPosition++;
        }
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
