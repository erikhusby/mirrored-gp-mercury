package org.broadinstitute.gpinformatics.athena.boundary.orders;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPKitRequestService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BadBusinessKeyException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    private ProductOrderDao productOrderDao;

    private ProductDao productDao;

    private QuoteService quoteService;

    private JiraService jiraService;

    private UserBean userBean;

    private BSPUserList userList;

    private BucketEjb bucketEjb;

    private SquidConnector squidConnector;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    private ProductOrderSampleDao productOrderSampleDao;

    private MercurySampleDao mercurySampleDao;

    private ProductOrderJiraUtil productOrderJiraUtil;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductOrderEjb() {
    }

    @Inject
    public ProductOrderEjb(ProductOrderDao productOrderDao,
                           ProductDao productDao,
                           QuoteService quoteService,
                           JiraService jiraService,
                           UserBean userBean,
                           BSPUserList userList,
                           BucketEjb bucketEjb,
                           SquidConnector squidConnector,
                           MercurySampleDao mercurySampleDao,
                           ProductOrderJiraUtil productOrderJiraUtil) {
        this.productOrderDao = productOrderDao;
        this.productDao = productDao;
        this.quoteService = quoteService;
        this.jiraService = jiraService;
        this.userBean = userBean;
        this.userList = userList;
        this.bucketEjb = bucketEjb;
        this.squidConnector = squidConnector;
        this.mercurySampleDao = mercurySampleDao;
        this.productOrderJiraUtil = productOrderJiraUtil;
    }

    private final Log log = LogFactory.getLog(ProductOrderEjb.class);

    /**
     * Remove all non-received samples from the order.
     */
    public void removeNonReceivedSamples(ProductOrder editOrder,
                                         MessageReporter reporter) throws NoSuchPDOException, IOException {
        // Note that calling getReceivedSampleCount() will cause the sample data for all samples to be
        // fetched if it hasn't been already. This is good because without it each call to getSampleData() below
        // would fetch it one sample at a time.
        List<ProductOrderSample> samplesToRemove =
                new ArrayList<>(editOrder.getSampleCount() - editOrder.getReceivedSampleCount());
        for (ProductOrderSample sample : editOrder.getSamples()) {
            if (!sample.getSampleData().isSampleReceived()) {
                samplesToRemove.add(sample);
            }
        }

        if (!samplesToRemove.isEmpty()) {
            removeSamples(editOrder.getJiraTicketKey(), samplesToRemove, reporter);
        }
    }

    /**
     * Persisting a Product order.  This method's primary job is to Support a call from the Product Order Action bean
     * to wrap the persistence of an order in a transaction.
     *
     * @param saveType            Indicates what type of persistence this is:  Save, Update, etc
     * @param editedProductOrder  The product order entity to be persisted
     * @param deletedIds          a collection that represents the ID's of productOrderKitDetails to be deleted form
     *                            the kit collection
     * @param kitDetailCollection a collection of product order details that have been created or updated from
     *                            the UI
     *
     * @throws IOException
     * @throws QuoteNotFoundException
     */
    public void persistProductOrder(ProductOrder.SaveType saveType, ProductOrder editedProductOrder,
                                    @Nonnull Collection<String> deletedIds,
                                    @Nonnull Collection<ProductOrderKitDetail> kitDetailCollection)
            throws IOException, QuoteNotFoundException {

        kitDetailCollection.removeAll(Collections.singleton(null));
        deletedIds.removeAll(Collections.singleton(null));

        editedProductOrder.prepareToSave(userBean.getBspUser(), saveType);

        if (editedProductOrder.isDraft()) {
            if (editedProductOrder.isSampleInitiation()) {
                Map<Long, ProductOrderKitDetail> mapKitDetailsByIDs = new HashMap<>();
                Iterator<ProductOrderKitDetail> kitDetailIterator =
                        editedProductOrder.getProductOrderKit().getKitOrderDetails().iterator();
                while (kitDetailIterator.hasNext()) {
                    ProductOrderKitDetail kitDetail = kitDetailIterator.next();
                    if (deletedIds.contains(kitDetail.getProductOrderKitDetailId().toString())) {
                        kitDetailIterator.remove();
                    } else {
                        mapKitDetailsByIDs.put(kitDetail.getProductOrderKitDetailId(), kitDetail);
                    }
                }

                for (ProductOrderKitDetail kitDetailUpdate : kitDetailCollection) {
                    if (kitDetailUpdate.getProductOrderKitDetailId() != null &&
                        mapKitDetailsByIDs.containsKey(kitDetailUpdate.getProductOrderKitDetailId())) {

                        mapKitDetailsByIDs.get(kitDetailUpdate.getProductOrderKitDetailId()).updateDetailValues(
                                kitDetailUpdate);

                    } else {
                        editedProductOrder.getProductOrderKit().addKitOrderDetail(kitDetailUpdate);
                    }
                }
            }
        } else {
            updateJiraIssue(editedProductOrder);
        }
        productOrderDao.persist(editedProductOrder);
    }

    /**
     * Looks up the quote for the pdo (if the pdo has one) in the
     * quote server.
     */
    void validateQuote(ProductOrder productOrder, QuoteService quoteService) throws QuoteNotFoundException {
        if (!StringUtils.isEmpty(productOrder.getQuoteId())) {
            try {
                quoteService.getQuoteByAlphaId(productOrder.getQuoteId());
            } catch (QuoteServerException e) {
                throw new RuntimeException("Failed to find quote for " + productOrder.getQuoteId(), e);
            }
        }
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
            String message = "Could not calculate risk.";
            log.error(message, ex);
            throw new InformaticsServiceException(message, ex);
        }

        // Set the create and modified information.
        editOrder.prepareToSave(userBean.getBspUser());
    }

    /**
     * Manually update risk of samples in a product order
     *
     * @param user            User performing action
     * @param productOrderKey Key of project being updated.
     * @param orderSamples    Samples who's risk is to be manually updated.
     * @param riskStatus      New risk status
     * @param riskComment     Comment of reason why samples' risk is being updated
     *
     * @throws IOException
     */
    public void addManualOnRisk(@Nonnull BspUser user, @Nonnull String productOrderKey,
                                List<ProductOrderSample> orderSamples, boolean riskStatus, @Nonnull String riskComment)
            throws IOException {
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

        // Add comment about the risk status to jira.
        updateJiraCommentForRisk(productOrderKey, user, riskComment, orderSamples.size(), riskStatus);
    }

    /**
     * Post a comment in jira about risk.
     *
     * @param jiraKey     Key of jira issue to add comment to.
     * @param comment     User supplied comment on change of risk for samples
     * @param sampleCount number of effected samples
     * @param isRisk      new risk of samples
     *
     * @throws IOException
     */
    private void updateJiraCommentForRisk(@Nonnull String jiraKey, @Nonnull BspUser bspUser, @Nonnull String comment,
                                          int sampleCount, boolean isRisk) throws IOException {
        String issueComment = buildJiraCommentForRiskString(comment, bspUser.getUsername(), sampleCount, isRisk);
        jiraService.addComment(jiraKey, issueComment);
    }

    String buildJiraCommentForRiskString(String comment, String username, int sampleCount, boolean isRisk) {
        return String.format("%s set manual on risk to %s for %d samples with comment:\n%s",
                username, isRisk, sampleCount, comment);
    }

    public void handleSamplesAdded(@Nonnull String productOrderKey, @Nonnull Collection<ProductOrderSample> newSamples,
                                   @Nonnull MessageReporter reporter) {
        ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);
        // Only add samples to the LIMS bucket if the order is ready for lab work.
        if (order.readyForLab()) {
            Collection<ProductOrderSample> samples = bucketEjb.addSamplesToBucket(order, newSamples);
            if (!samples.isEmpty()) {
                reporter.addMessage("{0} samples have been added to the pico bucket.", samples.size());
            }
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
        validateQuote(productOrder, quoteService);

        Transition transition = jiraService.findAvailableTransitionByName(productOrder.getJiraTicketKey(),
                JiraTransition.DEVELOPER_EDIT.getStateName());

        List<PDOUpdateField> pdoUpdateFields = new ArrayList<>(Arrays.asList(
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT, productOrder.getProduct().getProductName()),
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT_FAMILY,
                        productOrder.getProduct().getProductFamily().getName()),
                new PDOUpdateField(ProductOrder.JiraField.SAMPLE_IDS, productOrder.getSampleString(), true),
                new PDOUpdateField(ProductOrder.JiraField.REPORTER,
                        new CreateFields.Reporter(userList.getById(productOrder.getCreatedBy())
                                                          .getUsername()))));

        if (productOrder.getProduct().getSupportsNumberOfLanes()) {
            pdoUpdateFields.add(
                    new PDOUpdateField(ProductOrder.JiraField.LANES_PER_SAMPLE, productOrder.getLaneCount()));
        }

        pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.SUMMARY, productOrder.getTitle()));

        pdoUpdateFields.add(PDOUpdateField.createPDOUpdateFieldForQuote(productOrder));

        List<String> addOnList = new ArrayList<>(productOrder.getAddOns().size());
        for (ProductOrderAddOn addOn : productOrder.getAddOns()) {
            addOnList.add(addOn.getAddOn().getDisplayName());
        }
        Collections.sort(addOnList);
        pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.ADD_ONS, StringUtils.join(addOnList, "\n")));

        if (!StringUtils.isBlank(productOrder.getComments())) {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.DESCRIPTION, productOrder.getComments()));
        }

        if (productOrder.getFundingDeadline() == null) {
            pdoUpdateFields.add(PDOUpdateField.clearedPDOUpdateField(ProductOrder.JiraField.FUNDING_DEADLINE));
        } else {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.FUNDING_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(
                            productOrder.getFundingDeadline())));
        }

        if (productOrder.getPublicationDeadline() == null) {
            pdoUpdateFields.add(
                    PDOUpdateField.clearedPDOUpdateField(ProductOrder.JiraField.PUBLICATION_DEADLINE));
        } else {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.PUBLICATION_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(
                            productOrder.getPublicationDeadline())));
        }

        // Add the Requisition name to the list of fields when appropriate.
        if (ApplicationInstance.CRSP.isCurrent() && !StringUtils.isBlank(productOrder.getRequisitionName())) {
            pdoUpdateFields.add(new PDOUpdateField(ProductOrder.JiraField.REQUISITION_NAME,
                    productOrder.getRequisitionName()));
        }

        String[] customFieldNames = new String[pdoUpdateFields.size()];

        int i = 0;
        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFieldNames[i++] = pdoUpdateField.getDisplayName();
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getCustomFields(customFieldNames);

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(productOrder.getJiraTicketKey(), customFieldDefinitions.values());

        List<CustomField> customFields = new ArrayList<>();

        StringBuilder updateCommentBuilder = new StringBuilder();

        for (PDOUpdateField field : pdoUpdateFields) {
            String message = field.getUpdateMessage(productOrder, customFieldDefinitions, issueFieldsResponse);
            if (!message.isEmpty()) {
                customFields.add(field.createCustomField(customFieldDefinitions));
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
        private static final long serialVersionUID = -5418019063691592665L;

        public NoSuchPDOException(String s) {
            super(s);
        }
    }

    public static class SampleDeliveryStatusChangeException extends Exception {
        private static final long serialVersionUID = 8651172992194864707L;

        protected SampleDeliveryStatusChangeException(DeliveryStatus targetStatus,
                                                      @Nonnull List<ProductOrderSample> samples) {
            super(createErrorMessage(targetStatus, samples));
        }

        protected static String createErrorMessage(DeliveryStatus status,
                                                   List<ProductOrderSample> samples) {
            List<String> messages = new ArrayList<>();

            for (ProductOrderSample sample : samples) {
                messages.add(sample.getName() + " @ " + sample.getSamplePosition()
                             + " : current status " + (StringUtils.isNotBlank(
                        sample.getDeliveryStatus().getDisplayName()) ? sample.getDeliveryStatus().getDisplayName() :
                        "Not Started"));
            }

            return "Cannot transition samples to status " + (StringUtils.isNotBlank(status.getDisplayName()) ?
                    status.getDisplayName() : "Not Started") + ": " + StringUtils.join(messages, ", ");
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

    public ProductOrder findProductOrderByBusinessKeySafely(@Nonnull String jiraTicket,
                                                            ProductOrderDao.FetchSpec... fetchSpecs) {
        return productOrderDao.findByBusinessKey(jiraTicket, LockModeType.PESSIMISTIC_READ, fetchSpecs);
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
     * @throws SampleDeliveryStatusChangeException Thrown if any samples are found to not be in an acceptable starting status.
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
     * @param comment                    optional user supplied comment about this action.
     *
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     * @throws IOException
     */
    private void transitionSamplesAndUpdateTicket(String jiraTicketKey,
                                                  Set<DeliveryStatus> acceptableStartingStatuses,
                                                  DeliveryStatus targetStatus,
                                                  Collection<ProductOrderSample> samples, String comment)
            throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException {
        ProductOrder order = findProductOrder(jiraTicketKey);

        transitionSamples(order, acceptableStartingStatuses, targetStatus, samples);

        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} transitioned samples to status {1}: {2}\n\n{3}",
                getUserName(), targetStatus.getDisplayName(),
                StringUtils.join(ProductOrderSample.getSampleNames(samples), ","),
                StringUtils.stripToEmpty(comment)));
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

        JiraTransition(String stateName) {
            this.stateName = stateName;
        }

        public String getStateName() {
            return stateName;
        }
    }

    /**
     * JIRA Resolutions used by PDOs.
     */
    enum JiraResolution {
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
    protected enum JiraStatus {
        PENDING("Pending", JiraTransition.OPEN, JiraTransition.CLOSED),
        OPEN("Open", null, JiraTransition.COMPLETE_ORDER),
        // This status is only set in the JIRA UI. In Mercury it has the same behavior as Open.
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
         * Transition to use to get to 'Open'. Null indicates a transition is not possible.
         */
        private final JiraTransition toOpen;

        /**
         * Transition to use to get to 'Closed'. Null indicates a transition is not possible.
         */
        private final JiraTransition toClosed;

        JiraStatus(String text, JiraTransition toOpen, JiraTransition toClosed) {
            this.text = text;
            this.toOpen = toOpen;
            this.toClosed = toClosed;
        }

        /**
         * Given a JIRA issue, return its status as a JiraStatus.
         */
        public static JiraStatus fromIssue(JiraIssue issue) throws IOException {
            Object statusValue = issue.getField(ProductOrder.JiraField.STATUS.getName());
            String text = ((Map<?, ?>) statusValue).get("name").toString();
            for (JiraStatus status : values()) {
                if (status.text.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return UNKNOWN;
        }

        /**
         * Given a PDO status, return the transition required to get to the corresponding JIRA status from this one.
         */
        public JiraTransition getTransitionTo(OrderStatus orderStatus) {
            if (orderStatus == OrderStatus.Completed) {
                return toClosed;
            }
            return toOpen;
        }
    }

    /**
     * This is a version of {@link #updateOrderStatus} that does not propagate RuntimeExceptions since those cause a
     * transaction in progress to be marked for rollback.
     * Rollback on failures to update JIRA tickets with status changes is undesirable in billing as the status change is
     * fairly inconsequential in comparison to persisting database records of whether work was billed to the quote server.
     */
    public void updateOrderStatusNoRollback(@Nonnull String jiraTicketKey) throws NoSuchPDOException, IOException {
        try {
            updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
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
     */
    public void updateOrderStatus(@Nonnull String jiraTicketKey, @Nonnull MessageReporter reporter)
            throws NoSuchPDOException, IOException {
        // Since we can't directly change the JIRA status of a PDO, we need to use a JIRA transition which in turn will
        // update the status.
        ProductOrder order = findProductOrder(jiraTicketKey);
        if (order.updateOrderStatus()) {
            transitionIssueToSameOrderStatus(order);
            // The status was changed, let the user know.
            reporter.addMessage("The order status of ''{0}'' is now {1}.", jiraTicketKey, order.getOrderStatus());
        }
    }

    /**
     * If possible, update the JIRA issue so its status matches the status of the PDO in Mercury. No error is
     * generated if the transition is not possible, or if the JIRA status already matches Mercury.
     *
     * @param order the order to transition
     * @throws IOException
     */
    private void transitionIssueToSameOrderStatus(@Nonnull ProductOrder order) throws IOException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());
        JiraTransition transition = JiraStatus.fromIssue(issue).getTransitionTo(order.getOrderStatus());
        if (transition != null) {
            issue.postTransition(transition.stateName, getUserName() + " transitioned to " + transition.stateName);
        }
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
     */
    public void transitionJiraTicket(String jiraTicketKey, JiraResolution currentResolution, JiraTransition state,
                                      @Nullable String transitionComments) throws IOException {
        JiraIssue issue = jiraService.getIssue(jiraTicketKey);
        JiraResolution resolution = JiraResolution.fromString(issue.getResolution());
        if (currentResolution != resolution) {
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
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     */
    public void abandon(@Nonnull String jiraTicketKey, @Nullable String abandonComments)
            throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        productOrder.setOrderStatus(OrderStatus.Abandoned);

        // For Pending orders, don't change the sample states. This is so reporting can distinguish
        // abandoned samples vs samples that were removed because the PDO was never placed.
        if (!productOrder.isPending()) {
            transitionSamples(productOrder, EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED),
                    DeliveryStatus.ABANDONED, productOrder.getSamples());
        }

        // Currently not setting abandon comments into PDO comments, that seems too intrusive.  We will record the comments
        // with the JIRA ticket.
        transitionJiraTicket(jiraTicketKey, JiraResolution.CANCELLED, JiraTransition.CANCEL, abandonComments);
    }

    /**
     * Abandon a list of samples and add a message to the JIRA ticket to reflect this change.
     *
     * @param jiraTicketKey the order's JIRA key
     * @param samples       the samples to abandon
     * @param comment       optional user supplied comment about this action.
     */
    public void abandonSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<ProductOrderSample> samples,
                               @Nonnull String comment)
            throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException {
        transitionSamplesAndUpdateTicket(jiraTicketKey,
                EnumSet.of(DeliveryStatus.ABANDONED, DeliveryStatus.NOT_STARTED),
                DeliveryStatus.ABANDONED, samples,
                comment);
    }

    /**
     * Un-abandon a list of samples and add a message to the JIRA ticket to reflect this change.
     *
     * @param jiraTicketKey the order's JIRA key
     * @param sampleIds       the samples to un-abandon
     * @param comment       optional user supplied comment about this action.
     */
    public void unAbandonSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<Long> sampleIds,
                                 @Nonnull String comment, @Nonnull MessageReporter reporter)
            throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException {

        List<ProductOrderSample> samples = productOrderSampleDao.findListByList(ProductOrderSample.class,
                ProductOrderSample_.productOrderSampleId, sampleIds);

        if (!StringUtils.isBlank(comment)) {
            for (ProductOrderSample sample : samples) {
                if (sample.getDeliveryStatus() == DeliveryStatus.ABANDONED) {
                    sample.setSampleComment(comment);
                }
            }
        }

        transitionSamplesAndUpdateTicket(jiraTicketKey, EnumSet.of(DeliveryStatus.ABANDONED),
                DeliveryStatus.NOT_STARTED, samples, comment);

        reporter.addMessage("Un-Abandoned samples: {0}.",
                StringUtils.join(ProductOrderSample.getSampleNames(samples), ", "));
    }

    /**
     * Update JIRA state of an order based on a sample change operation.
     * <ul>
     *     <li>add a comment with the operation and the list of samples changed</li>
     *     <li>update the Sample IDs and Number of Samples fields</li>
     *     <li>output a message to the user about the operation</li>
     *     <li>if necessary, update the order status based on the new list of samples</li>
     * </ul>
     */
    private void updateSamples(ProductOrder order, Collection<ProductOrderSample> samples, MessageReporter reporter,
                               String operation) throws IOException, NoSuchPDOException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());

        String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samples), ",");
        issue.addComment(MessageFormat.format("{0} {1} samples: {2}.",
                userBean.getLoginUserName(), operation, nameList));
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.SAMPLE_IDS, order.getSampleString());
        productOrderJiraUtil.setCustomField(issue, ProductOrder.JiraField.NUMBER_OF_SAMPLES, order.getSamples().size());

        reporter.addMessage("{0} samples: {1}.", WordUtils.capitalize(operation), nameList);

        updateOrderStatus(order.getJiraTicketKey(), reporter);
    }

    /**
     * Given a PDO ID, add a list of samples to the PDO.  This will update the PDO's JIRA with a comment that the
     * samples have been added, and will notify LIMS that the samples are now present, and update the PDO's status
     * if necessary.
     *
     * @param jiraTicketKey the PDO key
     * @param samples       the samples to add
     */
    public void addSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<ProductOrderSample> samples,
                           @Nonnull MessageReporter reporter) throws NoSuchPDOException, IOException {
        ProductOrder order = findProductOrder(jiraTicketKey);
        order.addSamples(samples);

        ImmutableListMultimap<String, ProductOrderSample> samplesBySampleId =
                Multimaps.index(samples, new Function<ProductOrderSample, String>() {
                    @Override
                    public String apply(ProductOrderSample productOrderSample) {
                        return productOrderSample.getSampleKey();
                    }
                });

        Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(samplesBySampleId.keySet());

        for (Map.Entry<String, ProductOrderSample> productOrderSampleEntry : samplesBySampleId.entries()) {
            if (productOrderSampleEntry.getValue().getMercurySample() == null) {
                productOrderSampleEntry.getValue()
                        .setMercurySample(mercurySampleMap.get(productOrderSampleEntry.getKey()));
            }
        }

        order.prepareToSave(userBean.getBspUser());
        productOrderDao.persist(order);
        handleSamplesAdded(jiraTicketKey, samples, reporter);

        updateSamples(order, samples, reporter, "added");
    }

    public void removeSamples(@Nonnull String jiraTicketKey, @Nonnull Collection<ProductOrderSample> samples,
                              @Nonnull MessageReporter reporter) throws IOException, NoSuchPDOException {
        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        // If removeAll returns false, no samples were removed -- should never happen.
        if (productOrder.getSamples().removeAll(samples)) {
            String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samples), ",");
            productOrder.prepareToSave(userBean.getBspUser());
            productOrderDao.persist(productOrder);

            updateSamples(productOrder, samples, reporter, "deleted");
        }
    }

    /**
     * Encapsulates the logic for placing a product order.  This encompasses:
     * <ul>
     * <li>creating the PDO ticket in Jira</li>
     * <li>(if sample initiation) creating and submitting a kit request to BSP</li>
     * </ul>
     * <p/>
     * To avoid double ticket creations, we are employing a pessimistic lock on the Product order record.
     *
     * @param productOrderID    Database identifier of the Product order which we wish to find.  Used because at this
     *                          phase, the business key could change from draft-xxx to PDO-yyy.
     * @param businessKey       Business key by which to reference the currently persisted Product order
     * @param messageCollection Used to transmit errors or successes to the caller (Action bean) without returning
     */
    public ProductOrder placeProductOrder(@Nonnull Long productOrderID, String businessKey,
                                          @Nonnull MessageCollection messageCollection) {
        ProductOrder editOrder =
                productOrderDao.findByIdSafely(productOrderID, LockModeType.PESSIMISTIC_WRITE);

        if (editOrder == null) {
            throw new InformaticsServiceException(
                    "The product order was not found: " + ((businessKey == null) ? "" : businessKey));
        }
        if (editOrder.isSubmitted()) {
            throw new InformaticsServiceException("This product order " + editOrder.getBusinessKey() +
                                                  " has already been submitted to Jira");
        }
        editOrder.prepareToSave(userBean.getBspUser());
        try {
            if (editOrder.isDraft()) {
                // Only Draft orders are not already created in JIRA.
                productOrderJiraUtil.createIssueForOrder(editOrder);
            }
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
            editOrder.setPlacedDate(new Date());
            transitionIssueToSameOrderStatus(editOrder);

            // Now that the order is placed, add the comments about the samples to the issue.
            productOrderJiraUtil.addSampleComments(editOrder);

        } catch (IOException e) {
            String message = "Unable to create the Product Order in Jira";
            log.error(message, e);
            throw new InformaticsServiceException(message, e);
        }

        if (editOrder.isSampleInitiation()) {
            try {
                submitSampleKitRequest(editOrder, messageCollection);
            } catch (Exception e) {
                String errorMessage = "Unable to successfully complete the sample kit creation: ";
                log.error(errorMessage, e);
                messageCollection.addError(errorMessage + e.getMessage());
            }
        }

        return editOrder;
    }

    /**
     * Helper method to separate sample kit submission to its own transaction.  This will allow Product order creation
     * to succeed and commit the transition from Product Order draft to Submit even if the attempt to create a
     * sample kit in Bsp results in an exception, which should not roll back product order submission
     *
     * @param order             Order to which the new sample kit is to be associated
     * @param messageCollection Used to transmit errors or successes to the caller (Action bean) without returning
     *                          a value or throwing an exception.
     */
    public void submitSampleKitRequest(@Nonnull ProductOrder order, @Nonnull MessageCollection messageCollection) {
        String workRequestBarcode = bspKitRequestService.createAndSubmitKitRequestForPDO(order);
        order.getProductOrderKit().setWorkRequestId(workRequestBarcode);
        messageCollection.addInfo("Created BSP work request ''{0}'' for this order.", workRequestBarcode);
    }

    /**
     * This method will post the basic squid work request details entered by a user to create a project and work
     * request within squid for the product order represented in the input.
     *
     * @param productOrderKey Unique Jira key representing the product order for the resultant work request
     * @param squidInput      Basic information needed for squid to automatically create a work request for the
     *                        material information represented by the samples in the referenced product order
     *
     * @return work request output
     */
    public AutoWorkRequestOutput createSquidWorkRequest(@Nonnull String productOrderKey,
                                                        @Nonnull AutoWorkRequestInput squidInput) {

        ProductOrder order = productOrderDao.findByBusinessKey(productOrderKey);

        if (order == null) {
            throw new RuntimeException("Unable to find a product order with a key of " + productOrderKey);
        }

        AutoWorkRequestOutput workRequestOutput = squidConnector.createSquidWorkRequest(squidInput);

        order.setSquidWorkRequest(workRequestOutput.getWorkRequestId());

        try {
            addWorkRequestNotification(order, workRequestOutput, squidInput);
        } catch (IOException e) {
            log.info("Unable to post work request creation of " + workRequestOutput.getWorkRequestId() + " to Jira for "
                     + productOrderKey);
        }
        return workRequestOutput;
    }

    private void addWorkRequestNotification(@Nonnull ProductOrder pdo,
                                            @Nonnull AutoWorkRequestOutput createdWorkRequestResults,
                                            AutoWorkRequestInput squidInput)
            throws IOException {

        JiraIssue pdoIssue = jiraService.getIssue(pdo.getBusinessKey());

        pdoIssue.addComment(String.format("Created new Squid project %s for new Squid work request %s",
                createdWorkRequestResults.getProjectId(), createdWorkRequestResults.getWorkRequestId()));

        if (StringUtils.isNotBlank(squidInput.getLcsetId())) {
            pdoIssue.addLink(squidInput.getLcsetId());
//            pdoIssue.addComment(String.format("Work request %s is associated with LCSet %s",
//                    createdWorkRequestResults.getWorkRequestId(), squidInput.getLcsetId()));
        }
    }

    @Inject
    public void setProductOrderSampleDao(ProductOrderSampleDao productOrderSampleDao) {
        this.productOrderSampleDao = productOrderSampleDao;
    }
}
