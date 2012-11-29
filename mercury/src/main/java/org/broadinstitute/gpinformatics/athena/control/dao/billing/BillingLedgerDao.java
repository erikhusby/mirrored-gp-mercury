package org.broadinstitute.gpinformatics.athena.control.dao.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger_;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database interactions involving Billing Ledger
 */
public class BillingLedgerDao extends GenericDao {

    private enum BillingSessionInclusion { ALL, NO_SESSION_STARTED, SESSION_STARTED, SESSION_BILLED }

    public List<BillingLedger> findAll() {
        return findAll(BillingLedger.class);
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
    private Set<BillingLedger> findByOrderList(
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
        CriteriaQuery<BillingLedger> criteriaQuery = criteriaBuilder.createQuery(BillingLedger.class);

        Root<BillingLedger> ledgerRoot = criteriaQuery.from(BillingLedger.class);
        Join<BillingLedger, ProductOrderSample> orderSample = ledgerRoot.join(BillingLedger_.productOrderSample);
        Join<BillingLedger, BillingSession> billingSession = ledgerRoot.join(BillingLedger_.billingSession, JoinType.LEFT);

        // choose the appropriate predicate depending on whether we are passed ProductOrder entities or business keys
        Predicate orderInPredicate;
        if (orders != null) {
            orderInPredicate = orderSample.get(ProductOrderSample_.productOrder).in(orders);
        } else {
            Join<ProductOrderSample, ProductOrder> productOrderSampleProductOrderJoin = orderSample.join(ProductOrderSample_.productOrder);
            orderInPredicate = productOrderSampleProductOrderJoin.get(ProductOrder_.jiraTicketKey).in(productOrderBusinessKeys);
        }

        Predicate fullPredicate;
        if (inclusion == BillingSessionInclusion.NO_SESSION_STARTED) {
            // If the session is null, then this is uploaded and no session has started
            Predicate noSession = ledgerRoot.get(BillingLedger_.billingSession).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, noSession);
        } else if (inclusion == BillingSessionInclusion.SESSION_STARTED) {
            // If there is no billed date, the session is not closed. If there is no session, then it is only uploaded, not started.
            Predicate hasSession = ledgerRoot.get(BillingLedger_.billingSession).isNotNull();
            Predicate notBilled = billingSession.get(BillingSession_.billedDate).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, hasSession, notBilled);
        } else if (inclusion == BillingSessionInclusion.SESSION_BILLED) {
            // The ledger entry is billed when there is a billing session AND the message is Success. This will include
            // items that have been billed, but the session has not been closed
            Predicate hasSession = ledgerRoot.get(BillingLedger_.billingSession).isNotNull();
            Predicate isBilled = criteriaBuilder.equal(ledgerRoot.get(BillingLedger_.billingMessage), BillingSession.SUCCESS);
            fullPredicate = criteriaBuilder.and(orderInPredicate, hasSession, isBilled);
        } else {
            fullPredicate = orderInPredicate;
        }

        criteriaQuery.where(fullPredicate);

        try {
            return new HashSet<BillingLedger>(getEntityManager().createQuery(criteriaQuery).getResultList());
        } catch (NoResultException ignored) {
            return Collections.emptySet();
        }

    }

    private Set<BillingLedger> findByOrderList(@Nonnull List<String> productOrderBusinessKeys, BillingSessionInclusion inclusion) {
        return findByOrderList(null, productOrderBusinessKeys, inclusion);
    }

    private Set<BillingLedger> findByOrderList(@Nonnull ProductOrder[] orders, BillingSessionInclusion inclusion) {
        return findByOrderList(orders, null, inclusion);
    }

    public Set<BillingLedger> findByOrderList(@Nonnull ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.ALL);
    }

    public Set<BillingLedger> findWithoutBillingSessionByOrderList(@Nonnull ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.NO_SESSION_STARTED);
    }

    public Set<BillingLedger> findWithoutBillingSessionByOrderList(@Nonnull List<String> productOrderBusinessKeys) {
        return findByOrderList(productOrderBusinessKeys, BillingSessionInclusion.NO_SESSION_STARTED);
    }

    public Set<BillingLedger> findLockedOutByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_STARTED);
    }

    public Set<BillingLedger> findLockedOutByOrderList(List<String> productOrderBusinessKeys) {
        return findByOrderList(productOrderBusinessKeys, BillingSessionInclusion.SESSION_STARTED);
    }

    public Set<BillingLedger> findBilledByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_BILLED);
    }

    public void removeLedgerItemsWithoutBillingSession(ProductOrder[] orders) {
        Set<BillingLedger> ledgerItems = findWithoutBillingSessionByOrderList(orders);

        // Remove each item from the list and delete it from the ledger
        while (!ledgerItems.isEmpty()) {
            BillingLedger item = ledgerItems.iterator().next();
            ledgerItems.remove(item);
//            item.getProductOrderSample().getBillableItems().remove( item );
            remove(item);
        }
    }
}
