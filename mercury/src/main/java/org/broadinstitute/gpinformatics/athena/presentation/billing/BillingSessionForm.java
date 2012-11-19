package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionBean;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

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

    @Inject
    private BillingLedgerDao ledgerDao;

    @Inject
    private QuoteService quoteService;

    @Inject
    private FacesContext facesContext;

    public String bill() {
        String sessionKey =  billingSessionBean.getBillingSession().getBusinessKey();
        for (QuoteImportItem item : billingSessionBean.getBillingSession().getQuoteImportItems()) {

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            PriceItem quotePriceItem = new PriceItem();
            quotePriceItem.setName(item.getPriceItem().getName());
            quotePriceItem.setCategoryName(item.getPriceItem().getCategory());
            quotePriceItem.setPlatformName(item.getPriceItem().getPlatform());

            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            String pageUrl = request.getRequestURL().toString() + "/billing/view";

            try {
                String message = quoteService.registerNewWork(
                    quote, quotePriceItem, item.getQuantity(), pageUrl, "billingSession", sessionKey);

                item.setupBilledInfo("Billed Successfully");
                addInfoMessage("Sent to quote server " + message);
            } catch (Exception ex) {
                // Any exceptions in sending to the quote server will just be reported
                item.setupBillError(ex.getMessage());
                addErrorMessage(ex.getMessage());
            }
        }

        return null;
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
