package org.broadinstitute.gpinformatics.athena.control.dao.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database interactions involving Billing Ledger
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LedgerEntryDao extends GenericDao {

    public enum BillingSessionInclusion { ALL, NO_SESSION_STARTED, SESSION_STARTED, SESSION_BILLED, CONFIRMATION_UPLOAD }

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
        } else if (inclusion == BillingSessionInclusion.CONFIRMATION_UPLOAD) {
            // If there is no session AND the auto timestamp is null, an upload had to have been performed.
            Predicate noSession = ledgerRoot.get(LedgerEntry_.billingSession).isNull();
            Predicate noAutoBill = ledgerRoot.get(LedgerEntry_.autoLedgerTimestamp).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, noSession, noAutoBill);
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
                return new HashSet<>(ledgerEntryList);
        } catch (NoResultException ignored) {
            return Collections.emptySet();
        }
    }

    public Set<LedgerEntry> findByOrderList(@Nonnull List<String> productOrderBusinessKeys, BillingSessionInclusion inclusion) {
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

    public Set<LedgerEntry> findWithoutBillingSessionByOrderList(@Nonnull List<String> productOrderBusinessKeys,
                                                                 @Nonnull List<String> errorMessages) {
        Set<LedgerEntry> byOrderList =
                findByOrderList(productOrderBusinessKeys, BillingSessionInclusion.NO_SESSION_STARTED);


        List<Long> pdoIds = new ArrayList<>();
        List<LedgerEntry> entriesToRemove = new ArrayList<>();

        for (LedgerEntry ledgerEntry : byOrderList) {

            // Note: This was added as a Just In Case error check from the code that started allowing submitted
            // PDOs to have a null quote. I haven't been able to get a PDO where this state occurs without modifying
            // the database, but wanted there to be a nice error message here instead of an NPE at a later time.
            if (ledgerEntry.getProductOrderSample().getProductOrder().getQuoteId() == null) {
                if (pdoIds.add(ledgerEntry.getProductOrderSample().getProductOrder().getProductOrderId())) {

                    errorMessages.add(ledgerEntry.getProductOrderSample().getProductOrder().getBusinessKey()
                                      + " has been excluded as there is no Quote ID selected.");
                }
                entriesToRemove.add(ledgerEntry);
            }
        }
        byOrderList.removeAll(entriesToRemove);
        return byOrderList;
    }

    public Set<LedgerEntry> findUploadedUnbilledOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.CONFIRMATION_UPLOAD);
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
