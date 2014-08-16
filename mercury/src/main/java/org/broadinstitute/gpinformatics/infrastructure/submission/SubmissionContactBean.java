package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SubmissionContactBean implements Serializable {

    private static final long serialVersionUID = 8166404474924070240L;
    private String email;
    private String phone;
    private String lab;
    private String firstName;
    private String lastName;
    private String middleName;

    public SubmissionContactBean() {
    }

    public SubmissionContactBean(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public SubmissionContactBean(String firstName, String middleName, String lastName, String email, String phone,
                                 String lab) {
        this(firstName,lastName,email);
        this.phone = phone;
        this.lab = lab;
        this.middleName = middleName;
    }

    public String getEmail() {
        return email;
    }

    @XmlElement
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    @XmlElement
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLab() {
        return lab;
    }

    @XmlElement
    public void setLab(String lab) {
        this.lab = lab;
    }

    public String getFirstName() {
        return firstName;
    }

    @XmlElement
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @XmlElement
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    @XmlElement
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getFirstName()).append(getMiddleName()).append(getLastName())
                                    .append(getEmail()).append(getPhone()).append(getLab()).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, SubmissionContactBean.class)) {
            return false;
        }

        SubmissionContactBean castOther = OrmUtil.proxySafeCast(other, SubmissionContactBean.class);
        return new EqualsBuilder().append(getFirstName(), castOther.getFirstName())
                                  .append(getMiddleName(), castOther.getMiddleName())
                                  .append(getLastName(), castOther.getLastName())
                                  .append(getEmail(), castOther.getEmail())
                                  .append(getPhone(), castOther.getPhone())
                                  .append(getLab(), castOther.getLab()).isEquals();

    }


}
