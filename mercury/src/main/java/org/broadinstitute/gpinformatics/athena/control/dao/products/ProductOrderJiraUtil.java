package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that manages interactions between PDOs and jira.
 */
public class ProductOrderJiraUtil {

    private static class ProductOrderFields {
        private final Map<String, CustomFieldDefinition> submissionFields;
        private final List<CustomField> fields = new ArrayList<>();

        private ProductOrderFields(Map<String, CustomFieldDefinition> submissionFields) {
            this.submissionFields = submissionFields;
        }

        private void addValue(ProductOrder.JiraField field, Object value) {
            fields.add(new CustomField(submissionFields, field, value));
        }
    }

    /**
     * Create a Product Order's corresponding JIRA issue.
     * <ul>
     * <li>create a new JIRA issue using the product order state and store the key in the product order</li>
     * <li>assign the submitter as a watcher to the ticket</li>
     * <li>link the research project to the JIRA issue</li>
     * </ul>
     */
    public static void createIssueForOrder(@Nonnull ProductOrder order, @Nonnull JiraService jiraService)
            throws IOException {
        Product product = order.getProduct();
        List<ProductOrderAddOn> addOns = order.getAddOns();
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

        ProductOrderFields fields = new ProductOrderFields(submissionFields);

        fields.addValue(ProductOrder.JiraField.PRODUCT_FAMILY,
                product.getProductFamily() == null ? "" : product.getProductFamily().getName());

        fields.addValue(ProductOrder.JiraField.PRODUCT,
                product.getProductName() == null ? "" : product.getProductName());
        fields.addValue(ProductOrder.JiraField.QUOTE_ID, order.getQuoteStringForJiraTicket());

        if (!addOns.isEmpty()) {
            List<String> addOnsList = new ArrayList<>(addOns.size());
            for (ProductOrderAddOn addOn : addOns) {
                addOnsList.add(addOn.getAddOn().getDisplayName());
            }
            Collections.sort(addOnsList);
            fields.addValue(ProductOrder.JiraField.ADD_ONS, StringUtils.join(addOnsList, "\n"));
        }

        fields.addValue(ProductOrder.JiraField.SAMPLE_IDS, order.getSampleString());

        fields.addValue(ProductOrder.JiraField.NUMBER_OF_SAMPLES, order.getSamples().size());

        if (product.getSupportsNumberOfLanes()) {
            fields.addValue(ProductOrder.JiraField.LANES_PER_SAMPLE, order.getLaneCount());
        }

        if (order.getPublicationDeadline() != null) {
            fields.addValue(ProductOrder.JiraField.PUBLICATION_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(order.getPublicationDeadline()));
        }

        if (order.getFundingDeadline() != null) {
            fields.addValue(ProductOrder.JiraField.FUNDING_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(order.getFundingDeadline()));
        }

        if (order.getComments() != null) {
            fields.addValue(ProductOrder.JiraField.DESCRIPTION, order.getComments());
        }

        BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);

        CreateFields.IssueType issueType;
        if (product.isSampleInitiationProduct()) {
            issueType = CreateFields.IssueType.SAMPLE_INITIATION;
        } else {
            issueType = CreateFields.IssueType.PRODUCT_ORDER;
        }

        JiraIssue issue = jiraService.createIssue(
                CreateFields.ProjectType.PRODUCT_ORDERING, bspUserList.getById(order.getCreatedBy()).getUsername(),
                issueType, order.getTitle(), fields.fields);

        order.setJiraTicketKey(issue.getKey());
        issue.addLink(order.getResearchProject().getJiraTicketKey());
    }

    /**
     * <p>Add comments about the state of the samples to the order's JIRA issue:
     * <ul>
     * <li>Add any validation comments regarding the Samples contained within the order</li>
     * </ul>
     * </p>
     *
     * This is done separately from {@link #createIssueForOrder} because we want to delay adding these
     * comments until the order is Submitted. The reason for this is that the information about samples
     * is not complete until the samples have been received and processed in the lab.
     */
    public static void addSampleComments(ProductOrder order, JiraIssue issue) throws IOException {
        issue.addComment(StringUtils.join(order.getSampleSummaryComments(), "\n"));
        issue.addComment(StringUtils.join(order.getSampleValidationComments(), "\n"));
    }
}
