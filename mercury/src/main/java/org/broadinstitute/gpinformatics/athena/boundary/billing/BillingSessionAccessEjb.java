package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;

import javax.annotation.Nonnull;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton bean responsible for maintaining an application level lock on a given BillingSession.  This allows us to
 * avoid the Database level lock on the BillingSession.
 */
@Singleton
public class BillingSessionAccessEjb implements Serializable {

    private static final Log log = LogFactory.getLog(BillingSessionAccessEjb.class);

    @Inject
    BillingSessionDao billingSessionDao;

    private Map<String, Boolean> billingSessionLockStatus = new HashMap<>();

    /**
     * Obtains an advisory lock on a billing session for the purpose of performing billing against the Quote Server. It
     * is EXTREMELY important that this method be called outside of a transactional context so that it will begin and
     * commit its own transaction. It is, in fact, so important that I think we should consider marking this method as
     * TransactionAttributeType.NEVER and have it call a TransactionAttributeType.REQUIRED method to do the actual work.
     *
     * @param billingSessionKey business key of the billing session which is to be found and locked
     *
     * @return the locked billing session if this thread was able to successfully lock it for billing
     *
     * @throws BillingException if the billing session is locked for billing by another thread
     */
    public synchronized BillingSession findAndLockSession(@Nonnull String billingSessionKey) {

        BillingSession session;
        if(BooleanUtils.isTrue(billingSessionLockStatus.get(billingSessionKey))) {
            throw new BillingException(BillingEjb.LOCKED_SESSION_TEXT);
        }
        billingSessionLockStatus.put(billingSessionKey, Boolean.TRUE);
        session = billingSessionDao.findByBusinessKey(billingSessionKey);

        return session;
    }

    /**
     * Transactional method to unlock a billing session.  The end of the transaction will save the contents of the
     * billing session entity
     *
     * @param billingSession billing session to be unlocked
     */
    public synchronized void saveAndUnlockSession(@Nonnull BillingSession billingSession) {

        if(BooleanUtils.isFalse(billingSessionLockStatus.get(billingSession.getBusinessKey()))) {
            return;
        }
        log.info("Setting billing session BILL-" + billingSession.getBillingSessionId() + " to unlocked");
        billingSessionLockStatus.put(billingSession.getBusinessKey(), Boolean.FALSE);
        billingSessionDao.persist(billingSession);
    }

    /**
     * Helper method to determine if a Billing Session is currently being locked by the application for Billing
     * @param billingSessionKey business key of the billing session which is to be found and locked
     * @return
     */
    public boolean isSessionLocked(@Nonnull String billingSessionKey) {
        return BooleanUtils.isTrue(billingSessionLockStatus.get(billingSessionKey));
    }
}
