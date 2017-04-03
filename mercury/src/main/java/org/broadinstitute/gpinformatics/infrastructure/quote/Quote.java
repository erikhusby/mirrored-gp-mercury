package org.broadinstitute.gpinformatics.infrastructure.quote;

import clover.org.apache.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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

    // quick access Cache of quote items
    public HashMap<String, HashMap<String, HashMap<String, QuoteItem>>> quoteItemCache = new HashMap<>();


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

        FundingLevel singleLevel = getFirstRelevantFundingLevel();

        return !(singleLevel == null);
    }

    /**
     * Helper method to support SAP transition.  If there is only one funding level, this will return it.  Otherwise
     * Null will be returned
     *
     * @return Single funding level for the quote, or null if there is either more than one level or no level.
     */
    public FundingLevel getFirstRelevantFundingLevel() {
        FundingLevel singleLevel = null;

        for(FundingLevel level : quoteFunding.getFundingLevel()) {
            if (singleLevel == null) {
                singleLevel = level;
            } else {
                return null;
            }
        }
        return singleLevel;
    }

    /**
     * initialized the Multi level hash map to make accessing items in the quote item collection easier
     */
    public void initializeQuoteItemCache () {
        if(CollectionUtils.isNotEmpty(quoteItems)) {
            for (QuoteItem quoteItem : quoteItems) {
                if(!quoteItemCache.containsKey(quoteItem.getCategoryName())) {
                    quoteItemCache.put(quoteItem.getCategoryName(), new HashMap<String, HashMap<String, QuoteItem>>());
                }
                if(!quoteItemCache.get(quoteItem.getCategoryName()).containsKey(quoteItem.getPlatform())) {
                    quoteItemCache.get(quoteItem.getCategoryName()).put(quoteItem.getPlatform(),new HashMap<String, QuoteItem>());
                }
                if(!quoteItemCache.get(quoteItem.getCategoryName()).get(quoteItem.getPlatform()).containsKey(quoteItem.getName())) {
                    quoteItemCache.get(quoteItem.getCategoryName()).get(quoteItem.getPlatform()).put(quoteItem.getName(), quoteItem);
                }
            }
        }
    }

    /**
     * Access a quote item defined on the quote by key criteria.
     *
     * @param platform  Platform with which the desired quote item should be associated
     * @param category  Category with which the desired quote item should be associated
     * @param name      Name with which the desired quote item should be named
     * @return  specific QuoteItem on the quote when found, or null if it is not found
     */
    public QuoteItem findCachedQuoteItem(String platform, String category, String name) {

        QuoteItem foundItem = null;

        if(quoteItemCache.isEmpty()) {
            initializeQuoteItemCache();
        }
        if (quoteItemCache.containsKey(category) &&
            quoteItemCache.get(category).containsKey(platform) &&
            quoteItemCache.get(category).get(platform).containsKey(name)) {

            foundItem = quoteItemCache.get(category).get(platform).get(name);

        }

        return foundItem;
    }
}
