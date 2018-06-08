package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorEzPass extends ExternalLibraryProcessor {

    public ExternalLibraryProcessorEzPass(String sheetName) {
        super(sheetName);
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        additionalAssemblyInformation.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        additionalSampleInformation.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        collaboratorSampleId.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        dataAnalysisType.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        dataSubmission.add(getFromRow(dataRow, Headers.DATA_SUBMISSION));
        individualName.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        insertSize.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE));
        librarySize.add(getFromRow(dataRow, Headers.LIBRARY_SIZE));
        libraryType.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        molecularBarcodeName.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        numberOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        pooled.add(getFromRow(dataRow, Headers.POOLED));
        projectTitle.add(getFromRow(dataRow, Headers.PROJECT_TITLE));
        referenceSequence.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        sequencingTechnology.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        sex.add(getFromRow(dataRow, Headers.SEX));
        singleDoubleStranded.add(getFromRow(dataRow, Headers.SINGLE_DOUBLE_STRANDED));
        libraryName.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        concentration.add(getFromRow(dataRow, Headers.CONCENTRATION));
        volume.add(getFromRow(dataRow, Headers.VOLUME));
        squidProject.add(getFromRow(dataRow, Headers.SQUID_PROJECT));
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    // Only the first four words of header text are used and the rest are ignored.
    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        LIBRARY_NAME("Library Name", REQUIRED),
        TUBE_BARCODE("Sample Tube Barcode", OPTIONAL),
        COVERAGE("Coverage (lanes/sample)", REQUIRED),
        DATA_ANALYSIS_TYPE("Data Analysis Type", REQUIRED),
        INDIVIDUAL_NAME("Individual Name (Patient Id)", REQUIRED),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", REQUIRED),
        LIBRARY_SIZE("Library Size", REQUIRED),
        PROJECT_TITLE("Project Title (pipeline aggregator)", REQUIRED),
        REFERENCE_SEQUENCE("Reference Sequence", REQUIRED),
        CONCENTRATION("Concentration (ng/ul)", REQUIRED),
        VOLUME("Volume (ul)", REQUIRED),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", REQUIRED),

        // Optional header and data.
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Info", OPTIONAL),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", OPTIONAL),
        ORGANISM("Organism", OPTIONAL),
        POOLED("Pooled (Y/N)", OPTIONAL),
        SEX("Sex (M/F)", OPTIONAL),
        LIBRARY_TYPE("Library Type", OPTIONAL),
        READ_LENGTH("Desired Read Length", OPTIONAL),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", OPTIONAL),
        SQUID_PROJECT("Squid Project", OPTIONAL),

        // Ignored header and data is not saved.
        BLANK("", IGNORED),
        APPROVED_BY("Approved By", IGNORED),
        CELL_LINE("Cell Line", IGNORED),
        DATA_SUBMISSION("Data Submission", IGNORED),
        FUNDING_SOURCE("Funding Source", IGNORED),
        GSSR_OF_BAIT_POOL("GSSR # of Bait Pool", IGNORED),
        ILLUMINA_KIT_USED("Illumina or 454 Kit", IGNORED),
        INSERT_SIZE_RANGE("Insert Size Range", IGNORED),
        JUMP_SIZE("Jump Size", IGNORED),
        MOLECULAR_BARCODE_PLATE_ID("Molecular Barcode Plate Id", IGNORED),
        MOLECULAR_BARCODE_PLATE_WELL_ID("Molecular Barcode Plate Well Id", IGNORED),
        MOLECULAR_BARCODE_SEQUENCE("Molecular Barcode Sequence", IGNORED),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", IGNORED),
        RESTRICTION_ENZYMES("Restriction Enzyme", IGNORED),
        SAMPLE_NUMBER("Sample Number", IGNORED),
        SOURCE_SAMPLE_GSSR_ID("Source Sample Gssr Id", IGNORED),
        STRAIN("Strain", IGNORED),
        SUBMITTED_TO_GSSR("Submitted to Gssr", IGNORED),
        TISSUE_TYPE("Tissue Type", IGNORED),
        VIRTUAL_GSSR_ID("Virtual Gssr Id", IGNORED),
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
}
