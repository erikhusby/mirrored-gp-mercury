package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExternalLibraryProcessorEzPass extends TableProcessor {

    private List<String> headers = new ArrayList<>();
    private List<String> sampleNumber = new ArrayList<>();
    private List<String> sourceSampleGSSRId = new ArrayList<>();
    private List<String> virtualGSSRId = new ArrayList<>();
    private List<String> squidProject = new ArrayList<>();
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
    private List<String> gssrOfBaitPool = new ArrayList<>();
    private List<String> insertSizeRangeBp = new ArrayList<>();
    private List<String> librarySizeRangeBp = new ArrayList<>();
    private List<String> jumpSize = new ArrayList<>();
    private List<String> illuminaKitUsed = new ArrayList<>();
    private List<String> restrictionEnzymes = new ArrayList<>();
    private List<String> molecularBarcodePlateID = new ArrayList<>();
    private List<String> molecularBarcodePlateWellID = new ArrayList<>();
    private List<String> molecularBarcodeSequence = new ArrayList<>();
    private List<String> molecularBarcodeName = new ArrayList<>();
    private List<String> totalLibraryVolume = new ArrayList<>();
    private List<String> totalLibraryConcentration = new ArrayList<>();
    private List<String> singleDoubleStranded = new ArrayList<>();
    private List<String> additionalSampleInformation = new ArrayList<>();
    private List<String> fundingSource = new ArrayList<>();
    private List<String> coverage = new ArrayList<>();
    private List<String> requestedCompletionDate = new ArrayList<>();
    private List<String> dataSubmission = new ArrayList<>();
    private List<String> additionalAssemblyInformation = new ArrayList<>();
    private List<String> pooled = new ArrayList<>();
    private List<String> projectTitle = new ArrayList<>();
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
    private String genus;
    private String species;

    private List<String> barcodes = new ArrayList<>();

    public ExternalLibraryProcessorEzPass(String sheetName) {
        super(sheetName, TableProcessor.IgnoreTrailingBlankLines.YES);
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
                    city = preamble;
                    break;
                case 8:
                    state = preamble;
                    break;
                case 9:
                    zip = preamble;
                    break;
                case 10:
                    country = preamble;
                    break;
                case 11:
                    phone = preamble;
                    break;
                case 12:
                    email = preamble;
                    break;
                case 13:
                    break;
                case 14:
                    break;
                case 15:
                    break;
                case 16:
                    break;
                case 17:
                    genus = preamble;
                    break;
                case 18:
                    species = preamble;
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
                case 25:
                    break;
                case 26:
                    break;
                case 27:
                    break;
                default:
                    break;
            }

            index++;
        }

    }


    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {

        sampleNumber.add(dataRow.get(Headers.SAMPLE_NUMBER.getText()));
        barcodes.add(dataRow.get(Headers.TUBE_BARCODE.getText()));
        sourceSampleGSSRId.add(dataRow.get(Headers.SOURCE_SAMPLE_GSSR_ID.getText()));
        virtualGSSRId.add(dataRow.get(Headers.VIRTUAL_GSSR_ID.getText()));
        squidProject.add(dataRow.get(Headers.SQUID_PROJECT.getText()));
        sequencingTechnology.add(dataRow.get(Headers.SEQUENCING_TECHNOLOGY.getText()));
        individualName.add(dataRow.get(Headers.INDIVIDUAL_NAME.getText()));
        strain.add(dataRow.get(Headers.STRAIN.getText()));
        sex.add(dataRow.get(Headers.SEX.getText()));
        cellLine.add(dataRow.get(Headers.CELL_LINE.getText()));
        tissueType.add(dataRow.get(Headers.TISSUE_TYPE.getText()));
        collaboratorSampleId.add(dataRow.get(Headers.COLLABORATOR_SAMPLE_ID.getText()));
        singleSampleLibraryName.add(dataRow.get(Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        libraryType.add(dataRow.get(Headers.LIBRARY_TYPE.getText()));
        dataAnalysisType.add(dataRow.get(Headers.DATA_ANALYSIS_TYPE.getText()));
        referenceSequence.add(dataRow.get(Headers.REFERENCE_SEQUENCE.getText()));
        gssrOfBaitPool.add(dataRow.get(Headers.GSSR_OF_BAIT_POOL.getText()));
        insertSizeRangeBp.add(dataRow.get(Headers.INSERT_SIZE_RANGE_BP.getText()));
        librarySizeRangeBp.add(dataRow.get(Headers.LIBRARY_SIZE_RANGE_BP.getText()));
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
        fundingSource.add(dataRow.get(Headers.FUNDING_SOURCE.getText()));
        coverage.add(dataRow.get(Headers.COVERAGE.getText()));
        requestedCompletionDate.add(dataRow.get(Headers.REQUESTED_COMPLETION_DATE.getText()));
        dataSubmission.add(dataRow.get(Headers.DATA_SUBMISSION.getText()));
        additionalAssemblyInformation.add(dataRow.get(Headers.ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION.getText()));
        pooled.add(dataRow.get(Headers.POOLED.getText()));
        projectTitle.add(dataRow.get(Headers.PROJECT_TITLE.getText()));

    }

    protected ColumnHeader[] getColumnHeaders() {
        return VesselPooledTubesProcessor.Headers.values();
    }

    public void close() {
    }

    public enum Headers implements ColumnHeader {
        SAMPLE_NUMBER("Sample Number", ColumnHeader.OPTIONAL_HEADER, true),
        TUBE_BARCODE("Sample Tube Barcode", ColumnHeader.OPTIONAL_HEADER, true),
        SOURCE_SAMPLE_GSSR_ID("Source Sample GSSR ID", ColumnHeader.OPTIONAL_HEADER, true),
        VIRTUAL_GSSR_ID("Virtual GSSR ID", ColumnHeader.OPTIONAL_HEADER, true),
        SQUID_PROJECT("SQUID Project", ColumnHeader.OPTIONAL_HEADER, true),
        SEQUENCING_TECHNOLOGY("Sequencing Technology (Illumina/454/TechX Internal Other)", ColumnHeader.OPTIONAL_HEADER, true),
        STRAIN("Strain", ColumnHeader.OPTIONAL_HEADER, true),
        SEX("Sex (for non-human samples only)", ColumnHeader.OPTIONAL_HEADER, true),
        CELL_LINE("Cell Line", ColumnHeader.OPTIONAL_HEADER, true),
        TISSUE_TYPE("Tissue Type", ColumnHeader.OPTIONAL_HEADER, true),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", ColumnHeader.OPTIONAL_HEADER, true),
        INDIVIDUAL_NAME("Individual Name (aka Patient ID, Required for human subject samples)", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name (External Collaborator Library ID)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_TYPE("Library Type (see dropdown)", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_ANALYSIS_TYPE("Data Analysis Type (see dropdown)", ColumnHeader.OPTIONAL_HEADER, true),
        REFERENCE_SEQUENCE("Reference Sequence", ColumnHeader.OPTIONAL_HEADER, true),
        GSSR_OF_BAIT_POOL("GSSR # of Bait Pool (If submitting a hybrid selection library, please provide GSSR of bait pool used in experiment in order to properly run Hybrid Selection pipeline analyses)", ColumnHeader.OPTIONAL_HEADER, true),
        INSERT_SIZE_RANGE_BP("Insert Size Range bp. (i.e the library size without adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        LIBRARY_SIZE_RANGE_BP("Library Size Range bp. (i.e. the insert size plus adapters)", ColumnHeader.OPTIONAL_HEADER, true),
        JUMP_SIZE("Jump Size (kb) if applicable", ColumnHeader.OPTIONAL_HEADER, true),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used", ColumnHeader.OPTIONAL_HEADER, true),
        RESTRICTION_ENZYMES("Restriction Enzyme if applicable", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_PLATE_ID("Molecular barcode Plate ID", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_PLATE_WELL_ID("Molecular barcode Plate well ID", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_SEQUENCE("Molecular Barcode Sequence", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_BARCODE_NAME("Molecular Barcode Name", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_VOLUME("Total Library Volume (ul)", ColumnHeader.OPTIONAL_HEADER, true),
        TOTAL_LIBRARY_CONCENTRATION("Total Library Concentration (ng/ul)", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information", ColumnHeader.OPTIONAL_HEADER, true),
        FUNDING_SOURCE("Funding Source", ColumnHeader.OPTIONAL_HEADER, true),
        COVERAGE("Coverage (# Lanes/Sample)", ColumnHeader.OPTIONAL_HEADER, true),
        APPROVED_BY("Approved By", ColumnHeader.OPTIONAL_HEADER, true),
        REQUESTED_COMPLETION_DATE("Requested Completion Date", ColumnHeader.OPTIONAL_HEADER, true),
        DATA_SUBMISSION("Data Submission (Yes, Yes Later, or No)", ColumnHeader.OPTIONAL_HEADER, true),
        ADDITIONAL_ASSEMBLY_AND_ANALYSIS_INFORMATION("Additional Assembly and Analysis Information", ColumnHeader.OPTIONAL_HEADER, true),
        POOLED("Pooled", ColumnHeader.OPTIONAL_HEADER, true),
        PROJECT_TITLE("Project Title Description (e.g. MG1655 Jumping Library Dev.)", ColumnHeader.OPTIONAL_HEADER, true);
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

    public List<String> getPooled() {
        return pooled;
    }

    public List<String> getProjectTitle() {
        return projectTitle;
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

    public List<String> getTotalLibraryConcentration() {
        return totalLibraryConcentration;
    }

    public List<String> getTotalLibraryVolume() {
        return totalLibraryVolume;
    }

    public List<String> getMolecularBarcodeSequence() {
        return molecularBarcodeSequence;
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

    public List<String> getSquidProject() {
        return squidProject;
    }

      public List<String> getVirtualGSSRId() {
        return virtualGSSRId;
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

    public String getGenus() {
        return genus;
    }

    public String getSpecies() {
        return species;
    }

    public List<String> getSourceSampleGSSRId() {
        return sourceSampleGSSRId;
    }

}
