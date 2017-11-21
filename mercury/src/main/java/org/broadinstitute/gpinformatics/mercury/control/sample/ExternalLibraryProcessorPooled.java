package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorPooled extends ExternalLibraryProcessor {
    public ExternalLibraryProcessorPooled(String sheetName) {
        super(sheetName);
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber) {
        sequencingTechnology.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SEQUENCING_TECHNOLOGY.getText()));
        irbNumber.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.IRB_NUMBER.getText()));
        strain.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.STRAIN.getText()));
        sex.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SEX.getText()));
        cellLine.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.COLLABORATOR_SAMPLE_ID.getText()));
        individualName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.INDIVIDUAL_NAME.getText()));
        singleSampleLibraryName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        libraryType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.LIBRARY_TYPE.getText()));
        molecularBarcodeSequence.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MOLECULAR_BARCODE_SEQUENCE.getText()));
        molecularBarcodeName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MOLECULAR_BARCODE_NAME.getText()));
        pooled.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.POOLED.getText()));
        memberOfPool.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MEMBER_OF_POOL.getText()));
        submittedToGSSR.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SUBMITTED_TO_GSSR.getText()));
        derivedFrom.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DERIVED_FROM.getText()));
        insertSize.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySize.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.LIBRARY_SIZE_RANGE_BP.getText()));
        jumpSize.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.JUMP_SIZE.getText()));
        illuminaKitUsed.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ILLUMINA_KIT_USED.getText()));
        restrictionEnzymes.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.RESTRICTION_ENZYMES.getText()));
        totalLibraryVolume.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TOTAL_LIBRARY_VOLUME.getText()));
        totalLibraryConcentration.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TOTAL_LIBRARY_CONCENTRATION.getText()));
        additionalSampleInformation.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ADDITIONAL_SAMPLE_INFORMATION.getText()));
        singleDoubleStranded.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SINGLE_DOUBLE_STRANDED.getText()));
        readLength.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DESIRED_READ_LENGTH.getText()));
        projectTitle.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.PROJECT_TITLE.getText()));
        fundingSource.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.FUNDING_SOURCE.getText()));
        numberOfLanes.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.COVERAGE.getText()));
        approvedBy.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.APPROVED_BY.getText()));
        referenceSequence.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_ANALYSIS_TYPE.getText()));
        requestedCompletionDate.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_SUBMISSION.getText()));
        requiredControlledAccess.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.REQUIRED_ACCESS.getText()));
        accessList.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ACCESS_LIST.getText()));
        additionalAssemblyInformation.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        dataAnalysisType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_ANALYSIS_TYPE.getText()));
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
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information", REQUIRED),
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
