package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BadBusinessKeyException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link ProductOrder}s.
 */
public class ProductOrderEjb {

    private final ProductOrderDao productOrderDao;

    private final ProductDao productDao;

    private final QuoteService quoteService;

    private final JiraService jiraService;

    private final UserBean userBean;

    private final BSPUserList userList;

    private final LedgerEntryDao ledgerEntryDao;

    private final BSPSampleDataFetcher sampleDataFetcher;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductOrderEjb() {
        this(null, null, null, null, null, null, null, null);
    }

    @Inject
    public ProductOrderEjb(ProductOrderDao productOrderDao,
                           ProductDao productDao,
                           QuoteService quoteService,
                           JiraService jiraService,
                           UserBean userBean,
                           BSPUserList userList,
                           LedgerEntryDao ledgerEntryDao,
                           BSPSampleDataFetcher sampleDataFetcher) {
        this.productOrderDao = productOrderDao;
        this.productDao = productDao;
        this.quoteService = quoteService;
        this.jiraService = jiraService;
        this.userBean = userBean;
        this.userList = userList;
        this.ledgerEntryDao = ledgerEntryDao;
        this.sampleDataFetcher = sampleDataFetcher;
    }

    private final Log log = LogFactory.getLog(ProductOrderEjb.class);

    private void validateUniqueProjectTitle(ProductOrder productOrder) throws DuplicateTitleException {
        if (productOrderDao.findByTitle(productOrder.getTitle()) != null) {
            throw new DuplicateTitleException();
        }
    }

    private void validateQuote(ProductOrder productOrder) throws QuoteNotFoundException {
        try {
            quoteService.getQuoteByAlphaId(productOrder.getQuoteId());
        } catch (QuoteServerException e) {
            throw new RuntimeException(e);
        }
    }

    // Could be static, but EJB spec does not like it.
    private void setSamples(ProductOrder productOrder, List<String> sampleIds) throws NoSamplesException {
        if (sampleIds.isEmpty()) {
            throw new NoSamplesException();
        }

        List<ProductOrderSample> orderSamples = new ArrayList<>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        productOrder.setSamples(orderSamples);
    }

    private void setAddOnProducts(ProductOrder productOrder, List<String> addOnPartNumbers) {
        List<Product> addOns =
                addOnPartNumbers.isEmpty() ? new ArrayList<Product>() : productDao.findByPartNumbers(addOnPartNumbers);

        productOrder.updateAddOnProducts(addOns);
    }

    // Could be static, but EJB spec does not like it.
    private void setStatus(ProductOrder productOrder) {
        // DRAFT orders not yet supported; force state of new PDOs to Submitted.
        productOrder.setOrderStatus(OrderStatus.Submitted);
    }

    /**
     * Including {@link QuoteNotFoundException} since this is an expected failure that may occur in application
     * validation.  This automatically sets the status of the {@link ProductOrder} to submitted.
     *
     * @param productOrder          product order
     * @param productOrderSampleIds sample IDs
     * @param addOnPartNumbers      add-on part numbers
     *
     * @throws QuoteNotFoundException
     */
    public void save(
            ProductOrder productOrder, List<String> productOrderSampleIds, List<String> addOnPartNumbers)
            throws DuplicateTitleException, QuoteNotFoundException, NoSamplesException {
        validateUniqueProjectTitle(productOrder);
        validateQuote(productOrder);
        setSamples(productOrder, productOrderSampleIds);
        setAddOnProducts(productOrder, addOnPartNumbers);
        setStatus(productOrder);
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
    @Nonnull
    @DaoFree
    protected ProductOrderSample mapAliquotIdToSample(@Nonnull ProductOrder order, @Nonnull String aliquotId)
            throws Exception {

        // Convert aliquotId to BSP ID, if it's an LSID.
        if (!BSPUtil.isInBspFormat(aliquotId)) {
            aliquotId = BSPLSIDUtil.lsidToBareId(aliquotId);
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

        for (ProductOrderSample sample : order.getSamples()) {
            if (sample.getName().equals(sampleName) && sample.getAliquotId() == null) {
                sample.setAliquotId(aliquotId);
                return sample;
            }
        }

        throw new Exception(
                MessageFormat.format("Could not bill PDO {0}, Sample {1}, Aliquot {2}, all" +
                                     " samples have been assigned aliquots already.",
                        order.getBusinessKey(), sampleName, aliquotId));
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
     *         is currently locked out of billing.
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

        // Always bill if the sample is on risk, otherwise, check if the requirement is met for billing.
        if (sample.isOnRisk() || product.getRequirement().canBill(data)) {
            sample.autoBillSample(completedDate, 1);
        }

        return true;
    }

    /**
     * Calculate the risk for all samples on the product order specified by business key.
     *
     * @param productOrderKey The product order business key.
     *
     * @throws Exception Any errors in reporting the risk.
     */
    public void calculateRisk(String productOrderKey) throws Exception {
        calculateRisk(productOrderKey, null);
    }

    /**
     * Calculate the risk for all samples on the product order specified by business key. If the samples
     * parameter is null, then all samples on the order will be used.
     *
     * @param productOrderKey The product order business key.
     * @param samples         The samples to calculate risk on. Null if all.
     *
     * @throws Exception Any errors in reporting the risk.
     */
    public void calculateRisk(String productOrderKey, List<ProductOrderSample> samples) throws Exception {
        ProductOrder editOrder = productOrderDao.findByBusinessKey(productOrderKey);
        if (editOrder == null) {
            String message = MessageFormat.format("Invalid PDO key ''{0}'', for calculating risk.", productOrderKey);
            throw new BadBusinessKeyException(message);
        }

        try {
            // If null, then it will calculate for all samples.
            if (samples == null) {
                editOrder.calculateRisk();
            } else {
                editOrder.calculateRisk(samples);
            }
        } catch (Exception ex) {
            log.error("Could not calculate risk", ex);
            throw ex;
        }

        // Set the create and modified information.
        editOrder.prepareToSave(userBean.getBspUser());
    }

    public void setManualOnRisk(
            BspUser user, String productOrderKey,
            List<ProductOrderSample> orderSamples, boolean riskStatus, String riskComment) {

        ProductOrder editOrder = productOrderDao.findByBusinessKey(productOrderKey);

        // If we are creating a manual on risk, then need to set it up and persist it for reuse.
        RiskCriterion criterion = null;
        if (riskStatus) {
            criterion = RiskCriterion.createManual();
            productOrderDao.persist(criterion);
        }

        for (ProductOrderSample sample : orderSamples) {
            if (riskStatus) {
                sample.setManualOnRisk(criterion, riskComment);
            } else {
                sample.setManualNotOnRisk(riskComment);
            }
        }

        // Set the create and modified information.
        editOrder.prepareToSave(user);
    }

    /**
     * Utility class to help with mapping from display names of custom fields to their {@link CustomFieldDefinition}s,
     * as well as tracking changes that have been made to the values of those fields relative to the existing state
     * of the PDO JIRA ticket.
     * <p/>
     * This inner class has to be static or Weld crashes with ArrayIndexOutOfBoundsExceptions.
     */
    private static class PDOUpdateField {
        private final String displayName;
        private final Object newValue;
        private final CustomField.SubmissionField field;

        /**
         * True if the field being updated is a 'bulk' item.  This means that it should be shown as plural in the
         * message, and its contents won't be shown in the message.
         */
        private final boolean isBulkField;

        /**
         * Return the update message appropriate for this field.  If there are no changes this will return the empty
         * string, otherwise a string of the form "Product was updated from 'Old Product' to 'New Product'".
         *
         * @param productOrder             contains the new values
         * @param customFieldDefinitionMap contains the mapping from display names of fields to their JIRA IDs, needed
         *                                 to dig the old values contained in the fields out of the issueFieldsResponse
         * @param issueFieldsResponse      contains the old values
         *
         * @return the update message that goes in the JIRA ticket
         */
        public String getUpdateMessage(ProductOrder productOrder,
                                       Map<String, CustomFieldDefinition> customFieldDefinitionMap,
                                       IssueFieldsResponse issueFieldsResponse) {

            if (!customFieldDefinitionMap.containsKey(displayName)) {
                throw new RuntimeException(
                        "Custom field '" + displayName + "' not found in issue " + productOrder.getJiraTicketKey());
            }
            CustomFieldDefinition customFieldDefinition = customFieldDefinitionMap.get(displayName);

            Object previousValue = issueFieldsResponse.getFields().get(customFieldDefinition.getJiraCustomFieldId());

            Object oldValueToCompare = (previousValue != null) ? previousValue : "";
            Object newValueToCompare = newValue;

            if (newValue instanceof CreateFields.Reporter) {
                // Need to special case Reporter type for display and comparison.
                oldValueToCompare = ((Map<?, ?>) previousValue).get("name").toString();
                newValueToCompare = ((CreateFields.Reporter) newValue).getName();
            }
            if (!oldValueToCompare.equals(newValueToCompare)) {
                return displayName + (isBulkField ? " have " : " has ") + "been updated" +
                       (!isBulkField ? " from '" + oldValueToCompare + "' to '" + newValueToCompare + "'" : "") + ".\n";
            }
            return "";
        }

        public PDOUpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue,
                              boolean isBulkField) {
            this.field = field;
            displayName = field.getName();
            this.newValue = newValue;
            this.isBulkField = isBulkField;
        }

        public PDOUpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue) {
            this(field, newValue, false);
        }
    }

    /**
     * Update the JIRA issue, executing the 'Developer Edit' transition to effect edits of fields that are read-only
     * on the UI.  Add a comment to the issue to indicate what was changed and by whom.
     *
     * @param productOrder Product Order.
     *
     * @throws IOException
     */
    public void updateJiraIssue(ProductOrder productOrder) throws IOException, QuoteNotFoundException {

        validateQuote(productOrder);

        Transition transition = jiraService.findAvailableTransitionByName(productOrder.getJiraTicketKey(),
                JiraTransition.DEVELOPER_EDIT.getStateName());

        List<PDOUpdateField> pdoUpdateFields = new ArrayList<>(Arrays.asList(
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT, productOrder.getProduct().getProductName()),
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT_FAMILY,
                        productOrder.getProduct().getProductFamily().getName()),
                new PDOUpdateField(ProductOrder.JiraField.QUOTE_ID, productOrder.getQuoteId()),
                new PDOUpdateField(ProductOrder.JiraField.SAMPLE_IDS, productOrder.getSampleString(), true),
                new PDOUpdateField(ProductOrder.JiraField.REPORTER,
                        new CreateFields.Reporter(userList.getById(productOrder.getCreatedBy()).getUsername()))));

        // Add the Requisition key to the list of fields when appropriate.
        if (Deployment.isCRSP && !StringUtils.isBlank(productOrder.getRequisitionKey())) {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.REQUISITION_ID, productOrder.getRequisitionKey()));
        }

        String[] customFieldNames = new String[pdoUpdateFields.size()];

        int i = 0;
        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFieldNames[i++] = pdoUpdateField.displayName;
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getCustomFields(customFieldNames);

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(productOrder.getJiraTicketKey(), customFieldDefinitions.values());

        List<CustomField> customFields = new ArrayList<>();

        StringBuilder updateCommentBuilder = new StringBuilder();

        for (PDOUpdateField field : pdoUpdateFields) {
            String message = field.getUpdateMessage(productOrder, customFieldDefinitions, issueFieldsResponse);
            if (!message.isEmpty()) {
                customFields.add(new CustomField(customFieldDefinitions, field.field, field.newValue));
                updateCommentBuilder.append(message);
            }
        }
        String updateComment = updateCommentBuilder.toString();

        // If we detect from the comment that nothing has changed, make a note of that.  The user may have changed
        // something in the PDO that is not reflected in JIRA, like add-ons.
        String comment = "\n" + productOrder.getJiraTicketKey() + " was edited by "
                         + userBean.getLoginUserName() + "\n\n"
                         + (updateComment.isEmpty() ? "No JIRA Product Order fields were updated\n\n" : updateComment);

        jiraService.postNewTransition(productOrder.getJiraTicketKey(), transition, customFields, comment);
    }

    /**
     * Allow updated quotes, products, and add-ons.
     *
     * @param productOrder     product order
     * @param addOnPartNumbers add-on part numbers
     */
    public void update(ProductOrder productOrder, List<String> addOnPartNumbers) throws QuoteNotFoundException {
        // update JIRA ticket with new quote
        // GPLIM-488
        try {
            updateJiraIssue(productOrder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // In the PDO edit UI, if the user goes through and edits the quote and then hits 'Submit', this works
        // without the merge.  But if the user tabs out of the quote field before hitting 'Submit', this merge
        // is required because this method receives a detached ProductOrder instance.

        // Also we would get a detached instance after quote validation failures GPLIM-488
        ProductOrder updatedProductOrder = productOrderDao.getEntityManager().merge(productOrder);

        // update add-ons, first remove old
        for (ProductOrderAddOn productOrderAddOn : updatedProductOrder.getAddOns()) {
            productOrderDao.remove(productOrderAddOn);
        }

        // set new add-ons in
        Set<ProductOrderAddOn> productOrderAddOns = new HashSet<>();
        for (Product addOn : productDao.findByPartNumbers(addOnPartNumbers)) {
            ProductOrderAddOn productOrderAddOn = new ProductOrderAddOn(addOn, updatedProductOrder);
            productOrderDao.persist(productOrderAddOn);
            productOrderAddOns.add(productOrderAddOn);
        }

        updatedProductOrder.setProductOrderAddOns(productOrderAddOns);

    }

    public static class NoSuchPDOException extends Exception {
        public NoSuchPDOException(String s) {
            super(s);
        }
    }

    public static class SampleDeliveryStatusChangeException extends Exception {
        protected SampleDeliveryStatusChangeException(DeliveryStatus targetStatus,
                                                      @Nonnull List<ProductOrderSample> samples) {
            super(createErrorMessage(targetStatus, samples));
        }

        protected static String createErrorMessage(DeliveryStatus status,
                                                   List<ProductOrderSample> samples) {
            List<String> messages = new ArrayList<>();

            for (ProductOrderSample sample : samples) {
                messages.add(sample.getName() + " @ " + sample.getSamplePosition()
                             + " : current status " + sample.getDeliveryStatus().getDisplayName());
            }

            return "Cannot transition samples to status " + status.getDisplayName()
                   + ": " + StringUtils.join(messages, ", ");
        }
    }

    /**
     * Utility method to find PDO by JIRA ticket key and throw exception if it is not found.
     *
     * @param jiraTicketKey JIRA ticket key.
     *
     * @return {@link ProductOrder}
     *
     * @throws NoSuchPDOException
     */
    @Nonnull
    private ProductOrder findProductOrder(@Nonnull String jiraTicketKey) throws NoSuchPDOException {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraTicketKey);

        if (productOrder == null) {
            throw new NoSuchPDOException(jiraTicketKey);
        }

        return productOrder;
    }

    /**
     * Transition the delivery statuses of the specified samples in the DB.
     *
     * @param order                      PDO containing the samples in question.
     * @param acceptableStartingStatuses A Set of {@link DeliveryStatus}es
     *                                   in which samples are allowed to be before undergoing this transition.
     * @param targetStatus               The status into which the samples will be transitioned.
     * @param samples                    The samples in question.
     *
     * @throws SampleDeliveryStatusChangeException
     *          Thrown if any samples are found to not be in an acceptable starting status.
     */
    private void transitionSamples(ProductOrder order,
                                   Set<ProductOrderSample.DeliveryStatus> acceptableStartingStatuses,
                                   DeliveryStatus targetStatus,
                                   Collection<ProductOrderSample> samples) throws SampleDeliveryStatusChangeException {

        Set<ProductOrderSample> transitionSamples = new HashSet<>(samples);

        List<ProductOrderSample> untransitionableSamples = new ArrayList<>();

        for (ProductOrderSample sample : order.getSamples()) {
            // If the transition sample set is empty we try to transition all samples in the PDO.
            if (CollectionUtils.isEmpty(transitionSamples) || transitionSamples.contains(sample)) {
                if (!acceptableStartingStatuses.contains(sample.getDeliveryStatus())) {
                    untransitionableSamples.add(sample);
                    // Keep looping, find all the untransitionable samples and then throw a descriptive exception.
                } else {
                    sample.setDeliveryStatus(targetStatus);
                }
            }
        }

        if (!untransitionableSamples.isEmpty()) {
            throw new SampleDeliveryStatusChangeException(targetStatus, untransitionableSamples);
        }

        // Update the PDO as modified.
        order.prepareToSave(userBean.getBspUser());
    }

    /**
     * Transition the specified samples to the specified target status, adding a comment to the JIRA ticket, does NOT
     * transition the JIRA ticket status as this is called from sample transition methods only and not whole PDO transition
     * methods.  Per GPLIM-655 we need to update per-sample comments too, but not currently doing that.
     *
     * @param jiraTicketKey              JIRA ticket key.
     * @param acceptableStartingStatuses Acceptable staring statuses for samples.
     * @param targetStatus               New status for samples.
     * @param samples                    Samples to change.
     *
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     *
     * @throws IOException
     */
    private void transitionSamplesAndUpdateTicket(String jiraTicketKey,
                                                  Set<ProductOrderSample.DeliveryStatus> acceptableStartingStatuses,
                                                  DeliveryStatus targetStatus,
                                                  Collection<ProductOrderSample> samples)
            throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException {
        ProductOrder order = findProductOrder(jiraTicketKey);

        transitionSamples(order, acceptableStartingStatuses, targetStatus, samples);

        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());

        issue.addComment(MessageFormat.format("{0} transitioned samples to status {1}: {2}",
                getUserName(), targetStatus.getDisplayName(),
                StringUtils.join(ProductOrderSample.getSampleNames(samples), ",")));
    }

    /**
     * @return The name of the currently logged-in user or 'Mercury' if no logged in user (e.g. in a fixup test context).
     */
    private String getUserName() {
        String user = userBean.getLoginUserName();
        return user == null ? "Mercury" : user;
    }

    /**
     * JIRA Transition states used by PDOs.
     */
    public enum JiraTransition {
        ORDER_COMPLETE("Order Complete"),
        OPEN("Open"),
        CLOSED("Closed"),
        CANCEL("Cancel"),
        COMPLETE_ORDER("Complete Order"),
        CREATE_WORK_REQUEST("Create Work Request"),
        DEVELOPER_EDIT("Developer Edit");

        /**
         * The text that represents this transition state in JIRA.
         */
        private final String stateName;

        private JiraTransition(String stateName) {
            this.stateName = stateName;
        }

        public String getStateName() {
            return stateName;
        }
    }

    /**
     * JIRA Resolutions used by PDOs.
     */
    private enum JiraResolution {
        UNRESOLVED("Unresolved"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        /**
         * The text that represents this resolution in JIRA.
         */
        private final String text;

        JiraResolution(String text) {
            this.text = text;
        }

        static JiraResolution fromString(String text) {
            for (JiraResolution status : values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * JIRA Status used by PDOs. Each status contains two transitions. One will transition to Open, one to Closed.
     */
    private enum JiraStatus {
        OPEN("Open", null, JiraTransition.COMPLETE_ORDER),
        WORK_REQUEST_CREATED("Work Request Created", null, JiraTransition.ORDER_COMPLETE),
        CLOSED("Closed", JiraTransition.OPEN, null),
        REOPENED("Reopened", JiraTransition.OPEN, JiraTransition.CLOSED),
        CANCELLED("Cancelled", JiraTransition.OPEN, JiraTransition.CLOSED),
        UNKNOWN(null, null, null);

        /**
         * The text that represents this status in JIRA.
         */
        private final String text;

        /**
         * Transition to use to get to 'Open'. Null if we are already Open.
         */
        private final JiraTransition toOpen;

        /**
         * Transition to use to get to 'Closed'. Null if we are already Closed.
         */
        private final JiraTransition toClosed;

        JiraStatus(String text, JiraTransition toOpen, JiraTransition toClosed) {
            this.text = text;
            this.toOpen = toOpen;
            this.toClosed = toClosed;
        }

        static JiraStatus fromString(String text) {
            for (JiraStatus status : values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * This is a version of {@link #updateOrderStatus} that does not propagate RuntimeExceptions since those cause a
     * transaction in progress to be marked for rollback.
     * Rollback on failures to update JIRA tickets with status changes is undesirable in billing as the status change is
     * fairly inconsequential in comparison to persisting database records of whether work was billed to the quote server.
     */
    public void updateOrderStatusNoRollback(@Nonnull String jiraTicketKey)
            throws NoSuchPDOException, IOException, JiraIssue.NoTransitionException {

        try {
            updateOrderStatus(jiraTicketKey);
        } catch (RuntimeException e) {
            // If we failed to update the order status be sure to log it here as the caller will not be receiving a
            // RuntimeException due to transaction rollback issues and will not be aware there was a problem.
            log.error("Error updating order status for " + jiraTicketKey, e);
        }
    }

    /**
     * Update the order status of a PDO, based on the rules in {@link ProductOrder#updateOrderStatus}.  Any status
     * changes are pushed to JIRA as well, with a comment about the change and the current user.
     *
     * @param jiraTicketKey the key to update
     *
     * @return true if the status was changed
     *
     * @throws NoSuchPDOException
     * @throws IOException
     * @throws JiraIssue.NoTransitionException
     *
     */
    public boolean updateOrderStatus(@Nonnull String jiraTicketKey)
            throws NoSuchPDOException, IOException, JiraIssue.NoTransitionException {
        // Since we can't directly change the JIRA status of a PDO, we need to use a JIRA transition which in turn will
        // update the status.
        ProductOrder order = findProductOrder(jiraTicketKey);
        if (order.updateOrderStatus()) {
            String operation;
            JiraIssue issue = jiraService.getIssue(jiraTicketKey);
            Object statusValue = issue.getField(ProductOrder.JiraField.STATUS.getName());
            JiraStatus status = JiraStatus.fromString(((Map<?, ?>) statusValue).get("name").toString());
            JiraTransition transition;
            if (order.getOrderStatus() == OrderStatus.Completed) {
                operation = "Completed";
                transition = status.toClosed;
            } else {
                operation = "Opened";
                transition = status.toOpen;
            }
            if (transition != null) {
                issue.postTransition(transition.getStateName(),
                        getUserName() + " performed " + operation + " transition");
            }
            return true;
        }
        return false;
    }

    /**
     * Transition the specified JIRA ticket using the specified transition, adding the specified comment.
     *
     * @param jiraTicketKey      JIRA ticket key.
     * @param currentResolution  if the JIRA's resolution is already this value, do not do anything.
     * @param state              The transition to use.
     * @param transitionComments Comments to include as part of the transition, will be appended to the JIRA ticket.
     *
     * @throws IOException
     * @throws JiraIssue.NoTransitionException
     *                     Thrown if the specified transition is not available on the specified issue.
     */
    private void transitionJiraTicket(String jiraTicketKey,
                                      JiraResolution currentResolution,
                                      JiraTransition state,
                                      @Nullable String transitionComments)
            throws IOException, JiraIssue.NoTransitionException {
        JiraIssue issue = jiraService.getIssue(jiraTicketKey);
        JiraResolution resolution = JiraResolution.fromString(issue.getResolution());
        if (currentResolution == resolution) {
            String jiraCommentText = getUserName() + " performed " + state.getStateName() + " transition";
            if (transitionComments != null) {
                jiraCommentText = jiraCommentText + ": " + transitionComments;
            }
            issue.postTransition(state.getStateName(), jiraCommentText);
        }
    }

    /**
     * Abandon the whole PDO with a comment.
     *
     * @param jiraTicketKey   JIRA ticket key.
     * @param abandonComments Transition comments.
     *
     * @throws JiraIssue.NoTransitionException
     *
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     *
     */
    public void abandon(@Nonnull String jiraTicketKey, @Nullable String abandonComments)
            throws JiraIssue.NoTransitionException, NoSuchPDOException, SampleDeliveryStatusChangeException,
            IOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        productOrder.setOrderStatus(OrderStatus.Abandoned);

        transitionSamples(productOrder, EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED),
                DeliveryStatus.ABANDONED, productOrder.getSamples());

        // Currently not setting abandon comments into PDO comments, that seems too intrusive.  We will record the comments
        // with the JIRA ticket.
        transitionJiraTicket(jiraTicketKey, JiraResolution.CANCELLED, JiraTransition.CANCEL, abandonComments);
    }

    /**
     * Sample abandonment method with parameter types guessed as appropriate for use with Stripes.
     *
     * @param jiraTicketKey JIRA ticket key of the PDO in question
     * @param samples       the samples to abandon
     *
     * @throws IOException
     * @throws SampleDeliveryStatusChangeException
     *
     * @throws NoSuchPDOException
     */
    public void abandonSamples(@Nonnull String jiraTicketKey, Collection<ProductOrderSample> samples)
            throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException {
        transitionSamplesAndUpdateTicket(jiraTicketKey,
                EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED), DeliveryStatus.ABANDONED, samples);
    }
}
