package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ExternalLibraryProcessorPooledMultiOrganism extends TableProcessor {

    private List<String> headers = new ArrayList<>();
    private List<String> sequencingTechnology = new ArrayList<>();
    private List<String> strain = new ArrayList<>();
    private List<String> sex = new ArrayList<>();
    private List<String> cellLine = new ArrayList<>();
    private List<String> tissueType = new ArrayList<>();
    private List<String> collaboratorSampleId = new ArrayList<>();
    private List<String> individualName = new ArrayList<>();
    private List<String> singleSampleLibraryName = new ArrayList<>();
    private List<String> libraryType = new ArrayList<>();
    private List<String> dataAnalysisType = new ArrayList<>();
    private List<String> referenceSequence = new ArrayList<>();
    private List<String> insertSizeRangeBp = new ArrayList<>();
    private List<String> librarySizeRangeBp = new ArrayList<>();
    private List<String> jumpSize = new ArrayList<>();
    private List<String> illuminaKitUsed = new ArrayList<>();
    private List<String> restrictionEnzymes = new ArrayList<>();
    private List<String> molecularBarcodeSequence = new ArrayList<>();
    private List<String> molecularBarcodeName = new ArrayList<>();
    private List<String> totalLibraryVolume = new ArrayList<>();
    private List<String> totalLibraryConcentration = new ArrayList<>();
    private List<String> singleDoubleStranded = new ArrayList<>();
    private List<String> additionalSampleInformation = new ArrayList<>();
    private List<String> fundingSource = new ArrayList<>();
    private List<String> coverage = new ArrayList<>();
    private List<String> approvedBy = new ArrayList<>();
    private List<String> requestedCompletionDate = new ArrayList<>();
    private List<String> dataSubmission = new ArrayList<>();
    private List<String> additionalAssemblyInformation = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> irbNumber = new ArrayList<>();
    private List<String> desiredReadLength = new ArrayList<>();
    private List<String> projectTitle = new ArrayList<>();
    private List<String> requiredControlledAccess = new ArrayList<>();
    private List<String> accessList = new ArrayList<>();
    private List<String> pooled = new ArrayList<>();
    private List<String> memeberOfPool = new ArrayList<>();
    private List<String> submittedToGSSR = new ArrayList<>();
    private List<String> derivedFrom = new ArrayList<>();
    private List<String> organism = new ArrayList<>();
    private List<String> rowCount = new ArrayList<>();
    private String collaboratorName;
    private String firstName;
    private String lastName;
    private String organization;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
    private String email;

    public ExternalLibraryProcessorPooledMultiOrganism(String sheetName) {
        super(sheetName, TableProcessor.IgnoreTrailingBlankLines.YES);
    }

    @Override
    public int columnOffset() {
        return 1;
    }

    public List<String> getHeaderNames() {
        return headers;
    }


    public void processSubHeaders(List<String> headers) {
        this.headers = headers;
    }

    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
        int index = 0;
        for (String preamble : headers) {

            switch (index) {
                case 0:
                    break;
                case 1:
                    collaboratorName = preamble;
                    break;
                case 2:
                    firstName = preamble;
                    break;
                case 3:
                    lastName = preamble;
                    break;
                case 4:
                    organization = preamble;
                    break;
                case 5:
                    address = preamble;
                    break;
                case 6:
                    break;
                case 7:
                    break;
                case 8:
                    city = preamble;
                    break;
                case 9:
                    state = preamble;
                    break;
                case 10:
                    zip = preamble;
                    break;
                case 11:
                    country = preamble;
                    break;
                case 12:
                    phone = preamble;
                    break;
                case 13:
                    email = preamble;
                    break;
                case 14:
                    break;
                case 15:
                    break;
                case 16:
                    break;
                case 17:
                    break;
                case 18:
                    break;
                case 19:
                    break;
                case 20:
                    break;
                case 21:
                    break;
                case 22:
                    break;
                case 23:
                    break;
                case 24:
                    break;
                default:
                    break;
            }
            index++;
        }

    }


    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        rowCount.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ROW_COUNT.getText()));
        sequencingTechnology.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SEQUENCING_TECHNOLOGY.getText()));
        irbNumber.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.IRB_NUMBER.getText()));
        strain.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.STRAIN.getText()));
        sex.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SEX.getText()));
        cellLine.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.COLLABORATOR_SAMPLE_ID.getText()));
        singleSampleLibraryName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        individualName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.INDIVIDUAL_NAME.getText()));
        libraryType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.LIBRARY_TYPE.getText()));
        insertSizeRangeBp.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySizeRangeBp.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.LIBRARY_SIZE_RANGE_BP.getText()));
        jumpSize.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.JUMP_SIZE.getText()));
        illuminaKitUsed.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ILLUMINA_KIT_USED.getText()));
        restrictionEnzymes.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.RESTRICTION_ENZYMES.getText()));
        molecularBarcodeSequence.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MOLECULAR_BARCODE_SEQUENCE.getText()));
        molecularBarcodeName.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MOLECULAR_BARCODE_NAME.getText()));
        totalLibraryVolume.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TOTAL_LIBRARY_VOLUME.getText()));
        totalLibraryConcentration.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.TOTAL_LIBRARY_CONCENTRATION.getText()));
        additionalSampleInformation.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ADDITIONAL_SAMPLE_INFORMATION.getText()));
        singleDoubleStranded.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SINGLE_DOUBLE_STRANDED.getText()));
        desiredReadLength.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DESIRED_READ_LEGTH.getText()));
        projectTitle.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.PROJECT_TITLE.getText()));
        fundingSource.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.FUNDING_SOURCE.getText()));
        coverage.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.COVERAGE.getText()));
        requestedCompletionDate.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_SUBMISSION.getText()));
        dataAnalysisType.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_ANALYSIS_TYPE.getText()));
        referenceSequence.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_ANALYSIS_TYPE.getText()));
        additionalAssemblyInformation.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        requiredControlledAccess.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.REQUIRED_ACCESS.getText()));
        accessList.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.ACCESS_LIST.getText()));
        pooled.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.POOLED.getText()));
        memeberOfPool.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.MEMBER_OF_POOL.getText()));
        submittedToGSSR.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.SUBMITTED_TO_GSSR.getText()));
        derivedFrom.add(dataRow.get(ExternalLibraryProcessorPooledMultiOrganism.Headers.DERIVED_FROM.getText()));
        approvedBy.add(dataRow.get(Headers.APPROVED_BY.getText()));
        organism.add(dataRow.get(Headers.ORGANISIM.getText()));
    }

    protected ColumnHeader[] getColumnHeaders() {
        return VesselPooledTubesProcessor.Headers.values();
    }

    public void close() {
    }

    public enum Headers implements ColumnHeader {
        ROW_COUNT(null, ColumnHeader.OPTIONAL_HEADER, true),
        IRB_NUMBER("IRB Number", ColumnHeader.OPTIONAL_HEADER, true),
        SAMPLE_NUMBER("Sample Number", ColumnHeader.OPTIONAL_HEADER, true),
        TUBE_BARCODE("Tube barcode", ColumnHeader.OPTIONAL_HEADER, true),
        SEQUENCING_TECHNOLOGY("Sequencing Technology (Illumina/ 454)", ColumnHeader.OPTIONAL_HEADER, true),
        STRAIN("Strain", ColumnHeader.OPTIONAL_HEADER, true),
        SEX("Sex", ColumnHeader.OPTIONAL_HEADER, true),
        CELL_LINE("Cell Line", ColumnHeader.OPTIONAL_HEADER, true),
        TISSUE_TYPE("Tissue Type", ColumnHeader.OPTIONAL_HEADER, true),
        COLLABORATOR_SAMPLE_ID("Sample Collaborator ID (Biological Sample ID)", ColumnHeader.OPTIONAL_HEADER, true),
        INDIVIDUAL_NAME("Individual Name", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name (External Collaborator Library ID)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_TYPE("Library Type (see drop down)", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_ANALYSIS_TYPE("Data Analysis Type", ColumnHeader.OPTIONAL_HEADER, true),
        REFERENCE_SEQUENCE("Reference Sequence", ColumnHeader.OPTIONAL_HEADER, true),
        INSERT_SIZE_RANGE_BP("Insert Size Range (in bp. without adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_SIZE_RANGE_BP("Library Size Range (in bp. with adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        JUMP_SIZE("Jump Size (kb)", ColumnHeader.OPTIONAL_HEADER, true),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used  (see dropdown)", ColumnHeader.OPTIONAL_HEADER, true),
        RESTRICTION_ENZYMES("Restriction Enzyme (if applicable)", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_NAME("Molecular barcode name", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_VOLUME("Sample Volume (ul)", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_CONCENTRATION("Sample Concentration (ng/ul)", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", ColumnHeader.OPTIONAL_HEADER, true),
        DESIRED_READ_LEGTH("Desired Read Length for Illumina and note specific cluster density, if required . (See above)", ColumnHeader.OPTIONAL_HEADER, true),
        FUNDING_SOURCE("Funding Source", ColumnHeader.OPTIONAL_HEADER, true),
        COVERAGE("Coverage (# Lanes/Sample)", ColumnHeader.OPTIONAL_HEADER, true),
        APPROVED_BY("Approved By", ColumnHeader.OPTIONAL_HEADER, true),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_SUBMISSION("Data Submission (Yes, Yes Later, or No)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information (Please list who the data is going to and what they are to do with it.   (ie you, Sarah Youngs group, David Jaffe, Brian Haas)", ColumnHeader.OPTIONAL_HEADER, true),
        REQUIRED_ACCESS("Require controlled Access for Data (Yes/No)", ColumnHeader.OPTIONAL_HEADER, true),
        PROJECT_TITLE("Project Title (e.g. Lieberman_Aiden_Hi-C_GM12878_Rep2 library)", ColumnHeader.OPTIONAL_HEADER, true),
        ACCESS_LIST("IF Data Access Controlled is Desired, please Indicate individuals who should have access", ColumnHeader.OPTIONAL_HEADER, true),
        POOLED("Pooled (Y/N)", ColumnHeader.OPTIONAL_HEADER, true),
        MEMBER_OF_POOL("Member of Pool", ColumnHeader.OPTIONAL_HEADER, true),
        SUBMITTED_TO_GSSR("Submitted to GSSR (Y/N)", ColumnHeader.OPTIONAL_HEADER, true),
        DERIVED_FROM("Derived From", ColumnHeader.OPTIONAL_HEADER, true),
        ORGANISIM("Organism", ColumnHeader.OPTIONAL_HEADER, true);

        private String text = null;
        private boolean optionalHeader;
        private boolean isString;

        Headers(String text, boolean optionalHeader, boolean isString) {
            this.text = text;
            this.optionalHeader = optionalHeader;
            this.isString = isString;
        }

        @Override
        public String getText() {
            if (text == null) {
                return null;
            } else {
                return text.trim();
            }
        }

        @Override
        public boolean isRequiredHeader() {
            return true;
        }

        @Override
        public boolean isRequiredValue() {
            return this.optionalHeader;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return this.isString;
        }
    }


    public List<String> getOrganism() {
        return organism;
    }

    public List<String> getSequencingTechnology() {
        return sequencingTechnology;
    }

    public List<String> getPooled() {
        return pooled;
    }

    public List<String> getMemeberOfPool() {
        return memeberOfPool;
    }

    public List<String> getSubmittedToGSSR() {
        return submittedToGSSR;
    }

    public List<String> getDerivedFrom() {
        return derivedFrom;
    }

    public List<String> getAdditionalAssemblyInformation() {
        return additionalAssemblyInformation;
    }

    public List<String> getDataSubmission() {
        return dataSubmission;
    }

    public List<String> getRequestedCompletionDate() {
        return requestedCompletionDate;
    }

    public List<String> getApprovedBy() {
        return approvedBy;
    }

    public List<String> getCoverage() {
        return coverage;
    }

    public List<String> getFundingSource() {
        return fundingSource;
    }

    public List<String> getAdditionalSampleInformation() {
        return additionalSampleInformation;
    }

    public List<String> getSingleDoubleStranded() {
        return singleDoubleStranded;
    }

    public List<String> getDesiredReadLength() {
        return desiredReadLength;
    }

    public List<String> getAccessList() {
        return accessList;
    }

    public List<String> getRequiredControlledAccess() {
        return requiredControlledAccess;
    }

    public List<String> getTotalLibraryConcentration() {
        return totalLibraryConcentration;
    }

    public List<String> getTotalLibraryVolume() {
        return totalLibraryVolume;
    }

    public List<String> getProjectTitle() {
        return projectTitle;
    }

    public List<String> getMolecularBarcodeName() {
        return molecularBarcodeName;
    }

    public List<String> getMolecularBarcodeSequence() {
        return molecularBarcodeSequence;
    }

    public List<String> getIrbNumber() {
        return irbNumber;
    }

    public List<String> getRestrictionEnzymes() {
        return restrictionEnzymes;
    }

    public List<String> getIlluminaKitUsed() {
        return illuminaKitUsed;
    }

    public List<String> getJumpSize() {
        return jumpSize;
    }

    public List<String> getLibrarySizeRangeBp() {
        return librarySizeRangeBp;
    }

    public List<String> getInsertSizeRangeBp() {
        return insertSizeRangeBp;
    }

    public List<String> getReferenceSequence() {
        return referenceSequence;
    }

    public List<String> getDataAnalysisType() {
        return dataAnalysisType;
    }

    public List<String> getLibraryType() {
        return libraryType;
    }

    public List<String> getIndividualName() {
        return individualName;
    }

    public List<String> getTissueType() {
        return tissueType;
    }

    public List<String> getCellLine() {
        return cellLine;
    }

    public List<String> getSex() {
        return sex;
    }

    public List<String> getStrain() {
        return strain;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public List<String> getSingleSampleLibraryName() {
        return singleSampleLibraryName;
    }

    public List<String> getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public String getCountry() {
        return country;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

}
