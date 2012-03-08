package org.broadinstitute.sequel.jira;


import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.jira.JiraService;
import org.broadinstitute.sequel.control.jira.issue.CreateIssueResponse;
import org.broadinstitute.sequel.control.jira.issue.Visibility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.sequel.control.jira.issue.CreateIssueRequest.Fields.Issuetype.Name.Bug;

@Test(groups = EXTERNAL_INTEGRATION)
public class JiraServiceTest extends WeldBooter {

    private JiraService service;

    @BeforeClass
    public void initWeld() {
        service = weldUtil.getFromContainer(JiraService.class);
    }


    public void testCreation() {

        try {

            final CreateIssueResponse createIssueResponse =
                    service.createIssue("TP", Bug, "Summary created from SequeL", "Description created from SequeL" );

            final String key = createIssueResponse.getKey();

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
