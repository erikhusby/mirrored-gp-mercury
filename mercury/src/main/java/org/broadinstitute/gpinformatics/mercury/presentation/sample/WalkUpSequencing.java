package org.broadinstitute.gpinformatics.mercury.presentation.sample;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;

import java.io.Serializable;

/**
 * Represents the data from a walkup sequencing submission.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalkUpSequencing implements Serializable {
    private String submitDate;
    private String tubeBarcode;
    private String libraryName;
    private String labName;
    private String pooledSample;
    private String readType;
    private String isDualBarcoded;
    private String illuminaTech;
    private String reference;
    private String referenceVersion;
    private String fragmentSize;
    private String volume;
    private String concentration;
    private String concentrationUnit;
    private String indexLength;
    private String indexLength2;
    private String readLength;
    private String readLength2;
    private String laneQuantity;
    private String comments;
    private String analysisType;
    private String baitSetName;

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

    public String getReadLength1() {
        return readLength;
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

    public String getIndexLength1() {
        return indexLength;
    }

    public String getIndexLength() {
        return indexLength;
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

    public String getIsDualBarcoded() {
        return isDualBarcoded;
    }

    public void setIsDualBarcoded(String isDualBarcoded) {
        this.isDualBarcoded = isDualBarcoded;
    }

    public FlowcellDesignation.IndexType getIndexType() {
        return ExternalLibraryProcessor.isOneOf(isDualBarcoded, "no", "false") ? FlowcellDesignation.IndexType.SINGLE :
                ExternalLibraryProcessor.isOneOf(isDualBarcoded, "yes", "true") ? FlowcellDesignation.IndexType.DUAL :
                        FlowcellDesignation.IndexType.NONE;
    }

    public boolean isPairedEndRead() {
        return StringUtils.startsWithIgnoreCase(readType, "p");
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
}
