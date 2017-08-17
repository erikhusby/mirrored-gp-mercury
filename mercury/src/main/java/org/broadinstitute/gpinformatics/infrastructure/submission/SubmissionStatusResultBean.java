package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionStatusResultBean implements Serializable {
    private static final long serialVersionUID = 9068748107416910212L;

    @JsonProperty(value = "submissionStatuses")
    private List<SubmissionStatusDetailBean> submissionStatuses=new ArrayList<>();

    public SubmissionStatusResultBean() {
    }

    public SubmissionStatusResultBean(Collection<SubmissionStatusDetailBean> submissionStatuses) {
        this.submissionStatuses.addAll(submissionStatuses);
    }

    public List<SubmissionStatusDetailBean> getSubmissionStatuses () {
        return submissionStatuses;
    }

    public void setSubmissionStatuses (List<SubmissionStatusDetailBean> submissionStatuses) {
        this.submissionStatuses = submissionStatuses;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionStatusResultBean.class)) {
            return false;
        }

        SubmissionStatusResultBean castOther = OrmUtil.proxySafeCast(other, SubmissionStatusResultBean.class);
        return new EqualsBuilder().append(getSubmissionStatuses(), castOther.getSubmissionStatuses()).isEquals();
    }



    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSubmissionStatuses()).toHashCode();
    }
}
