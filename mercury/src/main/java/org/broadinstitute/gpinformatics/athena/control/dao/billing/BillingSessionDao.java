package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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

    public Object findByBusinessKey(@Nonnull String businessKey) {

        if (!businessKey.startsWith(BillingSession.ID_PREFIX)) {
            throw new IllegalArgumentException("Business key must start with: " + BillingSession.ID_PREFIX);
        }

        Long sessionId = Long.parseLong(businessKey.substring(3));

        return findSingle(BillingSession.class, BillingSession_.billingSessionId, sessionId);
    }
}
