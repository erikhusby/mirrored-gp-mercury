package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
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
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that manages interactions between PDOs and jira.
 */
@Stateful
public class ProductOrderJiraUtil {

    @Inject
    private JiraService jiraService;

    @Inject
    private UserBean userBean;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductOrderJiraUtil() {
    }

    @Inject
    public ProductOrderJiraUtil(JiraService jiraService,
                                UserBean userBean) {
        this.jiraService = jiraService;
        this.userBean = userBean;
    }

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
    public void createIssueForOrder(@Nonnull ProductOrder order) throws IOException {
        createIssueForOrder(order, Collections.emptyList(), null, null);
    }

    public void createIssueForOrder(@Nonnull ProductOrder order, List<String> watchers, String owner,
            String linkedIssue) throws IOException {
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

        if (StringUtils.isNotBlank(owner)) {
            jiraService.updateAssignee(issue.getKey(), owner);
        }
        for (String username : watchers) {
            jiraService.addWatcher(issue.getKey(), username);
        }
        if (StringUtils.isNotBlank(linkedIssue)) {
            jiraService.addLink(AddIssueLinkRequest.LinkType.Related, issue.getKey(), linkedIssue);
        }
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
    public void addSampleComments(ProductOrder order) throws IOException {
        JiraIssue issue = jiraService.getIssue(order.getJiraTicketKey());
        issue.addComment(StringUtils.join(order.getSampleSummaryComments(), "\n"));
        issue.addComment(StringUtils.join(order.getSampleValidationComments(), "\n"));
    }

    /**
     * Replace the PMs field in the JIRA issue for this order with the provided list of users.
     */
    public void setJiraPMsField(ProductOrder productOrder, List<BspUser> projectManagers) throws IOException {
        List<CustomField.NameContainer> managers = new ArrayList<>(projectManagers.size());
        for (BspUser manager : projectManagers) {
            managers.add(new CustomField.NameContainer(manager.getUsername()));
        }

        JiraIssue issue = jiraService.getIssue(productOrder.getJiraTicketKey());
        setCustomField(issue, ProductOrder.JiraField.PMS, managers);
    }

    public void setCustomField(JiraIssue issue, ProductOrder.JiraField field, Object value) throws IOException {
        issue.setCustomFieldUsingTransition(field, value, ProductOrderEjb.JiraTransition.DEVELOPER_EDIT.getStateName());
    }

    public void cancel(String jiraTicketKey, String comment) throws IOException {
        JiraIssue issue = jiraService.getIssue(jiraTicketKey);
        issue.postTransition("Cancel", comment);
    }
}
