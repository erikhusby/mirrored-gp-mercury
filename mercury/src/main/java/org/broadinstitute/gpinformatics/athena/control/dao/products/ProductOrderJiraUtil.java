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
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * Utility class that manages interactions between
 * PDOs and jira.
 */
public class ProductOrderJiraUtil {

    /**
     * This method encapsulates the set of steps necessary to finalize the submission of a product order.
     * This mainly deals with jira ticket creation.  This method will:
     * <ul>
     * <li>Create a new jira ticket and persist the reference to the ticket key</li>
     * <li>assign the submitter as a watcher to the ticket</li>
     * <li>Add a new comment listing all Samples contained within the order</li>
     * <li>Add any validation comments regarding the Samples contained within the order</li>
     * </ul>
     *
     * @throws IOException
     */
    public static void placeOrder(@Nonnull ProductOrder pdo,@Nonnull JiraService jiraService) throws IOException {
        pdo.setPlacedDate(new Date());
        Product product = pdo.getProduct();
        List<ProductOrderAddOn> addOns = pdo.getAddOns();
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();


        List<CustomField> listOfFields = new ArrayList<>();

        listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.PRODUCT_FAMILY,
                product.getProductFamily() == null ? "" : product.getProductFamily().getName()));

        listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.PRODUCT,
                product.getProductName() == null ? "" : product.getProductName()));
        listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.QUOTE_ID, pdo.getQuoteStringForJiraTicket()));

        if (!addOns.isEmpty()) {
            List<String> addOnsList = new ArrayList<>(addOns.size());
            for (ProductOrderAddOn addOn : addOns) {
                addOnsList.add(addOn.getAddOn().getDisplayName());
            }
            Collections.sort(addOnsList);
            listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.ADD_ONS, StringUtils.join(addOnsList, "\n")));
        }

        listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.SAMPLE_IDS, pdo.getSampleString()));

        listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.NUMBER_OF_SAMPLES,
                pdo.getSamples().size()));

        if (product.getSupportsNumberOfLanes()) {
            listOfFields.add(
                    new CustomField(submissionFields, ProductOrder.JiraField.LANES_PER_SAMPLE, pdo.getLaneCount()));
        }

        if (pdo.getPublicationDeadline() != null) {
            listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.PUBLICATION_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(pdo.getPublicationDeadline())));
        }

        if (pdo.getFundingDeadline() != null) {
            listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.FUNDING_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(pdo.getFundingDeadline())));
        }

        if (pdo.getComments() != null) {
            listOfFields.add(new CustomField(submissionFields, ProductOrder.JiraField.DESCRIPTION, pdo.getComments()));
        }

        BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);

        CreateFields.IssueType issueType;
        if (product.isSampleInitiationProduct()) {
            issueType = CreateFields.IssueType.SAMPLE_INITIATION;
        } else {
            issueType = CreateFields.IssueType.PRODUCT_ORDER;
        }

        JiraIssue issue = jiraService.createIssue(
                CreateFields.ProjectType.PRODUCT_ORDERING, bspUserList.getById(pdo.getCreatedBy()).getUsername(), issueType,
                pdo.getTitle(), listOfFields);

        pdo.setJiraTicketKey(issue.getKey());
        issue.addLink(pdo.getResearchProject().getJiraTicketKey());
    }

    /**
     * Add comments about the state of the samples to the order's JIRA issue. This is done separately from issue
     * creation because we want to delay adding these comments until the issue is placed.
     */
    public static void addSampleComments(ProductOrder order, JiraIssue issue) throws IOException {
        issue.addComment(StringUtils.join(order.getSampleSummaryComments(), "\n"));
        issue.addComment(StringUtils.join(order.getSampleValidationComments(), "\n"));
    }
}
