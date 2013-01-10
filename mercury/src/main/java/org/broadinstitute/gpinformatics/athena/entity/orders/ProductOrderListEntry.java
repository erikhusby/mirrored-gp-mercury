package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;

import java.io.Serializable;
import java.util.Date;
import java.util.regex.Matcher;

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

    private Date updatedDate;

    private Integer pdoSampleCount;

    private Long billingSessionId;

    private Long unbilledLedgerEntryCount = 0L;


    /**
     * Version of the constructor called by the non-ledger aware first pass query
     *
     * @param orderId
     * @param title
     * @param jiraTicketKey
     * @param orderStatus
     * @param productName
     * @param productFamilyName
     * @param researchProjectTitle
     * @param ownerId
     * @param updatedDate
     */
    public ProductOrderListEntry(Long orderId, String title, String jiraTicketKey, ProductOrder.OrderStatus orderStatus,
                                 String productName, String productFamilyName, String researchProjectTitle, Long ownerId,
                                 Date updatedDate, Integer pdoSampleCount) {
        this.orderId = orderId;
        this.title = title;
        this.jiraTicketKey = jiraTicketKey;
        this.orderStatus = orderStatus;
        this.productName = productName;
        this.productFamilyName = productFamilyName;
        this.researchProjectTitle = researchProjectTitle;
        this.ownerId = ownerId;
        this.updatedDate = updatedDate;
        this.pdoSampleCount = pdoSampleCount;
    }


    /**
     * Version of the constructor called by the ledger-aware second pass query.  These objects are essentially merged
     * into the objects from the first query.
     *
     * @param orderId
     * @param jiraTicketKey
     * @param billingSessionId
     * @param unbilledLedgerEntryCount
     */
    public ProductOrderListEntry(Long orderId, String jiraTicketKey, Long billingSessionId, Long unbilledLedgerEntryCount) {
        this.orderId = orderId;
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
        if (jiraTicketKey == null) {
            return ProductOrder.DRAFT_PREFIX + orderId;
        }

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
        return BillingSession.ID_PREFIX + billingSessionId;
    }

    public Integer getPdoSampleCount() {
        return pdoSampleCount;
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


    public boolean isValidJiraTicket() {
        if (jiraTicketKey == null) {
            return false;
        }

        return JiraTicket.PATTERN.matcher(jiraTicketKey).matches();
    }


    /**
     *
     * @return the numeric portion of the JIRA ticket key if this looks like a real JIRA ticket, otherwise return null.
     */
    public Integer getJiraTicketNumber() {

        if (jiraTicketKey == null) {
            return null;
        }

        Matcher matcher = JiraTicket.PATTERN.matcher(jiraTicketKey);

        if ( ! matcher.matches() || ! "PDO".equals(matcher.group(JiraTicket.PATTERN_GROUP_PREFIX))) {
            return null;
        }

        // pluck out the numeric portion of the JIRA ticket key and convert to Integer
        return Integer.valueOf(matcher.group(JiraTicket.PATTERN_GROUP_NUMBER));
    }


}
