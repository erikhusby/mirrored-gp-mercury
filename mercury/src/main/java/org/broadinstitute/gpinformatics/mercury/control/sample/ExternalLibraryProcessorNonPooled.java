package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorNonPooled extends ExternalLibraryProcessor {

    public ExternalLibraryProcessorNonPooled(String sheetName) {
        super(sheetName);
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber) {
        sequencingTechnology.add(dataRow.get(Headers.SEQUENCING_TECHNOLOGY.getText()));
        irbNumber.add(dataRow.get(Headers.IRB_NUMBER.getText()));
        strain.add(dataRow.get(Headers.STRAIN.getText()));
        sex.add(dataRow.get(Headers.SEX.getText()));
        cellLine.add(dataRow.get(Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(Headers.COLLABORATOR_SAMPLE_ID.getText()));
        individualName.add(dataRow.get(Headers.INDIVIDUAL_NAME.getText()));
        singleSampleLibraryName.add(dataRow.get(Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        libraryType.add(dataRow.get(Headers.LIBRARY_TYPE.getText()));
        insertSizeRangeBp.add(dataRow.get(Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySizeRangeBp.add(dataRow.get(Headers.LIBRARY_SIZE_RANGE_BP.getText()));
        jumpSize.add(dataRow.get(Headers.JUMP_SIZE.getText()));
        illuminaKitUsed.add(dataRow.get(Headers.ILLUMINA_KIT_USED.getText()));
        restrictionEnzymes.add(dataRow.get(Headers.RESTRICTION_ENZYMES.getText()));
        molecularBarcodeSequence.add(dataRow.get(Headers.MOLECULAR_BARCODE_SEQUENCE.getText()));
        molecularBarcodeName.add(dataRow.get((Headers.MOLECULAR_BARCODE_NAME.getText())));
        totalLibraryVolume.add(dataRow.get(Headers.TOTAL_LIBRARY_VOLUME.getText()));
        totalLibraryConcentration.add(dataRow.get(Headers.TOTAL_LIBRARY_CONCENTRATION.getText()));
        additionalSampleInformation.add(dataRow.get(Headers.ADDITIONAL_SAMPLE_INFORMATION.getText()));
        singleDoubleStranded.add(dataRow.get(Headers.SINGLE_DOUBLE_STRANDED.getText()));
        desiredReadLength.add(dataRow.get(Headers.DESIRED_READ_LENGTH.getText()));
        projectTitle.add(dataRow.get(Headers.PROJECT_TITLE.getText()));
        fundingSource.add(dataRow.get(Headers.FUNDING_SOURCE.getText()));
        coverage.add(dataRow.get(Headers.COVERAGE.getText()));
        approvedBy.add(dataRow.get(Headers.APPROVED_BY.getText()));
        referenceSequence.add(dataRow.get(Headers.REFERENCE_SEQUENCE.getText()));
        requestedCompletionDate.add(dataRow.get(Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(Headers.DATA_SUBMISSION.getText()));
        requiredControlledAccess.add(dataRow.get(Headers.REQUIRED_ACCESS.getText()));
        accessList.add(dataRow.get(Headers.ACCESS_LIST.getText()));
        additionalAssemblyInformation.add(dataRow.get(Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        dataAnalysisType.add(dataRow.get(Headers.DATA_ANALYSIS_TYPE.getText()));
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
        INSERT_SIZE_RANGE_BP("Insert Size Range bp.", REQUIRED),
        LIBRARY_SIZE_RANGE_BP("Library Size Range bp.", REQUIRED),
        JUMP_SIZE("Jump Size", OPTIONAL),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used", REQUIRED),
        RESTRICTION_ENZYMES("Restriction Enzyme if applicable", OPTIONAL),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecule barcode name", OPTIONAL),
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
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information", OPTIONAL),
        DATA_ANALYSIS_TYPE("Data Analysis Type", OPTIONAL),
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
