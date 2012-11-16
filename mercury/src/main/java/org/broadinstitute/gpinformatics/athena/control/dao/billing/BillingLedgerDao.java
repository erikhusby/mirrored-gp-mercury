package org.broadinstitute.gpinformatics.athena.control.dao.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Predicate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database interactions involving Billing Ledger
 */
public class BillingLedgerDao extends GenericDao {

    public List<BillingLedger> findAll() {
        return findAll(BillingLedger.class);
    }

    private Set<BillingLedger> findByOrderList(ProductOrder[] orders, boolean notInBillingSession) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BillingLedger> criteriaQuery = criteriaBuilder.createQuery(BillingLedger.class);

        Root<BillingLedger> ledgerRoot = criteriaQuery.from(BillingLedger.class);
        Join<BillingLedger, ProductOrderSample> orderSample = ledgerRoot.join(BillingLedger_.productOrderSample);

        Predicate orderInPredicate = orderSample.get(ProductOrderSample_.productOrder).in(orders);

        Predicate fullPredicate = orderInPredicate;
        if (notInBillingSession) {
            Predicate noSessionPredicate = ledgerRoot.get(BillingLedger_.billingSession).isNull();
            fullPredicate = criteriaBuilder.and(orderInPredicate, noSessionPredicate);
        }

        criteriaQuery.where(fullPredicate);

        try {
            return new HashSet<BillingLedger>(getEntityManager().createQuery(criteriaQuery).getResultList());
        } catch (NoResultException ignored) {
            return Collections.emptySet();
        }
    }

    public Set<BillingLedger> findByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, false);
    }

    public Set<BillingLedger> findWithoutBillingSessionByOrderList(ProductOrder[] orders) {
        return findByOrderList(orders, true);
    }
}
