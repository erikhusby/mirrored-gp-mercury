package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.UniqueMolecularIdentifierReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory.BARCODE_LENGTH;

/**
 * Creates UniqueMolecularIdentifier entities.
 */
public class UniqueMolecularIdentifierReagentFactory {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UniqueMolecularIdentifierReagentDao uniqueMolecularIdentifierReagentDao;

    /**
     * From a spreadsheet, creates vessels and associates them with UMI reagents.
     * @param spreadsheetStream spreadsheet with vessel barcodes, UMI length, and position
     * @param messageCollection errors are added to this
     * @return list of entities
     */
    public List<StaticPlate> buildUMIFromSpreadsheet(InputStream spreadsheetStream,
                                                     MessageCollection messageCollection) {
        try {
            UniqueMolecularIdentifierReagentProcessor processor = new UniqueMolecularIdentifierReagentProcessor("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(spreadsheetStream,
                    processor));
            Map<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> mapBarcodeToReagent =
                    processor.getMapBarcodeToReagentDto();
            Map<String, LabVessel> mapBarcodeToPlate =
                    labVesselDao.findByBarcodes(new ArrayList<>(mapBarcodeToReagent.keySet()));

            Map<String, UMIReagent> mapPlateToUMI = new HashMap<>();
            Set<Map.Entry<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>> entrySet =
                    mapBarcodeToReagent.entrySet();
            for (Map.Entry<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> entry: entrySet)
            {
                String plateBarcode = entry.getKey();
                UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto dto = entry.getValue();
                UMIReagent umiReagent =
                        uniqueMolecularIdentifierReagentDao.findByLocationAndLength(dto.getLocation(), dto.getLength());
                mapPlateToUMI.put(plateBarcode, umiReagent);
            }
            List<StaticPlate> staticPlates =
                    buildPlates(mapBarcodeToReagent, messageCollection, mapBarcodeToPlate, mapPlateToUMI);
            if (!messageCollection.hasErrors()) {
                labVesselDao.persistAll(staticPlates);
            }
            return staticPlates;
        } catch (InvalidFormatException | IOException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds UMI reagent entities.
     * @param mapBarcodeToUMI DTOs from spreadsheet
     * @param messageCollection errors are added to this
     * @param mapBarcodeToPlate from database, error if they exist
     * @param mapBarcodeToReagent from database, created if they don't exist
     * @return list of entities
     */
    @DaoFree
    public List<StaticPlate> buildPlates(
            Map<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> mapBarcodeToUMI,
            MessageCollection messageCollection, Map<String, LabVessel> mapBarcodeToPlate,
            Map<String, UMIReagent> mapBarcodeToReagent) {
        List<StaticPlate> plates = new ArrayList<>();
        for (Map.Entry<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> umiDtoEntry :
                mapBarcodeToUMI.entrySet()) {
            String plateBarcode = umiDtoEntry.getKey();
            UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto umiDto = umiDtoEntry.getValue();

            if (mapBarcodeToPlate.get(plateBarcode) != null) {
                LabVessel labVessel = mapBarcodeToPlate.get(plateBarcode);
                if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                    StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                    if (staticPlate.getPlateType() == StaticPlate.PlateType.IndexedAdapterPlate96 ||
                        staticPlate.getPlateType() == StaticPlate.PlateType.IndexedAdapterPlate384) {
                        Set<SampleInstanceV2> sampleInstances =
                                staticPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
                        if (sampleInstances != null && sampleInstances.size() > 0) {
                            boolean foundUmiReagent = false;
                            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                            for (Reagent reagent: sampleInstance.getReagents()) {
                                if (OrmUtil.proxySafeIsInstance(reagent, UMIReagent.class)) {
                                    foundUmiReagent = true;
                                    break;
                                }
                            }
                            if (foundUmiReagent) {
                                messageCollection.addError("Index Plate already has an associated UMI " + plateBarcode);
                            }
                        }
                    } else {
                        messageCollection.addError("Plate is already in the database: " + plateBarcode);
                        continue;
                    }
                }
            }

            String formattedBarcode = StringUtils.leftPad(plateBarcode, BARCODE_LENGTH, '0');
            StaticPlate plate =
                    new StaticPlate(formattedBarcode, StaticPlate.PlateType.UniqueMolecularIdentifierPlate96);
            plate.setCreatedOn(new Date());

            UMIReagent umiReagent = mapBarcodeToReagent.get(plateBarcode);
            if (umiReagent == null) {
                umiReagent = new UMIReagent(umiDto.getLocation(), umiDto.getLength());
            }

            // Previously registered Index Plates may already have Plate Wells associated to each Vessel Position.
            Map<VesselPosition, PlateWell> mapPositionToVessel = plate.getContainerRole().getMapPositionToVessel();
            for (VesselPosition vesselPosition: plate.getVesselGeometry().getVesselPositions()) {
                if (mapPositionToVessel != null && mapPositionToVessel.containsKey(vesselPosition)) {
                    PlateWell plateWell = mapPositionToVessel.get(vesselPosition);
                    plateWell.addReagent(umiReagent);
                } else {
                    PlateWell plateWell = new PlateWell(plate, vesselPosition);
                    plateWell.addReagent(umiReagent);
                    plate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                }
            }
            plates.add(plate);
        }
        return plates;
    }
}
