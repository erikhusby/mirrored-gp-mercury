package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.StreamUtils;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This handles the billing ledger items for product order samples
 *
 * @author hrafal
 */
@Entity
@Audited
@Table(name= "BILLING_LEDGER", schema = "athena")
public class LedgerEntry implements Serializable {
    /**
     * Date format used for displaying DCFM and the value of Date Complete inputs.
     */
    public static final String BILLING_LEDGER_DATE_FORMAT = "MMM d, yyyy";
    private static final long serialVersionUID = -4740767648087018522L;

    @Id
    @SequenceGenerator(name = "SEQ_LEDGER", schema = "athena", sequenceName = "SEQ_LEDGER", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LEDGER")
    private Long ledgerId;

    @Index(name = "ix_ledger_order_sample")
    @ManyToOne
    @JoinColumn(name = "PRODUCT_ORDER_SAMPLE_ID")
    private ProductOrderSample productOrderSample;

    // The auto ledger timestamp is set whenever the auto biller creates a ledger entry. This is used by the order
    // list page to show PDMs that they have items to review and upload. When the upload happens, the timestamp
    // is removed to indicate that auto billing cannot happen for this PDO any more.
    @Column(name = "AUTO_LEDGER_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date autoLedgerTimestamp;

    @Index(name = "ix_ledger_price_item")
    @ManyToOne
    @JoinColumn(name = "PRICE_ITEM_ID")
    private PriceItem priceItem;

    @Column(name = "QUANTITY")
    private BigDecimal quantity = BigDecimal.ZERO;

    @Index(name = "ix_ledger_billing_session")
    @ManyToOne
    @JoinColumn(name="BILLING_SESSION")
    private BillingSession billingSession;

    @Column(name = "WORK_COMPLETE_DATE")
    private Date workCompleteDate;

    @Column(name = "BILLING_MESSAGE")
    private String billingMessage;

    @Column(name ="QUOTE_ID")
    private String quoteId;

    @Column(name = "PRICE_ITEM_TYPE")
    @Enumerated(EnumType.STRING)
    private PriceItemType priceItemType;

    // work item id handed back from the quote server
    @Column(name = "QUOTE_SERVER_WORK_ITEM")
    private String workItem;

    @Column(name = "SAP_DELIVERY_DOCUMENT_ID")
    private String sapDeliveryDocumentId;

    @Column(name = "SAP_RETURN_ORDER_ID")
    private String sapReturnOrderId;

    @ManyToOne
    @JoinColumn(name = "SAP_ORDER_DETAIL_ID")
    private SapOrderDetail sapOrderDetail;

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @Column(name = "SAP_REPLACEMENT_PRICING")
    private Boolean sapReplacementPricing = Boolean.FALSE;

    public static final Predicate<LedgerEntry> IS_SUCCESSFULLY_BILLED = LedgerEntry::isSuccessfullyBilled;

    /**
     * Package private constructor for JPA use.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected LedgerEntry() {}

    public LedgerEntry(@Nonnull ProductOrderSample productOrderSample,
                       PriceItem priceItem,
                       @Nonnull Date workCompleteDate,
                       BigDecimal quantity) {
        this.productOrderSample = productOrderSample;
        this.priceItem = priceItem;
        this.quantity = quantity;
        this.workCompleteDate = workCompleteDate;
    }

    public LedgerEntry(@Nonnull ProductOrderSample productOrderSample,
                       Product product,
                       @Nonnull Date workCompleteDate,
                       BigDecimal quantity) {
        this.productOrderSample = productOrderSample;
        this.product = product;
        this.quantity = quantity;
        this.workCompleteDate = workCompleteDate;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public Date getAutoLedgerTimestamp() {
        return autoLedgerTimestamp;
    }

    public void setAutoLedgerTimestamp(Date autoLedgerTimestamp) {
        this.autoLedgerTimestamp = autoLedgerTimestamp;
    }

    /**
     * Should only be used by test code
     */
    public void setPriceItem(PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BillingSession getBillingSession() {
        return billingSession;
    }

    public void setBillingSession(BillingSession billingSession) {
        this.billingSession = billingSession;
    }

    public Date getWorkCompleteDate() {
        return workCompleteDate;
    }

    public void setWorkCompleteDate(Date workCompleteDate) {
        this.workCompleteDate = workCompleteDate;
    }

    public String getBillingMessage() {
        return billingMessage;
    }

    public void setBillingMessage(String billingMessage) {
        this.billingMessage = billingMessage;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Boolean getSapReplacementPricing() {
        return sapReplacementPricing;
    }

    public boolean hasSapReplacementPricing() {
        return sapReplacementPricing != null && sapReplacementPricing;
    }

    public void setSapReplacementPricing(Boolean sapReplacementCondition) {
        this.sapReplacementPricing = sapReplacementCondition;
    }

    /**
     * A ledger item is billed if either its message is the success status or the session has been billed. The
     * isBillingSessionBilled method is probably not needed, since all should be success, but I don't want to
     * have to fix the db if some do not have success (HBR)
     *
     * @return true if item was billed
     */
    public boolean isBilled() {
        return isBillingSessionBilled() || (isSuccessfullyBilled());
    }

    /**
     * @return If the billing session is billed, then this item is part of a fully, and successfully billed session.
     */
    private boolean isBillingSessionBilled() {
        return (billingSession != null) && (billingSession.getBilledDate() != null);
    }

    /**
     * Tells whether or not this ledger entry is currently being billed (is included in an incomplete billing session).
     *
     * @return true if this ledger entry is being billed; false if it is ready to bill or has been successfully billed
     */
    public boolean isBeingBilled() {
        return billingSession != null && !billingSession.isComplete();
    }

    /**
     * This ledger entry has a positively successful billing message.
     */
    public boolean isSuccessfullyBilled() {
        return BillingSession.SUCCESS.equals(billingMessage) || BillingSession.BILLING_CREDIT.equals(billingMessage);
    }

    public boolean isCredit() {
        return quantity < 0;
    }

    public boolean isCredited() {
        return StringUtils.isNotBlank(sapReturnOrderId);
    }

    /**
     * When 'this' LedgerEntry is for a credit, findCreditSource() finds which LedgerEntry(s) are the 'positive'
     * ledgers from which to deduct.
     *
     * @return a Map<LedgerEntry, Double> which represents the quantities to be deducted for each LedgerEntry.
     */
    public Map<LedgerEntry, Double> findCreditSource(){
        Predicate<LedgerEntry> hasDeliveryDocument =
            ledgerEntry -> StringUtils.isNotBlank(ledgerEntry.getSapDeliveryDocumentId());

        double ledgerQuantities =
            getProductOrderSample().getLedgerItems().stream()
                .filter(hasDeliveryDocument).mapToDouble(LedgerEntry::getQuantity).sum();

        if (!isCredit() || isCredited() || ledgerQuantities < 0) {
            return Collections.emptyMap();
        }
        Predicate<LedgerEntry> isNotCredited = StreamUtils.not(LedgerEntry::isCredited);
        Map<LedgerEntry, Double> creditSources = getProductOrderSample().getLedgerItems().stream()
            .filter(IS_SUCCESSFULLY_BILLED.and(isNotCredited))
            .collect(Collectors.groupingBy(ledger->ledger,Collectors.summingDouble(LedgerEntry::getQuantity)));
        return creditSources;

    }
    /**
     * This ledger has a billing message but it is not the success message, which means it is an error message.
     */
    public boolean isUnsuccessfullyBilled() {
        return billingMessage != null && !BillingSession.SUCCESS.equals(billingMessage);
    }

    public void removeFromSession() {
        billingSession = null;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public PriceItemType getPriceItemType() {
        return priceItemType;
    }

    public void setPriceItemType(PriceItemType priceItemType) {
        this.priceItemType = priceItemType;
    }

    public String getWorkItem() {
        return workItem;
    }

    public void setWorkItem(String workItem) {
        this.workItem = workItem;
    }

    public String getSapDeliveryDocumentId() {
        return sapDeliveryDocumentId;
    }

    public void setSapDeliveryDocumentId(String sapDeliveryDocumentId) {
        this.sapDeliveryDocumentId = sapDeliveryDocumentId;
    }

    public String getSapReturnOrderId() {
        return sapReturnOrderId;
    }

    public void setSapReturnOrderId(String sapReturnOrderId) {
        this.sapReturnOrderId = sapReturnOrderId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof LedgerEntry)) {
            return false;
        }

        LedgerEntry castOther = (LedgerEntry) other;
        EqualsBuilder ledgerEntryEqualsBuilder = new EqualsBuilder()
                .append(productOrderSample, castOther.getProductOrderSample())
                .append(priceItem, castOther.getPriceItem())
                .append(priceItemType, castOther.getPriceItemType())
                .append(quoteId, castOther.getQuoteId())
                .append(billingSession, castOther.getBillingSession());

        ledgerEntryEqualsBuilder.append(product, castOther.getProduct());
        return ledgerEntryEqualsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder ledgerEntryHashcodeBuilder = new HashCodeBuilder()
                .append(productOrderSample)
                .append(priceItem)
                .append(priceItemType)
                .append(quoteId)
                .append(billingSession);
        ledgerEntryHashcodeBuilder.append(product);
        return ledgerEntryHashcodeBuilder.toHashCode();
    }

    public Date getBucketDate() {
        Date bucketDate = null;
        if(billingSession != null) {
            bucketDate = billingSession.getBucketDate(workCompleteDate);
        }
        return bucketDate;    }

    public void setSapOrderDetail(SapOrderDetail sapOrderDetail) {
        this.sapOrderDetail = sapOrderDetail;
    }

    public SapOrderDetail getSapOrderDetail() {
        return sapOrderDetail;
    }

    public Set<LedgerEntry> getPreviouslyBilled() {
        return productOrderSample.getLedgerItems().stream().filter(IS_SUCCESSFULLY_BILLED)
            .collect(Collectors.toSet());
    }

    /**
     * The price item status on the ledger entry.
     */
    public enum PriceItemType {
        PRIMARY_PRICE_ITEM("Quote Item"),
        REPLACEMENT_PRICE_ITEM("Replacement Item"),
        ADD_ON_PRICE_ITEM("Quote Item");

        private final String quoteType;

        PriceItemType(String quoteType) {
            this.quoteType = quoteType;
        }

        public String getQuoteType() {
            return quoteType;
        }
    }
}
