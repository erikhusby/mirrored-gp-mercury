package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.ArrayList;
import java.util.List;


public class ExternalLibraryMapped {

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
    private String commonName;
    private String genus;
    private String species;
    private String irbRequired;

    public void mapPooledMultiOrg(ExternalLibraryProcessorPooledMultiOrganism spreadsheet) {

        setHeaders(spreadsheet.getHeaderNames());
        setSequencingTechnology(spreadsheet.getSequencingTechnology());
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
        setCellLine(spreadsheet.getCellLine());
        setTissueType(spreadsheet.getTissueType());
        setTissueType(spreadsheet.getTissueType());
        setCollaboratorSampleId(spreadsheet.getCollaboratorSampleId());
        setIndividualName(spreadsheet.getIndividualName());
        setSingleSampleLibraryName(spreadsheet.getSingleSampleLibraryName());
        setLibraryType(spreadsheet.getLibraryType());
        setDataAnalysisType(spreadsheet.getDataAnalysisType());
        setReferenceSequence(spreadsheet.getReferenceSequence());
        setInsertSizeRangeBp(spreadsheet.getInsertSizeRangeBp());
        setLibrarySizeRangeBp(spreadsheet.getLibrarySizeRangeBp());
        setJumpSize(spreadsheet.getJumpSize());
        setIlluminaKitUsed(spreadsheet.getIlluminaKitUsed());
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setMolecularBarcodeName(spreadsheet.getMolecularBarcodeName());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setSingleDoubleStranded(spreadsheet.getSingleDoubleStranded());
        setAdditionalSampleInformation(spreadsheet.getAdditionalSampleInformation());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setApprovedBy(spreadsheet.getApprovedBy());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setAdditionalAssemblyInformation(spreadsheet.getAdditionalAssemblyInformation());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setRequiredControlledAccess(spreadsheet.getRequiredControlledAccess());
        setAccessList(spreadsheet.getAccessList());
        setPooled(spreadsheet.getPooled());
        setSubmittedToGSSR(spreadsheet.getSubmittedToGSSR());
        setDerivedFrom(spreadsheet.getDerivedFrom());
        setOrganism(spreadsheet.getOrganism());
        setCollaboratorName(spreadsheet.getCollaboratorName());
        setFirstName(spreadsheet.getFirstName());
        setLastName(spreadsheet.getLastName());
        setOrganization(spreadsheet.getOrganization());
        setAddress(spreadsheet.getAddress());
        setCity(spreadsheet.getCity());
        setState(spreadsheet.getState());
        setZip(spreadsheet.getZip());
        setCountry(spreadsheet.getCountry());
        setPhone(spreadsheet.getPhone());
        setEmail(spreadsheet.getEmail());
        setCommonName(null);
        setGenus(null);
        setSpecies(null);
        setIrbRequired(null);

    }

    public void mapPooled(ExternalLibraryProcessorPooled spreadsheet) {

        setHeaders(spreadsheet.getHeaderNames());
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
        setCellLine(spreadsheet.getCellLine());
        setTissueType(spreadsheet.getTissueType());
        setCollaboratorSampleId(spreadsheet.getCollaboratorSampleId());
        setIndividualName(spreadsheet.getIndividualName());
        setSingleSampleLibraryName(spreadsheet.getSingleSampleLibraryName());
        setLibraryType(spreadsheet.getLibraryType());
        setDataAnalysisType(spreadsheet.getDataAnalysisType());
        setReferenceSequence(spreadsheet.getReferenceSequence());
        setInsertSizeRangeBp(spreadsheet.getInsertSizeRangeBp());
        setLibrarySizeRangeBp(spreadsheet.getLibrarySizeRangeBp());
        setJumpSize(spreadsheet.getJumpSize());
        setIlluminaKitUsed(spreadsheet.getIlluminaKitUsed());
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setMolecularBarcodeName(spreadsheet.getMolecularBarcodeName());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setSingleDoubleStranded(spreadsheet.getSingleDoubleStranded());
        setAdditionalSampleInformation(spreadsheet.getAdditionalSampleInformation());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setApprovedBy(spreadsheet.getApprovedBy());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setAdditionalAssemblyInformation(spreadsheet.getAdditionalAssemblyInformation());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setRequiredControlledAccess(spreadsheet.getRequiredControlledAccess());
        setPooled(spreadsheet.getPooled());
        setSubmittedToGSSR(spreadsheet.getSubmittedToGSSR());
        setDerivedFrom(spreadsheet.getDerivedFrom());
        setCollaboratorName(spreadsheet.getCollaboratorName());
        setFirstName(spreadsheet.getFirstName());
        setLastName(spreadsheet.getLastName());
        setOrganization(spreadsheet.getOrganization());
        setAddress(spreadsheet.getAddress());
        setCity(spreadsheet.getCity());
        setState(spreadsheet.getState());
        setZip(spreadsheet.getZip());
        setCountry(spreadsheet.getCountry());
        setPhone(spreadsheet.getPhone());
        setEmail(spreadsheet.getEmail());
        setCommonName(spreadsheet.getCommonName());
        setGenus(spreadsheet.getGenus());
        setSpecies(spreadsheet.getSpecies());
        setIrbRequired(spreadsheet.getIrbRequired());

    }

    public void mapNonPooled(ExternalLibraryProcessorNonPooled spreadsheet) {

        setHeaders(spreadsheet.getHeaderNames());
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
        setCellLine(spreadsheet.getCellLine());
        setTissueType(spreadsheet.getTissueType());
        setCollaboratorSampleId(spreadsheet.getCollaboratorSampleId());
        setIndividualName(spreadsheet.getIndividualName());
        setSingleSampleLibraryName(spreadsheet.getSingleSampleLibraryName());
        setLibraryType(spreadsheet.getLibraryType());
        setDataAnalysisType(spreadsheet.getDataAnalysisType());
        setReferenceSequence(spreadsheet.getReferenceSequence());
        setInsertSizeRangeBp(spreadsheet.getInsertSizeRangeBp());
        setLibrarySizeRangeBp(spreadsheet.getLibrarySizeRangeBp());
        setJumpSize(spreadsheet.getJumpSize());
        setIlluminaKitUsed(spreadsheet.getIlluminaKitUsed());
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setMolecularBarcodeName(spreadsheet.getMolecularBarcodeName());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setSingleDoubleStranded(spreadsheet.getSingleDoubleStranded());
        setAdditionalSampleInformation(spreadsheet.getAdditionalSampleInformation());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setApprovedBy(spreadsheet.getApprovedBy());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setAdditionalAssemblyInformation(spreadsheet.getAdditionalAssemblyInformation());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setRequiredControlledAccess(spreadsheet.getRequiredControlledAccess());
        setPooled(spreadsheet.getPooled());
        setCollaboratorName(spreadsheet.getCollaboratorName());
        setFirstName(spreadsheet.getFirstName());
        setLastName(spreadsheet.getLastName());
        setOrganization(spreadsheet.getOrganization());
        setAddress(spreadsheet.getAddress());
        setCity(spreadsheet.getCity());
        setState(spreadsheet.getState());
        setZip(spreadsheet.getZip());
        setCountry(spreadsheet.getCountry());
        setPhone(spreadsheet.getPhone());
        setEmail(spreadsheet.getEmail());
        setCommonName(spreadsheet.getCommonName());
        setGenus(spreadsheet.getGenus());
        setSpecies(spreadsheet.getSpecies());
        setIrbRequired(spreadsheet.getIrbRequired());

    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getSequencingTechnology() {
        return sequencingTechnology;
    }

    public void setSequencingTechnology(List<String> sequencingTechnology) {
        this.sequencingTechnology = sequencingTechnology;
    }

    public List<String> getStrain() {
        return strain;
    }

    public void setStrain(List<String> strain) {
        this.strain = strain;
    }

    public List<String> getSex() {
        return sex;
    }

    public void setSex(List<String> sex) {
        this.sex = sex;
    }

    public List<String> getCellLine() {
        return cellLine;
    }

    public void setCellLine(List<String> cellLine) {
        this.cellLine = cellLine;
    }

    public List<String> getTissueType() {
        return tissueType;
    }

    public void setTissueType(List<String> tissueType) {
        this.tissueType = tissueType;
    }

    public List<String> getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public void setCollaboratorSampleId(List<String> collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public List<String> getIndividualName() {
        return individualName;
    }

    public void setIndividualName(List<String> individualName) {
        this.individualName = individualName;
    }

    public List<String> getSingleSampleLibraryName() {
        return singleSampleLibraryName;
    }

    public void setSingleSampleLibraryName(List<String> singleSampleLibraryName) {
        this.singleSampleLibraryName = singleSampleLibraryName;
    }

    public List<String> getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(List<String> libraryType) {
        this.libraryType = libraryType;
    }

    public List<String> getDataAnalysisType() {
        return dataAnalysisType;
    }

    public void setDataAnalysisType(List<String> dataAnalysisType) {
        this.dataAnalysisType = dataAnalysisType;
    }

    public List<String> getReferenceSequence() {
        return referenceSequence;
    }

    public void setReferenceSequence(List<String> referenceSequence) {
        this.referenceSequence = referenceSequence;
    }

    public List<String> getInsertSizeRangeBp() {
        return insertSizeRangeBp;
    }

    public void setInsertSizeRangeBp(List<String> insertSizeRangeBp) {
        this.insertSizeRangeBp = insertSizeRangeBp;
    }

    public List<String> getLibrarySizeRangeBp() {
        return librarySizeRangeBp;
    }

    public void setLibrarySizeRangeBp(List<String> librarySizeRangeBp) {
        this.librarySizeRangeBp = librarySizeRangeBp;
    }

    public List<String> getJumpSize() {
        return jumpSize;
    }

    public void setJumpSize(List<String> jumpSize) {
        this.jumpSize = jumpSize;
    }

    public List<String> getIlluminaKitUsed() {
        return illuminaKitUsed;
    }

    public void setIlluminaKitUsed(List<String> illuminaKitUsed) {
        this.illuminaKitUsed = illuminaKitUsed;
    }

    public List<String> getRestrictionEnzymes() {
        return restrictionEnzymes;
    }

    public void setRestrictionEnzymes(List<String> restrictionEnzymes) {
        this.restrictionEnzymes = restrictionEnzymes;
    }

    public List<String> getMolecularBarcodeSequence() {
        return molecularBarcodeSequence;
    }

    public void setMolecularBarcodeSequence(List<String> molecularBarcodeSequence) {
        this.molecularBarcodeSequence = molecularBarcodeSequence;
    }

    public List<String> getMolecularBarcodeName() {
        return molecularBarcodeName;
    }

    public void setMolecularBarcodeName(List<String> molecularBarcodeName) {
        this.molecularBarcodeName = molecularBarcodeName;
    }

    public List<String> getTotalLibraryVolume() {
        return totalLibraryVolume;
    }

    public void setTotalLibraryVolume(List<String> totalLibraryVolume) {
        this.totalLibraryVolume = totalLibraryVolume;
    }

    public List<String> getTotalLibraryConcentration() {
        return totalLibraryConcentration;
    }

    public void setTotalLibraryConcentration(List<String> totalLibraryConcentration) {
        this.totalLibraryConcentration = totalLibraryConcentration;
    }

    public List<String> getSingleDoubleStranded() {
        return singleDoubleStranded;
    }

    public void setSingleDoubleStranded(List<String> singleDoubleStranded) {
        this.singleDoubleStranded = singleDoubleStranded;
    }

    public List<String> getAdditionalSampleInformation() {
        return additionalSampleInformation;
    }

    public void setAdditionalSampleInformation(List<String> additionalSampleInformation) {
        this.additionalSampleInformation = additionalSampleInformation;
    }

    public List<String> getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(List<String> fundingSource) {
        this.fundingSource = fundingSource;
    }

    public List<String> getCoverage() {
        return coverage;
    }

    public void setCoverage(List<String> coverage) {
        this.coverage = coverage;
    }

    public List<String> getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(List<String> approvedBy) {
        this.approvedBy = approvedBy;
    }

    public List<String> getRequestedCompletionDate() {
        return requestedCompletionDate;
    }

    public void setRequestedCompletionDate(List<String> requestedCompletionDate) {
        this.requestedCompletionDate = requestedCompletionDate;
    }

    public List<String> getDataSubmission() {
        return dataSubmission;
    }

    public void setDataSubmission(List<String> dataSubmission) {
        this.dataSubmission = dataSubmission;
    }

    public List<String> getAdditionalAssemblyInformation() {
        return additionalAssemblyInformation;
    }

    public void setAdditionalAssemblyInformation(List<String> additionalAssemblyInformation) {
        this.additionalAssemblyInformation = additionalAssemblyInformation;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public void setBarcodes(List<String> barcodes) {
        this.barcodes = barcodes;
    }

    public List<String> getIrbNumber() {
        return irbNumber;
    }

    public void setIrbNumber(List<String> irbNumber) {
        this.irbNumber = irbNumber;
    }

    public List<String> getDesiredReadLength() {
        return desiredReadLength;
    }

    public void setDesiredReadLength(List<String> desiredReadLength) {
        this.desiredReadLength = desiredReadLength;
    }

    public List<String> getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(List<String> projectTitle) {
        this.projectTitle = projectTitle;
    }

    public List<String> getRequiredControlledAccess() {
        return requiredControlledAccess;
    }

    public void setRequiredControlledAccess(List<String> requiredControlledAccess) {
        this.requiredControlledAccess = requiredControlledAccess;
    }

    public List<String> getAccessList() {
        return accessList;
    }

    public void setAccessList(List<String> accessList) {
        this.accessList = accessList;
    }

    public List<String> getPooled() {
        return pooled;
    }

    public void setPooled(List<String> pooled) {
        this.pooled = pooled;
    }

    public List<String> getMemeberOfPool() {
        return memeberOfPool;
    }

    public void setMemeberOfPool(List<String> memeberOfPool) {
        this.memeberOfPool = memeberOfPool;
    }

    public List<String> getSubmittedToGSSR() {
        return submittedToGSSR;
    }

    public void setSubmittedToGSSR(List<String> submittedToGSSR) {
        this.submittedToGSSR = submittedToGSSR;
    }

    public List<String> getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(List<String> derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public List<String> getOrganism() {
        return organism;
    }

    public void setOrganism(List<String> organism) {
        this.organism = organism;
    }

    public List<String> getRowCount() {
        return rowCount;
    }

    public void setRowCount(List<String> rowCount) {
        this.rowCount = rowCount;
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public void setCollaboratorName(String collaboratorName) {
        this.collaboratorName = collaboratorName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
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

    public void setIrbRequired(String irbRequired) {
        this.irbRequired = irbRequired;
    }

    public enum Headers {
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

        public String getText() {
            if (text == null) {
                return null;
            } else {
                return text.trim();
            }
        }
    }
}
