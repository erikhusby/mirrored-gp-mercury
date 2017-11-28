/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SubmissionSampleResultBean implements Serializable {
    private static final long serialVersionUID = 2014081301L;
    @JsonProperty
    private String accession;
    @JsonProperty
    private List<String> submittedSampleIds;

    public SubmissionSampleResultBean() {
    }

    public SubmissionSampleResultBean(String accession, String ... submittedSampleIds) {
        this.accession = accession;
        this.submittedSampleIds = Arrays.asList(submittedSampleIds);
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public List<String> getSubmittedSampleIds() {
        return submittedSampleIds;
    }

    public void setSubmittedSampleIds(List<String> submittedSampleIds) {
        this.submittedSampleIds = submittedSampleIds;
    }

    @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionSampleResultBean.class)) {
                return false;
            }

        SubmissionSampleResultBean castOther = OrmUtil.proxySafeCast(other, SubmissionSampleResultBean.class);
            return new EqualsBuilder().append(getAccession(), castOther.getAccession()).append(getSubmittedSampleIds(), castOther.getSubmittedSampleIds()).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(getAccession()).append(getSubmittedSampleIds()).toHashCode();
        }
}
