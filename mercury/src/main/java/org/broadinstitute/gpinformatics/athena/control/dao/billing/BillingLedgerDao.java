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
import java.util.Collections;
import java.util.List;

/**
 * Database interactions involving Billing Ledger
 */
public class BillingLedgerDao extends GenericDao {

    public List<BillingLedger> findAll() {
        return findAll(BillingLedger.class);
    }

    public List<BillingLedger> findByOrderList(List<ProductOrder> orders) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BillingLedger> criteriaQuery = criteriaBuilder.createQuery(BillingLedger.class);

        Root<BillingLedger> ledgerRoot = criteriaQuery.from(BillingLedger.class);
        Join<BillingLedger, ProductOrderSample> orderSample = ledgerRoot.join(BillingLedger_.productOrderSample);
        criteriaQuery.where(orderSample.get(ProductOrderSample_.productOrder).in(orders));

        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }
}
