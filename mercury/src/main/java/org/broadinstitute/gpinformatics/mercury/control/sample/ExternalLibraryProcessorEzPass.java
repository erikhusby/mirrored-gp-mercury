package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalLibraryProcessorEzPass extends ExternalLibraryProcessor {

    private List<String> additionalAssemblyInformations = new ArrayList<>();
    private List<String> additionalSampleInformations = new ArrayList<>();
    private List<String> aggregationParticles = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> dataAnalysisTypes = new ArrayList<>();
    private List<String> dataSubmissions = new ArrayList<>();
    private List<String> insertSizes = new ArrayList<>();
    private List<String> irbNumbers = new ArrayList<>();
    private List<String> libraryTypes = new ArrayList<>();
    private List<String> molecularBarcodeNames = new ArrayList<>();
    private List<String> numbersOfLanes = new ArrayList<>();
    private List<String> organisms = new ArrayList<>();
    private List<String> sampleNames = new ArrayList<>();
    private List<String> readLengths = new ArrayList<>();
    private List<String> referenceSequences = new ArrayList<>();
    private List<String> sequencerModelNames = new ArrayList<>();
    private List<String> sexes = new ArrayList<>();
    private List<String> libraryNames = new ArrayList<>();
    private List<String> concentrations = new ArrayList<>();
    private List<String> volumes = new ArrayList<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();

    public ExternalLibraryProcessorEzPass() {
        super(null);
    }

    // Only the first four words of header text are used and the rest are ignored.
    // Orderining of headers is only important for generating template spreadsheets in the ActionBean.
    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        SAMPLE_NUMBER("Sample Number", DataPresence.IGNORED),
        TUBE_BARCODE("Sample Tube Barcode", DataPresence.REQUIRED),
        SOURCE_SAMPLE_GSSR_ID("Source Sample Gssr Id", DataPresence.REQUIRED),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", DataPresence.REQUIRED),
        STRAIN("Strain", DataPresence.IGNORED),
        SEX("Sex (M/F)", DataPresence.OPTIONAL),
        CELL_LINE("Cell Line", DataPresence.IGNORED),
        TISSUE_TYPE("Tissue Type", DataPresence.IGNORED),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", DataPresence.REQUIRED),
        INDIVIDUAL_NAME("Individual Name (Patient Id)", DataPresence.REQUIRED),
        LIBRARY_NAME("Library Name", DataPresence.REQUIRED),
        LIBRARY_TYPE("Library Type", DataPresence.OPTIONAL),
        DATA_ANALYSIS_TYPE("Data Analysis Type", DataPresence.REQUIRED),
        REFERENCE_SEQUENCE("Reference Sequence", DataPresence.REQUIRED),
        GSSR_OF_BAIT_POOL("GSSR # of Bait Pool", DataPresence.IGNORED),
        INSERT_SIZE_RANGE("Insert Size Range", DataPresence.OPTIONAL),
        LIBRARY_SIZE("Library Size", DataPresence.IGNORED),
        JUMP_SIZE("Jump Size", DataPresence.IGNORED),
        ILLUMINA_KIT_USED("Illumina or 454 Kit", DataPresence.IGNORED),
        RESTRICTION_ENZYMES("Restriction Enzyme", DataPresence.IGNORED),
        MOLECULAR_BARCODE_PLATE_ID("Molecular Barcode Plate Id", DataPresence.IGNORED),
        MOLECULAR_BARCODE_PLATE_WELL_ID("Molecular Barcode Plate Well Id", DataPresence.IGNORED),
        MOLECULAR_BARCODE_SEQUENCE("Molecular Barcode Sequence", DataPresence.IGNORED),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", DataPresence.OPTIONAL),
        VOLUME("Volume (ul)", DataPresence.ONCE_PER_TUBE),
        CONCENTRATION("Concentration (ng/uL)", DataPresence.ONCE_PER_TUBE),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", DataPresence.IGNORED),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", DataPresence.OPTIONAL),
        PROJECT_TITLE("Project Title", DataPresence.IGNORED),
        FUNDING_SOURCE("Funding Source", DataPresence.IGNORED),
        COVERAGE("Coverage (lanes/sample)", DataPresence.REQUIRED),
        APPROVED_BY("Approved By", DataPresence.IGNORED),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", DataPresence.IGNORED),
        DATA_SUBMISSION("Data Submission", DataPresence.IGNORED),
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Info", DataPresence.OPTIONAL),
        POOLED("Pooled (Y/N)", DataPresence.IGNORED),
        MEMBER_OF_POOL("Member of Pool", DataPresence.IGNORED),
        VIRTUAL_GSSR_ID("Virtual Gssr Id", DataPresence.IGNORED),
        SQUID_PROJECT("Squid Project (pipeline aggregation)", DataPresence.OPTIONAL),

        REQUIRED_ACCESS("Require Controlled Access for Data", DataPresence.IGNORED),
        ACCESS_LIST("Data Access List", DataPresence.IGNORED),
        READ_LENGTH("Desired Read Length", DataPresence.OPTIONAL),
        ORGANISM("Organism", DataPresence.OPTIONAL),
        DERIVED_FROM("Derived From", DataPresence.IGNORED),
        SUBMITTED_TO_GSSR("Submitted to Gssr", DataPresence.IGNORED),
        BLANK("", DataPresence.IGNORED),
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

        @Override
        public DataPresence getDataPresenceIndicator() {
            return dataPresence;
        }
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        additionalAssemblyInformations.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        additionalSampleInformations.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        aggregationParticles.add(getFromRow(dataRow, Headers.SQUID_PROJECT));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        collaboratorParticipantIds.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        collaboratorSampleIds.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        concentrations.add(getFromRow(dataRow, Headers.CONCENTRATION));
        dataAnalysisTypes.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        dataSubmissions.add(getFromRow(dataRow, Headers.DATA_SUBMISSION));
        insertSizes.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE));
        libraryNames.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        libraryTypes.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        molecularBarcodeNames.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        numbersOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        organisms.add(getFromRow(dataRow, Headers.ORGANISM));
        readLengths.add(getFromRow(dataRow, Headers.READ_LENGTH));
        referenceSequences.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        sampleNames.add(getFromRow(dataRow, Headers.SOURCE_SAMPLE_GSSR_ID));
        sequencerModelNames.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        sexes.add(getFromRow(dataRow, Headers.SEX));
        volumes.add(getFromRow(dataRow, Headers.VOLUME));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    /**
     * Adds an error message if a field contains some characters that it shouldn't. Fields that
     * are entity keys (like analysisType), numerics, and categorical values are checked elsewhere.
     */
    @Override
    public void validateCharacterSet(List<SampleInstanceEjb.RowDto> dtos, MessageCollection messages) {
        for (SampleInstanceEjb.RowDto dto : dtos) {
            if (!StringUtils.containsOnly(dto.getBarcode(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.TUBE_BARCODE.getText(), SampleInstanceEjb.RESTRICTED_CHARS));
            }
            if (StringUtils.isNotBlank(dto.getAggregationParticle()) &&
                    !StringUtils.containsOnly(dto.getAggregationParticle(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.SQUID_PROJECT.getText(), SampleInstanceEjb.RESTRICTED_CHARS));
            }
            if (!StringUtils.containsOnly(dto.getCollaboratorParticipantId(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.INDIVIDUAL_NAME.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
            if (!StringUtils.containsOnly(dto.getCollaboratorSampleId(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.COLLABORATOR_SAMPLE_ID.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
            if (!StringUtils.containsOnly(dto.getLibraryName(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.LIBRARY_NAME.getText(), SampleInstanceEjb.RESTRICTED_CHARS));
            }
            if (!StringUtils.containsOnly(dto.getSampleName(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.SOURCE_SAMPLE_GSSR_ID.getText(), SampleInstanceEjb.RESTRICTED_CHARS));
            }
            if (StringUtils.isNotBlank(dto.getOrganism()) &&
                    !StringUtils.containsOnly(dto.getOrganism(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.ORGANISM.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
            if (StringUtils.isNotBlank(dto.getAdditionalAssemblyInformation()) &&
                    !StringUtils.containsOnly(dto.getAdditionalAssemblyInformation(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.ASSEMBLY_INFORMATION.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
            if (StringUtils.isNotBlank(dto.getAdditionalSampleInformation()) &&
                    !StringUtils.containsOnly(dto.getAdditionalSampleInformation(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.ADDITIONAL_SAMPLE_INFORMATION.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
            if (StringUtils.isNotBlank(dto.getLibraryType()) &&
                    !StringUtils.containsOnly(dto.getLibraryType(), SampleInstanceEjb.ALIAS_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                        Headers.LIBRARY_TYPE.getText(), SampleInstanceEjb.ALIAS_CHARS));
            }
        }
    }


    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    @Override
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite, MessageCollection messages) {
        Set<String> barcodeAndLibraryKeys = new HashSet<>();

        for (SampleInstanceEjb.RowDto dto : dtos) {

            // Each library name may appear only once per tube in the upload.
            if (!barcodeAndLibraryKeys.add(dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(), "Library Name"));
            }

            // Library name is assumed to be universally unique. If it's reused then it's assumed to be an
            // update of a previous upload and Overwrite must be set. Same with sample name and tube barcode.
            if (dto.getSampleInstanceEntity() != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        "Library", dto.getLibraryName()));
            }
            if (getSampleMap().get(dto.getSampleName()) != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        "Sample", dto.getLibraryName()));
            }
            if (getLabVesselMap().get(dto.getBarcode()) != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText(), dto.getBarcode()));
            }

            // If a tube appears multiple times in the upload, its volume and concentration values must be consistent.
            if (StringUtils.isNotBlank(dto.getBarcode())) {
                consistentTubeData(mapBarcodeToFirstRow.get(dto.getBarcode()), dto, messages);
            }

            if (StringUtils.isNotBlank(dto.getReferenceSequenceName()) &&
                    getReferenceSequenceMap().get(dto.getReferenceSequenceName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        "Reference Sequence", "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getMisName()) &&
                    getMolecularIndexingSchemeMap().get(dto.getMisName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        "Molecular Barcode Name", "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getAnalysisTypeName()) &&
                    getAnalysisTypeMap().get(dto.getAnalysisTypeName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        "Data Analysis Type", "Mercury"));
            }

            if (StringUtils.isNotBlank(dto.getSequencerModelName()) &&
                    getSequencerModelMap().get(dto.getSequencerModelName()) == null) {
                messages.addError(String.format(SampleInstanceEjb.UNKNOWN, dto.getRowNumber(),
                        "Sequencing Technology", "Mercury"));
            }

            // If the sample appears in multiple spreadsheet rows, the first row is used for all of the
            // sample metadata. For self-consistency the other rows must either match or have blank values.
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                consistentSampleData(mapSampleNameToFirstRow.get(dto.getSampleName()), dto, messages);
            }
            errorBspMetadataChanges(dto, overwrite, messages);
        }
    }

    /**
     * Makes the Samples, LabVessels, and SampleInstanceEntities but does not persist them.
     * New/modified entities are added to the dto shared collection getEntitiesToUpdate().
     * @return the list of SampleInstanceEntity, for testing purposes.
     */
    @Override
    public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> dtos) {
        makeTubesAndSamples(dtos);
        List<SampleInstanceEntity> sampleInstanceEntities = makeSampleInstanceEntities(dtos);
        return sampleInstanceEntities;
    }

    @Override
    public List<String> getAdditionalAssemblyInformations() {
        return additionalAssemblyInformations;
    }

    @Override
    public List<String> getAdditionalSampleInformations() {
        return additionalSampleInformations;
    }

    @Override
    public List<String> getBarcodes() {
        return barcodes;
    }

    @Override
    public List<String> getCollaboratorSampleIds() {
        return collaboratorSampleIds;
    }

    @Override
    public List<String> getNumbersOfLanes() {
        return numbersOfLanes;
    }

    @Override
    public List<String> getDataAnalysisTypes() {
        return dataAnalysisTypes;
    }

    @Override
    public List<String> getReadLengths() {
        return readLengths;
    }

    @Override
    public List<String> getCollaboratorParticipantIds() {
        return collaboratorParticipantIds;
    }

    @Override
    public List<String> getInsertSizes() {
        return insertSizes;
    }

    @Override
    public List<String> getIrbNumbers() {
        return irbNumbers;
    }

    @Override
    public List<String> getLibraryTypes() {
        return libraryTypes;
    }

    @Override
    public List<String> getMolecularBarcodeNames() {
        return molecularBarcodeNames;
    }

    @Override
    public List<String> getOrganisms() {
        return organisms;
    }

    @Override
    public List<String> getAggregationParticles() {
        return aggregationParticles;
    }

    @Override
    public List<String> getReferenceSequences() {
        return referenceSequences;
    }

    @Override
    public List<String> getSequencerModelNames() {
        return sequencerModelNames;
    }

    @Override
    public List<String> getSexes() {
        return sexes;
    }

    @Override
    public List<String> getLibraryNames() {
        return libraryNames;
    }

    @Override
    public List<String> getConcentrations() {
        return concentrations;
    }

    @Override
    public List<String> getVolumes() {
        return volumes;
    }

    @Override
    public List<String> getSampleNames() {
        return sampleNames;
    }

    @Override
    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }
}
