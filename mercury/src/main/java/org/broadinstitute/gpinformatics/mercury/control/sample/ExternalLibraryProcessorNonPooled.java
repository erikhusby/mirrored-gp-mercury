package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExternalLibraryProcessorNonPooled extends TableProcessor {
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
    private String commonName;
    private String genus;
    private String species;
    private String irbRequired;


    public ExternalLibraryProcessorNonPooled(String sheetName) {
        super(sheetName, TableProcessor.IgnoreTrailingBlankLines.YES);
    }


    public List<String> getHeaderNames() {
        return headers;
    }

    public int columnOffset() {
        return 1;
    }

    public void processSubHeaders(List<String> headers) {
        this.headers = headers;
    }


    public void processHeader(List<String> headers, int row) {
        int index = 0;
        for (String preamble : headers) {

            switch (index) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    collaboratorName = preamble;
                    break;
                case 3:
                    firstName = preamble;
                    break;
                case 4:
                    lastName = preamble;
                    break;
                case 5:
                    organization = preamble;
                    break;
                case 6:
                    address = preamble;
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
                    commonName = preamble;
                    break;
                case 18:
                    genus = preamble;
                    break;
                case 19:
                    species = preamble;
                    break;
                case 20:
                    irbRequired = preamble;
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
                    this.headers.add(preamble);
                    break;
            }

            index++;
        }

    }


    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {

        sequencingTechnology.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.SEQUENCING_TECHNOLOGY.getText()));
        irbNumber.add(dataRow.get(Headers.IRB_NUMBER.getText()));
        strain.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.STRAIN.getText()));
        sex.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.SEX.getText()));
        cellLine.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.COLLABORATOR_SAMPLE_ID.getText()));
        singleSampleLibraryName.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        libraryType.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.LIBRARY_TYPE.getText()));
        insertSizeRangeBp.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySizeRangeBp.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.LIBRARY_SIZE_RANGE_BP.getText()));
        jumpSize.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.JUMP_SIZE.getText()));
        illuminaKitUsed.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.ILLUMINA_KIT_USED.getText()));
        restrictionEnzymes.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.RESTRICTION_ENZYMES.getText()));
        molecularBarcodeSequence.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.MOLECULAR_BARCODE_SEQUENCE.getText()));
        molecularBarcodeName.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.MOLECULAR_BARCODE_NAME.getText()));
        totalLibraryVolume.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.TOTAL_LIBRARY_VOLUME.getText()));
        totalLibraryConcentration.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.TOTAL_LIBRARY_CONCENTRATION.getText()));
        additionalSampleInformation.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.ADDITIONAL_SAMPLE_INFORMATION.getText()));
        singleDoubleStranded.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.SINGLE_DOUBLE_STRANDED.getText()));
        desiredReadLength.add(dataRow.get(Headers.DESIRED_READ_LENGTH.getText()));
        projectTitle.add(dataRow.get(Headers.PROJECT_TITLE.getText()));
        fundingSource.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.FUNDING_SOURCE.getText()));
        coverage.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.COVERAGE.getText()));
        requestedCompletionDate.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.DATA_SUBMISSION.getText()));
        dataAnalysisType.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.DATA_ANALYSIS_TYPE.getText()));
        referenceSequence.add(dataRow.get(Headers.REFERENCE_SEQUENCE.getText()));
        additionalAssemblyInformation.add(dataRow.get(ExternalLibraryProcessorNonPooled.Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        requiredControlledAccess.add(dataRow.get(Headers.REQUIRED_ACCESS.getText()));
        accessList.add(dataRow.get(Headers.ACCESS_LIST.getText()));
        individualName.add(dataRow.get(Headers.INDIVIDUAL_NAME.getText()));
        approvedBy.add(dataRow.get(Headers.APPROVED_BY.getText()));

    }

    protected ColumnHeader[] getColumnHeaders() {
        return VesselPooledTubesProcessor.Headers.values();
    }

    public void close() {
    }

    private enum Headers implements ColumnHeader {
        IRB_NUMBER("IRB Number", ColumnHeader.OPTIONAL_HEADER, true),
        SAMPLE_NUMBER("Sample Number", ColumnHeader.OPTIONAL_HEADER, true),
        TUBE_BARCODE("Tube barcode", ColumnHeader.OPTIONAL_HEADER, true),
        SEQUENCING_TECHNOLOGY("Sequencing Technology (Illumina/454) ", ColumnHeader.OPTIONAL_HEADER, true),
        STRAIN("Strain", ColumnHeader.OPTIONAL_HEADER, true),
        SEX("Sex (for non-human samples only)", ColumnHeader.OPTIONAL_HEADER, true),
        CELL_LINE("Cell Line", ColumnHeader.OPTIONAL_HEADER, true),
        TISSUE_TYPE("Tissue Type", ColumnHeader.OPTIONAL_HEADER, true),
        COLLABORATOR_SAMPLE_ID("Sample Collaborator ID", ColumnHeader.OPTIONAL_HEADER, true),
        INDIVIDUAL_NAME("Individual Name (aka Patient ID, Required for human subject samples)", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name (External Collaborator Library ID)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_TYPE("Library Type (see dropdown)  ", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_ANALYSIS_TYPE("Data Analysis Type", ColumnHeader.OPTIONAL_HEADER, true),
        REFERENCE_SEQUENCE("Reference Sequence", ColumnHeader.OPTIONAL_HEADER, true),
        INSERT_SIZE_RANGE_BP("Insert Size Range bp. (i.e the library size without adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_SIZE_RANGE_BP("Library Size Range bp. (i.e. the insert size plus adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        JUMP_SIZE("Jump Size (kb) if applicable", ColumnHeader.OPTIONAL_HEADER, true),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used  (see dropdown) ", ColumnHeader.OPTIONAL_HEADER, true),
        RESTRICTION_ENZYMES("Restriction Enzyme if applicable", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_NAME("Molecule barcode name", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_VOLUME("Sample Volume (ul)", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_CONCENTRATION("Sample Concentration (ng/ul)", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", ColumnHeader.OPTIONAL_HEADER, true),
        DESIRED_READ_LENGTH("Desired Read Length for Illumina and note specific cluster density, if required . (See above)", ColumnHeader.OPTIONAL_HEADER, true),
        FUNDING_SOURCE("Funding Source", ColumnHeader.OPTIONAL_HEADER, true),
        COVERAGE("Coverage (# Lanes/Sample)", ColumnHeader.OPTIONAL_HEADER, true),
        APPROVED_BY("Approved By ", ColumnHeader.OPTIONAL_HEADER, true),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_SUBMISSION("Data Submission (Yes, Yes Later, or No)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information (Please list who the data is going to and what they are to do with it.   (ie you, Sarah Youngs group, David Jaffe, Brian Haas) ", ColumnHeader.OPTIONAL_HEADER, true),
        REQUIRED_ACCESS("Require controlled Access for Data (Yes/No)", ColumnHeader.OPTIONAL_HEADER, true),
        PROJECT_TITLE("Project Title (e.g. Lieberman_Aiden_Hi-C_GM12878_Rep2 library)", ColumnHeader.OPTIONAL_HEADER, true),
        ACCESS_LIST("IF Data Access Controlled is Desired, please Indicate individuals who should have access", ColumnHeader.OPTIONAL_HEADER, true);

        private final String text;
        private boolean optionalHeader;
        private boolean isString;

        Headers(String text, boolean optionalHeader, boolean isString) {
            this.text = text;
            this.optionalHeader = optionalHeader;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text.trim();
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


    public List<String> getDataSubmission() {
        return dataSubmission;
    }

    public List<String> getRequestedCompletionDate() {
        return requestedCompletionDate;
    }

    public List<String> getCoverage() {
        return coverage;
    }

    public List<String> getFundingSource() {
        return fundingSource;
    }

    public List<String> getDesiredReadLength() {
        return desiredReadLength;
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

    public List<String> getMolecularBarcodeSequence() {
        return molecularBarcodeSequence;
    }

    public List<String> getIrbNumber() {
        return irbNumber;
    }

    public List<String> getRestrictionEnzymes() {
        return restrictionEnzymes;
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

    public List<String> getPooled() {
        return null;
    }

    public List<String> getOrganism() {
        return null;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getGenus() {
        return genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getIrbRequired() {
        return irbRequired;
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

}
