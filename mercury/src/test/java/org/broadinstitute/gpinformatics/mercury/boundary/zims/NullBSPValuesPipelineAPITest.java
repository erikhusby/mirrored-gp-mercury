package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mocks.NullValuesBSPSampleSearchService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class NullBSPValuesPipelineAPITest extends Arquillian {

    public NullBSPValuesPipelineAPITest(){}

    @Inject
    IlluminaRunResource runLaneResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(TEST,
                MockThriftService.class,
                NullValuesBSPSampleSearchService.class
        ).addAsResource(ThriftFileAccessor.RUN_FILE);
    }

    @Test(groups = TestGroups.ALTERNATIVES)
    public void testNullBspSampleData() throws Exception {
        ZimsIlluminaRun run = runLaneResource.getRun(IlluminaRunResourceTest.RUN_NAME);
        Assert.assertNotNull(run.getError());
        Assert.assertTrue(run.getError().contains("BSP returned no"));
    }
}
