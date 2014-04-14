package org.broadinstitute.gpinformatics.mercury.presentation.admin;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionAccessEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * Action bean for admin page for viewing and clearing application-level billing session locks.
 */
@UrlBinding(value = BillingSessionAccessActionBean.BILLING_SESSION_ACCESS_ACTION)
public class BillingSessionAccessActionBean extends CoreActionBean {

    public static final String BILLING_SESSION_ACCESS_PAGE = "/admin/billing_session_access.jsp";
    public static final String BILLING_SESSION_ACCESS_ACTION = "/admin/billingSessionAccess.action";

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;

    private Collection<String> lockedSessions;

    private List<String> sessionsToUnlock;

    @DefaultHandler
    public Resolution list() {
        lockedSessions = billingSessionAccessEjb.getLockedSessions();
        return new ForwardResolution(BILLING_SESSION_ACCESS_PAGE);
    }

    public Resolution clearLocks() {
        for (String session : sessionsToUnlock) {
            billingSessionAccessEjb.unlockSession(session);
        }
        return new RedirectResolution(BILLING_SESSION_ACCESS_ACTION);
    }

    public Collection<String> getLockedSessions() {
        return lockedSessions;
    }

    public List<String> getSessionsToUnlock() {
        return sessionsToUnlock;
    }

    public void setSessionsToUnlock(List<String> sessionsToUnlock) {
        this.sessionsToUnlock = sessionsToUnlock;
    }
}
