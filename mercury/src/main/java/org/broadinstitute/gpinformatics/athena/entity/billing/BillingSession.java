package org.broadinstitute.gpinformatics.athena.entity.billing;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportInfo;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This handles the billing session.
 *
 */
@Entity
@Audited
@Table(name= "BILLING_SESSION", schema = "athena")
public class BillingSession implements Serializable {
    private static final long serialVersionUID = -5063307042006128046L;

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

    @Column(name="BILLING_SESSION_TYPE")
    @Enumerated(EnumType.STRING)
    private BillingSessionType billingSessionType;

    // Do NOT cascade removes because we want the ledger items to stay, but just have their billing session removed.
    @OneToMany(mappedBy = "billingSession", cascade = {CascadeType.PERSIST})
    private List<LedgerEntry> ledgerEntryItems;

    @Column(name="BILLING_SESSION_STATUS")
    @Enumerated(EnumType.STRING)
    private BillingSessionStatusType status;

    @Version
    private Long version;

    // Do NOT use eager fetches on this class unless you verify (via hibernate logging) that the pessimistic locking
    // required by BillingSessionDao will not result in eagerly fetched tables having "for update" database locks
    // applied to them

    protected BillingSession() {}

    public BillingSession(@Nonnull Long createdBy, Set<LedgerEntry> ledgerItems) {
        this.createdBy = createdBy;
        createdDate = new Date();
        for (LedgerEntry ledgerItem : ledgerItems) {
            ledgerItem.setBillingSession(this);
        }

        ledgerEntryItems = new ArrayList<>(ledgerItems);

        // Anything new is a daily rollup.
        billingSessionType = BillingSessionType.ROLLUP_DAILY;
    }

    /**
     * This is a 'special' constructor to recreate a billing session that may have been deleted. It is being built for
     * fixup tests. The billing session is considered billed even though nothing has gone to the quote server.
     *
     * @param billedDate If the desire is to set this as already billed (if something was billed already
     *                   in the quote server). If null, this is left open to bill and end.
     * @param createdBy The user who is creating this.
     * @param ledgerItems All the ledger entries that will be added to this session.
     */
    BillingSession(@Nullable Date billedDate, @Nonnull Long createdBy, Set<LedgerEntry> ledgerItems) {
        this(createdBy, ledgerItems);
        this.billedDate = billedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    @SuppressWarnings("UnusedDeclaration")
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

    public Long getBillingSessionId() {
        return billingSessionId;
    }

    public BillingSessionStatusType getStatus() {
        return status;
    }

    public void setStatus(BillingSessionStatusType status) {
        this.status = status;
    }

    /**
     * TODO SGM Javadoc
     * @return
     */
    public boolean isSessionLocked() {
        return ((status != null) && (status== BillingSessionStatusType.LOCKED_FOR_BILLING));
    }

    /**
     * TODO SGM Javadoc
     */
    public void lockSession() {
        status = BillingSessionStatusType.LOCKED_FOR_BILLING;
    }

    /**
     * TODO SGM Javadoc
     */
    public void unlockSession() {
        status = BillingSessionStatusType.UNLOCKED;
    }

    /**
     * @return A list of only the unbilled quote items for this session.
     */
    public List<QuoteImportItem> getUnBilledQuoteImportItems(PriceListCache priceListCache) {
        return getQuoteImportItems(priceListCache, false);
    }

    /**
     * @return A list of all the quote items for this session.
     */
    public List<QuoteImportItem> getQuoteImportItems(PriceListCache priceListCache) {
        return getQuoteImportItems(priceListCache, true);
    }

    private List<QuoteImportItem> getQuoteImportItems(PriceListCache priceListCache, boolean includeAll) {
        QuoteImportInfo quoteImportInfo = new QuoteImportInfo();

        for (LedgerEntry ledger : ledgerEntryItems) {
            // If we are including all, or if the item isn't billed, then add quantity.
            if (includeAll || !ledger.isBilled()) {
                quoteImportInfo.addQuantity(ledger);
            }
        }

        return quoteImportInfo.getQuoteImportItems(priceListCache);
    }

    public boolean cancelSession() {
        List<LedgerEntry> toRemove = new ArrayList<>();

        for (LedgerEntry ledgerItem : ledgerEntryItems) {
            if (!ledgerItem.isBilled()) {
                // Remove the billing session from the ledger item and hold onto the ledger item
                // to remove from the full list of ledger items.
                ledgerItem.removeFromSession();
                toRemove.add(ledgerItem);
            }
        }

        // Remove all items that do not have billing dates.
        ledgerEntryItems.removeAll(toRemove);

        boolean allRemoved = ledgerEntryItems.isEmpty();

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

    public Set<String> getProductOrderBusinessKeys() {
        // Get all product order keys across all ledger items, removing duplicates.
        Set<String> orderKeys = new HashSet<>();
        for (LedgerEntry ledgerEntry : ledgerEntryItems) {
            orderKeys.add(ledgerEntry.getProductOrderSample().getProductOrder().getBusinessKey());
        }
        return orderKeys;
    }

    public List<LedgerEntry> getLedgerEntryItems() {
        return ledgerEntryItems;
    }

    public Date getBucketDate(Date workCompleteDate) {
        return billingSessionType.getBucketDate(workCompleteDate);
    }

    public BillingSessionType getBillingSessionType() {
        return billingSessionType;
    }

    public void setBillingSessionType(BillingSessionType billingSessionType) {
        this.billingSessionType = billingSessionType;
    }

    /**
     * The session type supplies the method for rolling up a date into an appropriate bucket.
     */
    public enum BillingSessionType {
        ROLLUP_SEMI_MONTHLY(new DateRollupCalculator() {
            @Override
            public Date getBucketDate(Date workCompleteDate) {
                Calendar cal = new GregorianCalendar();
                cal.setTime(workCompleteDate);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                Calendar endOfPeriod = Calendar.getInstance();
                endOfPeriod.setTime(workCompleteDate);
                if (day <= 15) {
                    endOfPeriod.set(Calendar.DAY_OF_MONTH, 15);
                } else {
                    endOfPeriod.set(Calendar.DAY_OF_MONTH, endOfPeriod.getActualMaximum(Calendar.DAY_OF_MONTH));
                }

                return DateUtils.getEndOfDay(endOfPeriod.getTime());
            }
        }),
        ROLLUP_DAILY(new DateRollupCalculator() {
            @Override
            public Date getBucketDate(Date workCompleteDate) {
                Calendar endOfPeriod = Calendar.getInstance();
                endOfPeriod.setTime(workCompleteDate);

                // Set to the end of the day so anything that is ever sent with time will normalize to the same bucket.
                return DateUtils.getEndOfDay(endOfPeriod.getTime());
            }
        });

        private final DateRollupCalculator rollupCalculator;

        BillingSessionType(DateRollupCalculator rollupCalculator) {
            this.rollupCalculator = rollupCalculator;
        }

        public Date getBucketDate(Date workCompleteDate) {
            return rollupCalculator.getBucketDate(workCompleteDate);
        }
    }

    public enum BillingSessionStatusType {
        LOCKED_FOR_BILLING, UNLOCKED, CLOSED;
    }
}
