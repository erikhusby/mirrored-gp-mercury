package org.broadinstitute.gpinformatics.mercury.control.sample;

import java.util.ArrayList;
import java.util.List;


public class ExternalLibraryMapped {

    private List<String> headers = new ArrayList<>();
    private List<String> strain = new ArrayList<>();
    private List<String> sex = new ArrayList<>();
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
    private List<String> restrictionEnzymes = new ArrayList<>();
    private List<String> molecularBarcodeSequence = new ArrayList<>();
    private List<String> totalLibraryVolume = new ArrayList<>();
    private List<String> totalLibraryConcentration = new ArrayList<>();
    private List<String> fundingSource = new ArrayList<>();
    private List<String> coverage = new ArrayList<>();
    private List<String> requestedCompletionDate = new ArrayList<>();
    private List<String> dataSubmission = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> irbNumber = new ArrayList<>();
    private List<String> desiredReadLength = new ArrayList<>();
    private List<String> projectTitle = new ArrayList<>();
    private List<String> pooled = new ArrayList<>();
    private List<String> organism = new ArrayList<>();
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
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
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
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setPooled(spreadsheet.getPooled());
        setOrganism(spreadsheet.getOrganism());
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
        setCollaboratorName(spreadsheet.getCollaboratorName());
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
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
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setPooled(spreadsheet.getPooled());
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

        setCollaboratorName(spreadsheet.getCollaboratorName());
        setHeaders(spreadsheet.getHeaderNames());
        setStrain(spreadsheet.getStrain());
        setSex(spreadsheet.getSex());
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
        setRestrictionEnzymes(spreadsheet.getRestrictionEnzymes());
        setMolecularBarcodeSequence(spreadsheet.getMolecularBarcodeSequence());
        setTotalLibraryVolume(spreadsheet.getTotalLibraryVolume());
        setTotalLibraryConcentration(spreadsheet.getTotalLibraryConcentration());
        setFundingSource(spreadsheet.getFundingSource());
        setCoverage(spreadsheet.getCoverage());
        setRequestedCompletionDate(spreadsheet.getRequestedCompletionDate());
        setDataSubmission(spreadsheet.getDataSubmission());
        setBarcodes(spreadsheet.getBarcodes());
        setIrbNumber(spreadsheet.getIrbNumber());
        setDesiredReadLength(spreadsheet.getDesiredReadLength());
        setProjectTitle(spreadsheet.getProjectTitle());
        setPooled(spreadsheet.getPooled());
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

    public List<String> getPooled() {
        return pooled;
    }

    public void setPooled(List<String> pooled) {
        this.pooled = pooled;
    }

    public List<String> getOrganism() {
        return organism;
    }

    public void setOrganism(List<String> organism) {
        this.organism = organism;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public void setCollaboratorName(String collaboratorName) {
        this.collaboratorName = collaboratorName;
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
        ROW_COUNT(null),
        IRB_NUMBER("IRB Number"),
        SAMPLE_NUMBER("Sample Number"),
        TUBE_BARCODE("Tube barcode"),
        SEQUENCING_TECHNOLOGY("Sequencing Technology (Illumina/ 454)"),
        STRAIN("Strain"),
        SEX("Sex"),
        CELL_LINE("Cell Line"),
        TISSUE_TYPE("Tissue Type"),
        COLLABORATOR_SAMPLE_ID("Sample Collaborator ID (Biological Sample ID)"),
        INDIVIDUAL_NAME("Individual Name"),
        SINGLE_SAMPLE_LIBRARY_NAME("Library Name (External Collaborator Library ID)"),
        LIBRARY_TYPE("Library Type (see drop down)"),
        DATA_ANALYSIS_TYPE("Data Analysis Type"),
        REFERENCE_SEQUENCE("Reference Sequence"),
        INSERT_SIZE_RANGE_BP("Insert Size Range (in bp. without adapters)"),
        LIBRARY_SIZE_RANGE_BP("Library Size Range (in bp. with adapters)"),
        JUMP_SIZE("Jump Size (kb)"),
        ILLUMINA_KIT_USED("Illumina or 454 Kit Used  (see dropdown)"),
        RESTRICTION_ENZYMES("Restriction Enzyme (if applicable)"),
        MOLECULAR_BARCODE_SEQUENCE("Molecular barcode sequence"),
        MOLECULAR_BARCODE_NAME("Molecular barcode name"),
        TOTAL_LIBRARY_VOLUME("Sample Volume (ul)"),
        TOTAL_LIBRARY_CONCENTRATION("Sample Concentration (ng/ul)"),
        SINGLE_DOUBLE_STRANDED("Single/Double Stranded (S/D)"),
        ADDITIONAL_SAMPLE_INFORMATION("Additional Sample Information"),
        DESIRED_READ_LENGTH("Desired Read Length for Illumina and note specific cluster density, if required . (See above)"),
        FUNDING_SOURCE("Funding Source"),
        COVERAGE("Coverage (# Lanes/Sample)"),
        APPROVED_BY("Approved By"),
        REQUESTED_COMPLETION_DATE("Requested Completion Date"),
        DATA_SUBMISSION("Data Submission (Yes, Yes Later, or No)"),
        POOLED("Pooled (Y/N)"),
        ORGANISM("Organism");

        private String text = null;

        Headers(String text) {
            this.text = text;
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
