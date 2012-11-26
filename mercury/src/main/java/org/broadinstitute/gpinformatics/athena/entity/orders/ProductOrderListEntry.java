package org.broadinstitute.gpinformatics.athena.entity.orders;

import java.io.Serializable;
import java.util.Date;

/**
 * Non-entity used for optimizing the performance of the PDO list page.
 */
public class ProductOrderListEntry implements Serializable {

    private String title;

    private String jiraTicketKey;

    private String productName;

    private String productFamilyName;

    private ProductOrder.OrderStatus orderStatus;

    private String researchProjectTitle;

    private Long ownerId;

    private Date updatedDate;

    private Long billingSessionId;

    private Long unbilledLedgerEntryCount = 0L;


    /**
     * Version of the constructor called by the non-ledger aware first pass query
     *
     * @param title
     * @param jiraTicketKey
     * @param orderStatus
     * @param productName
     * @param productFamilyName
     * @param researchProjectTitle
     * @param ownerId
     * @param updatedDate
     */
    public ProductOrderListEntry(String title, String jiraTicketKey, ProductOrder.OrderStatus orderStatus,
                                 String productName, String productFamilyName, String researchProjectTitle, Long ownerId, Date updatedDate) {
        this.title = title;
        this.jiraTicketKey = jiraTicketKey;
        this.orderStatus = orderStatus;
        this.productName = productName;
        this.productFamilyName = productFamilyName;
        this.researchProjectTitle = researchProjectTitle;
        this.ownerId = ownerId;
        this.updatedDate = updatedDate;
    }


    /**
     * Version of the constructor called by the ledger-aware second pass query.  These objects are essentially merged
     * into the objects from the first query.
     *
     * @param jiraTicketKey
     * @param billingSessionId
     * @param unbilledLedgerEntryCount
     */
    public ProductOrderListEntry(String jiraTicketKey, Long billingSessionId, Long unbilledLedgerEntryCount) {
        this.jiraTicketKey = jiraTicketKey;
        this.billingSessionId = billingSessionId;
        this.unbilledLedgerEntryCount = unbilledLedgerEntryCount;
    }


    public String getTitle() {
        return title;
    }

    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    public String getBusinessKey() {
        return getJiraTicketKey();
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

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public String getBillingSessionBusinessKey() {
        if (billingSessionId == null) {
            return null;
        }
        return "BILL-" + billingSessionId;
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
}
