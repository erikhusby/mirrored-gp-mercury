
package org.broadinstitute.gpinformatics.infrastructure.submission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.broad.prodinfo.bean.generated.ObjectFactory;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement()
public class SubmissionRequestBean implements Serializable {

    private static final long serialVersionUID = -2195569074348464285L;
    private List<SubmissionBean> submissions;

    public SubmissionRequestBean() {
    }

    public SubmissionRequestBean(List<SubmissionBean> submissionBeans) {
        setSubmissions(submissionBeans);

    }

    public List<SubmissionBean> getSubmissions() {
        return submissions;
    }

    @XmlElement
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
