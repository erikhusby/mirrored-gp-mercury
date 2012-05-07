package org.broadinstitute.sequel.jira;


import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.infrastructure.jira.issue.Visibility;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = EXTERNAL_INTEGRATION)
public class JiraServiceTest extends ContainerTest {

    @Inject
    private JiraService service;

    public void testCreation() {

        try {

            final CreateIssueResponse createIssueResponse =
                    service.createIssue(JiraTicket.TEST_PROJECT_PREFIX, CreateIssueRequest.Fields.Issuetype.SequeL_Project, "Summary created from SequeL", "Description created from SequeL" );

            final String key = createIssueResponse.getTicketName();

            Assert.assertNotNull(key);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }



    public void testAddPublicComment() {

        try {

            service.addComment("TP-4", "Publicly visible comment added from SequeL");
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }

    }
    
    
    public void testAddRestrictedComment() {
        
        try {
            
            service.addComment("TP-5", "Administrator-only comment added from SequeL", Visibility.Type.role, Visibility.Value.Administrators );
        }
        catch (IOException iox) {

            Assert.fail(iox.getMessage());

        }
            
    }
}
