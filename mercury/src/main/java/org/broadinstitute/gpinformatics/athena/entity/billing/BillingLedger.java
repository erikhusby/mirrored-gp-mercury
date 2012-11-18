package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;

/**
 * This handles the billing ledger items for product order samples
 *
 * @author hrafal
 */
@Entity
@Audited
@Table(name= "BILLING_LEDGER", schema = "athena")
public class BillingLedger {
    @Id
    @SequenceGenerator(name = "SEQ_LEDGER", schema = "athena", sequenceName = "SEQ_LEDGER")
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

    BillingLedger() {}

    public BillingLedger(@Nonnull ProductOrderSample productOrderSample, @Nonnull PriceItem priceItem, double quantity) {
        this.productOrderSample = productOrderSample;
        this.priceItem = priceItem;
        this.quantity = quantity;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public void setProductOrderSample(ProductOrderSample productOrderSample) {
        this.productOrderSample = productOrderSample;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BillingSession getBillingSession() {
        return billingSession;
    }

    public void setBillingSession(BillingSession billingSession) {
        this.billingSession = billingSession;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof BillingLedger) ) {
            return false;
        }

        BillingLedger castOther = (BillingLedger) other;
        return new EqualsBuilder()
                .append(productOrderSample, castOther.getProductOrderSample())
                .append(priceItem, castOther.getPriceItem())
                .append(billingSession, castOther.getBillingSession()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productOrderSample)
                .append(priceItem)
                .append(billingSession).toHashCode();
    }
}
