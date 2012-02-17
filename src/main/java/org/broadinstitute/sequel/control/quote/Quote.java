package org.broadinstitute.sequel.control.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Quote")
public class Quote {
    private String alphanumericId;
    private String approvalStatus;
    private String id;


    public Quote() {}

    public Quote(String alphanumericId, String approvalStatus, String id) {
        this.alphanumericId = alphanumericId;
        this.approvalStatus = approvalStatus;
        this.id = id;
    }

    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name="alphanumericId")
    public String getAlphanumericId() {
        return alphanumericId;
    }

    public void setAlphanumericId(String alphanumericId) {
        this.alphanumericId = alphanumericId;
    }

    @XmlAttribute(name="approvalStatus")
    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
}
