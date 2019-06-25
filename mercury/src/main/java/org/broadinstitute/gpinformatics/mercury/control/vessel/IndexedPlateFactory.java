package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinitionWell;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class creates plates containing molecular index reagents
 */
@Stateless
public class IndexedPlateFactory {
    private static final Log LOG = LogFactory.getLog(IndexedPlateFactory.class);
    public static final int BARCODE_LENGTH = 12;
    public static final String DEFINITION_IN_USE = "Index Plate Definition \"%s\" is in use and cannot be changed.";
    public static final String NEEDS_OVERWRITE =
            "To overwrite the existing index plate definition Allow Overwrite must be checked.";
    public static final String NO_SUCH_DEFINITION = "No Index Plate Definition found for \"%s\".";
    public static final String UNKNOWN_MIS_NAME = "Unknown molecular index name \"%s\".";
    public static final String UNKNOWN_POSITION = "Unknown vessel position \"%s\".";
    public static final String UNKNOWN_STATIC_PLATE_TYPE = "Unknown static plate geometry \"%s\".";
    public static final String VESSELS_EXIST = "Found existing vessel(s) having label(s): %s.";
    public static final String WRONG_COLUMN_COUNT = "At row %d expected %d columns but found %d.";

    @Inject
    private MolecularIndexingSchemeFactory indexingSchemeFactory;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    public enum TechnologiesAndParsers {
        FOUR54_SINGLE("454 (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.FOUR54_A.getTechnology()),
                false),
        ION_SINGLE("Ion Torrent (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.ION_A.getTechnology()),
                false),
        ILLUMINA_SINGLE("Illumina (Single Index)",
                new IndexedPlateParserIDTSpreadsheetFormat(),
                true),
        ILLUMINA_TSCA("Illumina (TSCA)",
                new IndexedPlateParserTSCAFormat(),
                false),
        ILLUMINA_FP("Illumina (FP)",
                new IndexedPlateParserTSCAFormat(), StaticPlate.PlateType.IndexedAdapterPlate384,
                true);

        private final String prettyName;
        private final IndexedPlateParser indexedPlateParser;
        private final StaticPlate.PlateType plateType;
        private final boolean active;

        TechnologiesAndParsers(String name, IndexedPlateParser indexedPlateParser, boolean active) {
            prettyName = name;
            this.indexedPlateParser = indexedPlateParser;
            this.plateType = StaticPlate.PlateType.IndexedAdapterPlate96;
            this.active = active;
        }

        TechnologiesAndParsers(String prettyName,
                               IndexedPlateParser indexedPlateParser,
                               StaticPlate.PlateType plateType, boolean active) {
            this.prettyName = prettyName;
            this.indexedPlateParser = indexedPlateParser;
            this.plateType = plateType;
            this.active = active;
        }

        public String getPrettyName() {
            return prettyName;
        }

        public IndexedPlateParser getIndexedPlateParser() {
            return indexedPlateParser;
        }

        public StaticPlate.PlateType getPlateType() {
            return plateType;
        }

        public boolean isActive() {
            return active;
        }
    }

    public Map<String, StaticPlate> parseAndPersist(TechnologiesAndParsers technologiesAndParsers,
            InputStream inputStream) {
        Map<String, StaticPlate> platesByBarcode = parseStream(inputStream, technologiesAndParsers);
        for (StaticPlate staticPlate : platesByBarcode.values()) {
            if (staticPlateDao.findByBarcode(staticPlate.getLabel()) != null) {
                throw new RuntimeException("Plate already exists: " + staticPlate.getLabel());
            }
            staticPlateDao.persist(staticPlate);
            staticPlateDao.flush();
            staticPlateDao.clear();
        }
        return platesByBarcode;
    }

    public Map<String, StaticPlate> parseStream(InputStream inputStream,
                                                TechnologiesAndParsers technologiesAndParsers) {
        IndexedPlateParser parser = technologiesAndParsers.getIndexedPlateParser();
        List<PlateWellIndexAssociation> associations;
        try {
            associations = parser.parseInputStream(inputStream);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
        return uploadIndexedPlates(associations, technologiesAndParsers.getPlateType());
    }

    public Map<String, StaticPlate> uploadIndexedPlates(List<PlateWellIndexAssociation> plateWellIndexes,
                                                        StaticPlate.PlateType plateType) {
        Map<String, StaticPlate> platesByBarcode = new HashMap<>();
        Set<PlateWell> previousWells = new HashSet<>();

        for (PlateWellIndexAssociation plateWellIndex : plateWellIndexes) {
            StaticPlate plate = createOrGetPlate(
                    plateWellIndex,
                    platesByBarcode,
                    plateType);
            VesselPosition vesselPosition = VesselPosition.getByName(plateWellIndex.getWellName());
            PlateWell plateWell = new PlateWell(plate, vesselPosition);
            if (previousWells.contains(plateWell)) {
                throw new RuntimeException(
                        "Plate " + plate.getLabel() + " and well " + plateWellIndex.getWellName() +
                        " has been defined two or more times in the uploaded file.");
            }
            previousWells.add(plateWell);

            MolecularIndexingScheme indexingScheme = indexingSchemeFactory.findOrCreateIndexingScheme(
                    Arrays.asList(plateWellIndex.getPositionPairs()), false);
            plateWell.addReagent(new MolecularIndexReagent(indexingScheme));

            plate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }

        LOG.info("Number of plates: " + platesByBarcode.keySet().size());
        return platesByBarcode;
    }


    private StaticPlate createOrGetPlate(PlateWellIndexAssociation plateWellIndex,
                                         Map<String, StaticPlate> platesByBarcode, StaticPlate.PlateType plateType) {

        String formattedBarcode = StringUtils.leftPad(plateWellIndex.getPlateBarcode(), BARCODE_LENGTH, '0');
        StaticPlate plate = platesByBarcode.get(formattedBarcode);
        if (plate == null) {
            plate = new StaticPlate(formattedBarcode, plateType);
            plate.setCreatedOn(new Date());
            platesByBarcode.put(formattedBarcode, plate);
        }
        return plate;
    }

    public void findLayout(String plateName, List<List<String>> layout, MessageCollection messageCollection) {
        layout.clear();
        IndexPlateDefinition definition = staticPlateDao.findSingle(IndexPlateDefinition.class,
                IndexPlateDefinition_.definitionName, plateName);
        if (definition == null) {
            messageCollection.addWarning(NO_SUCH_DEFINITION, plateName);
        } else {
            Map<VesselGeometry.RowColumn, String> rowColumnToName = new HashMap<>();
            for (IndexPlateDefinitionWell well : definition.getDefinitionWells()) {
                VesselGeometry.RowColumn rowColumn =
                        definition.getVesselGeometry().getRowColumnForVesselPosition(well.getVesselPosition());
                rowColumnToName.put(rowColumn, well.getMolecularIndexingScheme().getName());
            }
            // The first layout row has the column names and the first column of every row has the row name.
            layout.add(new ArrayList<String>() {{
                add("");
                addAll(Arrays.asList(definition.getVesselGeometry().getColumnNames()));
            }});
            // Puts a value in the layout for each vessel position using either the reagent name or a blank if none.
            for (int rowIdx = 0; rowIdx < definition.getVesselGeometry().getRowCount(); ++rowIdx) {
                String rowName = definition.getVesselGeometry().getRowNames()[rowIdx];
                List<String> row = new ArrayList<>();
                layout.add(row);
                row.add(rowName);
                for (int colIdx = 0; colIdx < definition.getVesselGeometry().getColumnCount(); ++colIdx) {
                    row.add(StringUtils.trimToEmpty(
                            rowColumnToName.get(definition.getVesselGeometry().makeRowColumn(rowIdx + 1, colIdx + 1))));
                }
            }
        }
    }

    /** Parses the spreadsheet into list of header and data rows. */
    public List<List<String>> parseSpreadsheet(InputStream inputStream, int expectedNumberOfColumns,
            MessageCollection messageCollection) {
        GenericTableProcessor processor = new GenericTableProcessor();
        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
        } catch (Exception e) {
            messageCollection.addError("Error while reading Excel file: " + e.toString());
        }
        if (CollectionUtils.isEmpty(processor.getHeaderAndDataRows()) ||
                CollectionUtils.isEmpty(processor.getHeaderAndDataRows().get(0))) {
            messageCollection.addError("Spreadsheet has no content.");
        }
        int rowNumber = 0;
        for (List<String> row : processor.getHeaderAndDataRows()) {
            ++rowNumber;
            if (row.size() > 0 && row.size() != expectedNumberOfColumns) {
                messageCollection.addError(WRONG_COLUMN_COUNT, rowNumber, expectedNumberOfColumns, row.size());
            }
        }
        return processor.getHeaderAndDataRows().stream().filter(row -> row.size() > 0).collect(Collectors.toList());
    }

    public List<String> findPlateDefinitionNames() {
        return staticPlateDao.findIndexPlateDefinitionNames();

    }

    /**
     * Creates or updates an index plate definition from the vessel positions and index names found in the spreadsheet.
     *
     * @param plateName the index plate definition unique identifier.
     * @param spreadsheet is a list of rows, each row having a vessel position name in column 0 and an existing
     *                    molecular index scheme name in column 1. A header row is optional and if present
     *                    it is ignored.
     * @param allowOverwrite must be set to true if an existing plate should be overwritten.
     * @param messageCollection used to pass errors, warnings, and info back to the UI.
     */
    public void makeIndexPlateDefinition(String plateName, List<List<String>> spreadsheet,
            @NotNull VesselGeometry vesselGeometry, IndexPlateDefinition.ReagentType reagentType,
            boolean allowOverwrite, MessageCollection messageCollection) {

        IndexPlateDefinition definition = staticPlateDao.findSingle(IndexPlateDefinition.class,
                IndexPlateDefinition_.definitionName, plateName);
        if (definition == null) {
            definition = new IndexPlateDefinition(plateName, vesselGeometry, reagentType);
        } else if (allowOverwrite && definition.getPlateInstances().isEmpty()) {
            // Overwrites an existing definition if it has not been used.
            definition.getDefinitionWells().clear();
            definition.setVesselGeometry(vesselGeometry);
            definition.setReagentType(reagentType);
        } else if (!allowOverwrite && definition.getPlateInstances().isEmpty()) {
            messageCollection.addError(NEEDS_OVERWRITE);
            return;
        } else {
            messageCollection.addError(DEFINITION_IN_USE, plateName);
            return;
        }

        boolean hasHeaderRow = VesselPosition.getByName(spreadsheet.get(0).get(0)) == null;
        // Makes a map of the first two columns of the spreadsheet.
        Map<String, String> positionToMisName = spreadsheet.subList(hasHeaderRow ? 1 : 0, spreadsheet.size()).
                stream().collect(Collectors.toMap(row -> row.get(0), row ->row.get(1)));

        // Looks up molecular indexing schemes by name.
        Map<String, MolecularIndexingScheme> mapNameToMis =
                molecularIndexingSchemeDao.findByNames(positionToMisName.values()).
                        stream().
                        collect(Collectors.toMap(MolecularIndexingScheme::getName, Function.identity()));

        // Makes index plate definition wells for each of the given positions.
        final IndexPlateDefinition finalDef = definition;
        positionToMisName.forEach((position, misName) -> {
            VesselPosition vesselPosition = VesselPosition.getByName(position);
            if (vesselPosition == null) {
                messageCollection.addError(UNKNOWN_POSITION, misName);
            }
            MolecularIndexingScheme scheme = mapNameToMis.get(misName);
            if (scheme == null) {
                messageCollection.addError(UNKNOWN_MIS_NAME, misName);
            }
            finalDef.getDefinitionWells().add(new IndexPlateDefinitionWell(finalDef, scheme, vesselPosition));
        });

        if (!messageCollection.hasErrors()) {
            messageCollection.addInfo("Created index plate definition " + plateName);
            staticPlateDao.persist(finalDef);
        }
    }

    /**
     * Instantiates one or more index plates for the barcodes found in the spreadsheet.
     *
     * @param plateName identifies an existing index plate definition.
     * @param spreadsheet is a list of barcodes. These will be leading zero filled as necessary. A header row
     *                    is optional and if present it is ignored.
     * @param messageCollection used to pass errors, warnings, and info back to the UI.
     */
    public void makeIndexPlate(String plateName, List<List<String>> spreadsheet, MessageCollection messageCollection) {
        boolean hasHeaderRow = !NumberUtils.isDigits(spreadsheet.get(0).get(0));
        List<String> plateBarcodes = spreadsheet.subList(hasHeaderRow ? 1 : 0, spreadsheet.size()).
                stream().flatMap(List::stream).distinct().collect(Collectors.toList());

        // Plate barcodes must not already exist.
        String existing = staticPlateDao.findListByList(LabVessel.class, LabVessel_.label, plateBarcodes).
                stream().
                filter(Objects::nonNull).
                map(LabVessel::getLabel).
                sorted().collect(Collectors.joining(" "));
        if (!existing.isEmpty()) {
            messageCollection.addError(VESSELS_EXIST, existing);
            return;
        }
        // Index plate definition must exist.
        IndexPlateDefinition definition = staticPlateDao.findSingle(IndexPlateDefinition.class,
                IndexPlateDefinition_.definitionName, plateName);
        if (definition == null) {
            messageCollection.addError(NO_SUCH_DEFINITION, plateName);
            return;
        }
        StaticPlate.PlateType plateType = definition.getVesselGeometry() == VesselGeometry.G12x8 ?
                StaticPlate.PlateType.IndexedAdapterPlate96 : definition.getVesselGeometry() == VesselGeometry.G24x16 ?
                StaticPlate.PlateType.IndexedAdapterPlate384 : null;
        if (plateType == null) {
            messageCollection.addError(UNKNOWN_STATIC_PLATE_TYPE, definition.getVesselGeometry().name());
            return;
        }
        // Makes a map for each position defined in the index plate definition and the well reagent.
        Map<VesselPosition, MolecularIndexReagent> reagentMap = definition.getDefinitionWells().stream().
                collect(Collectors.toMap(IndexPlateDefinitionWell::getVesselPosition,
                        well -> new MolecularIndexReagent(well.getMolecularIndexingScheme())));

        List<StaticPlate> plateBatch = new ArrayList<>();
        List<String> paddedBarcodes = new ArrayList<>();
        for (String barcode : plateBarcodes) {
            // Persists batches periodically and also at the end to avoid out of memory errors.
            if (plateBatch.size() >= 100) {
                staticPlateDao.persistAll(plateBatch);
                plateBatch.clear();
            }
            String paddedBarcode = StringUtils.leftPad(barcode, BARCODE_LENGTH, '0');
            paddedBarcodes.add(paddedBarcode);
            StaticPlate staticPlate = new StaticPlate(paddedBarcode, plateType);
            staticPlate.setCreatedOn(new Date());
            plateBatch.add(staticPlate);
            definition.getPlateInstances().add(staticPlate);
            reagentMap.forEach((vesselPosition, molecularIndexReagent) -> {
                PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
                plateWell.addReagent(molecularIndexReagent);
                staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            });
        }
        staticPlateDao.persistAll(plateBatch);
        messageCollection.addInfo("Created index plates " + StringUtils.join(paddedBarcodes, " "));
    }
}
