package org.broadinstitute.gpinformatics.athena.control.dao.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.*;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database interactions involving Billing Ledger
 */
@Stateful
@RequestScoped
public class LedgerEntryDao extends GenericDao {

    private enum BillingSessionInclusion { ALL, NO_SESSION_STARTED, SESSION_STARTED, SESSION_BILLED }

    /**
     * Fetch the billed LedgerEntries with null quote IDs, join fetching their associated {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}s
     * for performance purposes (i.e. avoiding singleton-selects of what are probably nearly unique
     * {@link javax.persistence.ManyToOne} ProductOrderSamples).
     *
     * @return List of billed Ledger Entries with null quote IDs.
     */
    public List<LedgerEntry> findSuccessfullyBilledLedgerEntriesWithoutQuoteId() {
        return findAll(LedgerEntry.class, new GenericDaoCallback<LedgerEntry>() {
            @Override
            public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {

                CriteriaBuilder cb = getCriteriaBuilder();

                Join<LedgerEntry, BillingSession> ledgerBillingSessionJoin = root.join(LedgerEntry_.billingSession);

                criteriaQuery.where(
                        // Filter out ledgers that already have a quote assigned.
                        cb.isNull(root.get(LedgerEntry_.quoteId)),
                        // A predicate for billed ledgers only as we want unbilled ledgers to keep their null quote IDs
                        // until billing.  The predicate for this purpose is that the ledgers' billing session must
                        // exist (this is an inner join) and have a non-null billed date.  Note that this is not
                        // explicitly testing for *successful* billing (see comments in LedgerEntryDao), just a
                        // billing session with a billed date.
                        cb.isNotNull(ledgerBillingSessionJoin.get(BillingSession_.billedDate)),
                        // Only select ledger entries that were successfully billed.
                        cb.equal(root.get(LedgerEntry_.billingMessage), BillingSession.SUCCESS));

                // This runs very slowly without this fetch as we would singleton select thousands of ProductOrderSamples.
                root.fetch(LedgerEntry_.productOrderSample);
            }
        });
    }

    /**
     * Fetch the billed LedgerEntries so we can populate any new data.
     *
     * @return List of entries that were billed to the quote server
     */
    public List<LedgerEntry> findAllBilledEntries() {
        return findAll(LedgerEntry.class, new GenericDaoCallback<LedgerEntry>() {
            @Override
            public void callback(CriteriaQuery<LedgerEntry> criteriaQuery, Root<LedgerEntry> root) {

                CriteriaBuilder cb = getCriteriaBuilder();

                Join<LedgerEntry, BillingSession> ledgerBillingSessionJoin = root.join(LedgerEntry_.billingSession);

                // If it has a session and there is a quote, then this was billed to the quote server
                criteriaQuery.where(
                        cb.isNotNull(root.get(LedgerEntry_.quoteId)),
                        cb.isNotNull(ledgerBillingSessionJoin.get(BillingSession_.billedDate)));

                // This runs very slowly without this fetch as we would singleton select thousands of ProductOrderSamples.
                root.fetch(LedgerEntry_.productOrderSample);
            }
        });
    }

    public List<LedgerEntry> findAll() {
        return findAll(LedgerEntry.class);
    }

    /**
     * Version of the method that should work with either {@link ProductOrder} entities xor String ProductOrder
     * business keys (one or the other must be non-null).
     *
     * @param orders The product orders to look up
     * @param productOrderBusinessKeys The business key
     * @param inclusion include session or not
     *
     * @return The ledger items.
     */
    private Set<LedgerEntry> findByOrderList(
        @Nullable ProductOrder [] orders,
        @Nullable List<String> productOrderBusinessKeys,
        BillingSessionInclusion inclusion) {

        if (orders == null && productOrderBusinessKeys == null) {
            throw new RuntimeException("Must provide Product Order entities or business keys");
        }

        if (orders != null && productOrderBusinessKeys != null) {
            throw new RuntimeException("Cannot provide both Product Order entities and business keys");
        }

        if ((orders != null && orders.length == 0) || (productOrderBusinessKeys != null && productOrderBusinessKeys.isEmpty())) {
            return Collections.emptySet();
        }

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LedgerEntry> criteriaQuery = criteriaBuilder.createQuery(LedgerEntry.class);

        Root<LedgerEntry> ledgerRoot = criteriaQuery.from(LedgerEntry.class);
        Join<LedgerEntry, ProductOrderSample> orderSample = ledgerRoot.join(LedgerEntry_.productOrderSample);
        Join<LedgerEntry, BillingSession> billingSession = ledgerRoot.join(LedgerEntry_.billingSession, JoinType.LEFT);

        // choose the appropriate predicate depending on whether we are passed ProductOrder entities or business keys
        Predicate orderInPredicate;
        if (orders != null) {
            orderInPredicate = orderSample.get(ProductOrderSample_.productOrder).in((Object[])orders);
        } else {
            Join<ProductOrderSample, ProductOrder> productOrderSampleProductOrderJoin = orderSample.join(ProductOrderSample_.productOrder);
            orderInPredicate = productOrderSampleProductOrderJoin.get(ProductOrder_.jiraTicketKey).in(productOrderBusinessKeys);
        }

        Predicate fullPredicate;
        if (inclusion == BillingSessionInclusion.NO_SESSION_STARTED) {
            // If the session is null, then this is uploaded and no session has started
            Predicate noSession = ledgerRoot.get(LedgerEntry_.billingSession).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, noSession);
        } else if (inclusion == BillingSessionInclusion.SESSION_STARTED) {
            // If there is no billed date, the session is not closed. If there is no session, then it is only uploaded, not started.
            Predicate hasSession = ledgerRoot.get(LedgerEntry_.billingSession).isNotNull();
            Predicate notBilled = billingSession.get(BillingSession_.billedDate).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, hasSession, notBilled);
        } else if (inclusion == BillingSessionInclusion.SESSION_BILLED) {
            // The ledger entry is billed when there is a billing session AND the message is Success. This will include
            // items that have been billed, but the session has not been closed
            Predicate hasSession = ledgerRoot.get(LedgerEntry_.billingSession).isNotNull();
            Predicate isBilled = criteriaBuilder.equal(ledgerRoot.get(LedgerEntry_.billingMessage), BillingSession.SUCCESS);
            fullPredicate = criteriaBuilder.and(orderInPredicate, hasSession, isBilled);
        } else {
            fullPredicate = orderInPredicate;
        }

        criteriaQuery.where(fullPredicate);

        try {
            List<LedgerEntry> ledgerEntryList = getEntityManager().createQuery(criteriaQuery).getResultList();
                return new HashSet<LedgerEntry>(ledgerEntryList);
        } catch (NoResultException ignored) {
            return Collections.emptySet();
        }

    }

    private Set<LedgerEntry> findByOrderList(@Nonnull List<String> productOrderBusinessKeys, BillingSessionInclusion inclusion) {
        return findByOrderList(null, productOrderBusinessKeys, inclusion);
    }

    private Set<LedgerEntry> findByOrderList(@Nonnull ProductOrder[] orders, BillingSessionInclusion inclusion) {
        return findByOrderList(orders, null, inclusion);
    }

    public Set<LedgerEntry> findByOrderList(@Nonnull ProductOrder... orders) {
        return findByOrderList(orders, BillingSessionInclusion.ALL);
    }

    public Set<LedgerEntry> findWithoutBillingSessionByOrderList(@Nonnull ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.NO_SESSION_STARTED);
    }

    public Set<LedgerEntry> findWithoutBillingSessionByOrderList(@Nonnull List<String> productOrderBusinessKeys) {
        return findByOrderList(productOrderBusinessKeys, BillingSessionInclusion.NO_SESSION_STARTED);
    }

    public Set<LedgerEntry> findLockedOutByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_STARTED);
    }

    public Set<LedgerEntry> findLockedOutByOrderList(List<String> productOrderBusinessKeys) {
        return findByOrderList(productOrderBusinessKeys, BillingSessionInclusion.SESSION_STARTED);
    }

    public Set<LedgerEntry> findBilledByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_BILLED);
    }

    public void removeLedgerItemsWithoutBillingSession(ProductOrder[] orders) {
        Set<LedgerEntry> ledgerItems = findWithoutBillingSessionByOrderList(orders);

        // Remove each item from the list and delete it from the ledger
        while (!ledgerItems.isEmpty()) {
            LedgerEntry item = ledgerItems.iterator().next();
            ledgerItems.remove(item);
//            item.getProductOrderSample().getLedgerItems().remove( item );
            remove(item);
        }
    }

}
