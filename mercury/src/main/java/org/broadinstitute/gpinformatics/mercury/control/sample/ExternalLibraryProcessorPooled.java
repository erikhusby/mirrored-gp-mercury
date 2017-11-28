package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorPooled extends ExternalLibraryProcessor {
    public ExternalLibraryProcessorPooled(String sheetName) {
        super(sheetName);
    }

    private String getFromRow(Map<String, String> dataRow, Headers header) {
        return dataRow.get(getAdjustedNames().get(adjustHeaderName(header.getText())));
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber) {
        sequencingTechnology.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        irbNumber.add(getFromRow(dataRow, Headers.IRB_NUMBER));
        strain.add(getFromRow(dataRow, Headers.STRAIN));
        sex.add(getFromRow(dataRow, Headers.SEX));
        cellLine.add(getFromRow(dataRow, Headers.CELL_LINE));
        tissueType.add(getFromRow(dataRow, Headers.TISSUE_TYPE));
        collaboratorSampleId.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        individualName.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        singleSampleLibraryName.add(getFromRow(dataRow, Headers.SINGLE_SAMPLE_LIBRARY_NAME));
        libraryType.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        molecularBarcodeSequence.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_SEQUENCE));
        molecularBarcodeName.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        pooled.add(getFromRow(dataRow, Headers.POOLED));
        memberOfPool.add(getFromRow(dataRow, Headers.MEMBER_OF_POOL));
        submittedToGSSR.add(getFromRow(dataRow, Headers.SUBMITTED_TO_GSSR));
        derivedFrom.add(getFromRow(dataRow, Headers.DERIVED_FROM));
        insertSize.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE_BP));
        librarySize.add(getFromRow(dataRow, Headers.LIBRARY_SIZE_RANGE_BP));
        jumpSize.add(getFromRow(dataRow, Headers.JUMP_SIZE));
        illuminaKitUsed.add(getFromRow(dataRow, Headers.ILLUMINA_KIT_USED));
        restrictionEnzymes.add(getFromRow(dataRow, Headers.RESTRICTION_ENZYMES));
        totalLibraryVolume.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_VOLUME));
        totalLibraryConcentration.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_CONCENTRATION));
        additionalSampleInformation.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        singleDoubleStranded.add(getFromRow(dataRow, Headers.SINGLE_DOUBLE_STRANDED));
        readLength.add(getFromRow(dataRow, Headers.DESIRED_READ_LENGTH));
        projectTitle.add(getFromRow(dataRow, Headers.PROJECT_TITLE));
        fundingSource.add(getFromRow(dataRow, Headers.FUNDING_SOURCE));
        numberOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        approvedBy.add(getFromRow(dataRow, Headers.APPROVED_BY));
        referenceSequence.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        requestedCompletionDate.add(getFromRow(dataRow, Headers.REQUESTED_COMPLETION_DATE));
        dataSubmission.add(getFromRow(dataRow, Headers.DATA_SUBMISSION));
        requiredControlledAccess.add(getFromRow(dataRow, Headers.REQUIRED_ACCESS));
        accessList.add(getFromRow(dataRow, Headers.ACCESS_LIST));
        additionalAssemblyInformation.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        dataAnalysisType.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
    }

    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    public enum Headers implements ColumnHeader {
        SEQUENCING_TECHNOLOGY("Sequencing Technology", REQUIRED),
        IRB_NUMBER("IRB Number", REQUIRED),
        STRAIN("Strain", OPTIONAL),
        SEX("Sex", OPTIONAL),
        CELL_LINE("Cell Line", OPTIONAL),
        TISSUE_TYPE("Tissue Type", OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Sample Collaborator ID", REQUIRED),
        INDIVIDUAL_NAME("Individual Name", REQUIRED),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name", REQUIRED),
        LIBRARY_TYPE("Library Type", REQUIRED),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular barcode name", OPTIONAL),
        POOLED("Pooled", REQUIRED),
        MEMBER_OF_POOL("Member of Pool", REQUIRED),
        SUBMITTED_TO_GSSR("Submitted to GSSR", REQUIRED),
        DERIVED_FROM("Derived From", OPTIONAL),
        INSERT_SIZE_RANGE_BP("Insert Size Range", REQUIRED),
        LIBRARY_SIZE_RANGE_BP("Library Size Range", REQUIRED),
        JUMP_SIZE("Jump Size", OPTIONAL),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used", REQUIRED),
        RESTRICTION_ENZYMES("Restriction Enzyme", OPTIONAL),
        TOTAL_LIBRARY_VOLUME("Sample Volume", REQUIRED),
        TOTAL_LIBRARY_CONCENTRATION("Sample Concentration", REQUIRED),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", OPTIONAL),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded", REQUIRED),
        DESIRED_READ_LENGTH("Desired Read Length for Illumina and note specific cluster density, if required .", REQUIRED),
        PROJECT_TITLE("Project Title", REQUIRED),
        FUNDING_SOURCE("Funding Source", REQUIRED),
        COVERAGE("Coverage", REQUIRED),
        APPROVED_BY("Approved By", REQUIRED),
        REFERENCE_SEQUENCE("Reference Sequence", REQUIRED),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", REQUIRED),
        DATA_SUBMISSION("Data Submission", REQUIRED),
        REQUIRED_ACCESS("Require controlled Access for Data", REQUIRED),
        ACCESS_LIST("IF Data Access Controlled is Desired, please Indicate individuals who should have access", OPTIONAL),
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Information", REQUIRED),
        DATA_ANALYSIS_TYPE("Data Analysis Type", REQUIRED),
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
