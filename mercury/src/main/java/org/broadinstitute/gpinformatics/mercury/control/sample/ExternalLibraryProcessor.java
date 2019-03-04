package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalLibraryProcessor extends TableProcessor {
    private static final Log log = LogFactory.getLog(ExternalLibraryProcessor.class);
    // The spreadsheet cell must contain the DELETE_TOKEN in order to remove an existing sample metadata value.
    public static final String DELETE_TOKEN = "<delete>";

    // Maps tube barcode to the first dto that contains that barcode (and associated tube data).
    private Map<String, SampleInstanceEjb.RowDto> mapBarcodeToFirstRow = new HashMap<>();
    // Maps sample name to the first dto that contains that sample name (and associated sample data).
    private Map<String, SampleInstanceEjb.RowDto> mapSampleNameToFirstRow = new HashMap<>();
    // Spreadsheet's actual header names.
    private List<String> headerNames = new ArrayList<>();
    // Maps sample name to MercurySample.
    private Map<String, MercurySample> sampleMap = new HashMap<>();
    // Maps barcode to LabVessel.
    private Map<String, LabVessel> labVesselMap = new HashMap<>();
    // Maps sample name to BSP or Mercury SampleData.
    private Map<String, SampleData> fetchedData = new HashMap<>();
    // Maps of spreadsheet values to entities.
    private Map<String, AnalysisType> analysisTypeMap = new HashMap<>();
    // Maps of spreadsheet values to entities.
    private Map<String, ReagentDesign> baitMap = new HashMap<>();
    private Map<String, MolecularIndexingScheme> molecularIndexingSchemeMap = new HashMap<>();
    private Map<String, ReferenceSequence> referenceSequenceMap = new HashMap<>();
    private Map<String, String> aggregationDataTypeMap = new HashMap<>();
    private Set<Object> entitiesToUpdate = new HashSet<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();
    private List<String> aggregationParticles = new ArrayList<>();
    private List<String> aggregationDataTypes = new ArrayList<>();
    private List<String> baits = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    private List<String> concentrations = new ArrayList<>();
    private List<String> dataAnalysisTypes = new ArrayList<>();
    private List<String> fragmentSizes = new ArrayList<>();
    private List<String> insertSizes = new ArrayList<>();
    private List<String> libraryNames = new ArrayList<>();
    private List<String> molecularBarcodeNames = new ArrayList<>();
    private List<String> organisms = new ArrayList<>();
    private List<String> readLengths = new ArrayList<>();
    private List<String> referenceSequences = new ArrayList<>();
    // Root sample name are treated like sample metadata and get passed on in the pipeline query.
    // Roots are not part of Mercury's chain of custody.
    private List<String> rootSampleNames = new ArrayList<>();
    private List<String> sampleNames = new ArrayList<>();
    private List<String> sequencingTechnologies = new ArrayList<>();
    private List<String> sexes = new ArrayList<>();
    private List<String> umisPresents = new ArrayList<>();
    private List<String> volumes = new ArrayList<>();

    public ExternalLibraryProcessor() {
        super(null);
    }

    public enum DataPresence {REQUIRED, ONCE_PER_TUBE, OPTIONAL, IGNORED}
    private final static boolean EXPLICIT_DELETE = true;
    private final static int ALIAS_LENGTH_LIMIT = 25;
    private final static int AGGREGATION_PARTICLE_LENGTH_LIMIT = 20;

    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        TUBE_BARCODE("Sample Tube Barcode", DataPresence.REQUIRED),
        LIBRARY_NAME("Library Name", DataPresence.REQUIRED),
        SAMPLE_NAME("Broad Sample ID", DataPresence.OPTIONAL),
        ROOT_SAMPLE_NAME("Root Sample ID", DataPresence.OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", DataPresence.OPTIONAL),
        BAIT("Bait", DataPresence.OPTIONAL),
        DATA_AGGREGATOR("Data Aggregator/Project Title", DataPresence.OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", DataPresence.OPTIONAL, EXPLICIT_DELETE),
        INDIVIDUAL_NAME("Individual Name (Patient Id)", DataPresence.OPTIONAL, EXPLICIT_DELETE),
        SEX("Sex", DataPresence.OPTIONAL, EXPLICIT_DELETE),
        ORGANISM("Organism", DataPresence.OPTIONAL, EXPLICIT_DELETE),
        DATA_ANALYSIS_TYPE("Data Analysis Type", DataPresence.REQUIRED),
        AGGREGATION_DATA_TYPE("Aggregation Data Type", DataPresence.OPTIONAL),
        READ_LENGTH("Desired Read Length", DataPresence.REQUIRED),
        UMIS_PRESENT("UMIs Present", DataPresence.OPTIONAL),
        VOLUME("Volume", DataPresence.ONCE_PER_TUBE),
        FRAGMENT_SIZE("Fragment Size", DataPresence.ONCE_PER_TUBE),
        CONCENTRATION("Concentration (ng/uL)", DataPresence.ONCE_PER_TUBE),
        INSERT_SIZE_RANGE("Insert Size Range", DataPresence.OPTIONAL),
        REFERENCE_SEQUENCE("Reference Sequence", DataPresence.REQUIRED),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", DataPresence.REQUIRED),
        ;

        private final String text;
        private final DataPresence dataPresence;
        private final boolean explicitDelete;

        Headers(String text, DataPresence dataPresence) {
            this(text, dataPresence, false);
        }

        Headers(String text, DataPresence dataPresence, boolean explicitDelete) {
            this.text = text;
            this.dataPresence = dataPresence;
            this.explicitDelete = explicitDelete;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return dataPresence == DataPresence.REQUIRED;
        }

        @Override
        public boolean isRequiredValue() {
            return dataPresence == DataPresence.REQUIRED;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return true;
        }

        public DataPresence getDataPresenceIndicator() {
            return dataPresence;
        }

        public boolean isOncePerTube() {
            return dataPresence == DataPresence.ONCE_PER_TUBE;
        }

        /** Indicates a DELETE_TOKEN is needed to remove an existing value. */
        public boolean isExplicitDelete() {
            return explicitDelete;
        }
    }

    private final Map<Metadata.Key, String> keyToHeader = new HashMap<Metadata.Key, String>() {{
        put(Metadata.Key.SAMPLE_ID, Headers.COLLABORATOR_SAMPLE_ID.getText());
        put(Metadata.Key.ROOT_SAMPLE, Headers.ROOT_SAMPLE_NAME.getText());
        put(Metadata.Key.SAMPLE_ID, Headers.COLLABORATOR_SAMPLE_ID.getText());
        put(Metadata.Key.PATIENT_ID, Headers.INDIVIDUAL_NAME.getText());
        put(Metadata.Key.GENDER, Headers.SEX.getText());
        put(Metadata.Key.SPECIES, Headers.ORGANISM.getText());
    }};


    /** Returns the canonical header names, not the actual ones. */
    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    /** Returns names that appeared in the spreadsheet, not the canonical ones. */
    @Override
    public List<String> getHeaderNames() {
        return headerNames;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerNames.addAll(headers);
    }

    @Override
    public void close() {
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE, dataRowNumber));
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME, dataRowNumber));
        sampleNames.add(getFromRow(dataRow, Headers.SAMPLE_NAME, dataRowNumber));
        rootSampleNames.add(getFromRow(dataRow, Headers.ROOT_SAMPLE_NAME, dataRowNumber));
        molecularBarcodeNames.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME, dataRowNumber));
        baits.add(getFromRow(dataRow, Headers.BAIT, dataRowNumber));
	    aggregationParticles.add(getFromRow(dataRow, Headers.DATA_AGGREGATOR, dataRowNumber));
        collaboratorSampleIds.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID, dataRowNumber));
        collaboratorParticipantIds.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME, dataRowNumber));
        sexes.add(getFromRow(dataRow, Headers.SEX, dataRowNumber));
        organisms.add(getFromRow(dataRow, Headers.ORGANISM, dataRowNumber));
        dataAnalysisTypes.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE, dataRowNumber));
        aggregationDataTypes.add(getFromRow(dataRow, Headers.AGGREGATION_DATA_TYPE, dataRowNumber));
        readLengths.add(getFromRow(dataRow, Headers.READ_LENGTH, dataRowNumber));
        umisPresents.add(getFromRow(dataRow, Headers.UMIS_PRESENT, dataRowNumber));
        volumes.add(getFromRow(dataRow, Headers.VOLUME, dataRowNumber));
        fragmentSizes.add(getFromRow(dataRow, Headers.FRAGMENT_SIZE, dataRowNumber));
        concentrations.add(getFromRow(dataRow, Headers.CONCENTRATION, dataRowNumber));
        insertSizes.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE, dataRowNumber));
        referenceSequences.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE, dataRowNumber));
        sequencingTechnologies.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY, dataRowNumber));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities have already been fetched for the row data, and are accessed
     * through maps referenced in the dtos.
     */
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite, MessageCollection messages) {
        Set<String> uniqueBarcodeAndLibrary = new HashSet<>();
        Set<String> uniqueTubeAndMis = new HashSet<>();
        Set<String> uniqueSampleAndMis = new HashSet<>();

        for (SampleInstanceEjb.RowDto dto : dtos) {
            // If the sample appears in multiple spreadsheet rows, the first occurrence supplies all of the
            // sample metadata and subsequent rows must be consistent (match the first, or be blank).
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                consistentSampleData(mapSampleNameToFirstRow.get(dto.getSampleName()), dto, messages);
            }

            // If a tube appears multiple times in the upload, the vessel attributes must be consistent with
            // the first occurrence (match the first, or be blank).
            String barcode = dto.getBarcode();
            if (StringUtils.isNotBlank(barcode)) {
                consistentTubeData(mapBarcodeToFirstRow.get(barcode), dto, messages);
            }

            // Library name must be unique in each tube.
            if (!uniqueBarcodeAndLibrary.add(barcode + " " + dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(),
                        Headers.LIBRARY_NAME.getText()));
            }

            LabVessel tube = labVesselMap.get(barcode);
            // If an existing tube barcode is given then this upload will replace the contents
            // of the tube and overwrite must be set.
            if (tube != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber()));
            }

            if (StringUtils.isNotBlank(dto.getMisName())) {
                if (molecularIndexingSchemeMap.get(dto.getMisName()) == null) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.MOLECULAR_BARCODE_NAME.getText(), "Mercury"));
                }
                // Errors if a tube has duplicate Molecular Index Scheme.
                if (StringUtils.isNotBlank(barcode) && !uniqueTubeAndMis.add(barcode + " " + dto.getMisName())) {
                    messages.addError(String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, dto.getRowNumber(),
                            Headers.MOLECULAR_BARCODE_NAME.getText(), barcode));
                }
                // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme.
                // It's not an error as long as the tubes don't get pooled later on. This can't be known at
                // upload time, so only make a warning.
                String sampleMis = dto.getSampleName() + " " + dto.getMisName();
                if (!uniqueSampleAndMis.add(sampleMis)) {
                    messages.addWarning(String.format(SampleInstanceEjb.DUPLICATE_S_M, dto.getRowNumber(),
                            dto.getSampleName(), dto.getMisName()));
                }
            }

            // If a bait set name is given it must already be registered in Mercury.
            if (StringUtils.isNotBlank(dto.getBait()) && baitMap.get(dto.getBait()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.BAIT.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getAnalysisTypeName()) &&
                    analysisTypeMap.get(dto.getAnalysisTypeName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.DATA_ANALYSIS_TYPE.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getAggregationDataType()) &&
                    aggregationDataTypeMap.get(dto.getAggregationDataType()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.AGGREGATION_DATA_TYPE.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getReferenceSequence()) &&
                    referenceSequenceMap.get(dto.getReferenceSequence()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.REFERENCE_SEQUENCE.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getSequencingTechnology()) &&
                    IlluminaFlowcell.FlowcellType.getByTechnology(dto.getSequencingTechnology()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
            }

            // Compares the spreadsheet sample metadata to existing metadata to determine if there are updates.
            // Overwrite must be set to update Mercury metadata. Inherited metadata and BSP metadata cannot be updated.
            if (mapSampleNameToFirstRow.get(dto.getSampleName()).equals(dto)) {
                MercurySample mercurySample = sampleMap.get(dto.getSampleName());
                SampleData sampleData = getFetchedData().get(dto.getSampleName());
                SampleData rootSampleData = getFetchedData().get(dto.getRootSampleName());
                // Metadata inherited from the root sample is used for spreadsheet validation
                // only when the root sample exists and the sample metadata source is Mercury
                // (including when the sample is new to Mercury and not in BSP).
                boolean isMercurySource = (mercurySample != null &&
                        mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) ||
                        (mercurySample == null && sampleData == null);
                if (isMercurySource && rootSampleData != null && !dto.getSampleName().equals(dto.getRootSampleName())) {
                    Collection<Metadata> existingMetadata = makeSampleMetadata(rootSampleData, true);
                    Collection<Metadata> dtoMetadata = makeSampleMetadata(dto, true, false);
                    // Finds changes and new additions to the inheritance metadata.
                    Collection<Metadata> updates = CollectionUtils.subtract(dtoMetadata, existingMetadata);
                    if (!updates.isEmpty()) {
                        messages.addError(SampleInstanceEjb.ROOT_METADATA, dto.getRowNumber(),
                                formatMetadataChanges(updates, existingMetadata));
                    }
                }
                // Validates spreadsheet against sample metadata.
                if (sampleData != null) {
                    Collection<Metadata> existingMetadata = makeSampleMetadata(sampleData, false);
                    Collection<Metadata> dtoMetadata = makeSampleMetadata(dto, false, false);
                    Collection<Metadata> updates = CollectionUtils.subtract(dtoMetadata, existingMetadata);
                    if (!updates.isEmpty()) {
                        String message = formatMetadataChanges(updates, existingMetadata);
                        if (sampleData.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                            messages.addError(SampleInstanceEjb.BSP_METADATA, dto.getRowNumber(), message);
                        } else if (!overwrite) {
                            messages.addError(SampleInstanceEjb.MERCURY_METADATA, dto.getRowNumber(), message);
                        }
                    }
                }
            }

            // Checks alias length for limits imposed by the pipeline.
            Stream.of(Pair.of(dto.getCollaboratorSampleId(), Headers.COLLABORATOR_SAMPLE_ID),
                    Pair.of(dto.getCollaboratorParticipantId(), Headers.INDIVIDUAL_NAME)).
                    filter(pair -> StringUtils.isNotBlank(pair.getLeft()) &&
                            pair.getLeft().length() > ALIAS_LENGTH_LIMIT).
                    forEach(pair -> messages.addError(String.format(SampleInstanceEjb.TOO_LONG,
                            dto.getRowNumber(), pair.getRight().getText(), ALIAS_LENGTH_LIMIT)));
            // Checks aggregation particle length.
            if (StringUtils.isNotBlank(dto.getAggregationParticle()) &&
                    dto.getAggregationParticle().length() > AGGREGATION_PARTICLE_LENGTH_LIMIT) {
                messages.addError(String.format(SampleInstanceEjb.TOO_LONG, dto.getRowNumber(),
                        Headers.DATA_AGGREGATOR.getText(), AGGREGATION_PARTICLE_LENGTH_LIMIT));
            }
        }
    }

    private String formatMetadataChanges(Collection<Metadata> changes, Collection<Metadata> existing) {
        Map<Metadata.Key, String> existingValues = existing.stream().
                collect(Collectors.toMap(Metadata::getKey, Metadata::getValue));
        return StringUtils.trimToEmpty(changes.stream().
                map(metadata -> {
                    String oldValue = existingValues.get(metadata.getKey());
                    return String.format(StringUtils.isBlank(oldValue) ? "%s (currently blank)" : "%s (=%s)",
                            keyToHeader.get(metadata.getKey()), oldValue);
                }).
                sorted().
                collect(Collectors.joining(", ")));
    }

    /**
     * Makes the Samples, LabVessels, and SampleInstanceEntities but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     * @return the list of SampleInstanceEntity, for testing purposes.
     */
    public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos) {
        makeTubesAndSamples(dtos);
        return makeSampleInstanceEntities(dtos);
    }

    /**
     * Returns the data value in the row for the given header.
     */
    private String getFromRow(Map<String, String> dataRow, Headers header, int rowNumber) {
        String data = dataRow.get(header.getText());
        if (DELETE_TOKEN.equalsIgnoreCase(data)) {
            if (header.isRequiredValue()) {
                addDataMessage("Cannot use " + DELETE_TOKEN + " for " + header.getText(), rowNumber);
            }
            if (!header.isExplicitDelete()) {
                data = "";
            }
        }
        return data;
    }

    /** Converts the 0-based index into the row number shown at the far left side in Excel. */
    private int toRowNumber(int rowIndex) {
        return rowIndex + getHeaderRowIndex() + 2;
    }

    /** Converts the dto rowNumber to 0-based index for the spreadsheet data lookups. */
    private int toRowIndex(int rowNumber) {
        return rowNumber - getHeaderRowIndex() - 2;
    }

    public List<SampleInstanceEjb.RowDto> makeDtos(MessageCollection messages) {
        List<SampleInstanceEjb.RowDto> dtos = new ArrayList<>();
        for (int index = 0; index < Math.max(getLibraryNames().size(), getSampleNames().size()); ++index) {
            SampleInstanceEjb.RowDto dto = new SampleInstanceEjb.RowDto(toRowNumber(index));
            dtos.add(dto);

            dto.setBarcode(get(getBarcodes(), index));
            if (StringUtils.isNotBlank(dto.getBarcode()) && !mapBarcodeToFirstRow.containsKey(dto.getBarcode())) {
                mapBarcodeToFirstRow.put(dto.getBarcode(), dto);
            }
            dto.setLibraryName(get(getLibraryNames(), index));
            dto.setSampleName(get(getSampleNames(), index));
            if (StringUtils.isBlank(dto.getSampleName())) {
                // External library uploads do not require a sample name, but Mercury needs one so it uses the
                // library name, and sets impliedSampleName so that the pipeline query outputs a null sample name.
                dto.setImpliedSampleName(true);
                dto.setSampleName(dto.getLibraryName());
            }
            if (StringUtils.isNotBlank(dto.getSampleName()) &&
                    !mapSampleNameToFirstRow.containsKey(dto.getSampleName())) {
                mapSampleNameToFirstRow.put(dto.getSampleName(), dto);
            }
            dto.setRootSampleName(get(getRootSampleNames(), index));
            dto.setMisName(get(getMolecularBarcodeNames(), index));
            dto.setBait(get(getBaits(), index));
            dto.setAggregationParticle(get(getAggregationParticles(), index));
            dto.setCollaboratorSampleId(get(getCollaboratorSampleIds(), index));
            dto.setCollaboratorParticipantId(get(getCollaboratorParticipantIds(), index));
            dto.setSex(get(getSexes(), index));
            dto.setOrganism(get(getOrganisms(), index));
            dto.setAnalysisTypeName(get(getDataAnalysisTypes(), index));
            dto.setAggregationDataType(get(getAggregationDataTypes(), index));
            dto.setReadLength(asNonNegativeInteger(get(getReadLengths(), index),
                    Headers.READ_LENGTH.getText(), dto.getRowNumber(), messages));
            dto.setUmisPresent(get(getUmisPresents(), index));
            dto.setVolume(asNonNegativeBigDecimal(get(getVolumes(), index),
                    Headers.VOLUME.getText(), dto.getRowNumber(), messages));
            dto.setFragmentSize(asNonNegativeInteger(get(getFragmentSizes(), index),
                    Headers.FRAGMENT_SIZE.getText(), dto.getRowNumber(), messages));
            dto.setConcentration(asNonNegativeBigDecimal(get(getConcentrations(), index),
                    Headers.CONCENTRATION.getText(), dto.getRowNumber(), messages));
            dto.setInsertSize(asIntegerRange(get(getInsertSizes(), index),
                    Headers.INSERT_SIZE_RANGE.getText(), dto.getRowNumber(), messages));
            dto.setReferenceSequence(get(getReferenceSequences(), index));
            dto.setSequencingTechnology(get(getSequencingTechnologies(), index));
        }
        return dtos;
    }

    /**
     * Compares the metadata and other sample-related data on the found dto to the expected dto and errors if
     * values are inconsistent, i.e. values must match or be blank on the found dto.
     */
    private void consistentSampleData(SampleInstanceEjb.RowDto expected, SampleInstanceEjb.RowDto found,
            MessageCollection messages) {
        Stream.of(
                Triple.of(found.getRootSampleName(), expected.getRootSampleName(),
                        Headers.ROOT_SAMPLE_NAME.getText()),
                Triple.of(found.getSex(), expected.getSex(), Headers.SEX.getText()),
                Triple.of(found.getOrganism(), expected.getOrganism(), Headers.ORGANISM.getText()),
                Triple.of(found.getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(),
                        Headers.INDIVIDUAL_NAME.getText()),
                Triple.of(found.getCollaboratorSampleId(), expected.getCollaboratorSampleId(),
                        Headers.COLLABORATOR_SAMPLE_ID.getText())
        ).
                filter(triple -> StringUtils.isNotBlank(triple.getLeft())).
                filter(triple -> !Objects.equals(triple.getLeft(), triple.getMiddle())).
                forEach(triple -> messages.addError(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA,
                        found.getRowNumber(), triple.getRight(),
                        mapSampleNameToFirstRow.get(found.getSampleName()).getRowNumber(), found.getSampleName()));
    }

    /**
     * Compares the tube data on the found dto to the expected dto and errors if
     * values are inconsistent, i.e. values must match or be blank on the found dto.
     */
    private void consistentTubeData(SampleInstanceEjb.RowDto expected, SampleInstanceEjb.RowDto found,
            MessageCollection messages) {

        if (expected.getRowNumber() == found.getRowNumber()) {
            // If the Header enum has them as ONCE_PER_TUBE (rows need only provide the values in the first
            // row for each tube) then a missing value is checked here. If the Header enum has them as REQUIRED
            // then the missing value has already been checked for earlier in the code.
            if (Headers.VOLUME.isOncePerTube() &&
                    StringUtils.isBlank(get(getVolumes(), toRowIndex(found.getRowNumber())))) {
                messages.addError(String.format(SampleInstanceEjb.MISSING, found.getRowNumber(),
                        Headers.VOLUME.getText()));
            }
            if (Headers.CONCENTRATION.isOncePerTube() &&
                    StringUtils.isBlank(get(getConcentrations(), toRowIndex(found.getRowNumber())))) {
                messages.addError(String.format(SampleInstanceEjb.MISSING, found.getRowNumber(),
                        Headers.CONCENTRATION.getText()));
            }
            if (Headers.FRAGMENT_SIZE.isOncePerTube() &&
                    StringUtils.isBlank(get(getFragmentSizes(), toRowIndex(found.getRowNumber())))) {
                messages.addError(String.format(SampleInstanceEjb.MISSING, found.getRowNumber(),
                        Headers.FRAGMENT_SIZE.getText()));
            }
        } else {
            if (found.getVolume() != null && !Objects.equals(found.getVolume(), expected.getVolume())) {
                messages.addError(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        Headers.VOLUME.getText(),
                        mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(), found.getBarcode()));
            }
            if (found.getConcentration() != null &&
                    !Objects.equals(found.getConcentration(), expected.getConcentration())) {
                messages.addError(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        Headers.CONCENTRATION.getText(),
                        mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(), found.getBarcode()));
            }
            if (found.getFragmentSize() != null &&
                    !Objects.equals(found.getFragmentSize(), expected.getFragmentSize())) {
                messages.addError(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        Headers.FRAGMENT_SIZE.getText(),
                        mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(), found.getBarcode()));
            }
        }
    }

    /**
     * Does character set validation of the spreadsheet data.
     */
    public void validateCharacterSet(List<SampleInstanceEjb.RowDto> dtos, MessageCollection messages) {
        for (SampleInstanceEjb.RowDto dto : dtos) {
            // Barcode, library, and sample must be from the RESTRICTED_CHARS set.
            Stream.of(Pair.of(dto.getBarcode(), Headers.TUBE_BARCODE),
                    Pair.of(dto.getLibraryName(), Headers.LIBRARY_NAME),
                    Pair.of(dto.getSampleName(), Headers.SAMPLE_NAME),
                    Pair.of(dto.getAggregationParticle(), Headers.DATA_AGGREGATOR)).
                    filter(pair -> StringUtils.isNotBlank(pair.getKey()) &&
                            !StringUtils.containsOnly(pair.getKey(), SampleInstanceEjb.RESTRICTED_CHARS)).
                    forEachOrdered(pair ->
                            messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                                    pair.getValue().getText(), SampleInstanceEjb.RESTRICTED_CHARS)));

            // Sample aliases must be from the ALIAS_CHARS set.
            Stream.of(Pair.of(dto.getCollaboratorSampleId(), Headers.COLLABORATOR_SAMPLE_ID),
                    Pair.of(dto.getCollaboratorParticipantId(), Headers.INDIVIDUAL_NAME),
                    Pair.of(dto.getOrganism(), Headers.ORGANISM)).
                    filter(pair -> StringUtils.isNotBlank(pair.getKey()) &&
                            !DELETE_TOKEN.equals(pair.getKey()) &&
                            !StringUtils.containsOnly(pair.getKey(), SampleInstanceEjb.ALIAS_CHARS)).
                    forEachOrdered(pair ->
                            messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                                    pair.getValue().getText(), SampleInstanceEjb.ALIAS_CHARS)));
        }
    }

    /**
     * Creates/updates tube and sample for each unique barcode and sample name.
     */
    private void makeTubesAndSamples(List<SampleInstanceEjb.RowDto> dtos) {
        try {
            // Creates/updates tubes. Sets tube volume and concentration.
            dtos.stream().
                    map(SampleInstanceEjb.RowDto::getBarcode).
                    distinct().
                    forEach(barcode -> {
                        LabVessel labVessel = labVesselMap.get(barcode);
                        if (labVessel == null) {
                            labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                            labVesselMap.put(barcode, labVessel);
                        } else {
                            // Tube is being re-uploaded, so clearing old samples is appropriate.
                            labVessel.getMercurySamples().clear();
                            labVessel.getMetrics().clear();
                        }
                        SampleInstanceEjb.RowDto firstRowHavingTube = mapBarcodeToFirstRow.get(barcode);
                        labVessel.setVolume(firstRowHavingTube.getVolume());
                        labVessel.setConcentration(firstRowHavingTube.getConcentration());
                        SampleInstanceEjb.addLibrarySize(labVessel,
                                new BigDecimal(firstRowHavingTube.getFragmentSize()));
                    });

            // Creates/updates samples.
            dtos.stream().
                    map(SampleInstanceEjb.RowDto::getSampleName).
                    filter(StringUtils::isNotBlank).
                    distinct().
                    forEach(sampleName -> {
                        MercurySample mercurySample = sampleMap.get(sampleName);
                        if (mercurySample == null) {
                            SampleData sampleData = getFetchedData().get(sampleName);
                            MercurySample.MetadataSource source = (sampleData != null) ?
                                    // If the mercurySample is null but there is fetched sampleData
                                    // for the sample, assumes it must be a BSP sample.
                                    MercurySample.MetadataSource.BSP : MercurySample.MetadataSource.MERCURY;
                            mercurySample = new MercurySample(sampleName, source);
                            sampleMap.put(sampleName, mercurySample);
                        }
                        if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                            SampleInstanceEjb.RowDto firstDto = mapSampleNameToFirstRow.get(sampleName);
                            mercurySample.updateMetadata(makeSampleMetadata(firstDto, false, true));
                        }
                    });
        } catch (Exception e) {
            log.error(e);
        }
    }

    /** Makes or updates a sample along with its sample metadata. */
    private void createOrUpdateSample(SampleInstanceEjb.RowDto dto) {
    }

    /**
     * Creates/updates tube and sample for each dto row.
     */
    private List<SampleInstanceEntity> makeSampleInstanceEntities(List<SampleInstanceEjb.RowDto> rowDtos) {
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        for (SampleInstanceEjb.RowDto dto : rowDtos) {
            LabVessel labVessel = labVesselMap.get(dto.getBarcode());
            MercurySample mercurySample = sampleMap.get(dto.getSampleName());
            mercurySample.addLabVessel(labVessel);

            SampleInstanceEntity sampleInstanceEntity = makeSampleInstanceEntity(dto, labVessel, mercurySample);

            labVessel.getSampleInstanceEntities().add(sampleInstanceEntity);
            sampleInstanceEntities.add(sampleInstanceEntity);
        }
        return sampleInstanceEntities;
    }

    /**
     * Makes Metadata from the spreadsheet row.
     */
    private Set<Metadata> makeSampleMetadata(SampleInstanceEjb.RowDto dto, boolean onlyInheritanceMetadata,
            boolean includeMaterialType) {

        return (new ArrayList<Pair<String, Metadata.Key>>() {{
            add(Pair.of(dto.getCollaboratorSampleId(), Metadata.Key.SAMPLE_ID));
            add(Pair.of(dto.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID));
            add(Pair.of(dto.getSex(), Metadata.Key.GENDER));
            add(Pair.of(dto.getOrganism(), Metadata.Key.SPECIES));
            if (!onlyInheritanceMetadata) {
                add(Pair.of(dto.getRootSampleName(), Metadata.Key.ROOT_SAMPLE));
                if (includeMaterialType) {
                    add(Pair.of(dto.getMaterialType(), Metadata.Key.MATERIAL_TYPE));
                }
            }
        }}).stream().
                filter(pair -> StringUtils.isNotBlank(pair.getLeft())).
                map(pair -> DELETE_TOKEN.equalsIgnoreCase(pair.getLeft()) ?
                        new Metadata(pair.getRight(), "") :  // DELETE_TOKEN is blanked out.
                        new Metadata(pair.getRight(), pair.getLeft())).
                        collect(Collectors.toSet());
    }

    /**
     * Makes Metadata from SampleData.
     */
    private Set<Metadata> makeSampleMetadata(SampleData sampleData, boolean onlyInheritanceMetadata) {
        return (new ArrayList<Pair<String, Metadata.Key>>() {{
            add(Pair.of(sampleData.getCollaboratorsSampleName(), Metadata.Key.SAMPLE_ID));
            add(Pair.of(sampleData.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID));
            add(Pair.of(sampleData.getGender(), Metadata.Key.GENDER));
            add(Pair.of(sampleData.getOrganism(), Metadata.Key.SPECIES));
            if (!onlyInheritanceMetadata) {
                add(Pair.of(sampleData.getRootSample(), Metadata.Key.ROOT_SAMPLE));
                add(Pair.of(sampleData.getMaterialType(), Metadata.Key.MATERIAL_TYPE));
            }
        }}).stream().
                filter(pair -> StringUtils.isNotBlank(pair.getLeft())).
                map(pair -> new Metadata(pair.getRight(), pair.getLeft())).
                collect(Collectors.toSet());
    }

    /**
     * Makes a new SampleInstanceEntity from the row data.
     */
    private SampleInstanceEntity makeSampleInstanceEntity(SampleInstanceEjb.RowDto dto, LabVessel labVessel,
            MercurySample mercurySample) {

        SampleInstanceEntity sampleInstanceEntity = new SampleInstanceEntity();
        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setLibraryName(dto.getLibraryName());
        sampleInstanceEntity.setAggregationDataType(dto.getAggregationDataType());
        sampleInstanceEntity.setAggregationParticle(dto.getAggregationParticle());
        sampleInstanceEntity.setAnalysisType(analysisTypeMap.get(dto.getAnalysisTypeName()));
        sampleInstanceEntity.setInsertSize(dto.getInsertSize());
        sampleInstanceEntity.setMercurySample(mercurySample);
        sampleInstanceEntity.setImpliedSampleName(dto.isImpliedSampleName());
        sampleInstanceEntity.setMolecularIndexingScheme(molecularIndexingSchemeMap.get(dto.getMisName()));
        sampleInstanceEntity.setReadLength(dto.getReadLength());
        sampleInstanceEntity.setReagentDesign(baitMap.get(dto.getBait()));
        sampleInstanceEntity.setReferenceSequence(referenceSequenceMap.get(dto.getReferenceSequence()));
        sampleInstanceEntity.setSequencerModel(IlluminaFlowcell.FlowcellType.getByTechnology(
                dto.getSequencingTechnology()));
        sampleInstanceEntity.setUploadDate(new Date());
        sampleInstanceEntity.setUmisPresent(dto.getUmisPresent());
        return sampleInstanceEntity;
    }

    /**
     * Returns the list element or null if the list or element doesn't exist
     */
    private <T> T get(List<T> list, int index) {
        return SampleInstanceEjb.get(list, index);
    }

    /**
     * Converts to integer and returns 0 for a blank or non-numeric input.
     */
    public static Integer asInteger(String input) {
        return StringUtils.isNumeric(StringUtils.trimToEmpty(input)) ? new Integer(input) : 0;
    }

    /**
     * Converts to integer and returns null for a blank input. Issues error message for non-numeric input.
     */
    private static Integer asNonNegativeInteger(String value, String header, int rowNumber,
            @Nullable MessageCollection messages) {

        if (StringUtils.isNotBlank(value)) {
            if (StringUtils.isNumeric(value) && Integer.parseInt(value) >= 0) {
                return Integer.parseInt(value);
            } else if (messages != null) {
                messages.addError(String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, rowNumber, header));
            }
        }
        return null;
    }

    /**
     * Converts to BigDecimal and returns null for a blank input. Issues error message for non-numeric input.
     */
    public static BigDecimal asNonNegativeBigDecimal(String value, String header, int rowNumber,
            @Nullable MessageCollection messages) {

        if (StringUtils.isNotBlank(value)) {
            if (NumberUtils.isNumber(value) && Float.parseFloat(value) >= 0) {
                return MathUtils.scaleTwoDecimalPlaces(new BigDecimal(value));
            } else if (messages != null) {
                messages.addError(String.format(SampleInstanceEjb.NONNEGATIVE_DECIMAL, rowNumber, header));
            }
        }
        return null;
    }

    /**
     * Returns true if it starts with upper or lower case "y" "t" or 1, false if starts with
     * anything else, and null if it's blank.
     */
    public static Boolean asBoolean(String value) {
        return StringUtils.isBlank(value) ? null :
                "yt1".contains(value.toLowerCase().subSequence(0, 1)) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * If input is one integer, or two integers delimited by one of these chars: [ ,-_|/:] then
     * returns an integer range consisting of two integers delimited with a hyphen.
     */
    private static String asIntegerRange(String input, String header, int rowNumber, MessageCollection messages) {
        if (StringUtils.isNotBlank(input)) {
            Integer[] range = {null, null};
            String[] tokens = input.split("[ ,\\-_|/:]");
            for (String token : tokens) {
                if (StringUtils.isNumeric(token)) {
                    int value = Integer.parseInt(token);
                    if (value >= 0) {
                        // Accepts a single integer and turns it into a range.
                        if (tokens.length == 1) {
                            range[0] = value;
                            range[1] = value;
                        } else {
                            if (range[1] != null) {
                                // Too many integers.
                                messages.addError(String.format(SampleInstanceEjb.BAD_RANGE, rowNumber, header));
                                return null;
                            }
                            range[range[0] == null ? 0 : 1] = value;
                        }
                    }
                } else if (StringUtils.isNotBlank(token)) {
                    // Invalid character or word.
                    messages.addError(String.format(SampleInstanceEjb.BAD_RANGE, rowNumber, header));
                    return null;
                }
            }
            if (range[0] != null && range[1] != null) {
                return String.format("%d-%d", range[0], range[1]);
            }
            messages.addError(String.format(SampleInstanceEjb.BAD_RANGE, rowNumber, header));
        }
        return null;
    }

    /**
     * Maps of upload value to the corresponding entity.
     */
    public Map<String, MercurySample> getSampleMap() {
        return sampleMap;
    }

    public Map<String, LabVessel> getLabVesselMap() {
        return labVesselMap;
    }

    public Map<String, SampleData> getFetchedData() {
        return fetchedData;
    }

    public Map<String, AnalysisType> getAnalysisTypeMap() {
        return analysisTypeMap;
    }

    public Map<String, ReagentDesign> getBaitMap() {
        return baitMap;
    }

    public Map<String, ReferenceSequence> getReferenceSequenceMap() {
        return referenceSequenceMap;
    }

    public Map<String, MolecularIndexingScheme> getMolecularIndexingSchemeMap() {
        return molecularIndexingSchemeMap;
    }

    public Map<String, String> getAggregationDataTypeMap() {
        return aggregationDataTypeMap;
    }

    /**
     * The entities that need to be persisted.
     */
    public Set<Object> getEntitiesToPersist() {
        return entitiesToUpdate;
    }

    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }

    public List<String> getAggregationParticles() {
        return aggregationParticles;
    }

    public List<String> getBaits() {
        return baits;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public List<String> getCollaboratorParticipantIds() {
        return collaboratorParticipantIds;
    }

    public List<String> getCollaboratorSampleIds() {
        return collaboratorSampleIds;
    }

    public List<String> getConcentrations() {
        return concentrations;
    }

    public List<String> getDataAnalysisTypes() {
        return dataAnalysisTypes;
    }

    public List<String> getFragmentSizes() {
        return fragmentSizes;
    }

    public List<String> getInsertSizes() {
        return insertSizes;
    }

    public List<String> getLibraryNames() {
        return libraryNames;
    }

    public List<String> getMolecularBarcodeNames() {
        return molecularBarcodeNames;
    }

    public List<String> getOrganisms() {
        return organisms;
    }

    public List<String> getReadLengths() {
        return readLengths;
    }

    public List<String> getAggregationDataTypes() {
        return aggregationDataTypes;
    }

    public List<String> getReferenceSequences() {
        return referenceSequences;
    }

    public List<String> getRootSampleNames() {
        return rootSampleNames;
    }

    public List<String> getSampleNames() {
        return sampleNames;
    }

    public List<String> getSequencingTechnologies() {
        return sequencingTechnologies;
    }

    public List<String> getSexes() {
        return sexes;
    }

    public List<String> getUmisPresents() {
        return umisPresents;
    }

    public List<String> getVolumes() {
        return volumes;
    }
}
