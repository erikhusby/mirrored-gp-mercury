package org.broadinstitute.gpinformatics.mercury.integration.jira;


import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
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

    @BeforeMethod
    public void setUp() {
        service = JiraServiceProducer.testInstance();
    }

    public void testCreation() {

        try {

            Map<String, CustomFieldDefinition> requiredFields=
                    service.getRequiredFields(new CreateIssueRequest.Fields.Project(CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix()),
                                              CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);

            Collection<CustomField> customFieldList = new LinkedList<CustomField>();

            customFieldList.add(new CustomField(requiredFields.get("Protocol"),"test protocol"));
            customFieldList.add(new CustomField(requiredFields.get("Work Request ID(s)"),"WR 1 Billion!"));


                    //        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10020","Protocol",true),"test protocol"));
                    //        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10011","Work Request ID(s)",true),"WR 1 Billion!"));


            final CreateIssueResponse createIssueResponse =
                    service.createIssue(JiraTicket.TEST_PROJECT_PREFIX,
                                        CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel,
                                        "Summary created from Mercury", "Description created from Mercury",
                                        customFieldList);


            final String key = createIssueResponse.getTicketName();

            Assert.assertNotNull(key);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }



    public void testAddPublicComment() {

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
        
        try {
            
            service.addComment("LCSET-1678", "jira-users only comment added from Mercury", Visibility.Type.role, Visibility.Value.Administrators );
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }
            
    }

    public void test_custom_fields() throws IOException {
        Map<String, CustomFieldDefinition> customFields = null;
        customFields = service.getRequiredFields(new CreateIssueRequest.Fields.Project(
                CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix()),
                                                 CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
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
