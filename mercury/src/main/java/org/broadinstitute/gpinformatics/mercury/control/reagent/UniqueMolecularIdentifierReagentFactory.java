package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.UniqueMolecularIdentifierReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Creates UniqueMolecularIdentifier entities.
 */
@Dependent
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
    public List<LabVessel> buildUMIFromSpreadsheet(InputStream spreadsheetStream,
                                                   MessageCollection messageCollection) {
        try {
            UniqueMolecularIdentifierReagentProcessor processor = new UniqueMolecularIdentifierReagentProcessor("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(spreadsheetStream,
                    processor));
            if (messageCollection.hasErrors()) {
                return null;
            }
            Map<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>>
                    mapBarcodeToReagent = processor.getMapBarcodeToReagentDto();
            Map<String, LabVessel> mapBarcodeToPlate =
                    labVesselDao.findByBarcodes(new ArrayList<>(mapBarcodeToReagent.keySet()));

            Map<String, List<UniqueMolecularIdentifier>> mapPlateToUMI = new HashMap<>();
            Set<Map.Entry<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>>>
                    entrySet =
                    mapBarcodeToReagent.entrySet();
            for (Map.Entry<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>> entry: entrySet)
            {
                String plateBarcode = entry.getKey();
                List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> dtos = entry.getValue();
                for (UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto dto: dtos) {
                    UniqueMolecularIdentifier umiReagent =
                            uniqueMolecularIdentifierReagentDao
                                    .findByLocationAndLength(dto.getLocation(), dto.getLength(), dto.getSpacerLength());
                    if (umiReagent != null) {
                        if (mapPlateToUMI.get(plateBarcode) == null) {
                            mapPlateToUMI.put(plateBarcode, new ArrayList<UniqueMolecularIdentifier>());
                        }
                        mapPlateToUMI.get(plateBarcode).add(umiReagent);
                    }
                }
            }
            List<LabVessel> staticPlates =
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
     * @param mapBarcodeToLabVessel from database, error if they exist
     * @param mapBarcodeToReagent from database, created if they don't exist
     * @return list of entities
     */
    @DaoFree
    public List<LabVessel> buildPlates(
            Map<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>> mapBarcodeToUMI,
            MessageCollection messageCollection, Map<String, LabVessel> mapBarcodeToLabVessel,
            Map<String, List<UniqueMolecularIdentifier>> mapBarcodeToReagent) {

        Map<String, LabVessel> mapBarcodeToNewLabVessel = new TreeMap<>();
        Map<UniqueMolecularIdentifier, UMIReagent> umiReagentMap = new HashMap<>();

        for (Map.Entry<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>> umiDtoEntry :
                mapBarcodeToUMI.entrySet()) {
            String vesselBarcode = umiDtoEntry.getKey();
            List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> umiDtos = umiDtoEntry.getValue();
            for (UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto umiDto: umiDtos) {

                if (mapBarcodeToLabVessel.get(vesselBarcode) != null) {
                    LabVessel labVessel = mapBarcodeToLabVessel.get(vesselBarcode);

                    // Can add to pre-existing plates if they are index plates and have no UMI
                    if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                        StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                        if (staticPlate.getPlateType() == StaticPlate.PlateType.IndexedAdapterPlate96 ||
                            staticPlate.getPlateType() == StaticPlate.PlateType.IndexedAdapterPlate384) {
                            Set<SampleInstanceV2> sampleInstances =
                                    staticPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
                            if (sampleInstances != null && sampleInstances.size() > 0) {
                                boolean foundUmiReagent = false;
                                SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                                for (Reagent reagent : sampleInstance.getReagents()) {
                                    if (OrmUtil.proxySafeIsInstance(reagent, UMIReagent.class)) {
                                        foundUmiReagent = true;
                                        break;
                                    }
                                }
                                if (foundUmiReagent) {
                                    messageCollection
                                            .addError("Index Plate already has an associated UMI " + vesselBarcode);
                                }
                            }
                        } else {
                            messageCollection.addError("Plate is already in the database: " + vesselBarcode);
                            continue;
                        }
                    } else {
                        messageCollection.addError("Barcoded tube is already in the database " + vesselBarcode);
                    }
                }

                LabVessel labVessel = mapBarcodeToNewLabVessel.get(vesselBarcode);
                boolean isPlate = false;
                if (labVessel == null) {
                    if (umiDto.getVesselTypeGeometry() instanceof BarcodedTube.BarcodedTubeType) {
                        labVessel = new BarcodedTube(vesselBarcode,
                                (BarcodedTube.BarcodedTubeType) umiDto.getVesselTypeGeometry());
                        labVessel.setCreatedOn(new Date());
                    } else if (umiDto.getVesselTypeGeometry() instanceof StaticPlate.PlateType) {
                        labVessel =
                                new StaticPlate(vesselBarcode, (StaticPlate.PlateType) umiDto.getVesselTypeGeometry());
                        labVessel.setCreatedOn(new Date());
                        isPlate = true;
                    } else {
                        messageCollection.addError("Failed to create lab vessel for " + vesselBarcode);
                        return null;
                    }
                    mapBarcodeToNewLabVessel.put(vesselBarcode, labVessel);
                } else if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                    isPlate = true;
                }

                UMIReagent reagent = null;
                List<UniqueMolecularIdentifier> umiReagentList = mapBarcodeToReagent.get(vesselBarcode);
                if (umiReagentList != null) {
                    for (UniqueMolecularIdentifier uniqueMolecularIdentifier : umiReagentList) {
                        if (uniqueMolecularIdentifier.getSpacerLength() == umiDto.getSpacerLength() &&
                            uniqueMolecularIdentifier.getLength() == umiDto.getLength() &&
                            uniqueMolecularIdentifier.getLocation() == umiDto.getLocation()) {
                            reagent = uniqueMolecularIdentifier.getUmiReagent();
                            break;
                        }
                    }
                }
                if (reagent == null) {
                    UniqueMolecularIdentifier umi = new UniqueMolecularIdentifier(
                            umiDto.getLocation(), umiDto.getLength(), umiDto.getSpacerLength());
                    if (umiReagentMap.containsKey(umi)) {
                        reagent = umiReagentMap.get(umi);
                    } else {
                        reagent = new UMIReagent(umi);
                        umiReagentMap.put(umi, reagent);
                    }
                }

                // Previously registered Index Plates may already have Plate Wells associated to each Vessel Position.
                if (isPlate) {
                    StaticPlate plate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                    Map<VesselPosition, PlateWell> mapPositionToVessel =
                            plate.getContainerRole().getMapPositionToVessel();
                    for (VesselPosition vesselPosition : plate.getVesselGeometry().getVesselPositions()) {
                        if (mapPositionToVessel != null && mapPositionToVessel.containsKey(vesselPosition)) {
                            PlateWell plateWell = mapPositionToVessel.get(vesselPosition);
                            plateWell.addReagent(reagent);
                        } else {
                            PlateWell plateWell = new PlateWell(plate, vesselPosition);
                            plateWell.addReagent(reagent);
                            plate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                        }
                    }
                } else {
                    labVessel.addReagent(reagent);
                }
            }
        }
        return new ArrayList<>(mapBarcodeToNewLabVessel.values());
    }
}
