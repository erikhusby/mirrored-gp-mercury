package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus.Abandoned;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus.Complete;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.TransitionStates.Cancel;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus.ABANDONED;
import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample.DeliveryStatus.NOT_STARTED;

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


    private void setSamples(ProductOrder productOrder, List<String> sampleIds) throws NoSamplesException {
        if (sampleIds.isEmpty()) {
            throw new NoSamplesException();
        }

        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        productOrder.setSamples(orderSamples);
    }


    private void createJiraIssue(ProductOrder productOrder) {
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


    private void setStatus(ProductOrder productOrder) {
        // DRAFT orders not yet supported; force state of new PDOs to Submitted.
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
    }


    /**
     * Including {@link QuoteNotFoundException} since this is an expected failure that may occur in application validation
     *
     * @param productOrder
     * @param productOrderSamplesIds
     * @param addOnPartNumbers
     * @throws QuoteNotFoundException
     */
    public void save(ProductOrder productOrder, List<String> productOrderSamplesIds, List<String> addOnPartNumbers) throws DuplicateTitleException, QuoteNotFoundException, NoSamplesException {

        validateUniqueProjectTitle(productOrder);
        validateQuote(productOrder);
        setSamples(productOrder, productOrderSamplesIds);
        setAddOnProducts(productOrder, addOnPartNumbers);
        setStatus(productOrder);
        // create JIRA before we attempt to persist since that is more likely to fail
        createJiraIssue(productOrder);

        productOrderDao.persist(productOrder);

    }


    /**
     * Utility class to help with mapping from display names of custom fields to their {@link CustomFieldDefinition}s,
     * as well as tracking changes that have been made to the values of those fields relative to the existing state
     * of the PDO JIRA ticket.
     *
     * This inner class has to be static or Weld crashes with ArrayIndexOutOfBoundsExceptions
     */
    private static class PDOUpdateField {

        private String displayName;

        private String newValue;

        /**
         * Return the update message appropriate for this field.  If there are no changes this will return the empty
         * string, otherwise a string of the form "Product was updated from 'Old Product' to 'New Product'"
         *
         * @param productOrder contains the new values
         * @param customFieldDefinitionMap contains the mapping from display names of fields to their JIRA IDs, needed
         *                                 to dig the old values contained in the fields out of the issueFieldsResponse
         * @param issueFieldsResponse contains the old values
         * @return
         */
        public String getUpdateMessage(ProductOrder productOrder, Map<String, CustomFieldDefinition> customFieldDefinitionMap, IssueFieldsResponse issueFieldsResponse) {

            if ( ! customFieldDefinitionMap.containsKey(displayName)) {
                throw new RuntimeException("Custom field '" + displayName + "' not found in issue " + productOrder.getJiraTicketKey());
            }
            CustomFieldDefinition customFieldDefinition = customFieldDefinitionMap.get(displayName);

            String previousValue = issueFieldsResponse.getFields().get(customFieldDefinition.getJiraCustomFieldId());

            // this assumes all target fields are not nullable, which is currently true but may not be in the future
            if (previousValue == null) {
                throw new RuntimeException("Custom field value for '" + displayName + "' not found in issue '" + productOrder.getJiraTicketKey() + "'");
            }

            if ( ! previousValue.equals(newValue)) {
                return displayName + " was updated from '" + previousValue + "' to '" + newValue + "'\n";
            }
            return "";
        }

        public PDOUpdateField(@Nonnull String displayName, @Nonnull String newValue) {
            this.displayName = displayName;
            this.newValue = newValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getNewValue() {
            return newValue;
        }
    }


    /**
     * Update the JIRA issue, executing the 'Developer Edit' transition to effect edits of fields that are read-only
     * on the UI.  Add a comment to the issue to indicate what was changed and by whom.
     *
     * @param productOrder
     * @throws IOException
     */
    private void updateJiraIssue(ProductOrder productOrder) throws IOException {
        Transition transition = jiraService.findAvailableTransitionByName(productOrder.getJiraTicketKey(), "Developer Edit");

        PDOUpdateField [] pdoUpdateFields = new PDOUpdateField[] {
            new PDOUpdateField("Product", productOrder.getProduct().getProductName()),
            new PDOUpdateField("Product Family", productOrder.getProduct().getProductFamily().getName()),
            new PDOUpdateField("Quote ID", productOrder.getQuoteId())
        };

        List<String> customFieldNames = new ArrayList<String>();

        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFieldNames.add(pdoUpdateField.getDisplayName());
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions =
                jiraService.getCustomFields(customFieldNames.toArray(new String[]{}));

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(productOrder.getJiraTicketKey(), customFieldDefinitions.values());

        List<CustomField> customFields = new ArrayList<CustomField>();

        String updateComment = "";

        for (PDOUpdateField pdoUpdateField : pdoUpdateFields) {
            customFields.add(new CustomField(
                customFieldDefinitions.get(pdoUpdateField.getDisplayName()),
                pdoUpdateField.getNewValue(),
                CustomField.SingleFieldType.TEXT
            ));

            updateComment = updateComment + pdoUpdateField.getUpdateMessage(productOrder, customFieldDefinitions, issueFieldsResponse);
        }

        // if we detect from the comment that nothing has changed, make a note of that (maybe the user changed
        // something in the PDO that is not reflected in JIRA like add-ons)

        String comment = "\n" + productOrder.getJiraTicketKey() + " was edited by " + userBean.getBspUser().getUsername() + "\n\n";

        comment = comment + ("".equals(updateComment) ? "No JIRA Product Order fields were updated\n\n" : updateComment);

        jiraService.postNewTransition(productOrder.getJiraTicketKey(), transition, customFields, comment);
    }


    /**
     * Allow updated quotes, products, and add-ons.
     *
     * @param productOrder
     * @param selectedAddOnPartNumbers
     */
    public void update(final ProductOrder productOrder, final List<String> selectedAddOnPartNumbers) throws QuoteNotFoundException {

        validateQuote(productOrder);

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

    public static class NoCancelTransitionException extends Exception {
        public NoCancelTransitionException(String s) {
            super(s);
        }
    }

    public static class NoSuchPDOException extends Exception {
        public NoSuchPDOException(String s) {
            super(s);
        }
    }

    public static class SamplesNotAbandonableException extends Exception {

        private List<ProductOrderSample> unabandonableSamples = new ArrayList<ProductOrderSample>();

        public SamplesNotAbandonableException(List<ProductOrderSample> unabandonableSamples) {
            if (unabandonableSamples != null) {
                this.unabandonableSamples = unabandonableSamples;
            }
        }

        public List<ProductOrderSample> getUnabandonableSamples() {
            return unabandonableSamples;
        }

        @Override
        public String getMessage() {
            List<String> messagePieces = new ArrayList<String>();

            for (ProductOrderSample productOrderSample : getUnabandonableSamples()) {
                messagePieces.add(
                        productOrderSample.getSampleName() + " @ " + productOrderSample.getSamplePosition() +
                                " : status " + productOrderSample.getDeliveryStatus().getDisplayName());
            }

            ProductOrder productOrder = unabandonableSamples.get(0).getProductOrder();
            return "Cannot abandon samples in " + productOrder.getBusinessKey() + ": " + StringUtils.join(messagePieces, ", ");
        }
    }


    private ProductOrder findProductOrder(String jiraTicketKey) throws NoSuchPDOException {
        ProductOrder productOrder = productOrderDao.findByBusinessKey(jiraTicketKey);

        if (productOrder == null) {
            throw new NoSuchPDOException(jiraTicketKey);
        }

        return productOrder;
    }



    private void abandonSamples(ProductOrder productOrder, ProductOrderSample... productOrderSamples) throws SamplesNotAbandonableException {

        Set<ProductOrderSample> sampleSet = new HashSet<ProductOrderSample>(Arrays.asList(productOrderSamples));

        Set<ProductOrderSample.DeliveryStatus> abandonableDeliveryStatuses =
                EnumSet.of(ABANDONED, NOT_STARTED);

        List<ProductOrderSample> unabandonableSamples = new ArrayList<ProductOrderSample>();

        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            // if the sample name set is empty we try to abandon all samples in the PDO
            if (CollectionUtils.isEmpty(sampleSet) || sampleSet.contains(productOrderSample.getSampleName())) {
                if ( ! abandonableDeliveryStatuses.contains(productOrderSample.getDeliveryStatus())) {
                    unabandonableSamples.add(productOrderSample);
                    // keep looping, find all the unabandonable samples and then throw a descriptive exception
                } else {
                    productOrderSample.setDeliveryStatus(ABANDONED);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(unabandonableSamples)) {
            throw new SamplesNotAbandonableException(unabandonableSamples);
        }

    }


    public void abandon(String jiraTicketKey) throws NoCancelTransitionException, NoSuchPDOException, SamplesNotAbandonableException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        // try to abandon samples first (may fail if some are not in an abandonable delivery status)
        abandonSamples(productOrder);

        // then mark PDO as abandoned
        productOrder.setOrderStatus(Abandoned);

        try {
            Set<String> ALREADY_RESOLVED = new HashSet<String>(Arrays.asList("Cancelled", "Completed"));
            String resolution = jiraService.getResolution(jiraTicketKey);

            if ( ! ALREADY_RESOLVED.contains(resolution)) {

                Transition transition = jiraService.findAvailableTransitionByName(
                        productOrder.getJiraTicketKey(), Cancel.getStateName());

                if (transition == null) {
                    throw new NoCancelTransitionException("Cannot Cancel " + jiraTicketKey + " in resolution '" + resolution + "': no Cancel transition found");
                }

                String user = userBean.getLoginUserName();
                jiraService.postNewTransition(productOrder.getJiraTicketKey(), transition,
                        (user == null ? "Mercury" : user) + " abandoned " + jiraTicketKey);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Choosing parameter types that I'm guessing will best map to what we're going to get from Stripes, subject to
     * change
     *
     * @param jiraTicketKey
     * @param sampleIndices
     * @throws NoSuchPDOException
     * @throws SamplesNotAbandonableException
     * @throws IOException
     */
    public void abandonSamples(String jiraTicketKey, int... sampleIndices) throws NoSuchPDOException, SamplesNotAbandonableException, IOException {

        ProductOrder productOrder = findProductOrder(jiraTicketKey);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        for (int sampleIndex : sampleIndices) {
            productOrderSamples.add(productOrder.getSamples().get(sampleIndex));
        }

        abandonSamples(productOrder, productOrderSamples.toArray(new ProductOrderSample[0]));

        List<String> messagePieces = new ArrayList<String>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            messagePieces.add(productOrderSample.getSampleName() + " @ " + productOrderSample.getSamplePosition());
        }

        jiraService.addComment(productOrder.getJiraTicketKey(), userBean.getLoginUserName() + " abandoned samples:\n" + StringUtils.join(messagePieces, "\n"));
    }


    /**
     * Mark PDO as complete.  Should we also update the statuses of any non-ABANDONED, non-COMPLETE samples?
     *
     * @param productOrder
     */
    public void complete(ProductOrder productOrder) {

        productOrder.setOrderStatus(Complete);
    }
}
