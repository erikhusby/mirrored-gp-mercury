package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Represents the data from a walkup sequencing submission.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalkUpSequencing implements Serializable, SampleKitRequest.SampleKitRequestKey{
    private String emailAddress;
    private String submitDate;
    private String tubeBarcode;
    private String libraryName;
    private String labName;
    private String quote;
    private String pooledSample;
    private String readType;
    private String illuminaTech;
    private String reference;
    private String referenceVersion;
    private String fragmentSize;
    private String volume;
    private String concentration;
    private String concentrationUnit;
    private String readLength;
    private String readLength2;
    private String laneQuantity;
    private String comments;
    private String analysisType;
    private String baitSetName;

    public void setEmailAddress(String value) {
        this.emailAddress = value;
    }

    public String getEmailAddress() {
        return this.emailAddress;
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

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getPooledSample() {
        return pooledSample;
    }

    public void setPooledSample(String pooledSample) {
        this.pooledSample = pooledSample;
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

    public String getReadLength() {
        return readLength;
    }

    public void setReadLength(String readLength) {
        this.readLength = readLength;
    }

    public String getReadLength2() {
        return readLength2;
    }

    public void setReadLength2(String readLength2) {
        this.readLength2 = readLength2;
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

    public String getBaitSetName() {
        return baitSetName;
    }

    public void setBaitSetName(String baitSetName) {
        this.baitSetName = baitSetName;
    }

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    @Override
    public String getFirstName() {
        return null;
    }

    @Override
    public String getLastName() {
        return null;
    }

    @Override
    public String getOrganization() {
        return null;
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public String getCity() {
        return null;
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getPostalCode() {
        return null;
    }

    @Override
    public String getCountry() {
        return null;
    }

    @Override
    public String getPhone() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public String getCommonName() {
        return null;
    }

    @Override
    public String getGenus() {
        return null;
    }

    @Override
    public String getSpecies() {
        return null;
    }

    @Override
    public String getIrbApprovalRequired() {
        return null;
    }
}
