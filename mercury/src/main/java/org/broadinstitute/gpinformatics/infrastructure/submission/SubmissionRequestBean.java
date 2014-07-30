
package org.broadinstitute.gpinformatics.infrastructure.submission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement()
public class SubmissionRequestBean implements Serializable {

    private SubmissionBean[] submissions;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SubmissionRequestBean() {
    }

    public SubmissionBean[] getSubmissions() {
        return submissions;
    }

    @XmlElement
    public void setSubmissions(SubmissionBean... submissions) {
        this.submissions = submissions;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
