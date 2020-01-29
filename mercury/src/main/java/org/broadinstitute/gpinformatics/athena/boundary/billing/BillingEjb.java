package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.material.SAPMaterial;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
public class BillingEjb {
    private static final Log log = LogFactory.getLog(BillingEjb.class);

    public static final String NO_ITEMS_TO_BILL_ERROR_TEXT =
            "There are no items available to bill in this billing session";
    public static final String LOCKED_SESSION_TEXT =
            "This billing session is currently in the process of being processed for billing.  If you believe this " +
            "is in error, please contact the informatics group for assistance";

    /**
     * Encapsulates the results of a billing attempt on a {@link QuoteImportItem}, successful or otherwise.
     */
    public static class BillingResult {

        private final QuoteImportItem quoteImportItem;

        private String workId;

        private String errorMessage;
        private String sapBillingId;

        public BillingResult(@Nonnull QuoteImportItem quoteImportItem) {
            this.quoteImportItem = quoteImportItem;
        }

        public QuoteImportItem getQuoteImportItem() {
            return quoteImportItem;
        }

        public String getWorkId() {
            return workId;
        }

        void setWorkId(String workId) {
            this.workId = workId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isError() {
            return errorMessage != null;
        }

        public void setSapBillingId(String sapBillingId) {
            this.sapBillingId = sapBillingId;
        }

        public String getSapBillingId() {
            return sapBillingId;
        }

        public boolean isBilledInSap() {
            return StringUtils.isNotBlank(sapBillingId);
        }

        public boolean isBilledInQuoteServer() {
            return StringUtils.isNumeric(workId);
        }
    }

    private PriceListCache priceListCache;

    private BillingSessionDao billingSessionDao;

    private ProductOrderDao productOrderDao;

    private LedgerEntryDao ledgerEntryDao;

    SampleDataFetcher sampleDataFetcher;

    private AppConfig appConfig;
    private SapConfig sapConfig;
    private EmailSender emailSender;
    private TemplateEngine templateEngine;
    private BSPUserList bspUserList;

    private SAPProductPriceCache productPriceCache;

    public BillingEjb() {
        this(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Inject
    public BillingEjb(PriceListCache priceListCache,
                      BillingSessionDao billingSessionDao,
                      ProductOrderDao productOrderDao,
                      LedgerEntryDao ledgerEntryDao,
                      SampleDataFetcher sampleDataFetcher,
                      AppConfig appConfig, SapConfig sapConfig,
                      EmailSender emailSender,
                      TemplateEngine templateEngine,
                      BSPUserList bspUserList,
                      SAPProductPriceCache productPriceCache) {

        this.priceListCache = priceListCache;
        this.billingSessionDao = billingSessionDao;
        this.productOrderDao = productOrderDao;
        this.ledgerEntryDao = ledgerEntryDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.appConfig = appConfig;
        this.sapConfig = sapConfig;
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.bspUserList = bspUserList;
        this.productPriceCache = productPriceCache;
    }

    /**
     * Transactional method to end a billing session with appropriate handling for complete or partial failure.
     *
     * @param sessionKey The billing session's key
     */
    public void endSession(@Nonnull String sessionKey) {
        BillingSession billingSession = billingSessionDao.findByBusinessKeyWithLock(sessionKey);
        endSession(billingSession);
    }

    /**
     * Transactional method to end a billing session with appropriate handling for complete or partial failure.
     *
     * @param billingSession BillingSession to be ended.
     */
    public void endSession(@Nonnull BillingSession billingSession) {

        // Remove all the sessions from the non-billed items.
        boolean allFailed = billingSession.cancelSession();

        if (allFailed) {
            // If all removed then remove the session, totally.
            billingSessionDao.remove(billingSession);
        } else {
            // If some or all are billed, then just persist the updates.
            billingSessionDao.persist(billingSession);
        }
    }

    /**
     * Separation of the action of calling the quote server and updating the associated ledger entries.  This is to
     * separate the steps of billing a session into smaller finite transactions so we can record more to the database
     * sooner
     *
     * @param item                Representation of the quote and its ledger entries that are to be billed
     * @param quoteIsReplacing    Set if the price item is replacing a previously defined item.
     * @param quoteServerWorkItem the pointer back to the quote server transaction
     * @param sapDeliveryId
     * @param billingMessage
     */
    public void updateLedgerEntries(QuoteImportItem item, QuotePriceItem quoteIsReplacing, String quoteServerWorkItem,
                                    String sapDeliveryId, String billingMessage) {

        // Now that we have successfully billed, update the Ledger Entries associated with this QuoteImportItem
        // with the quote for the QuoteImportItem, add the priceItemType, and the success message.
        Collection<String> replacementPriceItemNames = new ArrayList<>();
        PriceItem priceItem = item.getPrimaryProduct().getPrimaryPriceItem();
        if(item.isQuoteServerOrder()) {
            Collection<QuotePriceItem> replacementPriceItems =
                    priceListCache.getReplacementPriceItems(priceItem);
            for (QuotePriceItem replacementPriceItem : replacementPriceItems) {
                replacementPriceItemNames.add(replacementPriceItem.getName());
            }
        }
        item.updateLedgerEntries(quoteIsReplacing, billingMessage, quoteServerWorkItem,
                replacementPriceItemNames, sapDeliveryId);
        billingSessionDao.flush();
    }

    /**
     * Separation of the action of calling the quote server and updating the associated ledger entries.  This is to
     * separate the steps of billing a session into smaller finite transactions so we can record more to the database
     * sooner
     *
     * @param item                Representation of the quote and its ledger entries that are to be billed
     * @param quoteServerWorkItem the pointer back to the quote server transaction
     * @param sapDeliveryId
     * @param billingMessage
     */
    public void updateSapLedgerEntries(QuoteImportItem item, String quoteServerWorkItem,
                                    String sapDeliveryId, String billingMessage) {

        item.updateSapLedgerEntries(billingMessage, quoteServerWorkItem,sapDeliveryId);
        billingSessionDao.flush();
    }

    /**
     * If the order's product supports automated billing, and it's not currently locked out,
     * generate a list of billing ledger items for the sample and add them to the billing ledger.
     *
     * @param orderKey          business key of order to bill for
     * @param aliquotId         the sample aliquot ID
     * @param completedDate     the date completed to use when billing
     * @param data              used to check and see if billing can occur
     * @param orderLockoutCache The cache by keys whether the order is locked out or not
     *
     * @return true if the auto-bill request was processed.  It will return false if PDO supports automated billing but
     * is currently locked out of billing.
     */
    public boolean autoBillSample(String orderKey, String aliquotId, Date completedDate,
                                  Map<String, MessageDataValue> data, Map<String, Boolean> orderLockoutCache)
            throws Exception {
        ProductOrder order = productOrderDao.findByBusinessKey(orderKey);
        if (order == null) {
            log.error(MessageFormat.format("Invalid PDO key ''{0}'', no billing will occur.", orderKey));
            return true;
        }

        Product product = order.getProduct();
        if (!product.isUseAutomatedBilling()) {
            log.debug(
                    MessageFormat.format("Product {0} does not support automated billing.", product.getProductName()));
            return true;
        }

        // Get the order's lock out state from the cache, if not there, query it and put into the cache for later.
        Boolean isOrderLockedOut = orderLockoutCache.get(order.getBusinessKey());
        if (isOrderLockedOut == null) {
            isOrderLockedOut = isAutomatedBillingLockedOut(order);
            orderLockoutCache.put(order.getBusinessKey(), isOrderLockedOut);
        }

        // Now can use the lockout boolean to decide whether to ignore the order for auto ledger entry.
        if (isOrderLockedOut) {
            log.error(MessageFormat.format("Cannot auto-bill order {0} because it is currently locked out.",
                    order.getJiraTicketKey()));

            // Return false to indicate we did not process the message.
            return false;
        }

        ProductOrderSample sample = mapAliquotIdToSample(order, aliquotId);
        if (sample == null) {
            log.info(MessageFormat.format(
                    "Could not bill PDO {0}, Aliquot {1}, all samples have been assigned aliquots already. This is likely rework or added coverage",
                    order.getBusinessKey(), aliquotId));
        } else {
            // Always bill if the sample is on risk, otherwise, check if the requirement is met for billing.
            if (sample.isOnRisk() || product.getRequirement().canBill(data)) {
                sample.autoBillSample(completedDate, BigDecimal.ONE);
            }
        }

        return true;
    }

    /**
     * Check and see if a given order is locked out, e.g. currently in a billing session or waiting for a billing
     * session because of the confirmation upload (and manual update) of the tracker spreadsheet.
     *
     * @param order the order to check
     *
     * @return true if the order is locked out.
     */
    private boolean isAutomatedBillingLockedOut(ProductOrder order) {
        ProductOrder[] orders = new ProductOrder[]{order};
        return !ledgerEntryDao.findUploadedUnbilledOrderList(orders).isEmpty() ||
               !ledgerEntryDao.findLockedOutByOrderList(orders).isEmpty();
    }

    /**
     * Convert a PDO aliquot into a PDO sample.  To do this:
     * <ol>
     * <li>Check & see if the aliquot is already set on a PDO sample. If so, we're done.</li>
     * <li>Convert aliquot ID to stock sample ID</li>
     * <li>Find sample with stock sample ID in PDO list with no aliquot set</li>
     * <li>set aliquot to passed in aliquot, persist data, and return the sample found</li>
     * </ol>
     */
    @DaoFree
    protected ProductOrderSample mapAliquotIdToSample(@Nonnull ProductOrder order, @Nonnull String aliquotId)
            throws Exception {

        // Convert aliquotId to BSP ID, if it's an LSID.
        if (!BSPUtil.isInBspFormat(aliquotId)) {
            aliquotId = BSPLSIDUtil.lsidToBareId(aliquotId); // todo jmt
        }

        for (ProductOrderSample sample : order.getSamples()) {
            if (aliquotId.equals(sample.getAliquotId())) {
                return sample;
            }
        }

        String sampleName = sampleDataFetcher.getStockIdForAliquotId(aliquotId);
        if (sampleName == null) {
            throw new Exception("Couldn't find a sample for aliquot: " + aliquotId);
        }

        boolean foundStock = false;
        for (ProductOrderSample sample : order.getSamples()) {
            if (sample.getName().equals(sampleName)) {
                foundStock = true;
                if (sample.getAliquotId() == null) {
                    sample.setAliquotId(aliquotId);
                    return sample;
                }
            }
        }

            /*
             * As long as a stock sample was found, then this is likely just rework or adding coverage. In this case, we can
             * actually ignore this aliquot because Picard will send Mercury identical metrics for each aliquot for the
             * aggregated sample, including one that matches the aliquot already saved on the PDO sample.
             */
        if (foundStock) {
            return null;
        }

        throw new Exception(
                MessageFormat.format("Could not bill PDO {0}, Sample {1}, Aliquot {2}, no matching sample in PDO.",
                        order.getBusinessKey(), sampleName, aliquotId)
        );
    }

    public void sendBillingCreditRequestEmail(QuoteImportItem quoteImportItem, Set<LedgerEntry> priorBillings,
                                              Long billedById) throws InformaticsServiceException {
        Collection<String> ccUsers = new HashSet<>(appConfig.getGpBillingManagers());
        BspUser billedBy = bspUserList.getById(billedById);
        if (billedBy!=null) {
            ccUsers.add(billedBy.getEmail());
        }
        BspUser orderPlacedBy = bspUserList.getById(quoteImportItem.getProductOrder().getCreatedBy());
        if (orderPlacedBy != null) {
            ccUsers.add(orderPlacedBy.getEmail());
        }

        Map<String, Object> rootMap = new HashMap<>();

        String sapDocuments = priorBillings.stream()
            .map(LedgerEntry::getSapDeliveryDocumentId)
            .filter(StringUtils::isNotBlank).distinct()
            .collect(Collectors.joining("<br/>"));

        StringBuilder discountText = new StringBuilder();
        if (StringUtils.equals(quoteImportItem.getQuotePriceType(), LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.getQuoteType())) {
            discountText.append(Boolean.TRUE.toString()).append(" -- ");
            final SAPMaterial discountedMaterial = productPriceCache.findByProduct(quoteImportItem.getProduct(),
                    quoteImportItem.getProductOrder().getSapCompanyConfigurationForProductOrder().getSalesOrganization());
            final BigDecimal discount = discountedMaterial.getPossibleDeliveryConditions().get(
                    DeliveryCondition.LATE_DELIVERY_DISCOUNT);

            discountText.append(NumberFormat.getCurrencyInstance().format(discount.doubleValue()));

        } else {
            discountText.append(Boolean.FALSE.toString());
        }

        rootMap.put("mercuryOrder", quoteImportItem.getProductOrder().getJiraTicketKey());
        rootMap.put("material", quoteImportItem.getProduct().getDisplayName());
        rootMap.put("sapOrderNumber", quoteImportItem.getProductOrder().getSapOrderNumber());
        rootMap.put("sapDeliveryDocuments", sapDocuments);
        rootMap.put("deliveryDiscount", discountText.toString());
        rootMap.put("quantity", quoteImportItem.getQuantity());

        String body;
        try {
            body = processTemplate(SapConfig.BILLING_CREDIT_TEMPLATE, rootMap);
        } catch (RuntimeException e) {
            throw new InformaticsServiceException("Error creating message body from template", e);
        }
        Deployment deployment = appConfig.getDeploymentConfig();
        boolean isProduction = deployment.equals(Deployment.PROD);

        emailSender.sendHtmlEmail(appConfig, sapConfig.getSapSupportEmail(), ccUsers,
            sapConfig.getSapReverseBillingSubject(), body, !isProduction, false);
    }

    protected String processTemplate(String template, Map<String, Object> objectMap) {
        StringWriter stringWriter = new StringWriter();
        templateEngine.processTemplate(template, objectMap, stringWriter);
        return stringWriter.toString();
    }
}
