package org.broadinstitute.gpinformatics.mercury.integration.jira;


import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = EXTERNAL_INTEGRATION)
public class JiraServiceTest {

    private JiraService service;

    private String pdoJiraKey;
    private String lcsetJiraKey;

    @BeforeMethod
    public void setUp() {
        service = JiraServiceProducer.testInstance();
    }

    /**
     * Disabled this because had to change createIssue to pass the Reporter field. We should allow null for jira types
     * that do not expose the reporter, so change the API to do that later.
     */
    @Test
    public void testCreation() {

        setUp();
        try {

            Map<String, CustomFieldDefinition> requiredFields=
                    service.getRequiredFields(new CreateIssueRequest.Fields.Project(
                            CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix()),
                                              CreateIssueRequest.Fields.Issuetype.WHOLE_EXOME_HYBSEL );

            Collection<CustomField> customFieldList = new LinkedList<CustomField>();

            customFieldList.add(new CustomField(requiredFields.get("Protocol"),"test protocol",
                                                CustomField.SingleFieldType.TEXT ));
            customFieldList.add(new CustomField(requiredFields.get("Work Request ID(s)"),"WR 1 Billion!",
                                                CustomField.SingleFieldType.TEXT ));


                    //        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10020","Protocol",true),"test protocol"));
                    //        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10011","Work Request ID(s)",true),"WR 1 Billion!"));


            final JiraIssue jiraIssue =
                    service.createIssue(CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix(), null,
                                        CreateIssueRequest.Fields.Issuetype.WHOLE_EXOME_HYBSEL,
                                        "Summary created from Mercury", "Description created from Mercury",
                                        customFieldList);


            final String lcsetJiraKey = jiraIssue.getTicketName();

            Assert.assertNotNull(lcsetJiraKey);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testCreatePdoTicket() {
        setUp();
        Collection<CustomField> customFieldList = new LinkedList<CustomField>();

        try {
            Map<String, CustomFieldDefinition> requiredFields =
                service.getRequiredFields(new CreateIssueRequest.Fields.Project(CreateIssueRequest.Fields.ProjectType.Product_Ordering.getKeyPrefix()),
                                                              CreateIssueRequest.Fields.Issuetype.PRODUCT_ORDER );

            Assert.assertTrue(requiredFields.keySet().contains(ProductOrder.RequiredSubmissionFields.PRODUCT_FAMILY.getFieldName()));


            customFieldList.add(new CustomField(requiredFields.get(ProductOrder.RequiredSubmissionFields.PRODUCT_FAMILY.getFieldName()),
                                                "Test Exome Express", CustomField.SingleFieldType.TEXT ));

            final JiraIssue jiraIssue =
                    service.createIssue(CreateIssueRequest.Fields.ProjectType.Product_Ordering.getKeyPrefix(), "hrafal",
                                        CreateIssueRequest.Fields.Issuetype.PRODUCT_ORDER,
                                        "Athena Test case:::  Test new Summary Addition",
                                        "Athena Test Case:  Test description setting",customFieldList);
            final String pdoJiraKey = jiraIssue.getTicketName();

            Assert.assertNotNull(pdoJiraKey);

        } catch (IOException ioe) {
            Assert.fail(ioe.getMessage());
        }
    }


    public void testAddWatcher() {

        setUp();
        try {
            service.addWatcher("PDO-8", "squid");
        } catch (IOException iox) {
            Assert.fail(iox.getMessage());
        }
    }

    @Test(enabled = false)
    public void testLinkTicket() {

        setUp();
        try {
            service.addLink(AddIssueLinkRequest.LinkType.Related, "PDO-8", "RP-1");
        } catch (IOException iox) {
            Assert.fail(iox.getMessage());
        }

    }

    public void testAddPublicComment() {

        setUp();
        try {

            service.addComment("LCSET-1678", "Publicly visible comment added from Mercury");
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }
    }
    

    @Test(enabled = false)
    // disabled until we can get test jira to keep squid user as an admin
    public void testAddRestrictedComment() {
        
        setUp();
        try {
            
            service.addComment("LCSET-1678", "jira-users only comment added from Mercury", Visibility.Type.role, Visibility.Value.Administrators );
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }
    }

    public void test_custom_fields() throws IOException {
        setUp();
        Map<String, CustomFieldDefinition> customFields = null;
        customFields = service.getRequiredFields(new CreateIssueRequest.Fields.Project(
                CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix()),
                                                 CreateIssueRequest.Fields.Issuetype.WHOLE_EXOME_HYBSEL );
        Assert.assertFalse(customFields.isEmpty());
        boolean foundLanesRequestedField = false;
        for (CustomFieldDefinition customField : customFields.values()) {
            System.out.println(customField.getName() + " id " + customField.getJiraCustomFieldId());
            if (customField.getName().equals("Lanes Requested")) {
                foundLanesRequestedField = true;
            }
        }
        Assert.assertTrue(foundLanesRequestedField);
    }
}
