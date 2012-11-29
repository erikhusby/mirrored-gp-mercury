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

    public enum RemoveStatus {
        AllRemoved,
        SomeRemoved,
        NoneRemoved
    }

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

    public List<QuoteImportItem> getUnBilledQuoteImportItems() {
        return getQuoteImportItems(false);
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        return getQuoteImportItems(true);
    }

    private List<QuoteImportItem> getQuoteImportItems(boolean includeUnbilled) {
        QuoteImportInfo quoteImportInfo = new QuoteImportInfo();

        for (BillingLedger ledger : billingLedgerItems) {

            // If we are not skipping unbilled then just add quantity, otherwise only include if
            // the message is null or not equal to success
            if (includeUnbilled ||
                (ledger.getBillingMessage() == null) ||
                !SUCCESS.equals(ledger.getBillingMessage()))
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

    public RemoveStatus cancelSession() {

        List<BillingLedger> toRemove = new ArrayList<BillingLedger>();

        RemoveStatus status = RemoveStatus.NoneRemoved;
        for (BillingLedger ledgerItem : billingLedgerItems) {

            // If any item is billed then allRemoved is false and we do not want to remove the item
            // In here we remove the billing session from the ledger item and hold onto the ledger item
            // to remove from the full list of ledger items.
            if (!SUCCESS.equals(ledgerItem.getBillingMessage())) {
                ledgerItem.setBillingSession(null);
                toRemove.add(ledgerItem);

                // If it is SomeRemoved, then there is some success, so only set all removed if this is the first
                // item or if it was previously all removed anyway.
                if (status != RemoveStatus.SomeRemoved) {
                    status = RemoveStatus.AllRemoved;
                }
            } else {
                // clear out the OK message
                ledgerItem.setBillingMessage(null);

                // If this IS none removed, then success indicates we are still none removed. Otherwise the
                // state is all or some, which either way, with a success means Some!
                if (status != RemoveStatus.NoneRemoved) {
                    status = RemoveStatus.SomeRemoved;
                }
            }
        }

        // Remove all items that do not have billing dates
        billingLedgerItems.removeAll(toRemove);

        return status;
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
        Set<ProductOrder> uniqueProductOrders = new HashSet<ProductOrder>();
        for (BillingLedger billingLedger : billingLedgerItems) {
            uniqueProductOrders.add(billingLedger.getProductOrderSample().getProductOrder());
        }

        // return it as an array
        return uniqueProductOrders.toArray(new ProductOrder[uniqueProductOrders.size()]);
    }
}
