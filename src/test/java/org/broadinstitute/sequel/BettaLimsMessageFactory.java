package org.broadinstitute.sequel;

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
        @SuppressWarnings({"NumericCastThatLosesPrecision"})
        char row = (char) ((int) 'A' + (positionNumber / NUMBER_OF_RACK_COLUMNS));
        int column = positionNumber % NUMBER_OF_RACK_COLUMNS;
        if(column == 0) {
            column = NUMBER_OF_RACK_COLUMNS;
            row--;
        }
        return row + String.valueOf(column);
    }

    public PlateTransferEventType buildRackToPlate(List<String> tubeBarcodes, String eventType,
            String rackBarcode, String plateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            PositionMapType sourcePositionMap = new PositionMapType();
            int rackPosition = 1;
            for (String barcode : tubeBarcodes) {
                ReceptacleType receptacleType = new ReceptacleType();
                receptacleType.setBarcode(barcode);
                receptacleType.setPosition(buildWellName(rackPosition));
                sourcePositionMap.getReceptacle().add(receptacleType);
                rackPosition++;
            }
            plateTransferEvent.setSourcePositionMap(sourcePositionMap);

            PlateType sourceRack = new PlateType();
            sourceRack.setBarcode(rackBarcode);
            sourceRack.setPhysType(PHYS_TYPE_TUBE_RACK);
            sourceRack.setSection(SECTION_ALL_96);
            plateTransferEvent.setSourcePlate(sourceRack);

            PlateType targetPlate = new PlateType();
            targetPlate.setBarcode(plateBarcode);
            targetPlate.setPhysType(PHYS_TYPE_EPPENDORF_96);
            targetPlate.setSection(SECTION_ALL_96);
            plateTransferEvent.setPlate(targetPlate);

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    public PlateTransferEventType buildPlateToPlate(String eventType, String sourcePlateBarcode, String targetPlateBarcode) {
        try {
            PlateTransferEventType plateTransferEvent = new PlateTransferEventType();
            plateTransferEvent.setEventType(eventType);
            plateTransferEvent.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
            
            PlateType sourcePlate = new PlateType();
            sourcePlate.setBarcode(sourcePlateBarcode);
            sourcePlate.setPhysType(PHYS_TYPE_TUBE_RACK);
            sourcePlate.setSection(SECTION_ALL_96);
            plateTransferEvent.setSourcePlate(sourcePlate);

            PlateType targetPlate = new PlateType();
            targetPlate.setBarcode(targetPlateBarcode);
            targetPlate.setPhysType(PHYS_TYPE_TUBE_RACK);
            targetPlate.setSection(SECTION_ALL_96);
            plateTransferEvent.setPlate(targetPlate);

            return plateTransferEvent;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
