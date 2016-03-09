
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SubmissionBean implements Serializable {
    private static final long serialVersionUID = 5575909517269494566L;
    private String uuid;
    private String studyContact;
    private BioProject bioproject;
    private SubmissionBioSampleBean biosample;
    private String submissionRepository;
    private String submissionType;

    public SubmissionBean() {
    }


    public SubmissionBean(String uuid, String studyContact, BioProject bioproject, SubmissionBioSampleBean biosample,
                          SubmissionRepository submissionRepository, SubmissionLibraryDescriptor submissionLibraryDescriptor) {
        this.uuid = uuid;
        this.studyContact = studyContact;
        this.bioproject = bioproject;
        this.biosample = biosample;
        this.submissionRepository = submissionRepository.getName();
        this.submissionType = submissionLibraryDescriptor.getName();
    }

    public String getUuid() {
        return uuid;
    }

    @XmlElement
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStudyContact() {
        return studyContact;
    }

    @XmlElement
    public void setStudyContact(String studyContact) {
        this.studyContact = studyContact;
    }

    public BioProject getBioproject() {
        return bioproject;
    }

    @XmlElement
    public void setBioproject(BioProject bioproject) {
        this.bioproject = bioproject;
    }

    public SubmissionBioSampleBean getBiosample() {
        return biosample;
    }

    @XmlElement
    public void setBiosample(SubmissionBioSampleBean biosample) {
        this.biosample = biosample;
    }

    public String getSubmissionRepository() {
        return submissionRepository;
    }

    @XmlElement(name="site")
    public void setSubmissionRepository(String submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    @XmlElement(name="datatype")
    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionBean.class)) {
            return false;
        }

        SubmissionBean castOther = OrmUtil.proxySafeCast(other, SubmissionBean.class);
        return new EqualsBuilder().append(getUuid(), castOther.getUuid())
                                  .append(getStudyContact(), castOther.getStudyContact())
                                  .append(getBioproject(), castOther.getBioproject())
                                  .append(getBiosample(), castOther.getBiosample())
                                  .append(getSubmissionRepository(), castOther.getSubmissionRepository())
                                  .append(getSubmissionType(), castOther.getSubmissionType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getUuid()).append(getStudyContact()).append(getBioproject())
                .append(getBiosample()).append(getSubmissionRepository()).append(getSubmissionType()).toHashCode();
    }
}
