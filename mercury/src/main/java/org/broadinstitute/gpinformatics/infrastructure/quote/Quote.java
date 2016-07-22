package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name="Quote")
public class Quote {


    private String alphanumericId;
    private ApprovalStatus approvalStatus;
    private Boolean isExpired;
    private String id;
    private String name;
    private QuoteFunding quoteFunding;
    private QuoteType quoteType;
    private Collection<QuoteItem> quoteItems = new ArrayList<> ();

    public Quote() {}

    public Quote(String alphanumericId, QuoteFunding quoteFunding, ApprovalStatus approvalStatus) {
        if (alphanumericId == null) {
            throw new NullPointerException("alphanumeric Id cannot be null.");
        }
        this.alphanumericId = alphanumericId;
        this.quoteFunding = quoteFunding;
        this.approvalStatus = approvalStatus;
    }

    @XmlElement(name = "QuoteFunding")
    public QuoteFunding getQuoteFunding() {
        return quoteFunding;
    }

    public void setQuoteFunding(QuoteFunding quoteFunding) {
        this.quoteFunding = quoteFunding;
    }

    @XmlElement(name = "Item")
    public Collection<QuoteItem> getQuoteItems() {
        return quoteItems;
    }

    public void setQuoteItems(Collection<QuoteItem> quoteItems) {
        this.quoteItems = quoteItems;
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
    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    @XmlAttribute(name="isExpired")
    public Boolean getExpired() {
        return isExpired;
    }

    public void setExpired(final Boolean expired) {
        isExpired = expired;
    }

    @XmlAttribute(name="quoteType")
    public QuoteType getQuoteType() {
        return quoteType;
    }

    public void setQuoteType(QuoteType quoteType) {
        this.quoteType = quoteType;
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

    public boolean isEligibleForSAP() {
        return !(quoteFunding.getFundingLevel().size() >1);
    }
}
