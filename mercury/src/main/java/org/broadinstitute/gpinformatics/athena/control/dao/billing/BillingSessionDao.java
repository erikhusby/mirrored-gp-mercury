package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.LockModeType;
import java.util.List;

/**
 * Database interactions involving Billing Sessions
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class BillingSessionDao extends GenericDao {

    public List<BillingSession> findAll() {
        return findAll(BillingSession.class);
    }

    public BillingSession findByBusinessKey(@Nonnull String businessKey, boolean withLock) {

        if (!businessKey.startsWith(BillingSession.ID_PREFIX)) {
            throw new IllegalArgumentException("Business key must start with: " + BillingSession.ID_PREFIX);
        }

        Long sessionId = Long.parseLong(businessKey.substring(BillingSession.ID_PREFIX.length()));

        /*
         * To prevent multiple users (or the same user with a double click) from bill the same work twice, we are
         * locking the record until the act of billing is complete.
         *
         * TODO  Examine other potential cases in the application that may need pessimistic locking
         */
        if(withLock) {
        return findSingleSafely(BillingSession.class, BillingSession_.billingSessionId, sessionId,
                LockModeType.PESSIMISTIC_READ);
        } else {
            return findSingle(BillingSession.class, BillingSession_.billingSessionId, sessionId);
        }
    }
}
