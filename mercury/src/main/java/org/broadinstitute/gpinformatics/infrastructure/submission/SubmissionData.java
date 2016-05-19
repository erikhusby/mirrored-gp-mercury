/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class SubmissionData implements Serializable {
    private static final long serialVersionUID = -5693294276238948886L;
    @JsonProperty(value = SubmissionDto.SubmissionField.UUID)
    private String uuid;
    @JsonProperty(value = SubmissionDto.SubmissionField.SAMPLE_NAME)
    private String sampleName;
    @JsonProperty(value = SubmissionDto.SubmissionField.DATA_TYPE)
    private String dataType;
    @JsonProperty(value = SubmissionDto.SubmissionField.PRODUCT_ORDERS)
    private String[] productOrdersString;
    @JsonProperty(value = SubmissionDto.SubmissionField.AGGREGATION_PROJECT)
    private String aggregationProject;
    @JsonProperty(value = SubmissionDto.SubmissionField.FILE_TYPE)
    private String fileTypeString;
    @JsonProperty(value = SubmissionDto.SubmissionField.VERSION)
    private Integer version;
    @JsonProperty(value = SubmissionDto.SubmissionField.QUALITY_METRIC)
    private String qualityMetricString;
    @JsonProperty(value = SubmissionDto.SubmissionField.CONTAMINATION_STRING)
    private String contamination;
    @JsonProperty(value = SubmissionDto.SubmissionField.FINGERPRINT_LOD)
    private String fingerprintLODString;
    @JsonProperty(value = SubmissionDto.SubmissionField.RESEARCH_PROJECT)
    private String researchProject;
    @JsonProperty(value = SubmissionDto.SubmissionField.LANES_IN_AGGREGATION)
    private Integer lanesInAggregation;
    @JsonProperty(value = SubmissionDto.SubmissionField.FILE_NAME)
    private String fileName;
    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_VERSION)
    private Integer submittedVersion;
    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_ERRORS)
    private String[] submittedErrors;
    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_STATUS)
    private String submittedStatus;
    @JsonProperty(value = SubmissionDto.SubmissionField.STATUS_DATE)
    private String statusDate;
    @JsonProperty(value = SubmissionDto.SubmissionField.BIO_PROJECT)
    private String bioProject;
    @JsonProperty(value = SubmissionDto.SubmissionField.LIBRARY_DESCRIPTOR)
    private String submissionLibraryDescriptor;
    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMISSION_SITE)
    private String submissionSite;

    public SubmissionData() {
    }

    public SubmissionData(SubmissionDto submissionDto) {
        setUuid(submissionDto.getUuid());
        setSampleName(submissionDto.getSampleName());
        setDataType(submissionDto.getDataType());
        setResearchProject(submissionDto.getResearchProject());
        setAggregationProject(submissionDto.getAggregationProject());
        setFileName(submissionDto.getFileName());
        setFileTypeString(submissionDto.getFileTypeString());
        setVersion(submissionDto.getVersion());
        setQualityMetricString(submissionDto.getQualityMetricString());
        setContamination(submissionDto.getContaminationString());
        setFingerprintLODString(submissionDto.getFingerprintLODString());
        setLanesInAggregation(submissionDto.getLanesInAggregation());
        setSubmittedVersion(submissionDto.getSubmittedVersion());
        setSubmittedStatus(submissionDto.getSubmittedStatus());
        setBioProject(submissionDto.getBioProject());
        Collection<String> productOrders = submissionDto.getProductOrdersString();
        setProductOrdersString(productOrders.toArray(new String[productOrders.size()]));
        List<String> submittedErrors = submissionDto.getSubmittedErrors();
        setSubmittedErrors(submittedErrors.toArray(new String[submittedErrors.size()]));
        setStatusDate(submissionDto.getStatusDate());
        setSubmissionSite(submissionDto.getSubmissionRepositoryName());
        setSubmissionLibraryDescriptor(submissionDto.getSubmissionLibraryDescriptor());
    }

    public String getAggregationProject() {
        return aggregationProject;
    }

    public void setAggregationProject(String aggregationProject) {
        this.aggregationProject = aggregationProject;
    }

    public String getBioProject() {
        return bioProject;
    }

    public void setBioProject(String bioProject) {
        this.bioProject = bioProject;
    }

    public String getContamination() {
        return contamination;
    }

    public void setContamination(String contamination) {
        this.contamination = contamination;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileTypeString() {
        return fileTypeString;
    }

    public void setFileTypeString(String fileTypeString) {
        this.fileTypeString = fileTypeString;
    }

    public String getFingerprintLODString() {
        return fingerprintLODString;
    }

    public void setFingerprintLODString(String fingerprintLODString) {
        this.fingerprintLODString = fingerprintLODString;
    }

    public Integer getLanesInAggregation() {
        return lanesInAggregation;
    }

    public void setLanesInAggregation(Integer lanesInAggregation) {
        this.lanesInAggregation = lanesInAggregation;
    }

    public String[] getProductOrdersString() {
        return productOrdersString;
    }

    public void setProductOrdersString(String[] productOrdersString) {
        this.productOrdersString = productOrdersString;
    }

    public String getQualityMetricString() {
        return qualityMetricString;
    }

    public void setQualityMetricString(String qualityMetricString) {
        this.qualityMetricString = qualityMetricString;
    }

    public String getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(String researchProject) {
        this.researchProject = researchProject;
    }

    public String getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(String statusDate) {
        this.statusDate = statusDate;
    }

    public String getSubmissionLibraryDescriptor() {
        return submissionLibraryDescriptor;
    }

    public void setSubmissionLibraryDescriptor(String submissionLibraryDescriptor) {
        this.submissionLibraryDescriptor = submissionLibraryDescriptor;
    }

    public String getSubmissionSite() {
        return submissionSite;
    }

    public void setSubmissionSite(String submissionSite) {
        this.submissionSite = submissionSite;
    }

    public String[] getSubmittedErrors() {
        return submittedErrors;
    }

    public void setSubmittedErrors(String[] submittedErrors) {
        this.submittedErrors = submittedErrors;
    }

    public String getSubmittedStatus() {
        return submittedStatus;
    }

    public void setSubmittedStatus(String submittedStatus) {
        this.submittedStatus = submittedStatus;
    }

    public Integer getSubmittedVersion() {
        return submittedVersion;
    }

    public void setSubmittedVersion(Integer submittedVersion) {
        this.submittedVersion = submittedVersion;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void updateStatusDetail(@Nonnull SubmissionStatusDetailBean statusDetail) {
        setUuid(statusDetail.getUuid());
        setBioProject(SubmissionDto.formatBioProject(statusDetail.getBioproject()));
        setSubmittedErrors(statusDetail.getErrors().toArray(new String[statusDetail.getErrors().size()]));
        if (statusDetail.getLastStatusUpdate() != null) {
            setStatusDate(SubmissionDto.DATE_FORMAT.format(statusDetail.getLastStatusUpdate()));
        }
        setSubmissionSite(statusDetail.getSite());
        setSubmittedStatus(statusDetail.getStatus());

        String submittedVersion = statusDetail.getSubmittedVersion();
        if (StringUtils.isNotBlank(submittedVersion)) {
            setSubmittedVersion(Integer.parseInt(submittedVersion));
        }
        setSubmissionLibraryDescriptor(statusDetail.getSubmissiondatatype());
    }
}
