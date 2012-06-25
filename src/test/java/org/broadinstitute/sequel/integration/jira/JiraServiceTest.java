package org.broadinstitute.sequel.integration.jira;


import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.sequel.infrastructure.jira.TestLabObsJira;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = EXTERNAL_INTEGRATION)
public class JiraServiceTest {

    private JiraService service;

    @BeforeMethod
    public void setUp() {
        service = new JiraServiceImpl(new TestLabObsJira());
    }

    public void testCreation() {

        try {

            final CreateIssueResponse createIssueResponse =
                    service.createIssue(JiraTicket.TEST_PROJECT_PREFIX, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel, "Summary created from SequeL", "Description created from SequeL" );

            final String key = createIssueResponse.getTicketName();

            Assert.assertNotNull(key);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }



    public void testAddPublicComment() {

        try {

            service.addComment("LCSET-1678", "Publicly visible comment added from SequeL");
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }

    }
    

    @Test(enabled = false)
    // disabled until we can get test jira to keep squid user as an admin
    public void testAddRestrictedComment() {
        
        try {
            
            service.addComment("LCSET-1678", "jira-users only comment added from SequeL", Visibility.Type.role, Visibility.Value.Administrators );
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }
            
    }

    public void test_custom_fields() throws IOException {
        Collection<CustomFieldDefinition> customFields = null;
        customFields = service.getCustomFields(new CreateIssueRequest.Fields.Project(Project.JIRA_PROJECT_PREFIX),CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        Assert.assertFalse(customFields.isEmpty());
        boolean foundLanesRequestedField = false;
        for (CustomFieldDefinition customField : customFields) {
            System.out.println(customField.getName() + " id " + customField.getJiraCustomFieldId());
            if (customField.getName().equals("Lanes Requested")) {
                foundLanesRequestedField = true;
            }
        }
        Assert.assertTrue(foundLanesRequestedField);

    }
}
