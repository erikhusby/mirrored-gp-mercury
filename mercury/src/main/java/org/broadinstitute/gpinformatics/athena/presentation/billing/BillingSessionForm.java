package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionBean;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Form operations for the billing session features
 */
@Named
@RequestScoped
public class BillingSessionForm extends AbstractJsfBean {

    @Inject
    private BillingSessionBean billingSessionBean;

    @Inject
    private BillingSessionDao sessionDao;

    public String bill() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    public String cancelSession() {
        billingSessionBean.getBillingSession().cancelSession();
        sessionDao.remove(billingSessionBean.getBillingSession());

        return "sessions?faces-redirect=true&includeViewParams=false";
    }

    public String downloadTracker() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    public String downloadQuoteItems() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}
