
package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.util.List;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionRequestBean implements Serializable {
    private static final long serialVersionUID = -2195569074348464285L;

    @JsonProperty(value = "onPremOrCloudSubmissions")
    private List<SubmissionBean> submissions;

    public SubmissionRequestBean() {
    }

    public SubmissionRequestBean(List<SubmissionBean> submissionBeans) {
        setSubmissions(submissionBeans);
    }

    public List<SubmissionBean> getSubmissions() {
        return submissions;
    }
    public void setSubmissions(List<SubmissionBean> submissions) {
        this.submissions = submissions;
    }

    @Override
    public boolean equals(Object other) {

        if(this == other) {
            return true;
        }

        if(other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionRequestBean.class)) {
            return false;
        }

        SubmissionRequestBean castOther = OrmUtil.proxySafeCast(other, SubmissionRequestBean.class);
        return new EqualsBuilder().append(getSubmissions(), castOther.getSubmissions()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSubmissions()).toHashCode();
    }

}
