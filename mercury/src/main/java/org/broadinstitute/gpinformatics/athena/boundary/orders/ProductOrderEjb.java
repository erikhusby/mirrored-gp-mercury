package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
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
import java.util.*;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus.Abandoned;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus.Complete;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.TransitionStates.Cancel;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus.*;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link ProductOrder}s
 */
public class ProductOrderEjb {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private QuoteService quoteService;

    @Inject
    private JiraService jiraService;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList userList;

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


    private static void setSamples(ProductOrder productOrder, List<String> sampleIds) throws NoSamplesException {
        if (sampleIds.isEmpty()) {
            throw new NoSamplesException();
        }

        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        productOrder.setSamples(orderSamples);
    }


    private static void createJiraIssue(ProductOrder productOrder) {
        try {
            productOrder.submitProductOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void setAddOnProducts(ProductOrder productOrder, List<String> addOnPartNumbers) {
        List<Product> addOns =
                addOnPartNumbers.isEmpty() ? new ArrayList<Product>() : productDao.findByPartNumbers(addOnPartNumbers);

        productOrder.updateAddOnProducts(addOns);
    }


    private static void setStatus(ProductOrder productOrder) {
        // DRAFT orders not yet supported; force state of new PDOs to Submitted.
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
    }

    /**
     * Including {@link QuoteNotFoundException} since this is an expected failure that may occur in application validation
     *

     * @param productOrder product order
     * @param productOrderSampleIds sample IDs
     * @param addOnPartNumbers add-on part numbers
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
     * This handles the creation of jira when placing an order.
     *
     * @param productOrder The order to place
     */
    public void placeOrder(ProductOrder productOrder) {
        // create JIRA before we attempt to persist since that is more likely to fail
        createJiraIssue(productOrder);
        productOrderDao.persist(productOrder);
    }

    /**
     * Utility class to help with mapping from display names of custom fields to their {@link CustomFieldDefinition}s,
     * as well as tracking changes that have been made to the values of those fields relative to the existing state
     * of the PDO JIRA ticket.
     *
     * This inner class has to be static or Weld crashes with ArrayIndexOutOfBoundsExceptions.
     */
    private static class PDOUpdateField {

        private final String displayName;
        private final Object newValue;
        private final CustomField.SubmissionField field;

        /** True if the field being updated is a 'bulk' item.  This means that it should be shown as plural in the
         * message, and its contents won't be shown in the message.
         */
        private final boolean isBulkField;

        /**
         * Return the update message appropriate for this field.  If there are no changes this will return the empty
         * string, otherwise a string of the form "Product was updated from 'Old Product' to 'New Product'".
         *
         * @param productOrder contains the new values
         * @param customFieldDefinitionMap contains the mapping from display names of fields to their JIRA IDs, needed
         *                                 to dig the old values contained in the fields out of the issueFieldsResponse
         * @param issueFieldsResponse contains the old values
         * @return the update message that goes in the JIRA ticket
         */
        public String getUpdateMessage(ProductOrder productOrder, Map<String, CustomFieldDefinition> customFieldDefinitionMap, IssueFieldsResponse issueFieldsResponse) {

            if (!customFieldDefinitionMap.containsKey(displayName)) {
                throw new RuntimeException(
                        "Custom field '" + displayName + "' not found in issue " + productOrder.getJiraTicketKey());
            }
            CustomFieldDefinition customFieldDefinition = customFieldDefinitionMap.get(displayName);

            Object previousValue = issueFieldsResponse.getFields().get(customFieldDefinition.getJiraCustomFieldId());

            // This assumes all target fields are not nullable, which is currently true but may not be in the future.
            if (previousValue == null) {
                throw new RuntimeException(
                        "Custom field value for '" + displayName + "' not found in issue '" + productOrder
                                .getJiraTicketKey() + "'");
            }

            Object oldValueToCompare = previousValue;
            Object newValueToCompare = newValue;

            if (newValue instanceof CreateFields.Reporter) {
                // Need to special case Reporter type for display and comparison.
                oldValueToCompare = ((Map<?, ?>)previousValue).get("name").toString();
                newValueToCompare = ((CreateFields.Reporter)newValue).getName();
            }
            if (!oldValueToCompare.equals(newValueToCompare)) {
                return displayName + (isBulkField ? " have " : " has ") + "been updated" +
                       (!isBulkField ? " from '" + oldValueToCompare + "' to '" + newValueToCompare + "'" : "") + ".\n";
            }
            return "";
        }

        public PDOUpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue, boolean isBulkField) {
            this.field = field;
            displayName = field.getFieldName();
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
     * @param productOrder product order
     * @throws IOException
     */
    public void updateJiraIssue(ProductOrder productOrder) throws IOException, QuoteNotFoundException {

        validateQuote(productOrder);

        Transition transition = jiraService.findAvailableTransitionByName(productOrder.getJiraTicketKey(),
                ProductOrder.TransitionStates.DeveloperEdit.getStateName());

        PDOUpdateField [] pdoUpdateFields = new PDOUpdateField[] {
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT, productOrder.getProduct().getProductName()),
                new PDOUpdateField(ProductOrder.JiraField.PRODUCT_FAMILY, productOrder.getProduct().getProductFamily().getName()),
                new PDOUpdateField(ProductOrder.JiraField.QUOTE_ID, productOrder.getQuoteId()),
                new PDOUpdateField(ProductOrder.JiraField.SAMPLE_IDS, productOrder.getSampleString(), true),
                new PDOUpdateField(ProductOrder.JiraField.REPORTER,
                        new CreateFields.Reporter(userList.getById(productOrder.getCreatedBy()).getUsername()))
        };

        String[] customFieldNames = new String[pdoUpdateFields.length];

        int i = 0;
        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFieldNames[i++] = pdoUpdateField.displayName;
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getCustomFields(customFieldNames);

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(productOrder.getJiraTicketKey(), customFieldDefinitions.values());

        List<CustomField> customFields = new ArrayList<CustomField>();

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
     * @param productOrder product order
     * @param selectedAddOnPartNumbers selected add-on part numbers
     */
    public void update(ProductOrder productOrder, List<String> selectedAddOnPartNumbers) throws QuoteNotFoundException {

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
        Set<ProductOrderAddOn> productOrderAddOns = new HashSet<ProductOrderAddOn>();
        for (Product addOn : productDao.findByPartNumbers(selectedAddOnPartNumbers)) {
            ProductOrderAddOn productOrderAddOn = new ProductOrderAddOn(addOn, updatedProductOrder);
            productOrderDao.persist(productOrderAddOn);
            productOrderAddOns.add(productOrderAddOn);
        }
        
        updatedProductOrder.setProductOrderAddOns(productOrderAddOns);

    }

    public static class NoTransitionException extends Exception {
        public NoTransitionException(String s) {
            super(s);
        }
    }

    public static class NoSuchPDOException extends Exception {
        public NoSuchPDOException(String s) {
            super(s);
        }
    }


    public static class SampleDeliveryStatusChangeException extends Exception {

        private final List<ProductOrderSample> samples;

        private final ProductOrderSample.DeliveryStatus targetedDeliveryStatus;

        protected SampleDeliveryStatusChangeException(ProductOrderSample.DeliveryStatus targetedDeliveryStatus, List<ProductOrderSample> samples) {
            this.targetedDeliveryStatus = targetedDeliveryStatus;
            this.samples = samples;
        }

        protected List<ProductOrderSample> getSamples() {
            return samples;
        }

        protected String getSampleMessage() {

            List<String> sampleMessagePieces = new ArrayList<String>();

            for (ProductOrderSample productOrderSample : getSamples()) {
                sampleMessagePieces.add(
                        productOrderSample.getSampleName() + " @ " + productOrderSample.getSamplePosition() +
                                " : current status " + productOrderSample.getDeliveryStatus().getDisplayName());
            }

            return StringUtils.join(sampleMessagePieces, ", ");
        }

        protected ProductOrder getProductOrder() {
            if (samples != null && !samples.isEmpty()) {
                return samples.get(0).getProductOrder();
            }

            return null;
        }

        @Override
        public String getMessage() {
            return "Cannot transition samples to status " + targetedDeliveryStatus.getDisplayName() + ": " + getSampleMessage();
        }
    }


    /**
     * Utility method to find PDO by JIRA ticket key and throw exception if it is not found
     *
     * @param jiraTicketKey JIRA ticket key
     *
     * @return {@link ProductOrder}
     *
     * @throws NoSuchPDOException
     */
    private ProductOrder findProductOrder(String jiraTicketKey) throws NoSuchPDOException {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraTicketKey);

        if (productOrder == null) {
            throw new NoSuchPDOException(jiraTicketKey);
        }

        return productOrder;
    }


    /**
     * Transition the delivery statuses of the specified samples in the DB.
     *
     * @param productOrder PDO containing the samples in question
     * @param acceptableStartingStatuses a Set of {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus}es
     *                                   in which samples are allowed to be in before undergoing this transition
     * @param targetStatus the status into which the samples will be transitioned
     * @param productOrderSamples the samples in question
     * @throws SampleDeliveryStatusChangeException thrown if any samples are found to not be in an acceptable starting status
     */
    private static void transitionSamples(ProductOrder productOrder,
                                          Set<ProductOrderSample.DeliveryStatus> acceptableStartingStatuses,
                                          ProductOrderSample.DeliveryStatus targetStatus,
                                          Collection<ProductOrderSample> productOrderSamples) throws SampleDeliveryStatusChangeException {

        Set<ProductOrderSample> sampleSet = new HashSet<ProductOrderSample>(productOrderSamples);

        List<ProductOrderSample> untransitionableSamples = new ArrayList<ProductOrderSample>();

        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            // if the sample name set is empty we try to transition all samples in the PDO
            if (CollectionUtils.isEmpty(sampleSet) || sampleSet.contains(productOrderSample)) {
                if ( ! acceptableStartingStatuses.contains(productOrderSample.getDeliveryStatus())) {
                    untransitionableSamples.add(productOrderSample);
                    // keep looping, find all the untransitionable samples and then throw a descriptive exception
                } else {
                    productOrderSample.setDeliveryStatus(targetStatus);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(untransitionableSamples)) {
            throw new SampleDeliveryStatusChangeException(targetStatus, untransitionableSamples);
        }
    }


    /**
     * Transition the specified samples to the specified target status, adding a comment to the JIRA ticket, does NOT
     * transition the JIRA ticket status as this is called from sample transition methods only and not whole PDO transition
     * methods.  Per GPLIM-655 we need to update per-sample comments too, but not currently doing that.
     *
     * @param jiraTicketKey JIRA ticket key
     * @param acceptableStartingStatuses acceptable statuses for samples to be found in
     * @param targetStatus status to change samples to
     * @param sampleIndices zero-based indices of samples in the PDO to update
     * @param sampleComments comments associated with each sample per update.
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     * @throws IOException
     */
    private void transitionSamplesAndUpdateTicket(String jiraTicketKey, Set<ProductOrderSample.DeliveryStatus> acceptableStartingStatuses,
                                                  ProductOrderSample.DeliveryStatus targetStatus, List<Integer> sampleIndices,
                                                  List<String> sampleComments) throws NoSuchPDOException, SampleDeliveryStatusChangeException, IOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();

        List<String> messagePieces = new ArrayList<String>();

        // ii = index of indices
        for (int ii = 0; ii < sampleIndices.size(); ii++) {
            int sampleIndex = sampleIndices.get(ii);

            ProductOrderSample productOrderSample = productOrder.getSamples().get(sampleIndex);
            // GPLIM-655 insert code here to append to sample comment history.  In the absence of this I'll just throw
            // a comment to JIRA
            productOrderSamples.add(productOrderSample);

            messagePieces.add(productOrderSample.getSampleName() + " at index " + productOrderSample.getSamplePosition() + ": " + sampleComments.get(ii));
        }

        transitionSamples(productOrder, acceptableStartingStatuses, targetStatus, productOrderSamples);

        jiraService.addComment(productOrder.getJiraTicketKey(), getUserName() + " transitioned samples to status " +
                targetStatus.getDisplayName() + ":\n" + StringUtils.join(messagePieces, "\n"));

    }


    /**
     * Returns the name of the currently logged-in user or 'Mercury' if no logged in user (e.g. in a fixup test context)
     *
     * @return
     */
    private String getUserName() {
        String user = userBean.getLoginUserName();
        return user == null ? "Mercury" : user;
    }


    /**
     * Transition the specified JIRA ticket using the specified transition, adding the specified comment.
     *
     * @param jiraTicketKey JIRA ticket key
     * @param alreadyResolvedResolutions if a JIRA ticket is found to already be in any of these states, do not do anything
     * @param transitionState the transition to use
     * @param transitionComments comments to include as part of the transition, will be appended to the JIRA ticket
     *
     * @throws IOException
     * @throws NoTransitionException Thrown if the specified transition is not available on the specified issue
     */
    private void transitionJiraTicket(String jiraTicketKey, String [] alreadyResolvedResolutions, ProductOrder.TransitionStates transitionState, String transitionComments) throws IOException, NoTransitionException {
        String resolution = jiraService.getResolution(jiraTicketKey);

        Set<String> alreadyResolvedResolutionsSet = new HashSet<String>(Arrays.asList(alreadyResolvedResolutions));

        if ( ! alreadyResolvedResolutionsSet.contains(resolution)) {

            Transition transition = jiraService.findAvailableTransitionByName(jiraTicketKey, transitionState.getStateName());

            if (transition == null) {
                throw new NoTransitionException(
                        "Cannot " + transitionState.getStateName() + " " + jiraTicketKey +
                                " in resolution '" + resolution + "': no " + transitionState.getStateName() + " transition found");
            }

            String jiraCommentText = getUserName() + " performed " + transitionState.getStateName() + " transition on " + jiraTicketKey;

            if (transitionComments != null) {
                jiraCommentText = jiraCommentText + ": " + transitionComments;
            }

            jiraService.postNewTransition(jiraTicketKey, transition, jiraCommentText);
        }
    }


    /**
     * Abandon the whole PDO with a comment
     *
     * @param jiraTicketKey JIRA ticket key
     * @param abandonComments transition comments
     * @throws NoTransitionException
     * @throws NoSuchPDOException
     * @throws SampleDeliveryStatusChangeException
     */
    public void abandon(@Nonnull String jiraTicketKey, @Nullable String abandonComments) throws NoTransitionException, NoSuchPDOException, SampleDeliveryStatusChangeException, IOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        // then mark PDO as abandoned
        productOrder.setOrderStatus(Abandoned);

        transitionSamples(productOrder, EnumSet.of(ABANDONED, NOT_STARTED), ABANDONED, productOrder.getSamples());

        // Currently not setting abandon comments into PDO comments, that seems too intrusive.  We will record the comments
        // with the JIRA ticket.

        transitionJiraTicket(jiraTicketKey, new String[] {"Cancelled"}, Cancel, abandonComments);
    }


    /**
     * Mark the whole PDO as complete, with a comment.
     *
     * @param jiraTicketKey JIRA ticket key of the PDO in question
     * @param completionComments comments to include in the JIRA ticket
     *
     * @throws SampleDeliveryStatusChangeException
     * @throws IOException
     * @throws NoTransitionException
     */
    public void complete(@Nonnull String jiraTicketKey, @Nullable String completionComments) throws SampleDeliveryStatusChangeException, IOException, NoTransitionException, NoSuchPDOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);
        productOrder.setOrderStatus(Complete);

        transitionSamples(productOrder, EnumSet.of(DELIVERED, NOT_STARTED), DELIVERED, productOrder.getSamples());

        // Currently not setting abandon comments into PDO comments, that seems too intrusive.  We will record the comments
        // with the JIRA ticket.

        transitionJiraTicket(jiraTicketKey, new String [] {"Complete"}, ProductOrder.TransitionStates.Complete, completionComments);
    }


    /**
     * Sample abandonment method with parameter types guessed as appropriate for use with Stripes
     *
     * @param jiraTicketKey JIRA ticket key of the PDO in question
     * @param sampleIndices zero-based indices of the samples to abandon
     * @param abandonmentComments comments to include with the abandonment of the samples.  Currently these comments go
     *                            only in the JIRA ticket, with GPLIM-655 they will also be added to the per-sample history
     * @throws IOException
     * @throws SampleDeliveryStatusChangeException
     * @throws NoSuchPDOException
     */
    public void abandonSamples(@Nonnull String jiraTicketKey, List<Integer> sampleIndices, List<String> abandonmentComments) throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException {
        transitionSamplesAndUpdateTicket(jiraTicketKey, EnumSet.of(ABANDONED, NOT_STARTED), ABANDONED, sampleIndices, abandonmentComments);
    }


    /**
     * Sample completion method with parameter types guessed as appropriate for use with Stripes
     *
     * @param jiraTicketKey JIRA ticket key of the PDO in question
     * @param sampleIndices zero-based indices of the samples to complete
     * @param completionComments comments to include with the completion of the samples.  Currently these comments go
     *                            only in the JIRA ticket, with GPLIM-655 they will also be added to the per-sample history
     * @throws IOException
     * @throws SampleDeliveryStatusChangeException
     * @throws NoSuchPDOException
     */
    public void completeSamples(@Nonnull String jiraTicketKey, List<Integer> sampleIndices, List<String> completionComments) throws IOException, SampleDeliveryStatusChangeException, NoSuchPDOException {
        transitionSamplesAndUpdateTicket(jiraTicketKey, EnumSet.of(DELIVERED, NOT_STARTED), DELIVERED, sampleIndices, completionComments);
    }

}
