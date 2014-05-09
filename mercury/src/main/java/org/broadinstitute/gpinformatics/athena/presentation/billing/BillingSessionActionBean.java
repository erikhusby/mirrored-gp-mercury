package org.broadinstitute.gpinformatics.athena.presentation.billing;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingAdaptor;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingException;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionAccessEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporterFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding("/billing/session.action")
public class BillingSessionActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/billing/sessions.jsp";
    private static final String SESSION_VIEW_PAGE = "/billing/view.jsp";
    private static final Log log = LogFactory.getLog(BillingSessionActionBean.class);

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private ProductOrderDao productOrderDao;

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

    @Inject
    private BillingAdaptor billingAdaptor;

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;

    @Inject
    private SampleLedgerExporterFactory sampleLedgerExporterFactory;

    private List<BillingSession> billingSessions;

    @Validate(required = true, on = {"bill", "endSession"})
    private String sessionKey;

    // parameter from quote server
    private String billingSession;

    // parameter from quote server
    private String workItemId;

    private BillingSession editSession;

    public static final String WORK_ITEM_URL_PARAMETER = "workId";

    /**
     * Initialize the session with the passed in key for display in the form.  Creation happens from a gesture in the
     * order list, so create is not needed here.
     */
    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {VIEW_ACTION, "downloadTracker", "downloadQuoteItems", "bill", "endSession"})
    public void init() {
        log.debug("In validation for billing");
        sessionKey = getContext().getRequest().getParameter("sessionKey");
        workItemId = getContext().getRequest().getParameter(WORK_ITEM_URL_PARAMETER);
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
            return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                    .addParameter("sessionKey", billingSession);
        }

        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    @HandlesEvent("downloadTracker")
    public Resolution downloadTracker() {

        List<ProductOrder> productOrders =
                productOrderDao.findListForBilling(editSession.getProductOrderBusinessKeys());

        SampleLedgerExporter exporter = sampleLedgerExporterFactory.makeExporter(productOrders);

        try {
            return new BillingTrackerResolution(exporter);
        } catch (IOException e) {
            addGlobalValidationError("Got an exception trying to download the billing tracker: " + e.getMessage());
            return new ForwardResolution(SESSION_VIEW_PAGE);
        }
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
        log.debug("Billing request for " + pageUrl);

        boolean errorsInBilling = false;

        List<BillingEjb.BillingResult> billingResults = null;
        try {
            billingResults = billingAdaptor.billSessionItems(pageUrl, sessionKey);

            for (BillingEjb.BillingResult billingResult : billingResults) {

                if (billingResult.isError()) {
                    errorsInBilling = true;
                    addGlobalValidationError(billingResult.getErrorMessage());

                } else {
                    String workUrl =
                            quoteLink.workUrl(billingResult.getQuoteImportItem().getQuoteId(),
                                    billingResult.getWorkId());

                    String link = "<a href=\"" + workUrl + "\" target=\"QUOTE\">click here</a>";
                    addMessage("Sent to quote server: " + link + " to see the value");
                }
            }
        } catch (BillingException e) {
            errorsInBilling = true;
            addGlobalValidationError(e.getMessage());
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

    private final Map<String, String> priceItemNameMap = new HashMap<>();

    public Map<String, String> getQuotePriceItemNameMap() {
        Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();
        if (priceItemNameMap.isEmpty() && !quotePriceItems.isEmpty()) {
            for (QuotePriceItem quotePriceItem : priceListCache.getQuotePriceItems()) {
                priceItemNameMap.put(quotePriceItem.getId(), quotePriceItem.getName());
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

    public boolean isBillingSessionLocked() {
        return billingSessionAccessEjb.isSessionLocked(editSession.getBusinessKey());
    }

    public String getQuoteUrl(String quote) {
        return quoteLink.quoteUrl(quote);
    }

    public String getQuoteWorkItemUrl(String quote,String workItem) {
        return quoteLink.workUrl(quote, workItem);
    }

    /**
     * Returns the work item id that should be highlighted.
     * Javascript uses this to perform the actual
     * styling.
     */
    public String getWorkItemIdToHighlight() {
        return workItemId;
    }
}
