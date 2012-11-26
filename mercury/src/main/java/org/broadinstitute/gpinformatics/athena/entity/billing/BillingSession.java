package org.broadinstitute.gpinformatics.athena.entity.billing;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportInfo;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.*;

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

    // Do NOT cascadee removes because we want the ledger items to stay, but just have their billing session removed
    @OneToMany(mappedBy = "billingSession", cascade = {CascadeType.PERSIST})
    private Set<BillingLedger> billingLedgerItems = new HashSet<BillingLedger> ();

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
            quoteImportInfo.addQuantity(ledger);
        }

        return quoteImportInfo.getQuoteImportItems();
    }

    public void setBillingLedgerItems(Set<BillingLedger> newBillingLedgerItems) {
        for (BillingLedger ledgerItem : newBillingLedgerItems) {
            ledgerItem.setBillingSession(this);
        }

        billingLedgerItems.clear();
        billingLedgerItems.addAll(newBillingLedgerItems);
    }

    public boolean cancelSession() {

        List<BillingLedger> toRemove = new ArrayList<BillingLedger>();

        boolean allRemoved = true;
        for (BillingLedger ledgerItem : billingLedgerItems) {

            // If any item is billed then allRemoved is false and we do not want to remove the item
            // In here we remove the billing session from the ledger item and hold onto the ledger item
            // to remove from the full list of ledger items.
            if (ledgerItem.getWorkCompleteDate() == null) {
                ledgerItem.setBillingSession(null);
                toRemove.add(ledgerItem);
            } else {
                allRemoved = false;
            }
        }

        // Remove all items that do not have billing dates
        billingLedgerItems.removeAll(toRemove);

        return allRemoved;
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

    public List<String> getProductOrderBusinessKeys() {
        List<String> ret = new ArrayList<String>();
        for (ProductOrder productOrder : getProductOrders()) {
            ret.add(productOrder.getBusinessKey());
        }

        return ret;
    }

    /**
     * @return Get all unique product orders in the billing session
     */
    public ProductOrder[] getProductOrders() {

        // Get all unique product Orders across all ledger items
        Set<ProductOrder> unqiueProductOrders = new HashSet<ProductOrder>();
        for (BillingLedger billingLedger : billingLedgerItems) {
            unqiueProductOrders.add(billingLedger.getProductOrderSample().getProductOrder());
        }

        // return it as an array
        return unqiueProductOrders.toArray(new ProductOrder[unqiueProductOrders.size()]);
    }
}
