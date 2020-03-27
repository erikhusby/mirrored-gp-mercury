package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.RunTimeAlternatives;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mocks.NullValuesBSPSampleSearchService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

@Test(groups = TestGroups.STUBBY)
public class NullBSPValuesPipelineAPITest extends StubbyContainerTest {
    @Inject
    IlluminaRunResource runLaneResource;

    @Inject
    private MockThriftService thriftService;

    @Inject
    private NullValuesBSPSampleSearchService bspSampleSearchService;

    public NullBSPValuesPipelineAPITest(){}

    @BeforeMethod
    public void beforeMethod() {
        RunTimeAlternatives.addThreadLocalAlternative(ThriftService.class, thriftService);
        RunTimeAlternatives.addThreadLocalAlternative(BSPSampleSearchService.class, bspSampleSearchService);
    }

    @AfterMethod
    public void afterMethod() {
        RunTimeAlternatives.clearThreadLocalAlternatives();
    }

    @Test(groups = TestGroups.STUBBY)
    public void testNullBspSampleData() throws Exception {
        ZimsIlluminaRun run = runLaneResource.getRun(IlluminaRunResourceTest.RUN_NAME);
        Assert.assertNotNull(run.getError());
        Assert.assertTrue(run.getError().contains("BSP returned no"), run.getError());
    }
}
