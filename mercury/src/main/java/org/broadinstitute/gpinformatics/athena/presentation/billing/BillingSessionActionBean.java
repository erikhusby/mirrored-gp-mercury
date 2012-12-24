package org.broadinstitute.gpinformatics.athena.presentation.billing;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao.FetchSpec.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/billing/session.action")
public class BillingSessionActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/billing/sessions.jsp";
    private static final String SESSION_VIEW_PAGE = "/billing/view.jsp";

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private QuoteService quoteService;

    @Inject
    private PriceListCache priceListCache;

    private List<BillingSession> billingSessions;

    private String sessionKey;

    private BillingSession editSession;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "downloadTracker", "downloadQuoteItems", "bill", "endSession"})
    public void init() {
        sessionKey = getContext().getRequest().getParameter("sessionKey");
        if (sessionKey != null) {
            editSession = billingSessionDao.findByBusinessKey(sessionKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        billingSessions = billingSessionDao.findAll();
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    @HandlesEvent("downloadTracker")
    public Resolution downloadTracker() {
        List<String> productOrderBusinessKeys = editSession.getProductOrderBusinessKeys();

        List<ProductOrder> productOrders =
                productOrderDao.findListByBusinessKeyList(productOrderBusinessKeys, Product, ResearchProject, Samples);

        Resolution downloadResolution =
            ProductOrderActionBean.getTrackerForOrders(this, productOrders, bspUserList);

        // If there is no file to download, just pass on the errors
        if (downloadResolution == null) {
            return new ForwardResolution(SESSION_VIEW_PAGE);
        }

        // Do the download
        return downloadResolution;
    }

    /**
     * This does the download and returns the proper resolution for download, if succesful. If not,
     * then it returns null.
     *
     * @return The resolution for th download
     */
    @HandlesEvent("downloadQuoteItems")
    public Resolution downloadQuoteItems() {
        OutputStream outputStream = null;

        try {
            String filename = editSession.getBusinessKey() + "_" + new Date();

            final File tempFile = File.createTempFile(filename, ".xls");
            outputStream = new FileOutputStream(tempFile);

            QuoteWorkItemsExporter exporter =
                    new QuoteWorkItemsExporter(editSession, editSession.getQuoteImportItems());

            exporter.writeToStream(outputStream, bspUserList);
            IOUtils.closeQuietly(outputStream);

            return new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response)
                        throws Exception {
                    InputStream inputStream = null;

                    try {
                        setFileDownloadHeaders("application/excel", tempFile.getName());
                        inputStream = new FileInputStream(tempFile);
                        IOUtils.copy(inputStream, getContext().getResponse().getOutputStream());
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (Exception ex) {
            addGlobalValidationError("Got an exception trying to download the billing tracker: " + ex.getMessage());
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    @HandlesEvent("bill")
    public Resolution bill() {

        boolean errorsInBilling = false;

        String sessionKey =  editSession.getBusinessKey();
        for (QuoteImportItem item : editSession.getUnBilledQuoteImportItems()) {

            Quote quote = new Quote();
            quote.setAlphanumericId(item.getQuoteId());

            PriceItem quotePriceItem = new PriceItem();
            quotePriceItem.setName(item.getPriceItem().getName());
            quotePriceItem.setCategoryName(item.getPriceItem().getCategory());
            quotePriceItem.setPlatformName(item.getPriceItem().getPlatform());

            String pageUrl = getContext().getRequest().getRequestURL().toString();

            try {
                String message = quoteService.registerNewWork(
                    quote, quotePriceItem, item.getWorkCompleteDate(), item.getQuantity(), pageUrl, "billingSession",
                    sessionKey);

                item.setBillingMessages(BillingSession.SUCCESS);
                addMessage("Sent to quote server " + message);
            } catch (Exception ex) {
                // Any exceptions in sending to the quote server will just be reported and will continue on to the next one
                item.setBillingMessages(ex.getMessage());
                addGlobalValidationError(ex.getMessage());
                errorsInBilling = true;
            }
        }

        // If there were no errors in billing, then end the session, which will add the billed date and remove all sessions from the ledger
        if (!errorsInBilling) {
            return endSession();
        }

        billingSessionDao.persist(editSession);

        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    @HandlesEvent("endSession")
    public Resolution endSession() {
        // Remove all the sessions from the non-billed items.
        boolean allFailed = editSession.cancelSession();

        if (allFailed) {
            // If all removed then remove the session, totally.
            billingSessionDao.remove(editSession);
        } else {
            // If some or all are billed, then just persist the updates.
            billingSessionDao.persist(editSession);
        }

        // Default is list
        return new RedirectResolution(BillingSessionActionBean.class);
    }

    public List<BillingSession> getBillingSessions() {
        return billingSessions;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public BillingSession getEditSession() {
        return editSession;
    }

    public void setEditSession(BillingSession editSession) {
        this.editSession = editSession;
    }

    private final Map<String, String> priceItemNameMap = new HashMap<String, String>();
    public Map<String, String> getQuotePriceItemNameMap() {
        Collection<PriceItem> priceItems = priceListCache.getPriceItems ();
        if (priceItemNameMap.isEmpty() && !priceItems.isEmpty()) {
            for (PriceItem priceItem : priceListCache.getPriceItems()) {
                priceItemNameMap.put(priceItem.getId(), priceItem.getName());
            }
        }

        return priceItemNameMap;
    }
}
