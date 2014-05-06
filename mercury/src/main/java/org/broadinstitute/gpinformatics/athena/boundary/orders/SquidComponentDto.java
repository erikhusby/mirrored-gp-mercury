package org.broadinstitute.gpinformatics.athena.boundary.orders;

import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SquidComponentDto implements Serializable {

    private String initiative;
    private String projectType;
    private String fundingSource;

    private String workRequestType;
    private String analysisType;
    private String referenceSequence;
    private String pairedSequence;

    private String executionType;

    public SquidComponentDto() {
    }


    public String getInitiative() {
        return initiative;
    }

    public void setInitiative(String initiative) {
        this.initiative = initiative;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public String getWorkRequestType() {
        return workRequestType;
    }

    public void setWorkRequestType(String workRequestType) {
        this.workRequestType = workRequestType;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getReferenceSequence() {
        return referenceSequence;
    }

    public void setReferenceSequence(String referenceSequence) {
        this.referenceSequence = referenceSequence;
    }

    public String getPairedSequence() {
        return pairedSequence;
    }

    public void setPairedSequence(String pairedSequence) {
        this.pairedSequence = pairedSequence;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }
}
