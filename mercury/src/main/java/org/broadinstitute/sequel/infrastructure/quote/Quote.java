package org.broadinstitute.sequel.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Quote")
public class Quote {
    private String alphanumericId;
    private String approvalStatus;
    private String id;
    private String name;
    private QuoteFunding quoteFunding;

    public Quote() {}

    public Quote(String alphanumericId,QuoteFunding quoteFunding) {
        if (alphanumericId == null) {
            throw new NullPointerException("alphanumeric Id cannot be null.");
        }
        this.alphanumericId = alphanumericId;
        this.quoteFunding = quoteFunding;
    }
    
    public Quote(String alphanumericId) {
        this(alphanumericId,null);
    }
    
    @XmlElement(name = "QuoteFunding")
    public QuoteFunding getQuoteFunding() {
        return quoteFunding;
    }

    public void setQuoteFunding(QuoteFunding quoteFunding) {
        this.quoteFunding = quoteFunding;
    }

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quote quote = (Quote) o;

        if (alphanumericId != null ? !alphanumericId.equals(quote.alphanumericId) : quote.alphanumericId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return alphanumericId != null ? alphanumericId.hashCode() : 0;
    }
    
    public String toString() {
        return alphanumericId;
    }
}
