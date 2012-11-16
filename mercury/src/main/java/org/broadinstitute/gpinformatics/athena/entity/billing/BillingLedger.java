package org.broadinstitute.gpinformatics.athena.entity.billing;

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
    private ProductOrderSample productOrderSample;

    @Index(name = "ix_ledger_price_item")
    @ManyToOne
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
}
