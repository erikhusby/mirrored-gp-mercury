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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class SubmissionSampleResultBean implements Serializable {
    private static final long serialVersionUID = 2014081301L;
    private String accession;
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

    @XmlElement
    public void setAccession(String accession) {
        this.accession = accession;
    }

    public List<String> getSubmittedSampleIds() {
        return submittedSampleIds;
    }

    @XmlElement
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
