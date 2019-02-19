package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    public enum DataPresence {REQUIRED, ONCE_PER_TUBE, OPTIONAL, IGNORED};

    // Maps tube barcode to the first dto that contains that barcode (and associated tube data).
    protected Map<String, SampleInstanceEjb.RowDto> mapBarcodeToFirstRow = new HashMap<>();
    // Maps sample name to the first dto that contains that sample name (and associated sample data).
    protected Map<String, SampleInstanceEjb.RowDto> mapSampleNameToFirstRow = new HashMap<>();
    // Spreadsheet's actual header names.
    protected List<String> headerNames = new ArrayList<>();
    // Maps sample name to MercurySample.
    private Map<String, MercurySample> sampleMap = new HashMap<>();
    // Maps barcode to LabVessel.
    private Map<String, LabVessel> labVesselMap = new HashMap<>();
    // Maps sample name to BSP or Mercury SampleData.
    private Map<String, SampleData> fetchedData = new HashMap<>();
    private Map<String, AnalysisType> analysisTypeMap = new HashMap<>();
    private Map<String, MolecularIndexingScheme> molecularIndexingSchemeMap = new HashMap<>();
    private Map<String, ReferenceSequence> referenceSequenceMap = new HashMap<>();
    private Set<Object> entitiesToUpdate = new HashSet<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();
    private List<String> aggregationParticles = new ArrayList<>();
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
    private List<String> rootSampleNames = new ArrayList<>();
    private List<String> sampleNames = new ArrayList<>();
    private List<String> sequencingTechnologies = new ArrayList<>();
    private List<String> sexes = new ArrayList<>();
    private List<String> umisPresents = new ArrayList<>();
    private List<String> volumes = new ArrayList<>();

    public ExternalLibraryProcessor() {
        super(null);
    }

    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        TUBE_BARCODE("Sample Tube Barcode", DataPresence.REQUIRED),
        LIBRARY_NAME("Library Name", DataPresence.REQUIRED),
        SAMPLE_NAME("Broad Sample ID", DataPresence.OPTIONAL),
        ROOT_SAMPLE_NAME("Root Sample ID", DataPresence.OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", DataPresence.OPTIONAL),
        BAIT("Bait", DataPresence.OPTIONAL),
        DATA_AGGREGATOR("Data Aggregator/Project Title", DataPresence.OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", DataPresence.OPTIONAL),
        INDIVIDUAL_NAME("Individual Name (Patient Id)", DataPresence.OPTIONAL),
        SEX("Sex", DataPresence.OPTIONAL),
        ORGANISM("Organism", DataPresence.OPTIONAL),
        DATA_ANALYSIS_TYPE("Data Analysis Type", DataPresence.REQUIRED),
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

        Headers(String text, DataPresence dataPresence) {
            this.text = text;
            this.dataPresence = dataPresence;
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
    }

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
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        sampleNames.add(getFromRow(dataRow, Headers.SAMPLE_NAME));
        rootSampleNames.add(getFromRow(dataRow, Headers.ROOT_SAMPLE_NAME));
        molecularBarcodeNames.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        baits.add(getFromRow(dataRow, Headers.BAIT));
	    aggregationParticles.add(getFromRow(dataRow, Headers.DATA_AGGREGATOR));
        collaboratorSampleIds.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        collaboratorParticipantIds.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        sexes.add(getFromRow(dataRow, Headers.SEX));
        organisms.add(getFromRow(dataRow, Headers.ORGANISM));
        dataAnalysisTypes.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        readLengths.add(getFromRow(dataRow, Headers.READ_LENGTH));
        umisPresents.add(getFromRow(dataRow, Headers.UMIS_PRESENT));
        volumes.add(getFromRow(dataRow, Headers.VOLUME));
        fragmentSizes.add(getFromRow(dataRow, Headers.FRAGMENT_SIZE));
        concentrations.add(getFromRow(dataRow, Headers.CONCENTRATION));
        insertSizes.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE));
        referenceSequences.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        sequencingTechnologies.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite, MessageCollection messages) {
        // At this point all data is in dtos, and entities referenced by the data are in maps.
        Set<String> uniqueBarcodeAndLibrary = new HashSet<>();
        Set<String> uniqueTubeAndMis = new HashSet<>();
        Set<String> uniqueSampleAndMis = new HashSet<>();

        for (SampleInstanceEjb.RowDto dto : dtos) {
            if (!overwrite) {
                // If library name, sample name, or tube barcode already exist in Mercury then this upload
                // is assumed to be an update of a previous upload and Overwrite must be set.
                if (dto.getSampleInstanceEntity() != null) {
                    messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                            Headers.LIBRARY_NAME.getText(), dto.getLibraryName()));
                }
                if (getSampleMap().get(dto.getSampleName()) != null) {
                    messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                            Headers.SAMPLE_NAME.getText(), dto.getLibraryName()));
                }
                if (getLabVesselMap().get(dto.getBarcode()) != null) {
                    messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                            Headers.TUBE_BARCODE.getText(), dto.getBarcode()));
                }
            } else {
                // The pipeline and elsewhere require a simple name so disallow chars that might cause trouble.
                if (!StringUtils.containsOnly(dto.getLibraryName(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                    messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                            Headers.LIBRARY_NAME.getText()));
                }

                // Tube barcode character set is restricted.
                if (!StringUtils.containsOnly(dto.getBarcode(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                    messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                            Headers.TUBE_BARCODE.getText()));
                }
            }

            // If the sample appears in multiple spreadsheet rows, the sample metadata values must match the
            // first occurrence, or be blank. The first occurrence must have all of the sample metadata.
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                consistentSampleData(mapSampleNameToFirstRow.get(dto.getSampleName()), dto, messages);
            }

            // If a tube appears multiple times in the upload, the vessel attributes must be consistent.
            String barcode = dto.getBarcode();
            if (StringUtils.isNotBlank(barcode)) {
                consistentTubeData(mapBarcodeToFirstRow.get(barcode), dto, messages);
            }

            // Library name must be unique in each tube.
            if (!uniqueBarcodeAndLibrary.add(barcode + " " + dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(),
                        Headers.LIBRARY_NAME.getText()));
            }

            LabVessel tube = getLabVesselMap().get(barcode);
            if (tube != null) {
                if (!overwrite) {
                    messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                            Headers.TUBE_BARCODE.getText(), barcode));
                }
            } else {
                // A new tube barcode character set is restricted.
                if (!StringUtils.containsOnly(barcode, SampleInstanceEjb.RESTRICTED_CHARS)) {
                    messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                            Headers.TUBE_BARCODE.getText(),
                            "composed of " + SampleInstanceEjb.RESTRICTED_MESSAGE));
                }
            }

            if (StringUtils.isNotBlank(dto.getMisName())) {
                if (getMolecularIndexingSchemeMap().get(dto.getMisName()) == null) {
                    messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                            Headers.MOLECULAR_BARCODE_NAME.getText(), "Mercury"));
                }
                // Errors if a tube has duplicate Molecular Index Scheme.
                if (StringUtils.isNotBlank(barcode) && !uniqueTubeAndMis.add(barcode + " " + dto.getMisName())) {
                    messages.addError(String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, dto.getRowNumber(),
                            Headers.MOLECULAR_BARCODE_NAME.getText(),  barcode));
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
            if (StringUtils.isNotBlank(dto.getBait()) && dto.getReagent() == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.BAIT.getText(), "Mercury"));
            }

            // It's an error if the upload refers to a root that doesn't exist and was not given in the upload.
            if (StringUtils.isNotBlank(dto.getRootSampleName()) &&
                    !getSampleMap().containsKey(dto.getRootSampleName()) &&
                    !mapSampleNameToFirstRow.containsKey(dto.getRootSampleName())) {
                messages.addError(SampleInstanceEjb.NONEXISTENT, dto.getRowNumber(),
                        Headers.ROOT_SAMPLE_NAME.getText(), dto.getRootSampleName(), "Mercury");
            }

            if (StringUtils.isNotBlank(dto.getAnalysisTypeName()) &&
                    getAnalysisTypeMap().get(dto.getAnalysisTypeName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.DATA_ANALYSIS_TYPE.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getReferenceSequence()) &&
                    getReferenceSequenceMap().get(dto.getReferenceSequence()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.REFERENCE_SEQUENCE.getText(), "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getSequencingTechnology()) &&
                    IlluminaFlowcell.FlowcellType.getByTechnology(dto.getSequencingTechnology()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"));
            }

            errorBspMetadataChanges(dto, overwrite, messages);
        }
    }

    /**
     * Makes the Samples, LabVessels, and SampleInstanceEntities but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     * @return the list of SampleInstanceEntity, for testing purposes.
     */
    public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos) {
        makeTubesAndSamples(dtos);
        List<SampleInstanceEntity> sampleInstanceEntities = makeSampleInstanceEntities(dtos);
        return sampleInstanceEntities;
    }

    /**
     * Uses the first non-blank data value for the given headers.
     */
    protected String getFromRow(Map<String, String> dataRow, ColumnHeader... headers) {
        for (ColumnHeader header : headers) {
            String data = dataRow.get(header.getText());
            if (StringUtils.isNotBlank(data)) {
                return data;
            }
        }
        return "";
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
    protected void consistentSampleData(SampleInstanceEjb.RowDto expected, SampleInstanceEjb.RowDto found,
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
    protected void consistentTubeData(SampleInstanceEjb.RowDto expected, SampleInstanceEjb.RowDto found,
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
     * Checks the uploaded metadata against existing MercurySample metadata.
     */
    protected void errorBspMetadataChanges(SampleInstanceEjb.RowDto dto, boolean overwrite,
            MessageCollection messages) {
        SampleData sampleData = getFetchedData().get(dto.getSampleName());
        if (sampleData != null) {
            // Excludes Material Type since user cannot control it.
            Set<Metadata> changes = makeSampleMetadata(dto).stream().
                    filter(metadata -> metadata.getKey() != Metadata.Key.MATERIAL_TYPE).
                    collect(Collectors.toSet());
            // Metadata equality is based on both the key and the value.
            changes.removeAll(makeSampleMetadata(sampleData));
            // Makes a string of header names of the columns having changes.
            String columnNames = changes.stream().
                    map(metadata -> metadata.getKey().getDisplayName()).
                    sorted().
                    collect(Collectors.joining(", "));
            // Checks BSP root sample name.
            if (StringUtils.isNotBlank(dto.getRootSampleName()) &&
                    sampleData.getMetadataSource() == MercurySample.MetadataSource.BSP &&
                    !dto.getRootSampleName().equals(sampleData.getRootSample())) {
                columnNames = StringUtils.join(columnNames, " Root Sample", ',');
            }
            // If the upload has different metadata than the MercurySample, it's an error if the
            // metadata source is BSP, or if the metadata source is Mercury and overwrite is not set.
            if (StringUtils.isNotBlank(columnNames)) {
                if (sampleData.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                    messages.addError(SampleInstanceEjb.BSP_METADATA, dto.getRowNumber(), columnNames,
                            dto.getSampleName());
                } else if (!overwrite) {
                    messages.addError(SampleInstanceEjb.PREXISTING_VALUES, dto.getRowNumber(), columnNames);
                }
            }
            // Issues a warning if the metadata source is Mercury and a root sample name was given.
            if (sampleData.getMetadataSource() == MercurySample.MetadataSource.MERCURY &&
                    StringUtils.isNotBlank(dto.getRootSampleName()) &&
                    !dto.getRootSampleName().equals(dto.getSampleName())) {
                messages.addWarning(SampleInstanceEjb.IGNORING_ROOT, dto.getRowNumber());
            }
        }
    }

    /**
     * Creates/updates tube and sample for each unique barcode and sample name.
     */
    protected void makeTubesAndSamples(List<SampleInstanceEjb.RowDto> dtos) {
        // Creates/updates tubes. Sets tube volume and concentration.
        dtos.stream().
                map(dto -> dto.getBarcode()).
                filter(s -> StringUtils.isNotBlank(s)).
                distinct().
                forEach(barcode -> {
                    LabVessel labVessel = getLabVesselMap().get(barcode);
                    if (labVessel == null) {
                        labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                        getLabVesselMap().put(barcode, labVessel);
                    } else {
                        // Tube is being re-uploaded, so clearing old samples is appropriate.
                        labVessel.getMercurySamples().clear();
                    }
                    SampleInstanceEjb.RowDto firstRowHavingTube = mapBarcodeToFirstRow.get(barcode);
                    labVessel.setVolume(firstRowHavingTube.getVolume());
                    labVessel.setConcentration(firstRowHavingTube.getConcentration());
                    SampleInstanceEjb.addLibrarySize(labVessel, new BigDecimal(firstRowHavingTube.getFragmentSize()));
                });

        // Collects all of the root sample names.
        Set<String> rootNames = dtos.stream().
                map(SampleInstanceEjb.RowDto::getRootSampleName).
                filter(rootName -> StringUtils.isNotBlank(rootName)).
                collect(Collectors.toSet());
        // Creates/updates samples, sorted so that any of the samples that are referenced as root samples in
        // the upload are created first.
        dtos.stream().
                map(SampleInstanceEjb.RowDto::getSampleName).
                filter(sampleName -> StringUtils.isNotBlank(sampleName)).
                distinct().
                sorted((String o1, String o2) -> (rootNames.contains(o1) ? 0 : 1) - (rootNames.contains(o2) ? 0 : 1)).
                forEach(sampleName ->
                {
                    SampleInstanceEjb.RowDto dto = mapSampleNameToFirstRow.get(sampleName);

                    MercurySample mercurySample = getSampleMap().get(sampleName);
                    SampleData sampleData = getFetchedData().get(sampleName);
                    if (mercurySample == null) {
                        if (sampleData != null) {
                            // Can really be only BSP sample data, since otherwise mercurySample would exist.
                            mercurySample = new MercurySample(sampleName, sampleData.getMetadataSource());
                            mercurySample.setSampleData(sampleData);
                        } else {
                            // Mercury sample data.
                            mercurySample = new MercurySample(sampleName, makeSampleMetadata(dto));
                        }
                        getSampleMap().put(sampleName, mercurySample);
                    } else if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                        // Adds new values or replaces existing values only for Mercury samples.
                        mercurySample.updateMetadata(makeSampleMetadata(dto));
                    }
                });
    }

    /**
     * Creates/updates tube and sample for each dto row.
     */
    protected List<SampleInstanceEntity> makeSampleInstanceEntities(List<SampleInstanceEjb.RowDto> rowDtos) {
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        for (SampleInstanceEjb.RowDto dto : rowDtos) {
            LabVessel labVessel = getLabVesselMap().get(dto.getBarcode());
            MercurySample mercurySample = getSampleMap().get(dto.getSampleName());
            mercurySample.addLabVessel(labVessel);

            SampleInstanceEntity sampleInstanceEntity = makeSampleInstanceEntity(dto, labVessel, mercurySample);

            labVessel.getSampleInstanceEntities().add(sampleInstanceEntity);
            dto.setSampleInstanceEntity(sampleInstanceEntity);
            sampleInstanceEntities.add(sampleInstanceEntity);
        }
        return sampleInstanceEntities;
    }

    /**
     * Makes Metadata from the spreadsheet row.
     */
    protected Set<Metadata> makeSampleMetadata(SampleInstanceEjb.RowDto dto) {
        // For each pair, if the data value is not blank, makes a new Metadata and puts it in a Set.
        Set<Metadata> metadata = Stream.of(
                Pair.of(dto.getCollaboratorSampleId(), Metadata.Key.SAMPLE_ID),
                Pair.of(dto.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID),
                Pair.of(dto.getSex(), Metadata.Key.GENDER),
                Pair.of(dto.getOrganism(), Metadata.Key.SPECIES),
                Pair.of(dto.getMaterialType(), Metadata.Key.MATERIAL_TYPE)).
                filter(pair -> StringUtils.isNotBlank(pair.getLeft())).
                map(pair -> new Metadata(pair.getRight(), pair.getLeft())).
                collect(Collectors.toSet());
        return metadata;
    }

    /**
     * Makes Metadata from SampleData.
     */
    protected Set<Metadata> makeSampleMetadata(SampleData sampleData) {
        // For each pair, if the data value is not blank, makes a new Metadata and puts it in a Set.
        return Stream.of(
                Pair.of(sampleData.getCollaboratorsSampleName(), Metadata.Key.SAMPLE_ID),
                Pair.of(sampleData.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID),
                Pair.of(sampleData.getGender(), Metadata.Key.GENDER),
                Pair.of(sampleData.getOrganism(), Metadata.Key.SPECIES),
                Pair.of(sampleData.getMaterialType(), Metadata.Key.MATERIAL_TYPE)).
                filter(pair -> StringUtils.isNotBlank(pair.getLeft())).
                map(pair -> new Metadata(pair.getRight(), pair.getLeft())).
                collect(Collectors.toSet());
    }

    /**
     * Makes a new SampleInstanceEntity from the row data.
     */
    protected SampleInstanceEntity makeSampleInstanceEntity(SampleInstanceEjb.RowDto dto, LabVessel labVessel,
            MercurySample mercurySample) {

        SampleInstanceEntity sampleInstanceEntity = dto.getSampleInstanceEntity();
        if (sampleInstanceEntity == null) {
            sampleInstanceEntity = new SampleInstanceEntity();
            sampleInstanceEntity.setSampleLibraryName(dto.getLibraryName());
        }
        // An existing Sample Instance Entity gets rewritten.
        sampleInstanceEntity.setAggregationParticle(dto.getAggregationParticle());
        sampleInstanceEntity.setAnalysisType(getAnalysisTypeMap().get(dto.getAnalysisTypeName()));
        sampleInstanceEntity.setInsertSize(dto.getInsertSize());
        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setMercurySample(mercurySample);
        sampleInstanceEntity.setMolecularIndexingScheme(getMolecularIndexingSchemeMap().get(dto.getMisName()));
        sampleInstanceEntity.setReadLength(dto.getReadLength());
        sampleInstanceEntity.setReagentDesign(dto.getReagent());
        sampleInstanceEntity.setReferenceSequence(getReferenceSequenceMap().get(dto.getReferenceSequence()));
        sampleInstanceEntity.setRootSample(getSampleMap().get(dto.getRootSampleName()));
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
     * Returns true if string indicates a true value, meaning it's "t", "true", "y", "yes",
     * or "1" (or any non-zero integer value), ignoring case.
     */
    private static boolean isTrue(String value) {
        return StringUtils.isNotBlank(value) && (isOneOf(value, "y", "yes", "t", "true") ||
                (StringUtils.isNumeric(value) && Integer.parseInt(value) != 0));
    }

    /**
     * Returns true if string is one of the given testString, ignoring case.
     */
    public static boolean isOneOf(String value, String... testStrings) {
        for (String testString : testStrings) {
            if (testString.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an integer range (two integers delimited with a hyphen), and adds an error message if the input
     * is not one or two integers delimited by space, comma, hyphen, underscore, pipe, slash, colon.
     */
    public static String asIntegerRange(String input, String header, int rowNumber, MessageCollection messages) {
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

    public Map<String, MolecularIndexingScheme> getMolecularIndexingSchemeMap() {
        return molecularIndexingSchemeMap;
    }

    public Map<String, ReferenceSequence> getReferenceSequenceMap() {
        return referenceSequenceMap;
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
