package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.GenericTableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.IndexPlateDefinitionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinitionWell;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class creates plates containing molecular index reagents
 */
@Stateless
public class IndexedPlateFactory {
    private static final Log LOG = LogFactory.getLog(IndexedPlateFactory.class);
    public static final int BARCODE_LENGTH = 12;
    public static final String CANNOT_REMOVE = "Cannot remove index plate%s that is in use: %s.";
    public static final String DEFINITION_SUFFIX = " definition";
    public static final String IN_USE = "Index plate%s is in use: %s.";
    public static final String INSTANCE_SUFFIX = " instance";
    public static final String NEEDS_OVERWRITE = "Replace Existing must be checked to overwrite an index plate%s.";
    public static final String NOT_FOUND = "No index plate%s found for \"%s\".";
    public static final String PLATES_REMOVED = "%d unused index plates were deleted from Mercury.";
    public static final String SPREADSHEET_EMPTY = "The spreadsheet has no data.";
    public static final String UNKNOWN_MIS_NAME = "Unknown molecular index name \"%s\".";
    public static final String UNKNOWN_POSITION = "Unknown vessel position \"%s\".";
    public static final String UNKNOWN_STATIC_PLATE_TYPE = "Unknown static plate geometry \"%s\".";
    public static final String WRONG_COLUMN_COUNT = "At row %d expected %d columns but found %d.";
    public static final String REMOVE_INDICATOR = "remove";

    @Inject
    private MolecularIndexingSchemeFactory indexingSchemeFactory;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private IndexPlateDefinitionDao indexPlateDefinitionDao;

    @Inject
    private UserBean userBean;

    @Inject
    LabVesselDao labVesselDao;

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

    private Map<String, StaticPlate> uploadIndexedPlates(List<PlateWellIndexAssociation> plateWellIndexes,
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

    public void findLayout(String selectedPlateName, List<List<String>> names, List<List<String>> sequences,
            MessageCollection messageCollection) {
        String plateName = selectionNameToPlateName(selectedPlateName);
        names.clear();
        sequences.clear();
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            messageCollection.addWarning(NOT_FOUND, DEFINITION_SUFFIX, plateName);
        } else {
            Map<VesselGeometry.RowColumn, String> rowColumnToName = new HashMap<>();
            Map<VesselGeometry.RowColumn, String> rowColumnToSequences = new HashMap<>();
            for (IndexPlateDefinitionWell well : definition.getDefinitionWells()) {
                VesselGeometry.RowColumn rowColumn =
                        definition.getVesselGeometry().getRowColumnForVesselPosition(well.getVesselPosition());
                rowColumnToName.put(rowColumn, well.getMolecularIndexingScheme().getName());
                String joinedPositions = well.getMolecularIndexingScheme().getIndexes().keySet().stream().
                        map(MolecularIndexingScheme.IndexPosition::getPosition).
                        collect(Collectors.joining(","));
                String joinedSequences = well.getMolecularIndexingScheme().getIndexes().values().stream().
                        map(MolecularIndex::getSequence).
                        collect(Collectors.joining(","));
                rowColumnToSequences.put(rowColumn, joinedPositions + ": " + joinedSequences);
            }

            // The first layout row has the column names and the first column of every row has the row name.
            names.add(new ArrayList<String>() {{
                add("");
                addAll(Arrays.asList(definition.getVesselGeometry().getColumnNames()));
            }});
            sequences.add(names.get(0));

            // For each well position, puts a value in the layout names and layout sequences, or blanks if none.
            for (int rowIdx = 0; rowIdx < definition.getVesselGeometry().getRowCount(); ++rowIdx) {
                List<String> nameRow = new ArrayList<>();
                List<String> sequenceRow = new ArrayList<>();
                names.add(nameRow);
                sequences.add(sequenceRow);
                String rowName = definition.getVesselGeometry().getRowNames()[rowIdx];
                nameRow.add(rowName);
                sequenceRow.add(rowName);
                for (int colIdx = 0; colIdx < definition.getVesselGeometry().getColumnCount(); ++colIdx) {
                    VesselGeometry.RowColumn rowCol =
                            definition.getVesselGeometry().makeRowColumn(rowIdx + 1, colIdx + 1);
                    nameRow.add(StringUtils.trimToEmpty(rowColumnToName.get(rowCol)));
                    sequenceRow.add(StringUtils.trimToEmpty(rowColumnToSequences.get(rowCol)));
                }
            }
        }
    }

    /** Parses the spreadsheet into list of header and data rows. */
    public List<List<String>> parseSpreadsheet(String filename, InputStream inputStream, int expectedNumberOfColumns,
            MessageCollection messageCollection) {
        List<List<String>> rows = Collections.emptyList();
        if (filename.toLowerCase().endsWith(".csv")) {
            // Parse as csv.
            try {
                rows = MayoManifestImportProcessor.parseAsCellGrid(IOUtils.toByteArray(inputStream),
                        filename, true, messageCollection);
            } catch (IOException e) {
                messageCollection.addError("Error while reading .csv file: " + e.toString());
            }
        } else {
            // Parse as Excel.
            GenericTableProcessor processor = new GenericTableProcessor();
            try {
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                // Removes blank rows.
                rows = processor.getHeaderAndDataRows().stream().
                        filter(columns -> columns.size() > 0).collect(Collectors.toList());
            } catch (Exception e) {
                messageCollection.addError("Error while reading Excel file: " + e.toString());
            }
        }
        if (CollectionUtils.isEmpty(rows) || CollectionUtils.isEmpty(rows.get(0))) {
            messageCollection.addError(SPREADSHEET_EMPTY);
        } else {
            for (int rowNumber = 1; rowNumber <= rows.size(); ++rowNumber) {
                int numberOfColumns = rows.get(rowNumber - 1).size();
                if (numberOfColumns != expectedNumberOfColumns) {
                    messageCollection.addError(WRONG_COLUMN_COUNT, rowNumber, expectedNumberOfColumns, numberOfColumns);
                }
            }
        }
        return rows;
    }

    /**
     * Populates the list with index plate definition names with reagent suffix.
     */
    public void findPlateDefinitionNames(List<String> plateNameSelection) {
        plateNameSelection.clear();
        indexPlateDefinitionDao.findAll(IndexPlateDefinition.class).forEach(definition ->
                plateNameSelection.add(
                        plateNameToSelectionName(definition.getDefinitionName(), definition.getReagentType())));
        plateNameSelection.sort(Comparator.naturalOrder());
    }

	public void renameDefinition(String selectedPlateName, String newDefinitionName, MessageCollection messageCollection) {
        if (indexPlateDefinitionDao.findByName(newDefinitionName) != null) {
            messageCollection.addError(IN_USE, DEFINITION_SUFFIX, newDefinitionName);
            return;
        }
        String plateName = selectionNameToPlateName(selectedPlateName);
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            messageCollection.addError(NOT_FOUND, DEFINITION_SUFFIX, plateName);
        } else {
            definition.setDefinitionName(newDefinitionName);
            messageCollection.addInfo("Index plate definition has been renamed to " + newDefinitionName);
        }
    }

    public void deleteDefinition(String selectedPlateName, MessageCollection messageCollection) {
        String plateName = selectionNameToPlateName(selectedPlateName);
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            messageCollection.addError(NOT_FOUND, DEFINITION_SUFFIX, plateName);
        } else {
            // The plate definition must have no instances.
            String instanceBarcodes = definition.getPlateInstances().stream().
                    map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(instanceBarcodes)) {
                messageCollection.addError(CANNOT_REMOVE, DEFINITION_SUFFIX, plateName);
            } else {
                indexPlateDefinitionDao.remove(definition);
                messageCollection.addInfo("Index plate definition has been deleted.");
            }
        }
    }

    /**
     * Looks up the index plates instantiated from the index plate definition.
     * Determines which plates have any events which indicates they are in use.
     *
     * @return pair of joined lists of plate barcodes, the left for unused plates, the right for plates in use.
     */
    public Pair<String, String> findInstances(String selectedPlateName, MessageCollection messageCollection) {
        String plateName = selectionNameToPlateName(selectedPlateName);
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            messageCollection.addWarning(NOT_FOUND, DEFINITION_SUFFIX, plateName);
        } else {
            Map<Boolean, List<StaticPlate>> mapUsageToPlate = definition.getPlateInstances().stream().
                    collect(Collectors.groupingBy(staticPlate -> CollectionUtils.isEmpty(staticPlate.getEvents())));
            String unused = mapUsageToPlate.get(Boolean.TRUE) == null ? "" :
                    mapUsageToPlate.get(Boolean.TRUE).stream().
                            map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" "));
            String inUse = mapUsageToPlate.get(Boolean.FALSE) == null ? "" :
                    mapUsageToPlate.get(Boolean.FALSE).stream().
                            map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" "));
            if (unused.isEmpty() && inUse.isEmpty()) {
                messageCollection.addInfo(NOT_FOUND, INSTANCE_SUFFIX, plateName);
            }
            return Pair.of(unused, inUse);
        }
        return Pair.of("", "");
    }

    /**
     * Looks up the index plate definition(s) that were used for instantiating the specified plates.
     * @return list of pairs of (plate definition name, joined list of plate barcodes)
     */
    public List<Pair<String, String>> findDefinitions(List<String> barcodes) {
        Map<String, List<StaticPlate>> definitionToInstance = staticPlateDao.findByBarcodes(barcodes).stream().
                collect(Collectors.groupingBy(staticPlate -> staticPlate.getIndexPlateDefinition() == null ?
                        "(none)" : staticPlate.getIndexPlateDefinition().getDefinitionName()));

        return definitionToInstance.keySet().stream().sorted().
                map(name -> Pair.of(name, definitionToInstance.get(name).stream().
                        map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" ")))).
                collect(Collectors.toList());
    }

    /** Deletes the given plate instances. */
 	public void deleteInstances(List<String> barcodes, MessageCollection messageCollection) {
        // mapUsageToPlate.get(TRUE) gives the unused plates, FALSE gives the in-use plates.
        Map<Boolean, List<StaticPlate>> mapUsageToPlate = staticPlateDao.findByBarcodes(barcodes).stream().
                collect(Collectors.groupingBy(staticPlate -> CollectionUtils.isEmpty(staticPlate.getEvents())));
        if (barcodes.size() == mapUsageToPlate.values().stream().mapToInt(List::size).sum()) {
            if (CollectionUtils.isEmpty(mapUsageToPlate.get(Boolean.FALSE))) {
                // Ensures that all plates are index plate instances before deleting any of them.
                String notIndexPlateInstances = mapUsageToPlate.get(Boolean.TRUE).stream().
                        filter(staticPlate -> staticPlate.getIndexPlateDefinition() == null).
                        map(LabVessel::getLabel).
                        sorted().distinct().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notIndexPlateInstances)) {
                    messageCollection.addError(NOT_FOUND, INSTANCE_SUFFIX, notIndexPlateInstances);
                } else {
                    removePlates(mapUsageToPlate.get(Boolean.TRUE));
                    messageCollection.addInfo(PLATES_REMOVED, barcodes.size());
                }
            } else {
                // One or more plates are in use.
                messageCollection.addError(CANNOT_REMOVE, "", mapUsageToPlate.get(Boolean.FALSE).stream().
                        map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" ")));
            }
        } else if (barcodes.size() > 0) {
            // Errors the input barcodes that aren't plate barcodes.
            messageCollection.addError(NOT_FOUND, "", CollectionUtils.subtract(barcodes,
                    mapUsageToPlate.values().stream().flatMap(List::stream).
                            map(StaticPlate::getLabel).collect(Collectors.toList())).
                    stream().sorted().collect(Collectors.joining(" ")));
        }
    }

    private void removePlates(List<StaticPlate> plates) {
        plates.forEach(staticPlate -> {
            IndexPlateDefinition indexPlateDefinition = staticPlate.getIndexPlateDefinition();
            if (indexPlateDefinition != null) {
                indexPlateDefinition.getPlateInstances().remove(staticPlate);
            }
            staticPlate.getContainerRole().getMapPositionToVessel().
                    forEach((position, well) -> staticPlateDao.remove(well));
            staticPlateDao.remove(staticPlate);
        });
        // Flush needed here to avoid unique index violation on plate label when the plate is being reused.
        staticPlateDao.flush();
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

        if (CollectionUtils.isEmpty(spreadsheet) || CollectionUtils.isEmpty(spreadsheet.get(0))) {
            messageCollection.addError(SPREADSHEET_EMPTY);
        }
        boolean isOverwrite = false;
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            definition = new IndexPlateDefinition(plateName, vesselGeometry, reagentType);
        } else if (allowOverwrite && definition.getPlateInstances().isEmpty()) {
            // Overwrites an existing definition if it has not been used.
            definition.getDefinitionWells().clear();
            definition.setVesselGeometry(vesselGeometry);
            definition.setReagentType(reagentType);
            isOverwrite = true;
        } else if (!allowOverwrite && definition.getPlateInstances().isEmpty()) {
            messageCollection.addError(NEEDS_OVERWRITE, DEFINITION_SUFFIX);
        } else {
            messageCollection.addError(IN_USE, DEFINITION_SUFFIX, plateName);
        }
        boolean hasHeaderRow = VesselPosition.getByName(spreadsheet.get(0).get(0)) == null;
        if (!messageCollection.hasErrors()) {
            if ((hasHeaderRow ? 2 : 1) > spreadsheet.size()) {
                messageCollection.addError("No data rows found");
            } else {
                for (int i = hasHeaderRow ? 1 : 0; i < spreadsheet.size(); ++i) {
                    if (spreadsheet.get(i).size() < 2 || StringUtils.isBlank(spreadsheet.get(i).get(0)) ||
                            StringUtils.isBlank(spreadsheet.get(i).get(1))) {
                        messageCollection.addError("Missing position or index name at row " + (i + 1));
                    }
                }
            }
        }
        if (!messageCollection.hasErrors()) {
            // Makes a map of the first two columns of the spreadsheet.
            Map<String, String> positionToMisName = spreadsheet.subList(hasHeaderRow ? 1 : 0, spreadsheet.size()).
                    stream().collect(Collectors.toMap(row -> row.get(0), row -> row.get(1)));

            // Looks up molecular indexing schemes by name.
            Map<String, MolecularIndexingScheme> mapNameToMis =
                    molecularIndexingSchemeDao.findByNames(positionToMisName.values()).
                            stream().
                            collect(Collectors.toMap(MolecularIndexingScheme::getName, Function.identity()));

            // Makes a list of the valid position names for this rack geometry.
            List<String> validPositionNames = Stream.of(vesselGeometry.getVesselPositions()).
                    map(VesselPosition::name).collect(Collectors.toList());

            // Makes index plate definition wells for each of the given positions.
            final IndexPlateDefinition finalDef = definition;
            positionToMisName.forEach((position, misName) -> {
                VesselPosition vesselPosition = VesselPosition.getByName(position);
                if (vesselPosition == null || !validPositionNames.contains(vesselPosition.name())) {
                    messageCollection.addError(UNKNOWN_POSITION, position);
                }

                MolecularIndexingScheme scheme = mapNameToMis.get(misName);
                if (scheme == null) {
                    messageCollection.addError(UNKNOWN_MIS_NAME, misName);
                }
                finalDef.getDefinitionWells().add(new IndexPlateDefinitionWell(finalDef, scheme, vesselPosition));
            });

            if (!messageCollection.hasErrors()) {
                messageCollection.addInfo((isOverwrite ? "Updated" : "Created") +
                        " index plate definition " + plateName);
                indexPlateDefinitionDao.persist(finalDef);
            }
        }
    }

    /**
     * Instantiates one or more index plates for the barcodes found in the spreadsheet.
     *
     * @param selectedPlateName identifies an existing index plate definition.
     * @param spreadsheet is a list of barcodes. These will be leading zero filled as necessary. A header row
     *                    is optional and if present it is ignored.
     * @param messageCollection used to pass errors, warnings, and info back to the UI.
     */
    public void makeIndexPlate(String selectedPlateName, List<List<String>> spreadsheet, @Nullable String salesOrderNumber,
            boolean replaceExisting, MessageCollection messageCollection) {

        if (CollectionUtils.isEmpty(spreadsheet) || CollectionUtils.isEmpty(spreadsheet.get(0))) {
            messageCollection.addError(SPREADSHEET_EMPTY);
            return;
        }

        int overwriteCount = 0;
        // If it starts with a number, it's a rack barcode.
        boolean hasHeaderRow = !CharUtils.isAsciiNumeric(spreadsheet.get(0).get(0).charAt(0));
        // Reads the plate barcodes from the spreadsheet and does leading zero fill as needed.
        List<String> plateBarcodes = spreadsheet.subList(hasHeaderRow ? 1 : 0, spreadsheet.size()).
                stream().flatMap(List::stream).distinct().
                map(barcode -> StringUtils.leftPad(barcode, BARCODE_LENGTH, '0')).
                collect(Collectors.toList());

        // Checks that each plate either does not exist, or if it does exist then replaceExisting is set
        // and the plate has no events.
        Map<Boolean, List<StaticPlate>> mapUsageToPlate = staticPlateDao.findByBarcodes(plateBarcodes).stream().
                collect(Collectors.groupingBy(staticPlate -> CollectionUtils.isEmpty(staticPlate.getEvents())));
        // mapUsageToPlate.get(TRUE) gives the unused plates, FALSE gives the in-use plates.
        if (CollectionUtils.isNotEmpty(mapUsageToPlate.get(Boolean.TRUE)) && !replaceExisting) {
            messageCollection.addError(NEEDS_OVERWRITE, "");
        }
        if (CollectionUtils.isNotEmpty(mapUsageToPlate.get(Boolean.FALSE))) {
            messageCollection.addError(IN_USE, "", mapUsageToPlate.get(Boolean.FALSE).stream().
                    map(StaticPlate::getLabel).sorted().collect(Collectors.joining(" ")));
        }
        // Index plate definition must exist.
        String plateName = selectionNameToPlateName(selectedPlateName);
        StaticPlate.PlateType plateType = null;
        IndexPlateDefinition definition = indexPlateDefinitionDao.findByName(plateName);
        if (definition == null) {
            messageCollection.addError(NOT_FOUND, DEFINITION_SUFFIX, plateName);
        } else {
            plateType = (definition.getVesselGeometry() == VesselGeometry.G12x8) ?
                    StaticPlate.PlateType.IndexedAdapterPlate96 :
                    (definition.getVesselGeometry() == VesselGeometry.G24x16) ?
                            StaticPlate.PlateType.IndexedAdapterPlate384 : null;
            if (plateType == null) {
                messageCollection.addError(UNKNOWN_STATIC_PLATE_TYPE, definition.getVesselGeometry().name());
            }
        }
        if (!messageCollection.hasErrors()) {
            // Removes any existing plates known to be unused so their labels can be reused.
            if (mapUsageToPlate.get(Boolean.TRUE) != null) {
                String notIndexPlateInstances = mapUsageToPlate.get(Boolean.TRUE).stream().
                        filter(staticPlate -> staticPlate.getIndexPlateDefinition() == null).
                        map(LabVessel::getLabel).
                        sorted().distinct().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notIndexPlateInstances)) {
                    // Existing plates must be index plate instances, only because it's unclear
                    // what the use case is if non-index plate instances are given.
                    messageCollection.addError(NOT_FOUND, INSTANCE_SUFFIX, notIndexPlateInstances);
                } else {
                    overwriteCount = mapUsageToPlate.get(Boolean.TRUE).size();
                    removePlates(mapUsageToPlate.get(Boolean.TRUE));
                }
            }
        }
        if (!messageCollection.hasErrors()) {
            // Makes a map of position and reagent from the index plate definition.
            Map<VesselPosition, MolecularIndexReagent> reagentMap = definition.getDefinitionWells().stream().
                    collect(Collectors.toMap(well -> well.getVesselPosition(),
                            well -> {
                                MolecularIndexingScheme mis = well.getMolecularIndexingScheme();
                                MolecularIndexReagent reagent = molecularIndexingSchemeDao.reagentFor(mis);
                                return (reagent == null) ? new MolecularIndexReagent(mis) : reagent;
                            }));
            molecularIndexingSchemeDao.persistAll(reagentMap.values());

            List<StaticPlate> plateBatch = new ArrayList<>();
            for (String barcode : plateBarcodes) {
                // Persists batches periodically and also at the end to avoid out of memory errors.
                if (plateBatch.size() >= 100) {
                    staticPlateDao.persistAll(plateBatch);
                    staticPlateDao.persist(definition);
                    staticPlateDao.flush();
                    staticPlateDao.clear();
                    plateBatch.clear();
                    // These will continue to be needed.
                    definition = indexPlateDefinitionDao.findByName(plateName);
                    reagentMap = definition.getDefinitionWells().stream().
                            collect(Collectors.toMap(well -> well.getVesselPosition(),
                                    well -> molecularIndexingSchemeDao.reagentFor(well.getMolecularIndexingScheme())));
                }
                // Creates the new index plate instance.
                StaticPlate staticPlate = new StaticPlate(barcode, plateType);
                staticPlate.setCreatedOn(new Date());
                staticPlate.setIndexPlateDefinition(definition);
                staticPlate.setSalesOrderNumber(salesOrderNumber);
                plateBatch.add(staticPlate);
                definition.getPlateInstances().add(staticPlate);
                reagentMap.forEach((vesselPosition, molecularIndexReagent) -> {
                    PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
                    plateWell.addReagent(molecularIndexReagent);
                    staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                });
            }
            staticPlateDao.persistAll(plateBatch);

            // Shows username, plate count, definition name.
            int newCount = plateBarcodes.size() - overwriteCount;
            if (overwriteCount == 0) {
                messageCollection.addInfo(String.format("%s created %d index plates from %s",
                        userBean.getBspUser().getUsername(), newCount, plateName));
            } else if (newCount == 0) {
                messageCollection.addInfo(String.format("%s updated %d index plates from %s",
                        userBean.getBspUser().getUsername(), overwriteCount, plateName));
            } else {
                messageCollection.addInfo(String.format("%s created %d and updated %d index plates from %s",
                        userBean.getBspUser().getUsername(), newCount, overwriteCount, plateName));
            }
        }
    }

    private final static String SEPARATOR = " - ";

    /** Converts a plate name and reagent type into a plateSelectionName for the UI. */
    public String plateNameToSelectionName(String plateName, IndexPlateDefinition.ReagentType reagentType) {
        return plateName + SEPARATOR + reagentType.name();
    }

    /** Converts a plateSelectionName into a plate name. */
    public String selectionNameToPlateName(String plateNameSelection) {
        return StringUtils.substringBeforeLast(plateNameSelection, SEPARATOR);
    }

    /**
     * Issues warnings and errors if the upload has problems.
     *
     * @param rows spreadsheet cell grid, including a header row.
     */
    public void validateTubesAndIndexNames(List<List<String>> rows, MessageCollection messages) {
        final int tubeBarcodeColumnIndex = 0;
        final int misNameColumnIndex = 1;

        // Assumes a header row is present and ignores it.
        if (CollectionUtils.isEmpty(rows) || rows.size() < 2) {
            messages.addError("Upload contains no data.");
            return;
        }

        List<String> barcodes = new ArrayList<>();
        List<String> misNames = new ArrayList<>();
        rows.subList(1, rows.size()).forEach(row -> {
            barcodes.add(row.get(tubeBarcodeColumnIndex).trim());
            misNames.add(row.size() < misNameColumnIndex ? "" : row.get(misNameColumnIndex).trim());
        });

        // Each row must have a non-blank barcode and non-blank mis name.
        String missingBarcodeRows = IntStream.range(0, barcodes.size()).
                filter(i -> StringUtils.isBlank(barcodes.get(i))).
                mapToObj(i -> String.valueOf(i + 2)). // outputs the 1-based spreadsheet row number.
                collect(Collectors.joining(", "));
        if (!missingBarcodeRows.isEmpty()) {
            messages.addError("Tube barcode is blank at rows " + missingBarcodeRows + ".");
        }
        String missingMisNameRows = IntStream.range(0, misNames.size()).
                filter(i -> StringUtils.isBlank(misNames.get(i))).
                mapToObj(i -> String.valueOf(i + 2)). // outputs the 1-based spreadsheet row number.
                collect(Collectors.joining(", "));
        if (!missingMisNameRows.isEmpty()) {
            messages.addError("Molecular index name is blank at rows " + missingMisNameRows + ".");
        }
        if (messages.hasErrors()) {
            return;
        }

        // Barcodes must be unique.
        Set<String> uniqueBarcodes = new HashSet<>();
        String duplicateBarcodeRows = IntStream.range(0, barcodes.size()).
                filter(i -> !uniqueBarcodes.add(barcodes.get(i))).
                mapToObj(i -> String.valueOf(i + 2)). // outputs the 1-based spreadsheet row number.
                collect(Collectors.joining(", "));
        if (!duplicateBarcodeRows.isEmpty()) {
            messages.addError("Duplicate tube barcode at rows " + duplicateBarcodeRows + ".");
        }

        // Checks that molecular indexing scheme names are valid, or the remove indicator.
        Map<String, MolecularIndexingScheme> mises = molecularIndexingSchemeDao.mapByNames(misNames);
        String invalidMisNameRows = IntStream.range(0, misNames.size()).
                filter(i -> mises.get(misNames.get(i)) == null && !misNames.get(i).equals(REMOVE_INDICATOR)).
                mapToObj(i -> String.valueOf(i + 2)).
                collect(Collectors.joining(", "));
        if (!invalidMisNameRows.isEmpty()) {
            messages.addError("Molecular index name at rows " + invalidMisNameRows + " is unregistered in Mercury.");
        }
        // Checks that all valid mis names have a reagent.
        Map<String, MolecularIndexReagent> reagents = new HashMap<>();
        mises.entrySet().stream().forEach(mapEntry ->
                reagents.put(mapEntry.getKey(), molecularIndexingSchemeDao.reagentFor(mapEntry.getValue())));
        String invalidReagentRows = IntStream.range(0, misNames.size()).
                filter(i -> mises.get(misNames.get(i)) != null && reagents.get(misNames.get(i)) == null).
                mapToObj(i -> String.valueOf(i + 2)).
                collect(Collectors.joining(", "));
        if (!invalidReagentRows.isEmpty()) {
            messages.addError("Molecular index at rows " + invalidReagentRows + " has no linked reagent in Mercury.");
        }

        // Normally the tube doesn't exist yet in Mercury, so warn if the tube exists and a reagent
        // is being added. But error if the tube already has a reagent and a new one is being added.
        // If the remove indicator is present only warn if the tube does not exist.
        Map<String, LabVessel> tubes = labVesselDao.findByBarcodes(barcodes);
        List<Integer> tubeExists = new ArrayList<>();
        List<Integer> tubeDoesntExist = new ArrayList<>();
        List<Integer> alreadyContains = new ArrayList<>();
        List<Integer> alreadyEmpty = new ArrayList<>();
        List<Integer> multipleReagents = new ArrayList<>();
        for (int i = 0; i < barcodes.size(); ++i) {
            int rowNumber = i + 2;
            LabVessel tube = tubes.get(barcodes.get(i));
            if (REMOVE_INDICATOR.equals(misNames.get(i))) {
                if (tube == null) {
                    tubeDoesntExist.add(rowNumber);
                } else if (tube.getReagentContents().isEmpty()) {
                    alreadyEmpty.add(rowNumber);
                } else if (tube.getReagentContents().size() > 1) {
                    multipleReagents.add(rowNumber);
                }
            } else if (tube != null) {
                // Looks for a molecular index reagent in the tube.
                if (tube.getSampleInstancesV2().stream().
                        anyMatch(sampleInstance -> CollectionUtils.isNotEmpty(sampleInstance.getReagents()))) {
                    alreadyContains.add(rowNumber);
                } else {
                    tubeExists.add(rowNumber);
                }
            }
        }
        if (!alreadyContains.isEmpty()) {
            messages.addError("Tube at rows " + StringUtils.join(alreadyContains, ", ") +
                    " already contains a molecular index.");
        }
        if (!tubeDoesntExist.isEmpty()) {
            messages.addError("Tube at rows " + StringUtils.join(tubeDoesntExist, ", ") + " is not in Mercury.");
        }
        if (!tubeExists.isEmpty()) {
            messages.addWarning("Tube at rows " + StringUtils.join(tubeExists, ", ") + " is already registered.");
        }
        if (!alreadyEmpty.isEmpty()) {
            messages.addWarning("Tube at rows " + StringUtils.join(alreadyEmpty, ", ") +
                    " does not contain a molecular index.");
        }
        if (!multipleReagents.isEmpty()) {
            messages.addWarning("Tube at rows " + StringUtils.join(multipleReagents, ", ") +
                    " contains more than one reagent and all of them will be removed.");
        }
        // Warns about short numeric barcodes that lack a leading zero, which is typical of an Excel upload.
        String missingZeroRowNumbers = IntStream.range(0, barcodes.size()).
                filter(i -> !barcodes.get(i).startsWith("0") && barcodes.get(i).length() < 10 &&
                        NumberUtils.isDigits(barcodes.get(i))).
                mapToObj(i -> String.valueOf(i + 2)).
                collect(Collectors.joining(", "));
        if (!missingZeroRowNumbers.isEmpty()) {
            messages.addWarning("Tube barcode appears to lack leading zeros at rows " + missingZeroRowNumbers + ".");
        }
        if (!messages.hasWarnings() && !messages.hasWarnings()) {
            messages.addInfo("Spreadsheet validation found no problems.");
        }
    }

    /**
     * Saves tubes and reagent contents.
     */
    public void saveTubesAndIndexNames(List<String> barcodes, List<String> misNames, MessageCollection messages) {
        Map<String, LabVessel> tubes = labVesselDao.findByBarcodes(barcodes);
        // Makes any new barcoded tubes needed.
        barcodes.stream().
                filter(barcode -> tubes.get(barcode) == null).
                forEach(barcode -> tubes.put(barcode, new BarcodedTube(barcode)));

        // Maps the mis name to the reagent.
        Map<String, MolecularIndexReagent> reagents = new HashMap<>();
        molecularIndexingSchemeDao.mapByNames(misNames).entrySet().stream().forEach(mapEntry ->
                reagents.put(mapEntry.getKey(), molecularIndexingSchemeDao.reagentFor(mapEntry.getValue())));

        int added = 0;
        int removed = 0;
        for (int i = 0; i < barcodes.size(); ++i) {
            LabVessel labVessel = tubes.get(barcodes.get(i));
            if (REMOVE_INDICATOR.equals(misNames.get(i))) {
                if (!labVessel.getReagentContents().isEmpty()) {
                    labVessel.getReagentContents().clear();
                    ++removed;
                }
            } else {
                MolecularIndexReagent reagent = reagents.get(misNames.get(i));
                // Just assert since this should already have been validated.
                assert (reagent != null);
                labVessel.addReagent(reagent);
                ++added;
            }
        }
        labVesselDao.persistAll(tubes.values());
        if (added > 0) {
            messages.addInfo("Added reagents to " + added + " tubes.");
        }
        if (removed > 0) {
            messages.addInfo("Removed reagents from " + removed + " tubes.");
        }
        if (added == 0 && removed == 0) {
            messages.addInfo("No changes were made.");
        }
    }
}
