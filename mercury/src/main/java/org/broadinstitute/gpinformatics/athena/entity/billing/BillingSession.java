package org.broadinstitute.gpinformatics.athena.entity.billing;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportInfo;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This handles the billing session
 *
 * @author hrafal
 */
@Entity
@Audited
@Table(name= "BILLING_SESSION", schema = "athena")
public class BillingSession {
    public static final String ID_PREFIX = "BILL-";

    @Id
    @SequenceGenerator(name = "SEQ_BILLING_SESSION", schema = "athena", sequenceName = "SEQ_BILLING_SESSION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BILLING_SESSION")
    @Column(name = "BILLING_SESSION_ID")
    private Long billingSessionId;

    @Column(name="CREATED_DATE")
    private Date createdDate;

    @Column(name="CREATED_BY")
    private Long createdBy;

    @Column(name="BILLED_DATE")
    private Date billedDate;

    @OneToMany(mappedBy = "billingSession", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    public Set<BillingLedger> billingLedgerItems = new HashSet<BillingLedger> ();

    BillingSession() {}

    public BillingSession(@Nonnull Long createdBy) {
        this.createdBy = createdBy;
        this.createdDate = new Date();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getBilledDate() {
        return billedDate;
    }

    public void setBilledDate(Date billedDate) {
        this.billedDate = billedDate;
    }

    public String getBusinessKey() {
        return ID_PREFIX + billingSessionId;
    }

    @Transient
    public List<QuoteImportItem> getQuoteImportItems() {
        QuoteImportInfo quoteImportInfo = new QuoteImportInfo();

        for (BillingLedger ledger : billingLedgerItems) {
            String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();
            PriceItem priceItem = ledger.getPriceItem();
            Double quantity = ledger.getQuantity();
            quoteImportInfo.addQuantity(quoteId, priceItem, quantity);
        }

        return quoteImportInfo.getQuoteImportItems();
    }

    public void setBillingLedgerItems(Set<BillingLedger> newBillingLedgerItems) {
        for (BillingLedger ledgerItem : newBillingLedgerItems) {
            ledgerItem.setBillingSession(this);
        }
    }

    public void cancelSession() {
        billingLedgerItems.clear();
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof BillingSession) ) {
            return false;
        }

        BillingSession castOther = (BillingSession) other;
        return new EqualsBuilder()
                .append(getBusinessKey(), castOther.getBusinessKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getBusinessKey()).toHashCode();
    }
}
