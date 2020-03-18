package org.broadinstitute.gpinformatics.athena.presentation.billing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.ActionBeanContext;
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
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionAccessEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteWorkItemsExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding("/billing/session.action")
public class BillingSessionActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/billing/sessions.jsp";
    private static final String SESSION_VIEW_PAGE = "/billing/view.jsp";
    private static final Log log = LogFactory.getLog(BillingSessionActionBean.class);
    public static final String SENT_TO_SAP = "Sent to SAP";
    public static final String SENT_TO_QUOTE_SERVER = "Sent to Quote Server";

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    private QuoteLink quoteLink;

    private SapQuoteLink sapQuoteLink;

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

    private List<BillingSession> billingSessions;

    @Validate(required = true, on = {"bill", "endSession"})
    private String sessionKey;

    public static final String SESSION_KEY_PARAMETER_NAME = "sessionKey";

    public static final String BILLING_SESSION_FROM_URL_PARAMETER = "billingSession";

    // parameter from quote server, comes over as &workId= in the url
    private String workId;

    public static final String WORK_ITEM_FROM_URL_PARAMETER = "workId";

    public static final String SAP_DELIVERY_DOCUMENT_ID_URL_PARAMETER = "sapDeliveryId";

    private String sapDeliveryId;

    private BillingSession editSession;

    /**
     * Initialize the session with the passed in key for display in the form.  Creation happens from a gesture in the
     * order list, so create is not needed here.
     */
    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {VIEW_ACTION, "downloadTracker", "downloadQuoteItems", "bill", "endSession"})
    public void init() {
        sessionKey = getContext().getRequest().getParameter(SESSION_KEY_PARAMETER_NAME);
        if (sessionKey != null) {
            editSession = billingSessionDao.findByBusinessKey(sessionKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        billingSessions = billingSessionDao.findAll();
        billingSessions.sort(Comparator.comparing(BillingSession::getBillingSessionId).reversed());
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        if (isPageBeingLoadedFromQuoteServerSourceLink()) {
            return redirectFromQuoteServer();
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_VIEW_PAGE);
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

    public List<QuoteImportItem> getQuoteImportItems() throws QuoteServerException {
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
            errorsInBilling = createBillingMessage(billingResults, this);
        } catch (Exception e) {
            errorsInBilling = true;
            addGlobalValidationError(e.getMessage());
        }


        if (errorsInBilling) {
            return getSourcePageResolution();
        }

        return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                .addParameter(SESSION_KEY_PARAMETER_NAME, editSession.getBusinessKey());
    }

    public boolean createBillingMessage(List<BillingEjb.BillingResult> billingResults,
                                        MessageReporter messageReporter) {
        boolean errorsInBilling = false;
        HashMultimap<ProductOrder.QuoteSourceType, String> billingDestinationMap = HashMultimap.create();
        for (BillingEjb.BillingResult billingResult : billingResults) {

            if (billingResult.isError()) {
                errorsInBilling = true;
                addGlobalValidationError(billingResult.getErrorMessage());
            } else {
                billingDestinationMap.putAll(getQuoteLink(billingResult.getQuoteImportItem()));
            }
        }
        billingDestinationMap.asMap().entrySet().stream().collect(
            Collectors.groupingBy(type -> type.getKey().isSapType())).forEach((isSap, entries) -> {
            if (isSap) {
                entries.forEach(quoteDest -> {
                    StringBuilder message = new StringBuilder(SENT_TO_SAP).append(" ");
                    quoteDest.getValue().forEach(quoteId -> message.append(quoteId).append(" "));
                    messageReporter.addMessage(message.toString().trim());
                });
            } else {
                entries.forEach(quoteDest -> {
                    StringBuilder message = new StringBuilder(SENT_TO_QUOTE_SERVER).append(" ");
                    quoteDest.getValue().forEach(quoteId -> message.append(quoteId).append(" "));
                    messageReporter.addMessage(message.toString());
                });
            }
        });
        if (!billingDestinationMap.isEmpty()) {
            messageReporter.addMessage("Click links to view quote.");
        }

        return errorsInBilling;
    }

    public Multimap<ProductOrder.QuoteSourceType, String> getQuoteLink(QuoteImportItem quoteImportItem) {
        Multimap<ProductOrder.QuoteSourceType, String> quoteMap = HashMultimap.create();
        StringBuffer quoteLinkBuffer = new StringBuffer("<a class='external' target='QUOTE' href='");
        ProductOrder.QuoteSourceType quoteSourceType = quoteImportItem.getProductOrder().getQuoteSource();

        if (quoteImportItem.isSapOrder()) {
            quoteLinkBuffer.append(sapQuoteLink.sapUrl(quoteImportItem.getQuoteId()));
        } else {
            String workItem = Optional.ofNullable(quoteImportItem.getSingleWorkItem()).orElse("");
            quoteLinkBuffer.append(quoteLink.workUrl(quoteImportItem.getQuoteId(), workItem));
        }
        quoteLinkBuffer.append("'>").append(quoteImportItem.getQuoteId()).append("</a>");
        quoteMap.put(quoteSourceType, quoteLinkBuffer.toString());
        return quoteMap;
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

    public boolean isBillingSessionLocked() {
        return billingSessionAccessEjb.isSessionLocked(editSession.getBusinessKey());
    }

    public String getQuoteUrl(String quote) {
        return quoteLink.quoteUrl(quote);
    }

    public String getSapQuoteUrl(String quote) {
        return sapQuoteLink.sapUrl(quote);
    }

    public String getQuoteWorkItemUrl(String quote,String workItem) {
        return quoteLink.workUrl(quote, workItem);
    }

    private QuoteServerParameters getQuoteServerParametersFromQuoteSourceLink() {
        return QuoteServerParameters.createFromQuoteSourceLink(getContext());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setWorkId(String workId) {
        this.workId = workId;
    }

    /**
     * Returns the work item id that should be highlighted.
     * Javascript uses this to perform the actual
     * styling.
     */
    public String getHighlightRow() {
        String highlightRowId = null;
        if (StringUtils.isNotBlank(workId)) {
            highlightRowId = workId;
        } else if (StringUtils.isNotBlank(sapDeliveryId)) {
            highlightRowId = sapDeliveryId;
        }
        return highlightRowId;
    }

    public String getSapDeliveryId() {
        return sapDeliveryId;
    }

    public void setSapDeliveryId(String sapDeliveryId) {
        this.sapDeliveryId = sapDeliveryId;
    }

    /**
     * Parameters that the quote server puts in the
     * link so that we show the right billing session
     * and the right work item.
     */
    private static class QuoteServerParameters {

        private final String billingSession;

        private final String workId;
        private final String sapDeliveryId;

        public QuoteServerParameters(String billingSession,
                                     String workId, String sapDeliveryId) {
            this.billingSession = billingSession;
            this.workId = workId;
            this.sapDeliveryId = sapDeliveryId;
        }

        private static QuoteServerParameters createFromQuoteSourceLink(ActionBeanContext context) {
            Optional<String> billingSessionFromUrl = Optional.ofNullable(context.getRequest().getParameter(BILLING_SESSION_FROM_URL_PARAMETER));
            Optional<String> workIdFromURL = Optional.ofNullable(context.getRequest().getParameter(WORK_ITEM_FROM_URL_PARAMETER));
            Optional<String> sapDeliveryId = Optional.ofNullable(context.getRequest().getParameter(SAP_DELIVERY_DOCUMENT_ID_URL_PARAMETER));
            QuoteServerParameters params = null;
            if (billingSessionFromUrl.isPresent()) {
                params = new QuoteServerParameters(billingSessionFromUrl.orElse("")
                        ,workIdFromURL.orElse(""), sapDeliveryId.orElse(""));
            }
            return params;
        }

        private RedirectResolution redirectFromQuoteServer() {
            final RedirectResolution redirectResolution =
                    new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION);
            redirectResolution.addParameter(SESSION_KEY_PARAMETER_NAME, billingSession);
            redirectResolution.addParameter(WORK_ITEM_FROM_URL_PARAMETER, workId);
            redirectResolution.addParameter(SAP_DELIVERY_DOCUMENT_ID_URL_PARAMETER, sapDeliveryId);
            return redirectResolution;
        }
    }

    private RedirectResolution redirectFromQuoteServer() {
        return getQuoteServerParametersFromQuoteSourceLink().redirectFromQuoteServer();
    }

    private boolean isPageBeingLoadedFromQuoteServerSourceLink() {
        return getQuoteServerParametersFromQuoteSourceLink() != null;
    }

    @Inject
    public void setQuoteLink(QuoteLink quoteLink) {
        this.quoteLink = quoteLink;
    }

    @Inject
    public void setSapQuoteLink(SapQuoteLink sapQuoteLink) {
        this.sapQuoteLink = sapQuoteLink;
    }
}
