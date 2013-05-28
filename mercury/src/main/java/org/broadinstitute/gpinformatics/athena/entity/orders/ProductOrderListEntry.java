package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;

import java.io.Serializable;
import java.util.Date;

/**
 * Non-entity used for optimizing the performance of the PDO list page.
 */
public class ProductOrderListEntry implements Serializable {

    private Long orderId;

    private String title;

    private String jiraTicketKey;

    private String productName;

    private String productFamilyName;

    private ProductOrder.OrderStatus orderStatus;

    private String researchProjectTitle;

    private Long ownerId;

    private Date placedDate;

    private String quoteId;

    private Long billingSessionId;

    private Long unbilledLedgerEntryCount = 0L;

    /**
     * Version of the constructor called by the non-ledger aware first pass query.
     *
     */
    public ProductOrderListEntry(Long orderId, String title, String jiraTicketKey, ProductOrder.OrderStatus orderStatus,
                                 String productName, String productFamilyName, String researchProjectTitle,
                                 Long ownerId, Date placedDate, String quoteId) {
        this.orderId = orderId;
        this.title = title;
        this.jiraTicketKey = jiraTicketKey;
        this.orderStatus = orderStatus;
        this.productName = productName;
        this.productFamilyName = productFamilyName;
        this.researchProjectTitle = researchProjectTitle;
        this.ownerId = ownerId;
        this.placedDate = placedDate;
        this.quoteId = quoteId;
    }

    /**
     * Version of the constructor called by the ledger-aware second pass query.  These objects are merged
     * into the objects from the first query.
     *
     */
    public ProductOrderListEntry(
        Long orderId, String jiraTicketKey, Long billingSessionId, Long unbilledLedgerEntryCount) {

        this.orderId = orderId;
        this.jiraTicketKey = jiraTicketKey;
        this.billingSessionId = billingSessionId;
        this.unbilledLedgerEntryCount = unbilledLedgerEntryCount;
    }

    private ProductOrderListEntry() {}

    public static ProductOrderListEntry createDummy() {
        return new ProductOrderListEntry();
    }

    public String getTitle() {
        return title;
    }

    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    public String getBusinessKey() {
        return ProductOrder.createBusinessKey(orderId, jiraTicketKey);
    }

    public String getProductName() {
        return productName;
    }

    public String getProductFamilyName() {
        return productFamilyName;
    }

    public ProductOrder.OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public String getResearchProjectTitle() {
        return researchProjectTitle;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Date getPlacedDate() {
        return placedDate;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getBillingSessionBusinessKey() {
        if (billingSessionId == null) {
            return null;
        }
        return BillingSession.ID_PREFIX + billingSessionId;
    }

    public Long getBillingSessionId() {
        return billingSessionId;
    }

    public void setBillingSessionId(Long billingSessionId) {
        this.billingSessionId = billingSessionId;
    }

    public Long getUnbilledLedgerEntryCount() {
        return unbilledLedgerEntryCount;
    }

    public void setUnbilledLedgerEntryCount(Long unbilledLedgerEntryCount) {
        this.unbilledLedgerEntryCount = unbilledLedgerEntryCount;
    }

    public boolean isEligibleForBilling() {
        return getUnbilledLedgerEntryCount() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductOrderListEntry)) return false;

        ProductOrderListEntry that = (ProductOrderListEntry) o;

        if (jiraTicketKey != null ? !jiraTicketKey.equals(that.jiraTicketKey) : that.jiraTicketKey != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return jiraTicketKey != null ? jiraTicketKey.hashCode() : 0;
    }

    public boolean isDraft() {
        return ProductOrder.OrderStatus.Draft == orderStatus;
    }

}
