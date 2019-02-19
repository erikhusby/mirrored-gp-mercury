/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.integration.jira;

import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class JiraServiceDbFreeTest {
    private JiraService jiraService;

    @BeforeTest
    public void setUp() {
        jiraService = JiraServiceTestProducer.stubInstance();
    }

    @Test(expectedExceptions = NoJiraTransitionException.class)
    public void testTransitionNotFound() {
        jiraService.findAvailableTransitionByName("YOMAMA-1234", "Wear Combat Boots");
        Assert.fail("You should not see this because I would have thrown a NoJiraTransitionException first.");
    }

    public void testTransitionWasFound() {
        String developerEditTransition = ResearchProjectEjb.JiraTransition.COMPLETE.getStateName();
        Transition availableTransitionByName =
                jiraService.findAvailableTransitionByName("YOMAMA-1234", developerEditTransition);
        Assert.assertEquals(availableTransitionByName.getName(), developerEditTransition);
    }

    @DataProvider(name = "projectTypes")
    public Iterator<Object[]> projectTypes() {
        Set<Object[]> testData = new HashSet<>();

        // All "normal" cases should map back to real ProjectTypes.
        for (CreateFields.ProjectType projectType : CreateFields.ProjectType.values()) {
            testData.add(new Object[]{String.format("%s-1234", projectType.getKeyPrefix()), projectType});
        }

        // Some specific oddball cases to should all return null.
        testData.add(new Object[]{"XTR", null});
        testData.add(new Object[]{"1234", null});
        testData.add(new Object[]{"X-TR1234", null});
        testData.add(new Object[]{"X-T-R1234", null});
        testData.add(new Object[]{"XTR-1-234", null});
        testData.add(new Object[]{"LCSET1234", null});
        testData.add(new Object[]{"LCSET-ABCD", null});
        testData.add(new Object[]{"ABCD-LCSET", null});
        testData.add(new Object[]{"", null});
        testData.add(new Object[]{null, null});

        return testData.iterator();

    }

    @Test(dataProvider = "projectTypes")
    public void testProjectTypeParsing(String issueKey, CreateFields.ProjectType projectType) throws Exception {
        assertThat(CreateFields.ProjectType.fromIssueKey(issueKey), equalTo(projectType));
    }
}
