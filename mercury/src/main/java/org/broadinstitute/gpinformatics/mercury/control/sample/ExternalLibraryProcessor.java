package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRowTableProcessor;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ExternalLibraryProcessor extends HeaderValueRowTableProcessor {
    public enum DataPresence {REQUIRED, ONCE_PER_TUBE, OPTIONAL, IGNORED};

    protected List<String> headerNames = new ArrayList<>();
    protected List<String> headerValueNames = new ArrayList<>();
    // Maps tube barcode to the first dto that contains that barcode (and associated tube data).
    protected Map<String, SampleInstanceEjb.RowDto> mapBarcodeToFirstRow = new HashMap<>();
    // Maps sample name to the first dto that contains that sample name (and associated sample data).
    protected Map<String, SampleInstanceEjb.RowDto> mapSampleNameToFirstRow = new HashMap<>();
    // Maps sample name to MercurySample.
    private Map<String, MercurySample> sampleMap = new HashMap<>();
    // Maps barcode to LabVessel.
    private Map<String, LabVessel> labVesselMap = new HashMap<>();
    // Maps sample name to BSP or Mercury SampleData.
    private Map<String, SampleData> fetchedData = new HashMap<>();
    // Maps ticket name to Jira ticket.
    private Map<String, JiraIssue> jiraIssueMap = new HashMap<>();
    private Map<String, AnalysisType> analysisTypeMap = new HashMap<>();
    private Map<String, MolecularIndexingScheme> molecularIndexingSchemeMap = new HashMap<>();
    private Map<String, ReferenceSequence> referenceSequenceMap = new HashMap<>();
    private Set<Object> entitiesToUpdate = new HashSet<>();
    private List<String> validAggregationDataTypes = new ArrayList();

    // Maps adjusted header name to actual header name.
    protected Map<String, String> adjustedNames = new HashMap<>();

    // These are the rows that consist of a header-value pair in two adjacent columns.
    public enum HeaderValueRows implements HeaderValueRow, ColumnHeader.Ignorable {
        EMAIL("Email:", DataPresence.IGNORED),
        ORGANIZATION("Organization:", DataPresence.IGNORED),
        FIRST_NAME("First Name:", DataPresence.IGNORED),
        LAST_NAME("Last Name:", DataPresence.IGNORED),
        ADDRESS("Address:", DataPresence.IGNORED),
        ROOM_NUMBER("Room Number:", DataPresence.IGNORED),
        CITY("City:", DataPresence.IGNORED),
        STATE("State:", DataPresence.IGNORED),
        POSTAL_CODE("Postal Code:", DataPresence.IGNORED),
        COUNTRY("Country:", DataPresence.IGNORED),
        PHONE("Phone:", DataPresence.IGNORED),
        COMMON_NAME("Common Name:", DataPresence.IGNORED),
        GENUS("Genus:", DataPresence.IGNORED),
        SPECIES("Species:", DataPresence.IGNORED),
        IRB_REQUIRED("IRB approval required: (Y/N)", DataPresence.IGNORED);

        private String text;
        private DataPresence dataPresence;

        private HeaderValueRows(String text, DataPresence dataPresence) {
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
        public DataPresence getDataPresenceIndicator() {
            return dataPresence;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return true;
        }
    }

    public ExternalLibraryProcessor(String sheetName) {
        super(sheetName);

        for (HeaderValueRow headerValueRow : getHeaderValueRows()) {
            headerValueNames.add(headerValueRow.getText());
        }
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return HeaderValueRows.values();
    }

    /**
     * Returns the canonical HeaderValueRow header names, not the ones that appeared in the spreadsheet.
     */
    @Override
    public List<String> getHeaderValueNames() {
        return headerValueNames;
    }

    /**
     * Returns the column header names that appeared in the spreadsheet, not the canonical ones.
     */
    @Override
    public List<String> getHeaderNames() {
        return headerNames;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerNames.addAll(headers);
        for (String header : headers) {
            adjustedNames.put(adjustHeaderName(header), header);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String adjustHeaderName(String headerCell) {
        return ExternalLibraryProcessor.fixupHeaderName(headerCell);
    }

    @Override
    public String adjustHeaderName(String headerCell, int numberOfWords) {
        return ExternalLibraryProcessor.fixupHeaderName(headerCell, numberOfWords);
    }

    /**
     * Normalizes the spreadsheet header names.
     * - Trims leading and trailing blanks from each word.
     * - Lower cases all words.
     * - Keeps only the first few words.
     * - Ignores everything after a parenthesis.
     * - Substitutes some of the words.
     */
    public static String fixupHeaderName(String headerCell) {
        return fixupHeaderName(headerCell, 4);
    }

    private static String fixupHeaderName(String headerCell, int numberOfWords) {
        final Map<String, String> substitutes = new HashMap<String, String>() {{
            put("molecule", "molecular");
            put("bp", "");
            put("bp.", "");
            put("if", "");
            put("for", "");
            put("requested", "");
            put("desired", "");
            put("illumina", "");
            put("no", "");
            put("no.", "number");
            put("total", "");
            put("range", "");
            put("description", "");
            put("*room", "room");
        }};
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String word : headerCell.trim().toLowerCase().
                replaceFirst("total library", "").
                replaceFirst("sample collaborator", "collaborator sample").
                replaceFirst("sample volume", "volume").
                replaceFirst("sample concentration", "concentration").
                replaceFirst("if applicable", "").
                replaceFirst("single sample", "").
                split(" ")) {
            if (++count > numberOfWords) {
                break;
            }
            if (word.startsWith("(")) {
                break;
            }
            word = StringUtils.stripStart(word, "*");
            word = StringUtils.stripEnd(word, ":");
            if (substitutes.containsKey(word)) {
                word = substitutes.get(word);
            }
            if (StringUtils.isNotBlank(word)) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(word);
            }
        }
        return builder.toString();
    }

    /**
     * Uses the first non-blank data value for the given headers.
     */
    protected String getFromRow(Map<String, String> dataRow, ColumnHeader... headers) {
        for (ColumnHeader header : headers) {
            String data = dataRow.get(getAdjustedNames().get(adjustHeaderName(header.getText())));
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
            dto.setAdditionalAssemblyInformation(get(getAdditionalAssemblyInformations(), index));
            dto.setAdditionalSampleInformation(get(getAdditionalSampleInformations(), index));
            dto.setAnalysisTypeName(get(getDataAnalysisTypes(), index));
            dto.setUmisPresent(get(getUmisPresents(), index));
            dto.setBait(get(getBaits(), index));
            dto.setBarcode(get(getBarcodes(), index));
            if (StringUtils.isNotBlank(dto.getBarcode()) && !mapBarcodeToFirstRow.containsKey(dto.getBarcode())) {
                mapBarcodeToFirstRow.put(dto.getBarcode(), dto);
            }
            dto.setCat(get(getCats(), index));
            dto.setCollaboratorParticipantId(get(getCollaboratorParticipantIds(), index));
            dto.setCollaboratorSampleId(get(getCollaboratorSampleIds(), index));
            dto.setConcentration(asNonNegativeBigDecimal(get(getConcentrations(), index),
                    ExternalLibraryProcessorNewTech.Headers.CONCENTRATION.getText(), dto.getRowNumber(), messages));
            dto.setInsertSize(asIntegerRange(get(getInsertSizes(), index),
                    ExternalLibraryProcessorNewTech.Headers.INSERT_SIZE_RANGE.getText(), dto.getRowNumber(), messages));
            dto.setIrbNumber(get(getIrbNumbers(), index));
            dto.setLibraryName(get(getLibraryNames(), index));
            dto.setLibraryType(get(getLibraryTypes(), index));
            dto.setLsid(get(getLsids(), index));
            dto.setMisName(get(getMolecularBarcodeNames(), index));
            dto.setNumberOfLanes(asNonNegativeInteger(get(getNumbersOfLanes(), index),
                    ExternalLibraryProcessorNewTech.Headers.COVERAGE.getText(), dto.getRowNumber(), messages));
            dto.setOrganism(get(getOrganisms(), index));
            dto.setParticipantId(get(getBroadParticipantIds(), index));
            dto.setAggregationParticle(get(getAggregationParticles(), index));
            dto.setReadLength(asNonNegativeInteger(get(getReadLengths(), index),
                    VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), dto.getRowNumber(), messages));
            dto.setReferenceSequence(get(getReferenceSequences(), index));
            dto.setReferenceSequenceName(get(getReferenceSequences(), index));
            dto.setRootSampleName(get(getRootSampleNames(), index));
            dto.setSampleName(get(getSampleNames(), index));
            if (StringUtils.isNotBlank(dto.getSampleName()) &&
                    !mapSampleNameToFirstRow.containsKey(dto.getSampleName())) {
                mapSampleNameToFirstRow.put(dto.getSampleName(), dto);
            }
            dto.setSequencerModelName(get(getSequencerModelNames(), index));
            dto.setSex(get(getSexes(), index));
            dto.setVolume(asNonNegativeBigDecimal(get(getVolumes(), index),
                    VesselPooledTubesProcessor.Headers.VOLUME.getText(), dto.getRowNumber(), messages));
            dto.setAggregationDataType(get(getAggregationDataTypes(), index));
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
                Triple.of(found.getRootSampleName(), expected.getRootSampleName(), "Root Sample"),
                Triple.of(found.getSex(), expected.getSex(), "Sex"),
                Triple.of(found.getOrganism(), expected.getOrganism(), "Organism"),
                Triple.of(found.getLsid(), expected.getLsid(), "Lsid"),
                Triple.of(found.getParticipantId(), expected.getParticipantId(), "Broad Participant Id"),
                Triple.of(found.getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(),
                        "Patient Id (Collaborator Participant ID)"),
                Triple.of(found.getCollaboratorSampleId(), expected.getCollaboratorSampleId(),
                        "Collaborator Sample Id")
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

        boolean volumeIsRequired = false;
        boolean concIsRequired = false;
        boolean expectConc = false;
        for (ColumnHeader header : getColumnHeaders()) {
            if (header.getText().toLowerCase().contains("volume")) {
                volumeIsRequired = header.isRequiredValue();
            }
            if (header.getText().toLowerCase().contains("concentration")) {
                expectConc = true;
                concIsRequired = header.isRequiredValue();
            }
        }
        if (expected.getRowNumber() == found.getRowNumber()) {
            // Volume and concentration (if this type of spreadsheet can provide it) are required for an
            // uploaded tube. If the Header enum has them as OPTIONAL (so that pooled tubes across multiple
            // rows need only provide the values in the first row for each tube) then a missing value is
            // checked here. If the Header enum has them as REQUIRED then the missing value has already been
            // checked for earlier in the code. Likewise if the string value can't be converted to a number.
            if (!volumeIsRequired && StringUtils.isBlank(get(getVolumes(), toRowIndex(found.getRowNumber())))) {
                messages.addError(String.format(SampleInstanceEjb.MISSING, found.getRowNumber(), "Volume"));
            }
            if (!concIsRequired && expectConc &&
                    StringUtils.isBlank(get(getConcentrations(), toRowIndex(found.getRowNumber())))) {
                messages.addError(String.format(SampleInstanceEjb.MISSING, found.getRowNumber(), "Concentration"));
            }
        } else {
            if (found.getVolume() != null && !Objects.equals(found.getVolume(), expected.getVolume())) {
                messages.addError(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        "Volume", mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(), found.getBarcode()));
            }
            if (found.getConcentration() != null &&
                    !Objects.equals(found.getConcentration(), expected.getConcentration())) {
                messages.addError(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        "Concentration", mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(),
                        found.getBarcode()));
            }
            // Issues warnings for mixed values that may be technically possible in a pooled tube.
            if (found.getInsertSize() != null && !Objects.equals(found.getInsertSize(), expected.getInsertSize())) {
                messages.addWarning(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        "Insert Size", mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(),
                        found.getBarcode()));
            }
            if (found.getReadLength() != null && !Objects.equals(found.getReadLength(), expected.getReadLength())) {
                messages.addWarning(String.format(SampleInstanceEjb.INCONSISTENT_TUBE, found.getRowNumber(),
                        "Read Length", mapBarcodeToFirstRow.get(found.getBarcode()).getRowNumber(),
                        found.getBarcode()));
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
     * Does character set validation checks on the data.
     */
    abstract public void validateCharacterSet(List<SampleInstanceEjb.RowDto> dtos, MessageCollection messages);

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    abstract public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite,
            MessageCollection messages);

    /**
     * Makes the Samples, LabVessels, and SampleInstanceEntities but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     *
     * @return the list of SampleInstanceEntity, for testing purposes.
     */
    abstract public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos);

    /**
     * Creates/updates tube and sample for each unique barcode and sample name.
     */
    protected void makeTubesAndSamples(List<SampleInstanceEjb.RowDto> dtos) {
        // Creates/updates tubes. Sets tube volume and concentration.
        dtos.stream().
                map(dto -> dto.getBarcode()).
                filter(s -> StringUtils.isNotBlank(s)).
                distinct().
                forEach(barcode ->
                {
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
        String organism = StringUtils.isNotBlank(dto.getOrganism()) ? dto.getOrganism() : getGenusAndSpecies();
        // For each pair, if the data value is not blank, makes a new Metadata and puts it in a Set.
        Set<Metadata> metadata = Stream.of(
                Pair.of(dto.getCollaboratorSampleId(), Metadata.Key.SAMPLE_ID),
                Pair.of(dto.getParticipantId(), Metadata.Key.BROAD_PARTICIPANT_ID),
                Pair.of(dto.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID),
                Pair.of(dto.getSex(), Metadata.Key.GENDER),
                Pair.of(organism, Metadata.Key.SPECIES),
                Pair.of(dto.getMaterialType(), Metadata.Key.MATERIAL_TYPE),
                Pair.of(dto.getLsid(), Metadata.Key.LSID)).
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
        Set<Metadata> metadata = Stream.of(
                Pair.of(sampleData.getCollaboratorsSampleName(), Metadata.Key.SAMPLE_ID),
                Pair.of(sampleData.getPatientId(), Metadata.Key.BROAD_PARTICIPANT_ID),
                Pair.of(sampleData.getCollaboratorParticipantId(), Metadata.Key.PATIENT_ID),
                Pair.of(sampleData.getGender(), Metadata.Key.GENDER),
                Pair.of(sampleData.getOrganism(), Metadata.Key.SPECIES),
                Pair.of(sampleData.getMaterialType(), Metadata.Key.MATERIAL_TYPE),
                Pair.of(sampleData.getSampleLsid(), Metadata.Key.LSID)).
                filter(pair -> StringUtils.isNotBlank(pair.getLeft())).
                map(pair -> new Metadata(pair.getRight(), pair.getLeft())).
                collect(Collectors.toSet());
        return metadata;
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
        sampleInstanceEntity.setAggregationDataType(dto.getAggregationDataType());
        sampleInstanceEntity.setAggregationParticle(dto.getAggregationParticle());
        sampleInstanceEntity.setAnalysisType(getAnalysisTypeMap().get(dto.getAnalysisTypeName()));
        sampleInstanceEntity.setComments(dto.getAdditionalSampleInformation() +
                ((StringUtils.isNotBlank(dto.getAdditionalSampleInformation()) &&
                        StringUtils.isNotBlank(dto.getAdditionalAssemblyInformation())) ? "; " : "") +
                dto.getAdditionalAssemblyInformation());
        sampleInstanceEntity.setInsertSize(dto.getInsertSize());
        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setLibraryType(dto.getLibraryType());
        sampleInstanceEntity.setMercurySample(mercurySample);
        sampleInstanceEntity.setMolecularIndexingScheme(getMolecularIndexingSchemeMap().get(dto.getMisName()));
        sampleInstanceEntity.setNumberLanes(dto.getNumberOfLanes());
        sampleInstanceEntity.setReadLength1(dto.getReadLength());
        sampleInstanceEntity.setReagentDesign(dto.getReagent());
        sampleInstanceEntity.setReferenceSequence(getReferenceSequenceMap().get(dto.getReferenceSequenceName()));
        sampleInstanceEntity.setRootSample(getSampleMap().get(dto.getRootSampleName()));
        sampleInstanceEntity.setSequencerModel(IlluminaFlowcell.FlowcellType.getTypeForExternalUiName(
                dto.getSequencerModelName()));
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
     * Returns a mapping of adjusted header name to actual header name.
     */
    public Map<String, String> getAdjustedNames() {
        return adjustedNames;
    }

    /**
     * Getters for the rows consisting of header-value pairs.
     */
    public String getAddress() {
        return headerValueMap.get(HeaderValueRows.ADDRESS.getText());
    }

    public String getCity() {
        return headerValueMap.get(HeaderValueRows.CITY.getText());
    }

    public String getCommonName() {
        return headerValueMap.get(HeaderValueRows.COMMON_NAME.getText());
    }

    public String getCountry() {
        return headerValueMap.get(HeaderValueRows.COUNTRY.getText());
    }

    public String getEmail() {
        return headerValueMap.get(HeaderValueRows.EMAIL.getText());
    }

    public void setEmail(String email) {
        headerValueMap.put(HeaderValueRows.EMAIL.getText(), email);
    }

    public String getFirstName() {
        return headerValueMap.get(HeaderValueRows.FIRST_NAME.getText());
    }

    public String getGenus() {
        return headerValueMap.get(HeaderValueRows.GENUS.getText());
    }

    public String getIrbApprovalRequired() {
        return headerValueMap.get(HeaderValueRows.IRB_REQUIRED.getText());
    }

    public String getLastName() {
        return headerValueMap.get(HeaderValueRows.LAST_NAME.getText());
    }

    public String getOrganization() {
        return headerValueMap.get(HeaderValueRows.ORGANIZATION.getText());
    }

    public void setOrganization(String organization) {
        headerValueMap.put(HeaderValueRows.ORGANIZATION.getText(), organization);
    }

    // Combines genus and species to use if organism is not specifed on a row.
    public String getGenusAndSpecies() {
        return StringUtils.trim(StringUtils.trimToEmpty(getGenus()) + " " + StringUtils.trimToEmpty(getSpecies()));
    }

    public String getPhone() {
        return headerValueMap.get(HeaderValueRows.PHONE.getText());
    }

    public String getPostalCode() {
        return headerValueMap.get(HeaderValueRows.POSTAL_CODE.getText());
    }

    public String getRoomNumber() {
        return headerValueMap.get(HeaderValueRows.ROOM_NUMBER.getText());
    }

    public String getSpecies() {
        return headerValueMap.get(HeaderValueRows.SPECIES.getText());
    }

    public String getState() {
        return headerValueMap.get(HeaderValueRows.STATE.getText());
    }

    public Map<String, String> getHeaderValueMap() {
        return headerValueMap;
    }

    /**
     * These are the spreadsheet data getters to be overridden, depending on the type of
     * spreadsheet being processed, defaulting to no data.
     */
    public List<String> getAdditionalAssemblyInformations() {
        return Collections.emptyList();
    }

    public List<String> getAdditionalSampleInformations() {
        return Collections.emptyList();
    }

    public List<String> getBarcodes() {
        return Collections.emptyList();
    }

    public List<String> getCollaboratorSampleIds() {
        return Collections.emptyList();
    }

    public List<String> getNumbersOfLanes() {
        return Collections.emptyList();
    }

    public List<String> getDataAnalysisTypes() {
        return Collections.emptyList();
    }

    public List<String> getUmisPresents() {
        return Collections.emptyList();
    }

    public List<String> getReadLengths() {
        return Collections.emptyList();
    }

    public List<String> getInsertSizes() {
        return Collections.emptyList();
    }

    public List<String> getIrbNumbers() {
        return Collections.emptyList();
    }

    public List<String> getLibraryTypes() {
        return Collections.emptyList();
    }

    public List<String> getMolecularBarcodeNames() {
        return Collections.emptyList();
    }

    public List<String> getOrganisms() {
        return Collections.emptyList();
    }

    public List<String> getAggregationParticles() {
        return Collections.emptyList();
    }

    public List<String> getAggregationDataTypes() {
        return Collections.emptyList();
    }

    public List<String> getReferenceSequences() {
        return Collections.emptyList();
    }

    public List<String> getSequencerModelNames() {
        return Collections.emptyList();
    }

    public List<String> getSexes() {
        return Collections.emptyList();
    }

    public List<String> getLibraryNames() {
        return Collections.emptyList();
    }

    public List<String> getConcentrations() {
        return Collections.emptyList();
    }

    public List<String> getVolumes() {
        return Collections.emptyList();
    }

    public List<String> getSampleNames() {
        return Collections.emptyList();
    }

    public List<String> getRootSampleNames() {
        return Collections.emptyList();
    }

    public List<String> getBaits() {
        return Collections.emptyList();
    }

    public List<String> getCats() {
        return Collections.emptyList();
    }

    public List<String> getCollaboratorParticipantIds() {
        return Collections.emptyList();
    }

    public List<String> getBroadParticipantIds() {
        return Collections.emptyList();
    }

    public List<String> getLsids() {
        return Collections.emptyList();
    }

    public List<Boolean> getRequiredValuesPresent() {
        return Collections.emptyList();
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

    public Map<String, JiraIssue> getJiraIssueMap() {
        return jiraIssueMap;
    }

    public Map<String, AnalysisType> getAnalysisTypeMap() {
        return analysisTypeMap;
    }

    public List<String> getValidAggregationDataTypes() {
        return validAggregationDataTypes;
    }

    public void setValidAggregationDataTypes(List<String> validAggregationDataTypes) {
        this.validAggregationDataTypes = validAggregationDataTypes;
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
}
