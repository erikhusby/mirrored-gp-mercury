
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@XmlAccessorType(XmlAccessType.FIELD)
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
    @JsonProperty
    private String broadProject;
    @JsonProperty
    private String bamVersion;

    public SubmissionBean() {
    }

    public SubmissionBean(String uuid, String studyContact, BioProject bioProject, SubmissionBioSampleBean submissionSample,
                          SubmissionRepository submissionRepository,
                          SubmissionLibraryDescriptor submissionLibraryDescriptor, String broadProject,
                          String bamVersion) {
        this.uuid = uuid;
        this.studyContact = studyContact;
        this.bioProject = bioProject;
        this.submissionSample = submissionSample;
        this.submissionRepository = submissionRepository.getName();
        this.submissionDatatype = submissionLibraryDescriptor.getName();
        this.broadProject = broadProject;
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

    public String getBroadProject() {
        return broadProject;
    }

    public void setBroadProject(String broadProject) {
        this.broadProject = broadProject;
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
            .append(getBroadProject(), that.getBroadProject())
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
            .append(getBroadProject())
            .append(getBamVersion())
            .toHashCode();
    }
}
