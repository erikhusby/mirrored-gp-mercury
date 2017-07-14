
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlAccessorOrder(value= XmlAccessOrder.ALPHABETICAL)
/*          + "        {\n"
                   + "            \"bamversion\": \"V1\",\n"
                   + "            \"bioproject\": {\n"
                   + "                \"accession\": \"PRJNA75333\"\n"
                   + "            },\n"
                   + "            \"submissionsample\": {\n"
                   + "                \"contact\": {\n"
                   + "                    \"email\": \"jgentry@broadinstitute.org\",\n"
                   + "                    \"firstName\": \"Jeff\",\n"
                   + "                    \"lab\": \"homer\",\n"
                   + "                    \"lastName\": \"Gentry\",\n"
                   + "                    \"middleName\": \"A\",\n"
                   + "                    \"phone\": \"617-555-9292\"\n"
                   + "                },\n"
                   + "            \"processinglocation\": \"OnPrem\",\n"
                   + "            \"sampleId\": \"S_2507\"\n"
                   + "        },\n"
                   + "            \"broadproject\": \"RP-1234\",\n"
                   + "            \"studyContact\": \"jgentry\",\n"
                   + "            \"site\": \"NCBI_PROTECTED\",\n"
                   + "            \"submissiondatatype\": \"Whole Genome\",\n"
                   + "            \"uuid\": \"7d835cc7-cd63-4cc6-9621-868155618745\"\n"
                   + "        },\n"
                   + "        {\n"*/
public class SubmissionBean implements Serializable {
    private static final long serialVersionUID = 5575909517269494566L;
    @XmlElement
    private String uuid;
    @XmlElement
    private String studyContact;
    @XmlElement
    private BioProject bioProject;
    @XmlElement
    private SubmissionBioSampleBean submissionSample;
    @XmlElement(name = "site")
    private String submissionRepository;
    @XmlElement(name = "submissionDatatype")
    private String submissionDatatype;
    @XmlElement
    private String broadProject;
    @XmlElement
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, SubmissionBean.class))) {
            return false;
        }

        if (!(o instanceof SubmissionBean)) {
            return false;
        }

        SubmissionBean that = OrmUtil.proxySafeCast(o, SubmissionBean.class);

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
