package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.Rack;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.RackPosition;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for tube barcode/ well pairs to output vessel barcode from Qiagen Rack File output
 */
public class QiagenRackFileParser {
    private static final Log logger = LogFactory.getLog(QiagenRackFileParser.class);

    public List<StationEventType> parse(Map<String, String> positionBarcodeMap, InputStream inputStream,
                                        MessageCollection messageCollection) {
        Rack rack = null;
        try {
            rack = unmarshal(inputStream);
        } catch (JAXBException e) {
            logger.error("Failed to unmarshal XML file", e);
            messageCollection.addError("Failed to unmarshal XML file");
            return null;
        }
        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();

        //Generate four random source plate barcodes
        String platePrefix = "QiagenSampleCarrier24";
        String sdf = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String sourcePlateBarcode = platePrefix + "_" + sdf;
        Map<String, List<RackPosition>> sourceContainerToRackPosition = new HashMap<>();
        for (RackPosition rackPosition: rack.getRackPosition()) {
            if (rackPosition.getSampleId().getValue() != null && !rackPosition.getSampleId().getValue().isEmpty()) {
                if (!sourceContainerToRackPosition.containsKey(sourcePlateBarcode)){
                    sourceContainerToRackPosition.put(sourcePlateBarcode, new ArrayList<RackPosition>());
                }
                sourceContainerToRackPosition.get(sourcePlateBarcode).add(rackPosition);
            }
        }

        PlateType destinationPlate = new PlateType();
        destinationPlate.setPhysType("TubeRack");
        destinationPlate.setBarcode(rack.getRackId().getValue());
        PositionMapType destinationPositionMap = new PositionMapType();
        destinationPositionMap.setBarcode(rack.getRackId().getValue());
        plateCherryPickEvent.getPlate().add(destinationPlate);
        plateCherryPickEvent.getPositionMap().add(destinationPositionMap);

        for (Map.Entry<String, List<RackPosition>> entry: sourceContainerToRackPosition.entrySet()) {
            String sourceBarcode = entry.getKey();
            PlateType plateType = new PlateType();
            plateType.setBarcode(sourceBarcode);
            plateType.setPhysType(RackOfTubes.RackType.QiasymphonyCarrier24.getDisplayName());
            PositionMapType positionMapType = new PositionMapType();
            positionMapType.setBarcode(sourceBarcode);
            VesselPosition[] vesselPositions =
                    RackOfTubes.RackType.QiasymphonyCarrier24.getVesselGeometry().getVesselPositions();
            for (RackPosition rackPosition: entry.getValue()) {
                ReceptacleType sourceReceptacleType = new ReceptacleType();
                sourceReceptacleType.setBarcode(rackPosition.getSampleId().getValue());
                int posIdx = rackPosition.getPositionIndex().getValue();
                String sourcePosition = vesselPositions[posIdx].name();
                sourceReceptacleType.setPosition(sourcePosition);
                sourceReceptacleType.setVolume(new BigDecimal(rackPosition.getTotalVolumeInUl().getValue()));
                positionMapType.getReceptacle().add(sourceReceptacleType);

                String destinationWell = rackPosition.getPositionName().getValue().replaceAll(":", "");
                ReceptacleType destinationReceptacleType = new ReceptacleType();
                destinationReceptacleType.setPosition(destinationWell);
                if (!positionBarcodeMap.containsKey(destinationWell)) {
                    messageCollection.addError("Missing well in rack scan " + destinationWell);
                } else {
                    destinationReceptacleType.setBarcode(positionBarcodeMap.get(destinationWell));
                    destinationPositionMap.getReceptacle().add(destinationReceptacleType);
                    CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
                    cherryPickSourceType.setBarcode(sourceBarcode);
                    cherryPickSourceType.setWell(sourcePosition);
                    cherryPickSourceType.setDestinationWell(destinationWell);
                    plateCherryPickEvent.getSource().add(cherryPickSourceType);
                }
            }
            plateCherryPickEvent.getSourcePlate().add(plateType);
            plateCherryPickEvent.getSourcePositionMap().add(positionMapType);
        }

        List<StationEventType> events = new ArrayList<>();
        events.add(plateCherryPickEvent);

        return events;
    }

//    @Override
//    public List<StationEventType> parse(InputStream inputStream, MessageCollection messageCollection) {
//        Rack rack = null;
//        try {
//            rack = unmarshal(inputStream);
//        } catch (JAXBException e) {
//            logger.error("Failed to unmarshal XML file", e);
//            messageCollection.addError("Failed to unmarshal XML file");
//            return null;
//        }
//        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
//
//        //Generate four random source plate barcodes
//        String platePrefix = "QiagenSampleCarrier24";
//        String sdf = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        List<String> sourcePlateBarcodes = Arrays.asList(
//                platePrefix + "_1_" + sdf,
//                platePrefix + "_2_" + sdf,
//                platePrefix + "_3_" + sdf,
//                platePrefix + "_4_" + sdf
//        );
//
//        Map<String, List<RackPosition>> sourceContainerToRackPosition = new HashMap<>();
//        for (RackPosition rackPosition: rack.getRackPosition()) {
//            if (rackPosition.getSampleId().getValue() != null && !rackPosition.getSampleId().getValue().isEmpty()) {
//                int posIndex = rackPosition.getPositionIndex().getValue();
//                int sourcePlateBarcodeIndex = posIndex / 32;
//                String sourcePlateBarcode = sourcePlateBarcodes.get(sourcePlateBarcodeIndex);
//                if (!sourceContainerToRackPosition.containsKey(sourcePlateBarcode)){
//                    sourceContainerToRackPosition.put(sourcePlateBarcode, new ArrayList<RackPosition>());
//                }
//                sourceContainerToRackPosition.get(sourcePlateBarcode).add(rackPosition);
//            }
//        }
//
//        PlateType destinationPlate = new PlateType();
//        destinationPlate.setPhysType("TubeRack");
//        destinationPlate.setBarcode(rack.getRackId().getValue());
//        PositionMapType destinationPositionMap = new PositionMapType();
//        destinationPositionMap.setBarcode(rack.getRackId().getValue());
//        plateCherryPickEvent.getPlate().add(destinationPlate);
//        plateCherryPickEvent.getPositionMap().add(destinationPositionMap);
//
//        //Build Sources
//        //TODO replace with carrier details
//        for (Map.Entry<String, List<RackPosition>> entry: sourceContainerToRackPosition.entrySet()) {
//            String sourcePlateBarcode = entry.getKey();
//            PlateType plateType = new PlateType();
//            plateType.setBarcode(sourcePlateBarcode);
//            plateType.setPhysType(RackOfTubes.RackType.HamiltonSampleCarrier32.getDisplayName());
//            PositionMapType positionMapType = new PositionMapType();
//            positionMapType.setBarcode(sourcePlateBarcode);
//            VesselPosition[] vesselPositions =
//                    RackOfTubes.RackType.HamiltonSampleCarrier24.getVesselGeometry().getVesselPositions();
//            for (RackPosition rackPosition: entry.getValue()) {
//                ReceptacleType sourceReceptacleType = new ReceptacleType();
//                sourceReceptacleType.setBarcode(rackPosition.getSampleId().getValue());
//                int posIdx = rackPosition.getPositionIndex().getValue() % 24;
//                String sourcePosition = vesselPositions[posIdx].name();
//                sourceReceptacleType.setPosition(sourcePosition);
//                sourceReceptacleType.setVolume(new BigDecimal(rackPosition.getTotalVolumeInUl().getValue()));
//                positionMapType.getReceptacle().add(sourceReceptacleType);
//
//                String destinationWell = rackPosition.getPositionName().getValue().replaceAll(":", "");
//                ReceptacleType destinationReceptacleType = new ReceptacleType();
//                destinationReceptacleType.setPosition(destinationWell);
//                if (!positionBarcodeMap.containsKey(destinationWell)) {
//                    messageCollection.addError("Missing well in rack scan " + destinationWell);
//                } else {
//                    destinationReceptacleType.setBarcode(positionBarcodeMap.get(destinationWell));
//                    destinationPositionMap.getReceptacle().add(destinationReceptacleType);
//                    CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
//                    cherryPickSourceType.setBarcode(sourcePlateBarcode);
//                    cherryPickSourceType.setWell(sourcePosition);
//                    cherryPickSourceType.setDestinationWell(destinationWell);
//                    plateCherryPickEvent.getSource().add(cherryPickSourceType);
//                }
//            }
//            plateCherryPickEvent.getSourcePlate().add(plateType);
//            plateCherryPickEvent.getSourcePositionMap().add(positionMapType);
//        }
//
//        List<StationEventType> events = new ArrayList<>();
//        events.add(plateCherryPickEvent);
//        return events;
//    }

    public Rack unmarshal(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbUnmarshaller = JAXBContext.newInstance(Rack.class);
        Unmarshaller unmarshaller = jaxbUnmarshaller.createUnmarshaller();
        return (Rack) unmarshaller.unmarshal(inputStream);
    }
}
