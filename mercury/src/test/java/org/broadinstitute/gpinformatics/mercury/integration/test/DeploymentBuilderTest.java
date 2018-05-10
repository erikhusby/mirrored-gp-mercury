package org.broadinstitute.gpinformatics.mercury.integration.test;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Confirm the Stubby Builder actually puts stub alternatives in CDI? <br />
 * Disabled because otherwise all our tests would fail anyways
 */
@Test(groups = TestGroups.STUBBY, enabled = false)
public class DeploymentBuilderTest extends StubbyContainerTest {

    @Inject
    private JiraService service;

    @Test(enabled = false)
    public void testInjection() {
        Assert.assertEquals(service.getClass().getSimpleName(), "JiraServiceStub");
    }
}
