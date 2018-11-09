package org.broadinstitute.gpinformatics.mercury.integration.test;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * Confirm the Stubby Builder actually puts stub alternatives in CDI? <br />
 * Disabled because otherwise all our tests would fail anyways
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class DeploymentBuilderTest extends StubbyContainerTest {

    public DeploymentBuilderTest(){}

    @Inject
    private JiraService service;

    @Test
    public void testCdiAlternativesInjection() {
        Assert.assertEquals(service.getClass().getSimpleName(), "JiraServiceStub", "Injected artifact expected to be a stub implementation");
    }
}
