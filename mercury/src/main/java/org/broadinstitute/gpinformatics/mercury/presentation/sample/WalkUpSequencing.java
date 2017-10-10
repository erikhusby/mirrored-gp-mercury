package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import java.io.Serializable;


public class WalkUpSequencing implements Serializable{

    private String emailAddress;
    private String submitDate;
    private String tubeBarcode;
    private String libraryName;
    private String labName;
    private String projManager;
    private String projManagerEmail;
    private String quote;
    private String sampleLocation; //Sample location: 7CC or 320Charles',
    private Boolean isPooledSample;
    private String readType;
    private String illuminaTech;
    private String dataDelivery;
    private String reference;
    private String referenceVersion;
    private String fragmentSize;
    private String volume;
    private String concentration;
    private String concentrationUnit;
    private String isCustomPrimers;
    private String isPhix;
    private String phixPercentage;
    private String isDualBarcoded;
    private String isMonotemplate;
    private String readLength;
    private String readLength2;
    private String indexLength;
    private String indexLength2;
    private String laneQuantity;
    private String comments;
    private String analysisType;
    private String hybridAnalysisType;
    private String baitSetName;
    private String enzyme;
    private String fragSizeRange;
    private String status;
    private String regulatoryType;
    private String irbNumber;
    private String jiraIssueKey;
//    private Date createTime;
//    private Date updateTime;
//    private Date completeDate;
    private String flowcellLaneDesignated; //Lane Number range designated in a flowcell.
    private String flowcellDesignation;
    private String libraryConstructionMethod;
    private String quantificationMethod;




    public void setEmailAddress(String value) {
        this.emailAddress = value;
    }

    public String getEmailAddress() { return this.emailAddress;}

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public String getTubeBarcode() {
        return tubeBarcode;
    }

    public void setTubeBarcode(String tubeBarcode) {
        this.tubeBarcode = tubeBarcode;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getProjManager() {
        return projManager;
    }

    public void setProjManager(String projManager) {
        this.projManager = projManager;
    }

    public String getProjManagerEmail() {
        return projManagerEmail;
    }

    public void setProjManagerEmail(String projManagerEmail) {
        this.projManagerEmail = projManagerEmail;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getSampleLocation() {
        return sampleLocation;
    }

    public void setSampleLocation(String sampleLocation) {
        this.sampleLocation = sampleLocation;
    }

    public boolean getIsPooledSample() {  return isPooledSample;   }

    public void setPooledSample(Boolean pooledSample) {
        isPooledSample = pooledSample;
    }

    public String getReadType() {
        return readType;
    }

    public void setReadType(String readType) {
        this.readType = readType;
    }

    public String getIlluminaTech() {
        return illuminaTech;
    }

    public void setIlluminaTech(String illuminaTech) {
        this.illuminaTech = illuminaTech;
    }

    public String getDataDelivery() {
        return dataDelivery;
    }

    public void setDataDelivery(String dataDelivery) {
        this.dataDelivery = dataDelivery;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getFragmentSize() {
        return fragmentSize;
    }

    public void setFragmentSize(String fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getConcentration() {
        return concentration;
    }

    public void setConcentration(String concentration) {
        this.concentration = concentration;
    }

    public String getConcentrationUnit() {
        return concentrationUnit;
    }

    public void setConcentrationUnit(String concentrationUnit) {
        this.concentrationUnit = concentrationUnit;
    }

    public String getIsCustomPrimers() {
        return isCustomPrimers;
    }

    public void setIsCustomPrimers(String intisCustomPrimers) {
        this.isCustomPrimers = intisCustomPrimers;
    }

    public String getIsPhix() {
        return isPhix;
    }

    public void setIsPhix(String isPhix) {
        this.isPhix = isPhix;
    }

    public String getPhixPercentage() {
        return phixPercentage;
    }

    public void setPhixPercentage(String phixPercentage) {
        this.phixPercentage = phixPercentage;
    }

    public String getIsDualBarcoded() {
        return isDualBarcoded;
    }

    public void setIsDualBarcoded(String isDualBarcoded) {
        this.isDualBarcoded = isDualBarcoded;
    }

    public String getIsMonotemplate() {
        return isMonotemplate;
    }

    public void setIsMonotemplate(String isMonotemplate) {
        this.isMonotemplate = isMonotemplate;
    }

    public String getReadLength() {
        return readLength;
    }

    public void setReadLength(String readLength) {
        this.readLength = readLength;
    }

    public String getReadLength2() {
      return  readLength2;
    }

    public void setReadLength2(String readLength2) {
        this.readLength2 = readLength2;
    }

    public String getIndexLength() {
      return   indexLength;
    }

    public void setIndexLength(String indexLength) {
        this.indexLength = indexLength;
    }

    public String getIndexLength2() {
        return indexLength2;
    }

    public void setIndexLength2(String indexLength2) {
        this.indexLength2 = indexLength2;
    }

    public String getLaneQuantity() {
        return laneQuantity;
    }

    public void setLaneQuantity(String laneQuantity) {
        this.laneQuantity = laneQuantity;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getHybridAnalysisType() {
        return hybridAnalysisType;
    }

    public void setHybridAnalysisType(String hybridAnalysisType) {
        this.hybridAnalysisType = hybridAnalysisType;
    }

    public String getBaitSetName() {
        return baitSetName;
    }

    public void setBaitSetName(String baitSetName) {
        this.baitSetName = baitSetName;
    }

    public String getEnzyme() {
        return enzyme;
    }

    public void setEnzyme(String enzyme) {
        this.enzyme = enzyme;
    }

    public String getFragSizeRange() {
        return fragSizeRange;
    }

    public void setFragSizeRange(String fragSizeRange) {
        this.fragSizeRange = fragSizeRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegulatoryType() {
        return regulatoryType;
    }

    public void setRegulatoryType(String regulatoryType) {
        this.regulatoryType = regulatoryType;
    }

    public String getIrbNumber() {
        return irbNumber;
    }

    public void setIrbNumber(String irbNumber) {
        this.irbNumber = irbNumber;
    }

    public String getJiraIssueKey() {
        return jiraIssueKey;
    }

    public void setJiraIssueKey(String jiraIssueKey) {
        this.jiraIssueKey = jiraIssueKey;
    }

    public String getFlowcellLaneDesignated() {
        return flowcellLaneDesignated;
    }

    public void setFlowcellLaneDesignated(String flowcellLaneDesignated) {
        this.flowcellLaneDesignated = flowcellLaneDesignated;
    }

    public String getFlowcellDesignation() {
        return flowcellDesignation;
    }

    public void setFlowcellDesignation(String flowcellDesignation) {
        this.flowcellDesignation = flowcellDesignation;
    }

    public String getLibraryConstructionMethod() {
        return libraryConstructionMethod;
    }

    public void setLibraryConstructionMethod(String libraryConstructionMethod) {
        this.libraryConstructionMethod = libraryConstructionMethod;
    }

    public String getQuantificationMethod() {
        return quantificationMethod;
    }

    public void setQuantificationMethod(String quantificationMethod) {
        quantificationMethod = quantificationMethod;
    }



}
