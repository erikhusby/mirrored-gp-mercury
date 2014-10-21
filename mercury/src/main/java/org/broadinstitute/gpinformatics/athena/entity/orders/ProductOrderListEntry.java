package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Non-entity used for optimizing the performance of the PDO list page.
 */
public class ProductOrderListEntry implements Serializable {

    private static final long serialVersionUID = 6343514424527232374L;

    private final Long orderId;

    private final ProductOrder productOrder;

    private Long billingSessionId;

    private final long constructedCount;

    private long readyForReviewCount;

    private long readyForBillingCount;

    private ProductOrderListEntry(Long orderId, String title, String jiraTicketKey,
                                  ProductOrder.OrderStatus orderStatus, String productName, Product product,
                                  String productFamilyName,
                                  String researchProjectTitle,
                                  Long ownerId, Date placedDate, Integer laneCount, String quoteId,
                                  Long billingSessionId,
                                  long constructedCount) {
        this.orderId = orderId;
        productOrder = new ProductOrder();
        productOrder.setTitle(title);
        productOrder.setJiraTicketKey(jiraTicketKey);
        productOrder.setOrderStatus(orderStatus);
        productOrder.setProduct(product);
        productOrder.setCreatedBy(ownerId);
        productOrder.setPlacedDate(placedDate);
        productOrder.setLaneCount(laneCount != null ? laneCount : 0);
        productOrder.setQuoteId(quoteId);
        ResearchProject researchProject = new ResearchProject();
        researchProject.setTitle(researchProjectTitle);
        productOrder.setResearchProject(researchProject);
        this.billingSessionId = billingSessionId;

        // This count is used by the query that needs to populate one of the two other counts.
        this.constructedCount = constructedCount;
    }

    /**
     * Version of the constructor called by the non-ledger aware first pass query.
     *
     */
    @SuppressWarnings("UnusedDeclaration")
    // This is called through reflection and only appears to be unused.
    public ProductOrderListEntry(Long orderId, String title, String jiraTicketKey, ProductOrder.OrderStatus orderStatus,
                                 String productName, Product product, String productFamilyName,
                                 String researchProjectTitle, Long ownerId,
                                 Date placedDate, Integer laneCount, String quoteId) {

        // No billing session and a the constructed count is set to 0 because it is not used for this constructor.
        this(orderId, title, jiraTicketKey, orderStatus, productName, product, productFamilyName,
                researchProjectTitle, ownerId, placedDate, laneCount, quoteId, null, 0);
    }

    /**
     * Version of the constructor called by the ledger-aware second pass query, these objects are merged
     * into the objects from the first query.
     */
    // This is called through reflection and only appears to be unused.
    @SuppressWarnings("UnusedDeclaration")
    public ProductOrderListEntry(
        Long orderId, String jiraTicketKey, Long billingSessionId, long constructedCount) {
        this(orderId, null, jiraTicketKey, null, null, null, null, null, null, null, null, null, billingSessionId,
                constructedCount);
    }

    private ProductOrderListEntry() {
        this(null, null, null, 0);
    }

    public static ProductOrderListEntry createDummy() {
        return new ProductOrderListEntry();
    }

    /**
     * @return true if this entry matches any of the statuses in the list.
     */
    public boolean matchStatuses(Collection<LedgerStatus> ledgerStatuses) {
        for (LedgerStatus selectedStatus : ledgerStatuses) {
            if (selectedStatus.entryMatch(this)) {
                return true;
            }
        }
        return false;
    }

    public String getTitle() {
        return productOrder.getTitle();
    }

    public String getJiraTicketKey() {
        return productOrder.getJiraTicketKey();
    }

    public String getBusinessKey() {
        return ProductOrder.createBusinessKey(orderId, productOrder.getJiraTicketKey());
    }

    public String getProductName() {
        return (productOrder.getProduct() != null) ? productOrder.getProduct().getProductName() : "";
    }

    public Product getProduct() {
        return productOrder.getProduct();
    }

    public String getProductFamilyName() {
        return (productOrder.getProduct() != null) ? productOrder.getProduct().getProductFamily().getName() : "";
    }

    public ProductOrder.OrderStatus getOrderStatus() {
        return productOrder.getOrderStatus();
    }

    public String getResearchProjectTitle() {
        return productOrder.getResearchProject().getTitle();
    }

    public Long getOwnerId() {
        return productOrder.getCreatedBy();
    }

    public Date getPlacedDate() {
        return productOrder.getPlacedDate();
    }

    public Integer getLaneCount() {
        return productOrder.getLaneCount();
    }

    public String getQuoteId() {
        return productOrder.getQuoteId();
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

    public boolean isDraft() {
        return productOrder.isDraft();
    }

    public static Collection<Long> getProductOrderIDs(List<ProductOrderListEntry> productOrderListEntries) {
        Collection<Long> pdoIds = new ArrayList<>(productOrderListEntries.size());
        for (ProductOrderListEntry entry : productOrderListEntries){
            pdoIds.add(entry.orderId);
        }

        return pdoIds;
    }

    /**
     * This enum defines the different states that the PDO is in based on the ledger status of all samples in the order.
     * This status does not included 'Billed' because that is orthogonal to this status. Once something is billed, it
     * is, essentially in NOTHING_NEW state again and new billing can happen at any time. When something is auto billed,
     * the status becomes READY_FOR_REVIEW, which means more auto billing can happen, but there is some work to look
     * at. The PDM can download the tracker at any time when there is no billing, but can only upload when auto billing
     * is not happening (and billing is happening).
     * <p/>
     * Once the PDM or Billing Manager does an upload, the state is changed to READY_TO_BILL and the auto-biller will
     * not process any more entries for this PDO until a billing session has been completed on it.
     * <p/>
     * These statuses are calculated on-demand based on the ledger entries.
     */
    public enum LedgerStatus {
        NOTHING_NEW("None", false) {
            @Override
            public boolean entryMatch(ProductOrderListEntry entry) {
                // Drafts are always new.
                return entry.isDraft() ||
                       (!entry.isReadyForBilling() && !entry.isReadyForReview() && !entry.isBilling());
            }

            @Override
            public Predicate buildPredicate(CriteriaBuilder criteriaBuilder,
                                            SetJoin<ProductOrderSample, LedgerEntry> sampleLedgerEntryJoin,
                                            Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin) {
                return null;
            }

            @Override
            public void updateEntryCount(ProductOrderListEntry entry, Long count) {
                throw new IllegalArgumentException("Can only fetch ready to bill or ready for review");
            }
        },
        READY_FOR_REVIEW("Review") {
            @Override
            public boolean entryMatch(ProductOrderListEntry entry) {
                // Ready for review is ALWAYS indicated when there are ledger entries. Billing locks out
                // automated ledger entry. For now, so does ready to bill, but that may change.
                return entry.isReadyForReview();
            }

            @Override
            public Predicate buildPredicate(CriteriaBuilder criteriaBuilder,
                                            SetJoin<ProductOrderSample, LedgerEntry> sampleLedgerEntryJoin,
                                            Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin) {
                // The billing session is null but the auto bill timestamp is NOT null.
                return criteriaBuilder.and(
                        criteriaBuilder.isNull(sampleLedgerEntryJoin.get(LedgerEntry_.billingSession)),
                        criteriaBuilder.isNotNull(sampleLedgerEntryJoin.get(LedgerEntry_.autoLedgerTimestamp)));
            }

            @Override
            public void updateEntryCount(ProductOrderListEntry entry, Long count) {
                entry.readyForReviewCount = count;
            }
        },
        READY_TO_BILL("Ready to Bill") {
            @Override
            public boolean entryMatch(ProductOrderListEntry entry) {
                // Ready for review overrides ready for billing. May not come up for a while because of
                // lockouts, but this is the way the visual works, so using this.
                return entry.isReadyForBilling() && !entry.isReadyForReview();
            }

            @Override
            public Predicate buildPredicate(CriteriaBuilder criteriaBuilder,
                                            SetJoin<ProductOrderSample, LedgerEntry> sampleLedgerEntryJoin,
                                            Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin) {
                // The billing session is null but the auto bill timestamp is null.
                return criteriaBuilder.and(
                        criteriaBuilder.isNull(sampleLedgerEntryJoin.get(LedgerEntry_.billingSession)),
                        criteriaBuilder.isNull(sampleLedgerEntryJoin.get(LedgerEntry_.autoLedgerTimestamp)));
            }

            @Override
            public void updateEntryCount(ProductOrderListEntry entry, Long count) {
                entry.readyForBillingCount = count;
            }
        },
        BILLING("Billing Started") {
            @Override
            public boolean entryMatch(ProductOrderListEntry entry) {
                return entry.isBilling();
            }

            @Override
            public Predicate buildPredicate(CriteriaBuilder criteriaBuilder,
                                            SetJoin<ProductOrderSample, LedgerEntry> sampleLedgerEntryJoin,
                                            Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin) {
                // The session is NOT null, but the session's billed date IS null.
                return criteriaBuilder.and(
                        criteriaBuilder.isNull(ledgerEntryBillingSessionJoin.get(BillingSession_.billedDate)),
                        criteriaBuilder.isNotNull(sampleLedgerEntryJoin.get(LedgerEntry_.billingSession)),
                        criteriaBuilder.isNull(sampleLedgerEntryJoin.get(LedgerEntry_.autoLedgerTimestamp)));
            }

            @Override
            public void updateEntryCount(ProductOrderListEntry entry, Long count) {
                // Nothing to do.
            }
        };

        private final String displayName;

        public final boolean canCreateQuery;

        LedgerStatus(String displayName, boolean canCreateQuery) {
            this.displayName = displayName;
            this.canCreateQuery = canCreateQuery;
        }

        LedgerStatus(String displayName) {
            this(displayName, true);
        }

        /**
         * @return true if the entry matches this status.
         */
        public abstract boolean entryMatch(ProductOrderListEntry entry);

        /**
         * Get all status values using the name strings.
         *
         * @param statusStrings The desired list of statuses.
         *
         * @return The statuses that are listed.
         */
        public static List<LedgerStatus> getFromNames(List<String> statusStrings) {
            if (CollectionUtils.isEmpty(statusStrings)) {
                return Collections.emptyList();
            }

            List<LedgerStatus> statuses = new ArrayList<>(statusStrings.size());
            for (String statusString : statusStrings) {
                statuses.add(LedgerStatus.valueOf(statusString));
            }

            return statuses;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return the criteria predicate that selects for PDOs with this ledger status.
         */
        public abstract Predicate buildPredicate(CriteriaBuilder criteriaBuilder,
                                                 SetJoin<ProductOrderSample, LedgerEntry> sampleLedgerEntryJoin,
                                                 Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin);

        /**
         * Update the count in the product order for this status.
         */
        public abstract void updateEntryCount(ProductOrderListEntry entry, Long count);
    }
}
