package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.Map;

public class ExternalLibraryProcessorEzPass extends ExternalLibraryProcessor {

    public ExternalLibraryProcessorEzPass(String sheetName) {
        super(sheetName);
    }

    private String getFromRow(Map<String, String> dataRow, Headers header) {
        return dataRow.get(getAdjustedNames().get(adjustHeaderName(header.getText())));
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        additionalAssemblyInformation.add(getFromRow(dataRow, Headers.ASSEMBLY_INFORMATION));
        additionalSampleInformation.add(getFromRow(dataRow, Headers.ADDITIONAL_SAMPLE_INFORMATION));
        approvedBy.add(getFromRow(dataRow, Headers.APPROVED_BY));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        cellLine.add(getFromRow(dataRow, Headers.CELL_LINE));
        collaboratorSampleId.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        dataAnalysisType.add(getFromRow(dataRow, Headers.DATA_ANALYSIS_TYPE));
        dataSubmission.add(getFromRow(dataRow, Headers.DATA_SUBMISSION));
        fundingSource.add(getFromRow(dataRow, Headers.FUNDING_SOURCE));
        gssrOfBaitPool.add(getFromRow(dataRow, Headers.GSSR_OF_BAIT_POOL));
        illuminaKitUsed.add(getFromRow(dataRow, Headers.ILLUMINA_KIT_USED));
        individualName.add(getFromRow(dataRow, Headers.INDIVIDUAL_NAME));
        insertSize.add(getFromRow(dataRow, Headers.INSERT_SIZE_RANGE_BP));
        jumpSize.add(getFromRow(dataRow, Headers.JUMP_SIZE));
        librarySize.add(getFromRow(dataRow, Headers.LIBRARY_SIZE_RANGE_BP));
        libraryType.add(getFromRow(dataRow, Headers.LIBRARY_TYPE));
        molecularBarcodeName.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_NAME));
        molecularBarcodePlateID.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_PLATE_ID));
        molecularBarcodePlateWellID.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_PLATE_WELL_ID));
        molecularBarcodeSequence.add(getFromRow(dataRow, Headers.MOLECULAR_BARCODE_SEQUENCE));
        numberOfLanes.add(getFromRow(dataRow, Headers.COVERAGE));
        pooled.add(getFromRow(dataRow, Headers.POOLED));
        projectTitle.add(getFromRow(dataRow, Headers.PROJECT_TITLE));
        referenceSequence.add(getFromRow(dataRow, Headers.REFERENCE_SEQUENCE));
        requestedCompletionDate.add(getFromRow(dataRow, Headers.REQUESTED_COMPLETION_DATE));
        restrictionEnzymes.add(getFromRow(dataRow, Headers.RESTRICTION_ENZYMES));
        sampleNumber.add(getFromRow(dataRow, Headers.SAMPLE_NUMBER));
        sequencingTechnology.add(getFromRow(dataRow, Headers.SEQUENCING_TECHNOLOGY));
        sex.add(getFromRow(dataRow, Headers.SEX));
        singleDoubleStranded.add(getFromRow(dataRow, Headers.SINGLE_DOUBLE_STRANDED));
        singleSampleLibraryName.add(getFromRow(dataRow, Headers.SINGLE_SAMPLE_LIBRARY_NAME));
        sourceSampleGSSRId.add(getFromRow(dataRow, Headers.SOURCE_SAMPLE_GSSR_ID));
        squidProject.add(getFromRow(dataRow, Headers.SQUID_PROJECT));
        strain.add(getFromRow(dataRow, Headers.STRAIN));
        tissueType.add(getFromRow(dataRow, Headers.TISSUE_TYPE));
        totalLibraryConcentration.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_CONCENTRATION));
        totalLibraryVolume.add(getFromRow(dataRow, Headers.TOTAL_LIBRARY_VOLUME));
        virtualGSSRId.add(getFromRow(dataRow, Headers.VIRTUAL_GSSR_ID));
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
        ASSEMBLY_INFORMATION("Additional Assembly and Analysis Information", OPTIONAL),
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
