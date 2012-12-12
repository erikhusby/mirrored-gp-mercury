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
    public static final String SUCCESS = "Billed Successfully";

    @Id
    @SequenceGenerator(name = "SEQ_BILLING_SESSION", schema = "athena", sequenceName = "SEQ_BILLING_SESSION", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BILLING_SESSION")
    @Column(name = "BILLING_SESSION_ID")
    private Long billingSessionId;

    @Column(name="CREATED_DATE")
    private Date createdDate;

    @Column(name="CREATED_BY")
    private Long createdBy;

    @Column(name="BILLED_DATE")
    private Date billedDate;

    // Do NOT cascade removes because we want the ledger items to stay, but just have their billing session removed.
    @OneToMany(mappedBy = "billingSession", cascade = {CascadeType.PERSIST})
    private Set<BillingLedger> billingLedgerItems;

    BillingSession() {}

    public BillingSession(@Nonnull Long createdBy, Set<BillingLedger> ledgerItems) {
        this.createdBy = createdBy;
        createdDate = new Date();
        for (BillingLedger ledgerItem : ledgerItems) {
            ledgerItem.setBillingSession(this);
        }

        billingLedgerItems = new HashSet<BillingLedger>(ledgerItems);
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

    /**
     * @return A list of only the unbilled quote items for this session.
     */
    public List<QuoteImportItem> getUnBilledQuoteImportItems() {
        return getQuoteImportItems(false);
    }

    /**
     * @return A list of all the quote items for this session.
     */
    public List<QuoteImportItem> getQuoteImportItems() {
        return getQuoteImportItems(true);
    }

    private List<QuoteImportItem> getQuoteImportItems(boolean includeAll) {
        QuoteImportInfo quoteImportInfo = new QuoteImportInfo();

        for (BillingLedger ledger : billingLedgerItems) {
            // If we are including all, or if the item isn't billed, then add quantity.
            if (includeAll || !ledger.isBilled()) {
                quoteImportInfo.addQuantity(ledger);
            }
        }

        return quoteImportInfo.getQuoteImportItems();
    }

    public boolean cancelSession() {
        List<BillingLedger> toRemove = new ArrayList<BillingLedger>();

        for (BillingLedger ledgerItem : billingLedgerItems) {
            if (!ledgerItem.isBilled()) {
                // Remove the billing session from the ledger item and hold onto the ledger item
                // to remove from the full list of ledger items.
                ledgerItem.removeFromSession();
                toRemove.add(ledgerItem);
            }
        }

        // Remove all items that do not have billing dates.
        billingLedgerItems.removeAll(toRemove);

        boolean allRemoved = billingLedgerItems.isEmpty();

        if (!allRemoved) {
            // Anything that has been billed will be attached to this session and those are now ALL billed.
            setBilledDate(new Date());
        }

        return allRemoved;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BillingSession)) {
            return false;
        }

        BillingSession castOther = (BillingSession) other;
        return new EqualsBuilder().append(getBusinessKey(), castOther.getBusinessKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getBusinessKey()).toHashCode();
    }

    public List<String> getProductOrderBusinessKeys() {
        // Get all unique product Orders across all ledger items
        Set<ProductOrder> productOrders = new HashSet<ProductOrder>();
        for (BillingLedger billingLedger : billingLedgerItems) {
            productOrders.add(billingLedger.getProductOrderSample().getProductOrder());
        }

        List<String> ret = new ArrayList<String>(productOrders.size());
        for (ProductOrder productOrder : productOrders) {
            ret.add(productOrder.getBusinessKey());
        }

        return ret;
    }
}
