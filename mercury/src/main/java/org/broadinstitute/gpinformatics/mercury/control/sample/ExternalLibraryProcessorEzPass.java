package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorEzPass extends ExternalLibraryProcessor {

    public ExternalLibraryProcessorEzPass(String sheetName) {
        super(sheetName);
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber) {
        sampleNumber.add(dataRow.get(Headers.SAMPLE_NUMBER.getText()));
        barcodes.add(dataRow.get(Headers.TUBE_BARCODE.getText()));
        sourceSampleGSSRId.add(dataRow.get(Headers.SOURCE_SAMPLE_GSSR_ID.getText()));
        virtualGSSRId.add(dataRow.get(Headers.VIRTUAL_GSSR_ID.getText()));
        squidProject.add(dataRow.get(Headers.SQUID_PROJECT.getText()));
        sequencingTechnology.add(dataRow.get(Headers.SEQUENCING_TECHNOLOGY.getText()));
        strain.add(dataRow.get(Headers.STRAIN.getText()));
        sex.add(dataRow.get(Headers.SEX.getText()));
        cellLine.add(dataRow.get(Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(Headers.COLLABORATOR_SAMPLE_ID.getText()));
        individualName.add(dataRow.get(Headers.INDIVIDUAL_NAME.getText()));
        singleSampleLibraryName.add(dataRow.get(Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        libraryType.add(dataRow.get(Headers.LIBRARY_TYPE.getText()));
        dataAnalysisType.add(dataRow.get(Headers.DATA_ANALYSIS_TYPE.getText()));
        referenceSequence.add(dataRow.get(Headers.REFERENCE_SEQUENCE.getText()));
        gssrOfBaitPool.add(dataRow.get(Headers.GSSR_OF_BAIT_POOL.getText()));
        insertSize.add(dataRow.get(Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySize.add(dataRow.get(Headers.LIBRARY_SIZE_RANGE_BP.getText()));
        jumpSize.add(dataRow.get(Headers.JUMP_SIZE.getText()));
        illuminaKitUsed.add(dataRow.get(Headers.ILLUMINA_KIT_USED.getText()));
        restrictionEnzymes.add(dataRow.get(Headers.RESTRICTION_ENZYMES.getText()));
        molecularBarcodePlateID.add(dataRow.get(Headers.MOLECULAR_BARCODE_PLATE_ID.getText()));
        molecularBarcodePlateWellID.add(dataRow.get(Headers.MOLECULAR_BARCODE_PLATE_WELL_ID.getText()));
        molecularBarcodeSequence.add(dataRow.get(Headers.MOLECULAR_BARCODE_SEQUENCE.getText()));
        molecularBarcodeName.add(dataRow.get(Headers.MOLECULAR_BARCODE_NAME.getText()));
        totalLibraryVolume.add(dataRow.get(Headers.TOTAL_LIBRARY_VOLUME.getText()));
        totalLibraryConcentration.add(dataRow.get(Headers.TOTAL_LIBRARY_CONCENTRATION.getText()));
        singleDoubleStranded.add(dataRow.get(Headers.SINGLE_DOUBLE_STRANDED.getText()));
        additionalSampleInformation.add(dataRow.get(Headers.ADDITIONAL_SAMPLE_INFORMATION.getText()));
        projectTitle.add(dataRow.get(Headers.PROJECT_TITLE.getText()));
        fundingSource.add(dataRow.get(Headers.FUNDING_SOURCE.getText()));
        numberOfLanes.add(dataRow.get(Headers.COVERAGE.getText()));
        approvedBy.add(dataRow.get(Headers.APPROVED_BY.getText()));
        requestedCompletionDate.add(dataRow.get(Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(Headers.DATA_SUBMISSION.getText()));
        additionalAssemblyInformation.add(dataRow.get(Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        pooled.add(dataRow.get(Headers.POOLED.getText()));
    }

    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    public enum Headers implements ColumnHeader {
        SAMPLE_NUMBER("Sample Number", OPTIONAL),
        TUBE_BARCODE("Sample Tube Barcode", OPTIONAL),
        SOURCE_SAMPLE_GSSR_ID("Source Sample GSSR ID", OPTIONAL),
        VIRTUAL_GSSR_ID("Virtual GSSR ID", OPTIONAL),
        SQUID_PROJECT("SQUID Project", OPTIONAL),
        SEQUENCING_TECHNOLOGY("Sequencing Technology", REQUIRED),
        STRAIN("Strain", OPTIONAL),
        SEX("Sex", OPTIONAL),
        CELL_LINE("Cell Line", OPTIONAL),
        TISSUE_TYPE("Tissue Type", OPTIONAL),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", REQUIRED),
        INDIVIDUAL_NAME("Individual Name", REQUIRED),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name", REQUIRED),
        LIBRARY_TYPE("Library Type", REQUIRED),
        DATA_ANALYSIS_TYPE("Data Analysis Type", REQUIRED),
        REFERENCE_SEQUENCE("Reference Sequence", REQUIRED),
        GSSR_OF_BAIT_POOL("GSSR # of Bait Pool", OPTIONAL),
        INSERT_SIZE_RANGE_BP("Insert Size Range bp.", REQUIRED),
        LIBRARY_SIZE_RANGE_BP("Library Size Range bp.", REQUIRED),
        JUMP_SIZE("Jump Size", OPTIONAL),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used", REQUIRED),
        RESTRICTION_ENZYMES("Restriction Enzyme if applicable", OPTIONAL),
        MOLECULAR_BARCODE_PLATE_ID("Molecular barcode Plate ID", OPTIONAL),
        MOLECULAR_BARCODE_PLATE_WELL_ID("Molecular barcode Plate well ID", OPTIONAL),
        MOLECULAR_BARCODE_SEQUENCE("Molecular Barcode Sequence", OPTIONAL),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", OPTIONAL),
        TOTAL_LIBRARY_VOLUME("Total Library Volume", REQUIRED),
        TOTAL_LIBRARY_CONCENTRATION("Total Library Concentration", REQUIRED),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded", REQUIRED),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", OPTIONAL),
        PROJECT_TITLE("Project Title Description", OPTIONAL),
        FUNDING_SOURCE("Funding Source", OPTIONAL),
        COVERAGE("Coverage", OPTIONAL),
        APPROVED_BY("Approved By", OPTIONAL),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", OPTIONAL),
        DATA_SUBMISSION("Data Submission", OPTIONAL),
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information", OPTIONAL),
        POOLED("Pooled", OPTIONAL),
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
