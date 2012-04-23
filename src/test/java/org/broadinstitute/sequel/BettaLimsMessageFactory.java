package org.broadinstitute.sequel;

import org.broadinstitute.sequel.bettalims.jaxb.CherryPickSourceType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateCherryPickEvent;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.bettalims.jaxb.PlateType;
import org.broadinstitute.sequel.bettalims.jaxb.PositionMapType;
import org.broadinstitute.sequel.bettalims.jaxb.ReceptacleType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A factory for BettaLIMSMessage JAXB beans
 */
public class BettaLimsMessageFactory {
    public static final int NUMBER_OF_RACK_COLUMNS = 12;
    public static final String PHYS_TYPE_TUBE_RACK = "TubeRack";
    public static final String SECTION_ALL_96 = "ALL96";
    public static final String PHYS_TYPE_EPPENDORF_96 = "Eppendorf96";

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
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            plateTransferEvent.setSourcePositionMap(buildPositionMap(tubeBarcodes));
            plateTransferEvent.setSourcePlate(buildRack(rackBarcode));

            plateTransferEvent.setPlate(buildPlate(plateBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private PlateType buildRack(String rackBarcode) {
        PlateType sourceRack = new PlateType();
        sourceRack.setBarcode(rackBarcode);
        sourceRack.setPhysType(PHYS_TYPE_TUBE_RACK);
        sourceRack.setSection(SECTION_ALL_96);
        return sourceRack;
    }

    private PositionMapType buildPositionMap(List<String> tubeBarcodes) {
        PositionMapType sourcePositionMap = new PositionMapType();
        int rackPosition = 1;
        for (String barcode : tubeBarcodes) {
            addReceptacleToPositionMap(rackPosition, sourcePositionMap, barcode);
            rackPosition++;
        }
        return sourcePositionMap;
    }

    public PlateTransferEventType buildPlateToRack(String eventType, String plateBarcode, String rackBarcode,
            List<String> tubeBarcodes) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            plateTransferEvent.setSourcePlate(buildPlate(plateBarcode));

            plateTransferEvent.setPositionMap(buildPositionMap(tubeBarcodes));
            plateTransferEvent.setPlate(buildRack(rackBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private PlateType buildPlate(String plateBarcode) {
        PlateType sourcePlate = new PlateType();
        sourcePlate.setBarcode(plateBarcode);
        sourcePlate.setPhysType(PHYS_TYPE_EPPENDORF_96);
        sourcePlate.setSection(SECTION_ALL_96);
        return sourcePlate;
    }

    public PlateTransferEventType buildRackToRack(String eventType, String sourceRackBarcode, List<String> sourceTubeBarcodes,
            String targetRackBarcode, List<String> targetTubeBarcodes) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            plateTransferEvent.setSourcePositionMap(buildPositionMap(sourceTubeBarcodes));
            plateTransferEvent.setSourcePlate(buildRack(sourceRackBarcode));

            plateTransferEvent.setPositionMap(buildPositionMap(targetTubeBarcodes));
            plateTransferEvent.setPlate(buildPlate(targetRackBarcode));

            return plateTransferEvent;
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
            plateCherryPickEvent.setEventType(eventType);
            plateCherryPickEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

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

    private void addReceptacleToPositionMap(int rackPosition, PositionMapType targetPositionMap, String barcode) {
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setBarcode(barcode);
        receptacleType.setPosition(buildWellName(rackPosition));
        targetPositionMap.getReceptacle().add(receptacleType);
    }

    public PlateTransferEventType buildPlateToPlate(String eventType, String sourcePlateBarcode, String targetPlateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            plateTransferEvent.setSourcePlate(buildRack(sourcePlateBarcode));

            plateTransferEvent.setPlate(buildRack(targetPlateBarcode));

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
