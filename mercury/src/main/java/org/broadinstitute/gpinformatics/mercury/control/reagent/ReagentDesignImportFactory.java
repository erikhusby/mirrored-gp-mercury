package org.broadinstitute.gpinformatics.mercury.control.reagent;

import clover.org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.DesignedReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Dependent
public class ReagentDesignImportFactory {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private DesignedReagentDao designedReagentDao;

    /**
     * From a spreadsheet, creates tubes and associates them with Reagent Designs.
     * @param inputStream spreadsheet with tube barcodes
     * @param messageCollection errors are added to this
     * @return list of entities
     */
    public List<BarcodedTube> buildTubesFromSpreadsheet(InputStream inputStream,
                                                        MessageCollection messageCollection) {
        try {
            ReagentDesignImportProcessor processor = new ReagentDesignImportProcessor();
            List<ReagentDesignImportProcessor.ReagentImportDto> dtos = processor.parse(inputStream, messageCollection);

            if (messageCollection.hasErrors() || dtos == null) {
                return null;
            }

            List<String> tubeBarcodes = processor.getTubeBarcodes();
            for (LabVessel labVessel: labVesselDao.findByListIdentifiers(tubeBarcodes)) {
                messageCollection.addError(String.format(
                        "Barcode \"%s\" already exists.", labVessel.getLabel()));
            }

            Map<String, ReagentDesign> mapNameToReagentDesign = new HashMap<>();
            Map<Triple<ReagentDesign, String, Date>, DesignedReagent> mapDtoToDesign = new HashMap<>();
            List<BarcodedTube> barcodedTubeList = new ArrayList<>();
            for (ReagentDesignImportProcessor.ReagentImportDto dto: dtos) {
                if (!mapNameToReagentDesign.containsKey(dto.getDesignName())) {
                    ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(dto.getDesignName());
                    if (reagentDesign == null) {
                        messageCollection.addError("Failed to find design name " + dto.getDesignName());
                        continue;
                    }
                    mapNameToReagentDesign.put(dto.getDesignName(), reagentDesign);
                }

                ReagentDesign reagentDesign = mapNameToReagentDesign.get(dto.getDesignName());

                String tubeBarcode = StringUtils.leftPad(dto.getTubeBarcode(), 10, '0');

                DesignedReagent reagent = null;
                Triple<ReagentDesign, String, Date> uniqueReagentTriple =
                        Triple.of(reagentDesign, dto.getLotNumber(), dto.getExpirationDate());
                if (mapDtoToDesign.containsKey(uniqueReagentTriple)) {
                    reagent = mapDtoToDesign.get(uniqueReagentTriple);
                } else {
                    reagent = designedReagentDao.findByReagentLotDesignAndExpiration(reagentDesign, dto.getLotNumber(),
                                    dto.getExpirationDate());

                    if (reagent == null) {
                        reagent = new DesignedReagent(reagentDesign);
                        reagent.setExpiration(dto.getExpirationDate());
                        reagent.setLot(dto.getLotNumber());
                        reagent.setName(dto.getDesignName());
                        Metadata synthesisDate = new Metadata(Metadata.Key.SYNTHESIS_DATE, dto.getSynthesisDate());
                        Metadata manufacturingDate =
                                new Metadata(Metadata.Key.MANUFACTURING_DATE, dto.getManufacturingDate());
                        Metadata storageConditions =
                                new Metadata(Metadata.Key.STORAGE_CONDITIONS, dto.getStorageConditions());
                        Metadata manufacturerDesignId =
                                new Metadata(Metadata.Key.MANUFACTURER_DESIGN_ID, dto.getDesignId());

                        Set<Metadata> metadata = new HashSet<>(Arrays.asList(synthesisDate, manufacturingDate,
                                storageConditions, manufacturerDesignId));
                        reagent.addMetadata(metadata);
                        mapDtoToDesign.put(uniqueReagentTriple, reagent);
                    }
                }


                BarcodedTube barcodedTube = new BarcodedTube(tubeBarcode);
                barcodedTube.addReagent(reagent);
                barcodedTubeList.add(barcodedTube);
                barcodedTube.setVolume(new BigDecimal(dto.getVolume()));

                BigDecimal mass = new BigDecimal(dto.getMass());
                barcodedTube.setMass(mass);
            }

            return barcodedTubeList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
