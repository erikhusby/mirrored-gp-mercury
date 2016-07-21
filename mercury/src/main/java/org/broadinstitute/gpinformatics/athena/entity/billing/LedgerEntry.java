package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * This handles the billing ledger items for product order samples
 *
 * @author hrafal
 */
@Entity
@Audited
@Table(name= "BILLING_LEDGER", schema = "athena")
public class LedgerEntry implements Serializable {
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

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @Column(name = "QUANTITY")
    private double quantity;

    @Index(name = "ix_ledger_billing_session")
    @ManyToOne
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

    /**
     * Package private constructor for JPA use.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected LedgerEntry() {}

    private LedgerEntry(@Nonnull ProductOrderSample productOrderSample,
                       @Nonnull PriceItem priceItem,
                       @Nonnull Date workCompleteDate,
                       double quantity) {
        this.productOrderSample = productOrderSample;
        this.priceItem = priceItem;
        this.quantity = quantity;
        this.workCompleteDate = workCompleteDate;
    }

    public LedgerEntry(ProductOrderSample productOrderSample,
                       PriceItem priceItem, Date workCompleteDate,
                       Product product, double quantity) {
        this(productOrderSample, priceItem, workCompleteDate, quantity);
        this.product = product;
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

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
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



    /**
     * A ledger item is billed if either its message is the success status or the session has been billed. The
     * isBillingSessionBilled method is probably not needed, since all should be success, but I don't want to
     * have to fix the db if some do not have success (HBR)
     *
     * @return true if item was billed
     */
    public boolean isBilled() {
        return isBillingSessionBilled() || (billingMessage != null && billingMessage.equals(BillingSession.SUCCESS));
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
        return BillingSession.SUCCESS.equals(billingMessage);
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof LedgerEntry)) {
            return false;
        }

        LedgerEntry castOther = (LedgerEntry) other;
        return new EqualsBuilder()
                .append(productOrderSample, castOther.getProductOrderSample())
                .append(priceItem, castOther.getPriceItem())
                .append(priceItemType, castOther.getPriceItemType())
                .append(quoteId, castOther.getQuoteId())
                .append(billingSession, castOther.getBillingSession())
                .append(product, castOther.getProduct()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productOrderSample)
                .append(priceItem)
                .append(priceItemType)
                .append(quoteId)
                .append(billingSession)
                .append(product).toHashCode();
    }

    public Date getBucketDate() {
        return billingSession.getBucketDate(workCompleteDate);
    }

    public Product getProduct() {
        return product;
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
