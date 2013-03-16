package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
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

    @Index(name = "ix_ledger_price_item")
    @ManyToOne
    @JoinColumn(name = "PRICE_ITEM_ID")
    private PriceItem priceItem;

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

    /**
     * Package private constructor for JPA use.
     */
    @SuppressWarnings("UnusedDeclaration")
    LedgerEntry() {}

    public LedgerEntry(@Nonnull ProductOrderSample productOrderSample,
                       @Nonnull PriceItem priceItem,
                       @Nonnull Date workCompleteDate,
                       double quantity) {
        this.productOrderSample = productOrderSample;
        this.priceItem = priceItem;
        this.quantity = quantity;
        this.workCompleteDate = workCompleteDate;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
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
                .append(quoteId, castOther.getQuoteId())
                .append(billingSession, castOther.getBillingSession()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productOrderSample)
                .append(priceItem)
                .append(quoteId)
                .append(billingSession).toHashCode();
    }
}
