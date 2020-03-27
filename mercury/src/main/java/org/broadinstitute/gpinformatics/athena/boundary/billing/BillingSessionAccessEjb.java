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
import java.util.ArrayList;
import java.util.Collection;
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
     * Obtains an advisory lock on a billing session for the purpose of performing billing against the Quote Server.
     *
     * @param billingSessionKey business key of the billing session which is to be found and locked
     * @return the locked billing session if this thread was able to successfully lock it for billing
     * @throws BillingException if the billing session is locked for billing by another thread
     */
    public synchronized BillingSession findAndLockSession(@Nonnull String billingSessionKey) {

        BillingSession session;
        if (BooleanUtils.isTrue(billingSessionLockStatus.get(billingSessionKey))) {
            throw new BillingException(BillingEjb.LOCKED_SESSION_TEXT);
        }
        log.info("Setting billing session " + billingSessionKey + " to locked");
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

        if (BooleanUtils.isFalse(billingSessionLockStatus.get(billingSession.getBusinessKey()))) {
            return;
        }
        log.info("Setting billing session BILL-" + billingSession.getBillingSessionId() + " to unlocked");
        billingSessionLockStatus.put(billingSession.getBusinessKey(), Boolean.FALSE);
        billingSessionDao.persist(billingSession);
        billingSessionDao.flush();
    }

    /**
     * Helper method to determine if a Billing Session is currently being locked by the application for Billing. This
     * must be synchronized in order to ensure a consistent read of changes made from other threads.
     *
     * @param billingSessionKey business key of the billing session which is to be found and locked
     * @return true if the billing session is locked; false otherwise
     */
    public synchronized boolean isSessionLocked(@Nonnull String billingSessionKey) {
        return BooleanUtils.isTrue(billingSessionLockStatus.get(billingSessionKey));
    }

    /**
     * Returns a collection of the billing sessions that are currently locked. This method is synchronized so that it
     * gives a consistent snapshot of the lock status even if locks are actively being obtained and released. For use by
     * an admin interface for clearing stale locks.
     *
     * @return the locked billing session keys
     */
    public synchronized Collection<String> getLockedSessions() {
        Collection<String> lockedSessions = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : billingSessionLockStatus.entrySet()) {
            if (BooleanUtils.isTrue(entry.getValue())) {
                lockedSessions.add(entry.getKey());
            }
        }
        return lockedSessions;
    }

    /**
     * Forcefully unlocks a session. For use by an admin interface for clearing stale locks.
     *
     * @param sessionKey    the billing session key to unlock
     */
    public synchronized void unlockSession(String sessionKey) {
        billingSessionLockStatus.put(sessionKey, Boolean.FALSE);
    }
}
