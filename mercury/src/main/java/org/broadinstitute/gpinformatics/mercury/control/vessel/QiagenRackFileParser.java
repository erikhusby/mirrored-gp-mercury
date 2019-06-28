package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.Rack;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.RackPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for tube barcode/ well pairs to output vessel barcode from Qiagen Rack File output
 */
public class QiagenRackFileParser {
    private static final Log logger = LogFactory.getLog(QiagenRackFileParser.class);

    public void attachSourcePlateData(PlateTransferEventType plateTransferEventType, InputStream inputStream,
                                      MessageCollection messageCollection) {
        Rack rack = null;
        try {
            rack = unmarshal(inputStream);
        } catch (JAXBException e) {
            logger.error("Failed to unmarshal XML file", e);
            messageCollection.addError("Failed to unmarshal XML file");
            return;
        }

        if (plateTransferEventType.getSourcePlate() == null || plateTransferEventType.getPlate() == null ||
            plateTransferEventType.getSourcePositionMap() == null || plateTransferEventType.getPositionMap() == null) {
            messageCollection.addError("PlateTransferEvent not initialized before calling Qiagen Rack File Parser");
            return;
        }

        RackOfTubes.RackType rackType = RackOfTubes.RackType.getByName(
                plateTransferEventType.getSourcePlate().getPhysType());
        if (rackType == null) {
            messageCollection.addError("Unknown rack type: " + plateTransferEventType.getSourcePlate().getPhysType());
            return;
        }

        String instrument = rack.getModificationRecord().getInstrument().getValue();
        if (mapSerialNumberToMachineName.containsKey(instrument)) {
            instrument = mapSerialNumberToMachineName.get(instrument);
        }
        plateTransferEventType.setStation(instrument);

        //Generate new source plate barcode since Qiasymphony doesn't supply one.
        String platePrefix = "QiagenSampleCarrier24";
        String sdf = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String sourcePlateBarcode = platePrefix + "_" + sdf;
        PlateType sourcePlate = plateTransferEventType.getSourcePlate();
        PositionMapType sourcePositionMap = plateTransferEventType.getSourcePositionMap();
        sourcePlate.setBarcode(sourcePlateBarcode);
        sourcePositionMap.setBarcode(sourcePlateBarcode);

        PlateType destinationPlate = plateTransferEventType.getPlate();
        destinationPlate.setBarcode(rack.getRackId().getValue());
        PositionMapType destinationPositionMap = plateTransferEventType.getPositionMap();
        destinationPositionMap.setBarcode(rack.getRackId().getValue());
        plateTransferEventType.setPlate(destinationPlate);

        SBSSection sbsSectionSource = SBSSection.getBySectionName(sourcePlate.getSection());
        SBSSection sbsSectionDestination = SBSSection.getBySectionName(destinationPlate.getSection());

        List<VesselPosition> destinationSectionWells = sbsSectionDestination.getWells();
        List<VesselPosition> sourceSectionWells = sbsSectionSource.getWells();

        for (RackPosition rackPosition: rack.getRackPosition()) {
            if (rackPosition.getSampleId().getValue() != null && !rackPosition.getSampleId().getValue().isEmpty()) {
                String destinationWell = rackPosition.getPositionName().getValue().replaceAll(":", "");
                VesselPosition vesselPosition = VesselPosition.getByName(destinationWell);
                if (vesselPosition == null) {
                    messageCollection.addError("Failed to find position name " + destinationWell);
                    continue;
                }

                int indexOfDestWellInSection = destinationSectionWells.indexOf(vesselPosition);
                if (indexOfDestWellInSection == -1) {
                    messageCollection.addError(String.format("Failed to find destination well %s in section %s",
                            vesselPosition.name(), sbsSectionDestination.getSectionName()));
                    return;
                } else if (indexOfDestWellInSection > sourceSectionWells.size() - 1) {
                    messageCollection.addError(String.format("Source Section %s and destination section %s size aren't equal",
                            sbsSectionSource.getSectionName(), sbsSectionDestination.getSectionName()));
                    return;
                }

                VesselPosition sourceVesselPosition = sourceSectionWells.get(indexOfDestWellInSection);
                ReceptacleType sourceReceptacleType = new ReceptacleType();
                sourceReceptacleType.setBarcode(rackPosition.getSampleId().getValue());
                sourceReceptacleType.setPosition(sourceVesselPosition.name());
                plateTransferEventType.getSourcePositionMap().getReceptacle().add(sourceReceptacleType);

                ReceptacleType destinationReceptacleType = new ReceptacleType();
                destinationReceptacleType.setPosition(vesselPosition.name());
                destinationReceptacleType.setVolume(new BigDecimal(rackPosition.getTotalVolumeInUl().getValue()));
                destinationPositionMap.getReceptacle().add(destinationReceptacleType);
            }
        }
    }

    public Rack unmarshal(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbUnmarshaller = JAXBContext.newInstance(Rack.class);
        Unmarshaller unmarshaller = jaxbUnmarshaller.createUnmarshaller();
        return (Rack) unmarshaller.unmarshal(inputStream);
    }

    static final Map<String, String> mapSerialNumberToMachineName = new HashMap<>();
    static {
        mapSerialNumberToMachineName.put("qssp34733", "Beethoven");
        mapSerialNumberToMachineName.put("qssp35174", "Air Bud");
    }
}
