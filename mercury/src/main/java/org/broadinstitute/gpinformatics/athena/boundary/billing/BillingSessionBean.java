package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named
@RequestScoped
public class BillingSessionBean {

    @Inject
    private BillingSessionDao sessionDao;

    /** All sessions that exist so we can display them in the UI **/
    private List<BillingSession> allSessions;
    private BillingSession billingSession;

    /**
     * Returns a list of all product orders. Only actually fetches the list from the database once per request
     * (as a result of this bean being @RequestScoped).
     *
     * @return list of all product orders
     */
    public List<BillingSession> getAllSessions() {
        if (allSessions == null) {
            allSessions = sessionDao.findAll();
        }

        return allSessions;
    }

    public BillingSession getBillingSession() {
        return billingSession;
    }

    public void setBillingSession(BillingSession billingSession) {
        this.billingSession = billingSession;
    }
}
