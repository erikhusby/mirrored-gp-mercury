
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SubmissionBean implements Serializable {

    private String uuid;
    private String studyContact;
    private BioProject bioproject;
    private SubmissionBioSampleBean biosample;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SubmissionBean() {
    }


    public SubmissionBean(String uuid, String studyContact,
                          BioProject bioproject,
                          SubmissionBioSampleBean biosample) {
        this.uuid = uuid;
        this.studyContact = studyContact;
        this.bioproject = bioproject;
        this.biosample = biosample;
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

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
