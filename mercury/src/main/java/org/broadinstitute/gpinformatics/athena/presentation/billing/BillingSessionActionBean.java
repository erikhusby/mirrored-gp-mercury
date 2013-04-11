package org.broadinstitute.gpinformatics.athena.presentation.billing;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao.FetchSpec.*;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding("/billing/session.action")
public class BillingSessionActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/billing/sessions.jsp";
    private static final String SESSION_VIEW_PAGE = "/billing/view.jsp";

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private QuoteService quoteService;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private BillingEjb billingEjb;

    private List<BillingSession> billingSessions;

    @Validate(required = true, on = {"bill", "endSession"})
    private String sessionKey;

    // parameter from quote server
    private String billingSession;

    private BillingSession editSession;

    /**
     * Initialize the session with the passed in key for display in the form.  Creation happens from a gesture in the
     * order list, so create is not needed here.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, "downloadTracker", "downloadQuoteItems", "bill", "endSession"})
    public void init() {
        sessionKey = getContext().getRequest().getParameter("sessionKey");
        if (sessionKey != null) {
            editSession = billingSessionDao.findByBusinessKey(sessionKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        billingSessions = billingSessionDao.findAll();
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        // If a billing session is sent, then this is coming from the quote server and it needs to be redirected to view.
        if (!StringUtils.isBlank(billingSession)) {
            return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION).addParameter("sessionKey", billingSession);
        }

        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    @HandlesEvent("downloadTracker")
    public Resolution downloadTracker() {
        List<String> productOrderBusinessKeys = editSession.getProductOrderBusinessKeys();

        List<ProductOrder> productOrders =
                productOrderDao.findListByBusinessKeyList(productOrderBusinessKeys, Product, ResearchProject, Samples);

        Resolution downloadResolution =
            ProductOrderActionBean.getTrackerForOrders(this, productOrders, priceItemDao, bspUserList, priceListCache);

        // If there is no file to download, just pass on the errors.
        // FIXME: this logic is bogus, getTrackerForOrders doesn't return null on error.
        if (downloadResolution == null) {
            return new ForwardResolution(SESSION_VIEW_PAGE);
        }

        // Do the download.
        return downloadResolution;
    }

    /**
     * This does the download and returns the proper resolution for download, if successful. If not,
     * then it returns null.
     *
     * @return The resolution for the download
     */
    @HandlesEvent("downloadQuoteItems")
    public Resolution downloadQuoteItems() {
        OutputStream outputStream = null;

        try {
            String filename = editSession.getBusinessKey() + "_" + new Date();
            filename = filename.replaceAll(":", "_");

            final File tempFile = File.createTempFile(filename, ".xls");
            outputStream = new FileOutputStream(tempFile);

            QuoteWorkItemsExporter exporter =
                    new QuoteWorkItemsExporter(editSession, getQuoteImportItems());

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
            return getSourcePageResolution();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        return editSession.getQuoteImportItems(priceListCache);
    }

    @HandlesEvent("bill")
    public Resolution bill() {

        String pageUrl = getContext().getRequest().getRequestURL().toString();
        List<BillingEjb.BillingResult> billingResults = billingEjb.bill(pageUrl, sessionKey);

        boolean errorsInBilling = false;

        for (BillingEjb.BillingResult billingResult : billingResults) {

            if (billingResult.isError()) {
                errorsInBilling = true;
                addGlobalValidationError(billingResult.getErrorMessage());

            } else {
                String workUrl =
                        quoteLink.workUrl(billingResult.getQuoteImportItem().getQuoteId(), billingResult.getWorkId());

                String link = "<a href=\"" + workUrl + "\" target=\"QUOTE\">click here</a>";
                addMessage("Sent to quote server: " + link + " to see the value");
            }
        }

        if (errorsInBilling) {
            return getSourcePageResolution();
        }

        return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                .addParameter("sessionKey", editSession.getBusinessKey());
    }


    @HandlesEvent("endSession")
    public Resolution endSession() {
        billingEjb.endSession(sessionKey);

        // The default Resolution is LIST_ACTION.
        return new RedirectResolution(BillingSessionActionBean.class);
    }


    public List<BillingSession> getBillingSessions() {
        return billingSessions;
    }

    public String getSessionKey() {
        return sessionKey;
    }


    @SuppressWarnings("UnusedDeclaration")
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public BillingSession getEditSession() {
        return editSession;
    }

    @SuppressWarnings("UnusedDeclaration")
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

    @SuppressWarnings("UnusedDeclaration")
    public String getBillingSession() {
        return billingSession;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setBillingSession(String billingSession) {
        this.billingSession = billingSession;
    }
}
