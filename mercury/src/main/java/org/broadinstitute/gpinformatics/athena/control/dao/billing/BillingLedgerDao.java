package org.broadinstitute.gpinformatics.athena.control.dao.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger_;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
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

    private Set<BillingLedger> findByOrderList(@Nonnull ProductOrder[] orders, BillingSessionInclusion inclusion) {
        if (orders.length == 0) {
            return Collections.emptySet();
        }

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BillingLedger> criteriaQuery = criteriaBuilder.createQuery(BillingLedger.class);

        Root<BillingLedger> ledgerRoot = criteriaQuery.from(BillingLedger.class);
        Join<BillingLedger, ProductOrderSample> orderSample = ledgerRoot.join(BillingLedger_.productOrderSample);
        Join<BillingLedger, BillingSession> billingSession = ledgerRoot.join(BillingLedger_.billingSession);

        Predicate orderInPredicate = orderSample.get(ProductOrderSample_.productOrder).in(orders);

        Predicate fullPredicate;
        if (inclusion == BillingSessionInclusion.NO_SESSION_STARTED) {
            Predicate noSession = ledgerRoot.get(BillingLedger_.billingSession).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, noSession);
        } else if (inclusion == BillingSessionInclusion.SESSION_STARTED) {
            Predicate hasSession = ledgerRoot.get(BillingLedger_.billingSession).isNotNull();
            Predicate notBilled = billingSession.get(BillingSession_.billedDate).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, hasSession, notBilled);
        } else if (inclusion == BillingSessionInclusion.SESSION_BILLED) {
            Predicate hasSession = ledgerRoot.get(BillingLedger_.billingSession).isNotNull();
            Predicate isBilled = billingSession.get(BillingSession_.billedDate).isNotNull();
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

    public Set<BillingLedger> findByOrderList(@Nonnull ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.ALL);
    }

    public Set<BillingLedger> findWithoutBillingSessionByOrderList(@Nonnull ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.NO_SESSION_STARTED);
    }

    public Set<BillingLedger> findLockedOutByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_STARTED);
    }

    public Set<BillingLedger> findBilledByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, BillingSessionInclusion.SESSION_BILLED);
    }
}
