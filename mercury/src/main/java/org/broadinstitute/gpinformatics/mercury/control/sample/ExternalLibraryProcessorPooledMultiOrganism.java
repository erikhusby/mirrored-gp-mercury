package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

/** Handles both  ExternalLibraryPooled  and  ExternalLibraryPooledMultiOrganism  spreadsheets. */

public class ExternalLibraryProcessorPooledMultiOrganism extends ExternalLibraryProcessor {
    public ExternalLibraryProcessorPooledMultiOrganism(String sheetName) {
        super(sheetName);
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        accessList.add(getFromRow(dataRow, Headers.ACCESS_LIST));
        additionalAssemblyInformation.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        additionalSampleInformation.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        approvedBy.add(getFromRow(dataRow, Headers.APPROVED_BY));
        cellLine.add(getFromRow(dataRow, Headers.CELL_LINE));
        collaboratorSampleId.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        dataAnalysisType.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        dataSubmission.add(getFromRow(dataRow, Headers.DATA_SUBMISSION));
        derivedFrom.add(getFromRow(dataRow, Headers.DERIVED_FROM));
        fundingSource.add(getFromRow(dataRow, Headers.FUNDING_SOURCE));
        illuminaKitUsed.add(getFromRow(dataRow, Headers.ILLUMINA_KIT_USED));
        individualName.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        insertSize.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE));
        irbNumber.add(getFromRow(dataRow, Headers.IRB_NUMBER));
        jumpSize.add(getFromRow(dataRow, Headers.JUMP_SIZE));
        librarySize.add(getFromRow(dataRow, Headers.LIBRARY_SIZE_RANGE));
        libraryType.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        memberOfPool.add(getFromRow(dataRow, Headers.MEMBER_OF_POOL));
        molecularBarcodeName.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        molecularBarcodeSequence.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_SEQUENCE));
        numberOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        organism.add(getFromRow(dataRow, Headers.ORGANISM));
        pooled.add(getFromRow(dataRow, Headers.POOLED));
        projectTitle.add(getFromRow(dataRow, Headers.PROJECT_TITLE));
        readLength.add(getFromRow(dataRow, Headers.DESIRED_READ_LENGTH));
        referenceSequence.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        requestedCompletionDate.add(getFromRow(dataRow, Headers.REQUESTED_COMPLETION_DATE));
        requiredControlledAccess.add(getFromRow(dataRow, Headers.REQUIRED_ACCESS));
        restrictionEnzymes.add(getFromRow(dataRow, Headers.RESTRICTION_ENZYMES));
        sequencingTechnology.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        sex.add(getFromRow(dataRow, Headers.SEX));
        singleDoubleStranded.add(getFromRow(dataRow, Headers.SINGLE_DOUBLE_STRANDED));
        singleSampleLibraryName.add(getFromRow(dataRow, Headers.SINGLE_SAMPLE_LIBRARY_NAME));
        strain.add(getFromRow(dataRow, Headers.STRAIN));
        submittedToGSSR.add(getFromRow(dataRow, Headers.SUBMITTED_TO_GSSR));
        tissueType.add(getFromRow(dataRow, Headers.TISSUE_TYPE));
        totalLibraryConcentration.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_CONCENTRATION));
        totalLibraryVolume.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_VOLUME));
    }

    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    // Only the first four words of header text are used and the rest are ignored.
    public enum Headers implements ColumnHeader {
        ACCESS_LIST("If Data Access Controlled", OPTIONAL),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", OPTIONAL),
        APPROVED_BY("Approved By", OPTIONAL),
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Info", OPTIONAL),
        BLANK("", OPTIONAL),
        CELL_LINE("Cell Line", OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Sample Collaborator ID", REQUIRED),
        COVERAGE("Coverage", REQUIRED),
        DATA_ANALYSIS_TYPE("Data Analysis Type", REQUIRED),
        DATA_SUBMISSION("Data Submission", OPTIONAL),
        DERIVED_FROM("Derived From", OPTIONAL),
        DESIRED_READ_LENGTH("Desired Read Length for", REQUIRED),
        FUNDING_SOURCE("Funding Source", OPTIONAL),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used", OPTIONAL),
        INDIVIDUAL_NAME("Individual Name", REQUIRED),
        INSERT_SIZE_RANGE("Insert Size Range", OPTIONAL),
        IRB_NUMBER("IRB Number", REQUIRED),
        JUMP_SIZE("Jump Size", OPTIONAL),
        LIBRARY_SIZE_RANGE("Library Size Range", REQUIRED),
        LIBRARY_TYPE("Library Type", REQUIRED),
        MEMBER_OF_POOL("Member of Pool", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular barcode name", OPTIONAL),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence", OPTIONAL),
        ORGANISM("Organism", OPTIONAL),
        POOLED("Pooled", REQUIRED),
        PROJECT_TITLE("Project Title", REQUIRED),
        REFERENCE_SEQUENCE("Reference Sequence", REQUIRED),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", OPTIONAL),
        REQUIRED_ACCESS("Require controlled Access for Data", OPTIONAL),
        RESTRICTION_ENZYMES("Restriction Enzyme", OPTIONAL),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", REQUIRED),
        SEX("Sex", OPTIONAL),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded", REQUIRED),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name", REQUIRED),
        STRAIN("Strain", OPTIONAL),
        SUBMITTED_TO_GSSR("Submitted to GSSR", OPTIONAL),
        TISSUE_TYPE("Tissue Type", OPTIONAL),
        TOTAL_LIBRARY_CONCENTRATION("Sample Concentration", REQUIRED),
        TOTAL_LIBRARY_VOLUME("Sample Volume", REQUIRED),
        ;

        private final String text;
        private boolean requiredHeader;
        private boolean requiredValue;
        private boolean isString = true;
        private boolean isDate = false;

        Headers(String text, boolean isRequired) {
            this.text = text;
            this.requiredHeader = isRequired;
            this.requiredValue = isRequired;
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
        public boolean isDateColumn() {
            return isDate;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
        }
    }
}
