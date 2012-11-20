package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionBean;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

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
    private QuoteService quoteService;

    @Inject
    private FacesContext facesContext;

    @Inject
    private BSPUserList bspUserList;

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
                // Any exceptions in sending to the quote server will just be reported and will continue on to the next one
                item.setupBillError(ex.getMessage());
                addErrorMessage(ex.getMessage());
            }
        }

        sessionDao.persist(billingSessionBean.getBillingSession());

        return null;
    }

    public String cancelSession() {
        // Remove all the sessions from the non-billed items
        boolean allRemoved = billingSessionBean.getBillingSession().cancelSession();

        // If all removed then remove the session, totally. If some are billed, then just persist the updates
        if (allRemoved) {
            sessionDao.remove(billingSessionBean.getBillingSession());
        } else {
            sessionDao.persist(billingSessionBean.getBillingSession());
        }

        return "sessions?faces-redirect=true&includeViewParams=false";
    }

    public String downloadTracker() {
        try {
            String filename =
                "BillingTracker-" +
                AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".xls";

            OutputStream outputStream = AbstractSpreadsheetExporter.beginSpreadsheetDownload(facesContext, filename);

            ProductOrder[] selectedProductOrders = billingSessionBean.getBillingSession().getProductOrders();

            // dummy code to plumb in spreadsheet write.  this is not the right format and it's only doing the first PDO!
            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(selectedProductOrders, bspUserList);
            sampleLedgerExporter.writeToStream(outputStream);

            facesContext.responseComplete();
        } catch (Exception ex) {
            addErrorMessage("Got an exception trying to download the billing tracker: " + ex.getMessage());
        }

        return null;
    }

    public void downloadQuoteItems() throws IOException {
        QuoteWorkItemsExporter exporter =
            new QuoteWorkItemsExporter(
                billingSessionBean.getBillingSession(), billingSessionBean.getBillingSession().getQuoteImportItems());
        FacesContext fc = FacesContext.getCurrentInstance();

        String filename = billingSessionBean.getBillingSession().getBusinessKey() + "_" + new Date() + ".xls";
        OutputStream outputStream = AbstractSpreadsheetExporter.beginSpreadsheetDownload(fc, filename);
        exporter.writeToStream(outputStream, bspUserList);

        fc.responseComplete();
    }
}
