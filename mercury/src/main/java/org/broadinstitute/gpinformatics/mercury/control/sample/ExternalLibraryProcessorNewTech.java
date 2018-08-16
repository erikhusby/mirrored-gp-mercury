package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil.isInBspFormat;

/** Handles both Pooled, Non-pooled, and MultiOrganism New Tech spreadsheets. */

public class ExternalLibraryProcessorNewTech extends ExternalLibraryProcessor {
    private List<String> additionalAssemblyInformations = new ArrayList<>();
    private List<String> additionalSampleInformations = new ArrayList<>();
    private List<String> aggregationParticles = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    private List<String> concentrations = new ArrayList<>();
    private List<String> dataAnalysisTypes = new ArrayList<>();
    private List<String> insertSizes = new ArrayList<>();
    private List<String> irbNumbers = new ArrayList<>();
    private List<String> libraryNames = new ArrayList<>();
    private List<String> librarySizes = new ArrayList<>();
    private List<String> libraryTypes = new ArrayList<>();
    private List<String> molecularBarcodeNames = new ArrayList<>();
    private List<String> numbersOfLanes = new ArrayList<>();
    private List<String> organisms = new ArrayList<>();
    private List<String> pooleds = new ArrayList<>();
    private List<String> readLengths = new ArrayList<>();
    private List<String> referenceSequences = new ArrayList<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();
    private List<String> sampleNames = new ArrayList<>();
    private List<String> sequencerModeNames = new ArrayList<>();
    private List<String> sexes = new ArrayList<>();
    private List<String> volumes = new ArrayList<>();
    private SampleKitRequest sampleKitRequest;

    public ExternalLibraryProcessorNewTech(String sheetName) {
        super(sheetName);
    }

    // Only the first four words of header text are used and the rest are ignored.
    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", OPTIONAL),
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Info", OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", REQUIRED),
        CONCENTRATION("Concentration (ng/uL)", REQUIRED),
        COVERAGE("Coverage (lanes/sample)", REQUIRED),
        DATA_ANALYSIS_TYPE("Data Analysis Type", REQUIRED),
        INDIVIDUAL_NAME("Individual Name (Patient Id)", REQUIRED),
        INSERT_SIZE_RANGE("Insert Size Range", OPTIONAL),
        LIBRARY_NAME("Library Name", REQUIRED),
        LIBRARY_SIZE("Library Size", REQUIRED),
        LIBRARY_TYPE("Library Type", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", OPTIONAL),
        ORGANISM("Organism", OPTIONAL),
        POOLED("Pooled (Y/N)", OPTIONAL),
        PROJECT_TITLE("Project Title (pipeline aggregator)", REQUIRED),
        READ_LENGTH("Desired Read Length", OPTIONAL),
        REFERENCE_SEQUENCE("Reference Sequence", REQUIRED),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", REQUIRED),
        SEX("Sex (M/F)", OPTIONAL),
        TUBE_BARCODE("Sample Tube Barcode", OPTIONAL),
        VOLUME("Volume (uL)", REQUIRED),

        // Ignored header and data is not saved.
        BLANK("", IGNORED),
        APPROVED_BY("Approved By", IGNORED),
        CELL_LINE("Cell Line", IGNORED),
        ACCESS_LIST("Data Access List", IGNORED),
        DATA_SUBMISSION("Data Submission", IGNORED),
        DERIVED_FROM("Derived From", IGNORED),
        FUNDING_SOURCE("Funding Source", IGNORED),
        IRB_NUMBER("IRB Number", IGNORED),
        ILLUMINA_KIT_USED("Illumina or 454 Kit", IGNORED),
        JUMP_SIZE("Jump Size", IGNORED),
        MEMBER_OF_POOL("Member of Pool", IGNORED),
        MOLECULAR_BARCODE_SEQUENCE("Molecular Barcode Sequence", IGNORED),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", IGNORED),
        REQUIRED_ACCESS("Require Controlled Access for Data", IGNORED),
        RESTRICTION_ENZYMES("Restriction Enzyme", IGNORED),
        SAMPLE_NUMBER("Sample Number", IGNORED),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", IGNORED),
        STRAIN("Strain", IGNORED),
        SUBMITTED_TO_GSSR("Submitted to Gssr", IGNORED),
        TISSUE_TYPE("Tissue Type", IGNORED),
        ;

        private final String text;
        private boolean requiredHeader;
        private boolean requiredValue;
        private boolean ignoredValue;
        private boolean isString = true;
        private boolean isDate = false;

        Headers(String text, Boolean isRequired) {
            this.text = text;
            this.requiredHeader = Boolean.TRUE.equals(isRequired);
            this.requiredValue = Boolean.TRUE.equals(isRequired);
            this.ignoredValue = (isRequired == IGNORED);
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return requiredHeader;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }

        @Override
        public boolean isIgnoredValue() {
            return ignoredValue;
        }

        @Override
        public boolean isDateColumn() {
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        additionalAssemblyInformations.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        additionalSampleInformations.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        aggregationParticles.add(getFromRow(dataRow, Headers.PROJECT_TITLE));
        collaboratorParticipantIds.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        collaboratorSampleIds.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        concentrations.add(getFromRow(dataRow, Headers.CONCENTRATION));
        dataAnalysisTypes.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        insertSizes.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE));
        librarySizes.add(getFromRow(dataRow, Headers.LIBRARY_SIZE));
        libraryTypes.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        molecularBarcodeNames.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        numbersOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        organisms.add(getFromRow(dataRow, Headers.ORGANISM));
        pooleds.add(getFromRow(dataRow, Headers.POOLED));
        readLengths.add(getFromRow(dataRow, Headers.READ_LENGTH));
        referenceSequences.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        sequencerModeNames.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        sexes.add(getFromRow(dataRow, Headers.SEX));
        volumes.add(getFromRow(dataRow, Headers.VOLUME));

        // Uses the library name for the tube barcode, unless its present in the spreadsheet.
        String library = getFromRow(dataRow, Headers.LIBRARY_NAME);
        libraryNames.add(library);
        String tubeBarcode = getFromRow(dataRow, Headers.TUBE_BARCODE);
        barcodes.add(StringUtils.isNotBlank(tubeBarcode) ? tubeBarcode : library);
        // Uses the library name for the sample name.
        sampleNames.add(library);

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    /**
     * Does self-consistency and other validation checks on the data.
     * Entities fetched for the row data are accessed through maps referenced in the dtos.
     */
    @Override
    public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite, MessageCollection messages) {
        Set<String> uniqueBarcodes = new HashSet<>();
        Set<String> uniqueLibraryNames = new HashSet<>();

        for (SampleInstanceEjb.RowDto dto : dtos) {

            // A library name must only appear in one row.
            if (!uniqueLibraryNames.add(dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(), "Library Name"));
            }
            // The pipeline and elsewhere require a simple name so disallow chars that might cause trouble.
            if (!StringUtils.containsOnly(dto.getLibraryName(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(), "Library Name"));
            }

            // Disallow a library name that could cause the sample to collide with an existing or future BSP sample.
            if (isInBspFormat(dto.getLibraryName())) {
                messages.addError(String.format(SampleInstanceEjb.BSP_FORMAT, dto.getRowNumber(),
                        "Library Name", dto.getLibraryName()));
            }

            if (dto.getSampleInstanceEntity() != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        "Library", dto.getLibraryName()));
            }

            MercurySample mercurySample = getSampleMap().get(dto.getSampleName());
            if (mercurySample != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        "Sample", dto.getLibraryName()));
            }

            // If a non-BSP Broad Sample needs to be created its name must not collide with a future BSP SM-id.
            if (mercurySample == null && StringUtils.isNotBlank(dto.getSampleName())) {
                SampleData sampleData = getFetchedData().get(dto.getSampleName());
                if (isInBspFormat(dto.getSampleName()) && (sampleData == null ||
                        sampleData.getMetadataSource().equals(MercurySample.MetadataSource.MERCURY))) {
                    messages.addError(String.format(SampleInstanceEjb.BSP_FORMAT, dto.getRowNumber(),
                            "Broad Sample", dto.getSampleName()));
                }
            }

            // Tube barcode must be unique in the spreadsheet.
            if (!uniqueBarcodes.add(dto.getBarcode())) {
                messages.addError(String.format(SampleInstanceEjb.DUPLICATE, dto.getRowNumber(),
                        ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText()));
            }
            if (getLabVesselMap().get(dto.getBarcode()) != null && !overwrite) {
                messages.addError(String.format(SampleInstanceEjb.PREXISTING, dto.getRowNumber(),
                        ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText(), dto.getBarcode()));
            } else {
                // The tube barcode character set is restricted.
                if (!StringUtils.containsOnly(dto.getBarcode(), SampleInstanceEjb.RESTRICTED_CHARS)) {
                    messages.addError(String.format(SampleInstanceEjb.INVALID_CHARS, dto.getRowNumber(),
                            "Tube barcode"));
                }
                // A new tube should not have a barcode that may collide with Matrix tubes.
                if (dto.getBarcode().length() == 10 && dto.getBarcode().matches("[0-9]+")) {
                    messages.addWarning(String.format(SampleInstanceEjb.MERCURY_FORMAT, dto.getRowNumber(),
                            (dto.getLibraryName().equals(dto.getBarcode()) ? "Library Name" : "Tube Barcode"),
                            dto.getBarcode()));
                }
            }
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
    public List<String> getSampleNames() {
        return libraryNames;
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
    public List<String> getLibrarySizes() {
        return librarySizes;
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
    public List<String> getPooleds() {
        return pooleds;
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
        return sequencerModeNames;
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
    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }

    @Override
    public SampleKitRequest getSampleKitRequest() {
        return sampleKitRequest;
    }

    @Override
    public void setSampleKitRequest(SampleKitRequest sampleKitRequest) {
        this.sampleKitRequest = sampleKitRequest;
    }

    @Override
    public boolean supportsSampleKitRequest() {
        return true;
    }
}
