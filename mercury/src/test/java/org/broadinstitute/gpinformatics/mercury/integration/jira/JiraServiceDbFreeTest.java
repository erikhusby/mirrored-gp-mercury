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
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class JiraServiceDbFreeTest {
    private JiraService jiraService;

    @BeforeTest
    public void setUp() {
        jiraService = JiraServiceProducer.stubInstance();
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
}
