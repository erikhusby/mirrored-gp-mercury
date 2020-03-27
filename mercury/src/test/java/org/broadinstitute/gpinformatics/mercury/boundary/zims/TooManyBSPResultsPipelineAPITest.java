package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.RunTimeAlternatives;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mocks.TooManySamplesBSPSampleSearchService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class TooManyBSPResultsPipelineAPITest extends StubbyContainerTest {

    public TooManyBSPResultsPipelineAPITest(){}

    @Inject
    IlluminaRunResource runLaneResource;

    @Inject
    TooManySamplesBSPSampleSearchService tooManySamplesBSPSampleSearchService;

    @Inject
    MockThriftService mockThriftService;

    @BeforeMethod
    public void beforeMethod() {
        // Sets the alternative implementations.
        RunTimeAlternatives.addThreadLocalAlternative(BSPSampleSearchService.class,
                tooManySamplesBSPSampleSearchService);
        RunTimeAlternatives.addThreadLocalAlternative(ThriftService.class, mockThriftService);
    }

    @AfterMethod
    public void afterMethod() {
        RunTimeAlternatives.clearThreadLocalAlternatives();
    }

    @Test(groups = TestGroups.STUBBY)
    public void testTooManyBSPResults() {
        ZimsIlluminaRun run = runLaneResource.getRun(IlluminaRunResourceTest.RUN_NAME);
        Assert.assertNotNull(run.getError());
        Assert.assertTrue(run.getError().contains("BSP"), run.getError());
    }
}
