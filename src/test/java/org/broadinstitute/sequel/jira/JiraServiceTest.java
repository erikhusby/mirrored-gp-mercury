package org.broadinstitute.sequel.jira;


import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.jira.JiraService;
import org.broadinstitute.sequel.control.jira.issue.CreateResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.sequel.control.jira.issue.CreateRequest.Fields.Issuetype.IssuetypeName.Bug;


public class JiraServiceTest extends WeldBooter {

    private JiraService service;

    @BeforeClass
    public void initWeld() {
        service = weldUtil.getFromContainer(JiraService.class);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testCreation() {

        try {

            final CreateResponse createResponse =
                    service.createIssue("TP", Bug, "Summary created from SequeL", "Description created from SequeL" );

            final String key = createResponse.getKey();

            Assert.assertNotNull(key);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
