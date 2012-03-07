package org.broadinstitute.sequel.jira;


import org.broadinstitute.sequel.control.jira.JiraService;
import org.broadinstitute.sequel.control.jira.issue.CreateRequest;
import org.broadinstitute.sequel.control.jira.issue.CreateResponse;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;



public class JiraServiceTest {

    private JiraService service;

    @BeforeClass(groups = "ExternalIntegration")
    public void initWeld() {
        WeldContainer weld = new Weld().initialize();
        service = weld.instance().select(JiraService.class).get();
    }

    @Test
    public void testCreation() {

        try {
            final CreateResponse createResponse = service.createIssue("TP", CreateRequest.Fields.Issuetype.IssuetypeName.Bug, "This is the summary created from SequeL", "This is the description created from SequeL" );
            final String key = createResponse.getKey();
            Assert.assertNotNull(key);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
