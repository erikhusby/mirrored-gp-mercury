package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;

import java.io.Serializable;
import java.util.Date;

/**
 * Non-entity used for optimizing the performance of the PDO list page.
 */
public class ProductOrderListEntry implements Serializable {

    private static final long serialVersionUID = 6343514424527232374L;

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

    private final long constructedCount;

    private long readyForReviewCount = 0L;

    private long readyForBillingCount = 0L;

    /**
     * Version of the constructor called by the non-ledger aware first pass query.
     *
     */
    @SuppressWarnings("UnusedDeclaration")
    // This is called through reflection and only appears to be unused.
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

        // The query that generates this does not update counts, so this value is not used here.
        this.constructedCount = 0L;
    }

    /**
     * Version of the constructor called by the ledger-aware second pass query, these objects are merged
     * into the objects from the first query.
     */
    @SuppressWarnings("UnusedDeclaration")
    // This is called through reflection and only appears to be unused.
    public ProductOrderListEntry(
        Long orderId, String jiraTicketKey, Long billingSessionId, Long constructedCount) {

        this.orderId = orderId;
        this.jiraTicketKey = jiraTicketKey;
        this.billingSessionId = billingSessionId;

        // This count is used by the query that needs to populate one of the two other counts.
        this.constructedCount = constructedCount;
    }

    private ProductOrderListEntry() {
        constructedCount = 0L;
    }

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

    public boolean isBilling() {
        return getBillingSessionBusinessKey() != null;
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

    public Long getConstructedCount() {
        return constructedCount;
    }

    public void setReadyForBillingCount(Long readyForBillingCount) {
        this.readyForBillingCount = readyForBillingCount;
    }

    public void setReadyForReviewCount(Long readyForReviewCount) {
        this.readyForReviewCount = readyForReviewCount;
    }

    /**
     * If the transient readyForBillingCount is set (done by the order list entry dao), then this is ready for
     * billing when there are entries ready for billing and nothing ready for review. Ready for review means there
     * are items that have been auto-billed but not reviewed. Was relying on if test ordering, but the extra check
     * seems worth doing here.
     *
     * @return Is this pdo entry ready for billing?
     */
    public boolean isReadyForBilling() {
        return !isReadyForReview() && (readyForBillingCount > 0);
    }

    /**
     * If there are any ledger entries on any sample that are ready for review, the readyForReviewCount will be the
     * number in this state.
     *
     * @return Is this pdo entry ready for review?
     */
    public boolean isReadyForReview() {
        return readyForReviewCount > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductOrderListEntry)) return false;

        ProductOrderListEntry that = (ProductOrderListEntry) o;

        return !(jiraTicketKey != null ? !jiraTicketKey.equals(that.jiraTicketKey) : that.jiraTicketKey != null);

    }

    @Override
    public int hashCode() {
        return jiraTicketKey != null ? jiraTicketKey.hashCode() : 0;
    }

    public boolean isDraft() {
        return ProductOrder.OrderStatus.Draft == orderStatus;
    }

}
