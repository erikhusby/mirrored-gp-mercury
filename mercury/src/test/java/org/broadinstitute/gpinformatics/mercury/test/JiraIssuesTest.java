package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = EXTERNAL_INTEGRATION)
public class JiraIssuesTest {

    private JiraService jiraService = JiraServiceTestProducer.testInstance();

    @Test(enabled = true)
    public void testSubTasks() throws Exception {

        String issueKey = "DEV-6796";
        JiraIssue jiraIssue = null;
        try {

            jiraIssue = jiraService.getIssueInfo(issueKey, null);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(jiraIssue.getSubTasks().size(), 73);
    }
}

