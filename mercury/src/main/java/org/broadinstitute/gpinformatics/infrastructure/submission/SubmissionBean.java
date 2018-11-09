
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionBean implements Serializable {
    private static final long serialVersionUID = 5575909517269494566L;
    @JsonProperty
    private String uuid;
    @JsonProperty
    private String studyContact;
    @JsonProperty
    private BioProject bioProject;
    @JsonProperty
    private SubmissionBioSampleBean submissionSample;
    @JsonProperty(value= "site")
    private String submissionRepository;
    @JsonProperty(value = "submissionDatatype")
    private String submissionDatatype;
    @JsonProperty(value = "broadProject")
    private String aggregationProject;
    @JsonProperty
    private String bamVersion;

    public SubmissionBean() {
    }

    public SubmissionBean(String uuid, String studyContact, BioProject bioProject, SubmissionBioSampleBean submissionSample,
                          SubmissionRepository submissionRepository,
                          SubmissionLibraryDescriptor submissionLibraryDescriptor, String aggregationProject,
                          String bamVersion) {
        this.uuid = uuid;
        this.studyContact = studyContact;
        this.bioProject = bioProject;
        this.submissionSample = submissionSample;
        this.submissionRepository = submissionRepository.getName();
        this.submissionDatatype = submissionLibraryDescriptor.getName();
        this.aggregationProject = aggregationProject;
        this.bamVersion = bamVersion;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStudyContact() {
        return studyContact;
    }

    public void setStudyContact(String studyContact) {
        this.studyContact = studyContact;
    }

    public BioProject getBioProject() {
        return bioProject;
    }

    public void setBioProject(BioProject bioProject) {
        this.bioProject = bioProject;
    }

    public SubmissionBioSampleBean getSubmissionSample() {
        return submissionSample;
    }

    public void setSubmissionSample(SubmissionBioSampleBean submissionSample) {
        this.submissionSample = submissionSample;
    }

    public String getSubmissionRepository() {
        return submissionRepository;
    }

    public void setSubmissionRepository(String submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public String getSubmissionDatatype() {
        return submissionDatatype;
    }

    public void setSubmissionDatatype(String submissionDatatype) {
        this.submissionDatatype = submissionDatatype;
    }

    public String getAggregationProject() {
        return aggregationProject;
    }

    public void setAggregationProject(String aggregationProject) {
        this.aggregationProject = aggregationProject;
    }

    public String getBamVersion() {
        return bamVersion;
    }

    public void setBamVersion(String bamVersion) {
        this.bamVersion = bamVersion;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionBean.class)) {
            return false;
        }

        if (!(other instanceof SubmissionBean)) {
            return false;
        }

        SubmissionBean that = OrmUtil.proxySafeCast(other, SubmissionBean.class);

        return new EqualsBuilder()
            .append(getUuid(), that.getUuid())
            .append(getStudyContact(), that.getStudyContact())
            .append(getBioProject(), that.getBioProject())
            .append(getSubmissionSample(), that.getSubmissionSample())
            .append(getSubmissionRepository(), that.getSubmissionRepository())
            .append(getSubmissionDatatype(), that.getSubmissionDatatype())
            .append(getAggregationProject(), that.getAggregationProject())
            .append(getBamVersion(), that.getBamVersion())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getUuid())
            .append(getStudyContact())
            .append(getBioProject())
            .append(getSubmissionSample())
            .append(getSubmissionRepository())
            .append(getSubmissionDatatype())
            .append(getAggregationProject())
            .append(getBamVersion())
            .toHashCode();
    }
}
