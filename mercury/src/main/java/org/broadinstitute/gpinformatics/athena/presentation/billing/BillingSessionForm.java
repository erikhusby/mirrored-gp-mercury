package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionBean;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderListBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Date;
import java.util.List;

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
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private QuoteService quoteService;

    @Inject
    private FacesContext facesContext;

    @Inject
    private BSPUserList bspUserList;

    public String bill() {

        boolean errorsInBilling = false;

        BillingSession session = billingSessionBean.getBillingSession();
        String sessionKey =  session.getBusinessKey();
        for (QuoteImportItem item : session.getUnBilledQuoteImportItems()) {

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            PriceItem quotePriceItem = new PriceItem();
            quotePriceItem.setName(item.getPriceItem().getName());
            quotePriceItem.setCategoryName(item.getPriceItem().getCategory());
            quotePriceItem.setPlatformName(item.getPriceItem().getPlatform());

            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            String pageUrl = request.getRequestURL().toString();

            try {
                String message = quoteService.registerNewWork(
                    quote, quotePriceItem, item.getWorkCompleteDate(), item.getQuantity(), pageUrl, "billingSession",
                    sessionKey);

                item.setBillingMessages(BillingSession.SUCCESS);
                addInfoMessage("Billing Session " + sessionKey + " sent to quote server. Response: " + message);
            } catch (Exception ex) {
                // Any exceptions in sending to the quote server will just be reported and will continue on to the next one
                item.setBillingMessages(ex.getMessage());
                addErrorMessage(ex.getMessage());
                errorsInBilling = true;
            }
        }

        // If there were no errors in billing, then end the session, which will add the billed date and remove all sessions from the ledger
        if (!errorsInBilling) {
            return endSession();
        }

        sessionDao.persist(session);
        return null;
    }

    public String endSession() {
        // Remove all the sessions from the non-billed items.
        boolean allFailed = billingSessionBean.getBillingSession().cancelSession();

        if (allFailed) {
            // If all removed then remove the session, totally.
            sessionDao.remove(billingSessionBean.getBillingSession());
        } else {
            // If some or all are billed, then just persist the updates.
            sessionDao.persist(billingSessionBean.getBillingSession());
        }

        // I do not use redirect method because I want includeViewParams to be false. This is because a remove (above)
        // can make the session Id invalid and we don't want that passed through.
        return "sessions?faces-redirect=true&includeViewParams=false";
    }
    
    public String downloadTracker() {
        List<String> productOrderBusinessKeys = billingSessionBean.getBillingSession().getProductOrderBusinessKeys();
        return ProductOrderListBean.getTrackerForOrders(productOrderBusinessKeys, bspUserList, billingLedgerDao, productOrderDao);
    }

    public String downloadQuoteItems() throws IOException {
        OutputStream outputStream = null;
        File tempFile = null;
        InputStream inputStream = null;

        try {
            String filename = billingSessionBean.getBillingSession().getBusinessKey() + "_" + new Date() + ".xls";

            tempFile = File.createTempFile(filename, "xls");
            outputStream = new FileOutputStream(tempFile);

            QuoteWorkItemsExporter exporter =
                    new QuoteWorkItemsExporter(
                            billingSessionBean.getBillingSession(), billingSessionBean.getBillingSession().getQuoteImportItems());

            exporter.writeToStream(outputStream, bspUserList);
            IOUtils.closeQuietly(outputStream);
            inputStream = new FileInputStream(tempFile);

            // This copies the inputStream as a faces download with the file name specified.
            AbstractSpreadsheetExporter.copyForDownload(inputStream, filename);
        } catch (Exception ex) {
            String message = "Got an exception trying to download the billing tracker: " + ex.getMessage();
            AbstractJsfBean.addMessage(null, FacesMessage.SEVERITY_ERROR, message, message);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
            FileUtils.deleteQuietly(tempFile);
        }

        return null;
    }
}
