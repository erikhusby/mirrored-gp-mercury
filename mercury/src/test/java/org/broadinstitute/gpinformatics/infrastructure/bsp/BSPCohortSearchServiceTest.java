package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPCohortSearchServiceTest {

    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testBasic() {
        Set<Cohort> rawCohorts = null;
        try {
            BSPConfig bspConfig = BSPConfig.produce(DEV);
            BSPCohortSearchService cohortSearchService = new BSPCohortSearchServiceImpl(bspConfig);
            rawCohorts = cohortSearchService.getAllCohorts();
        } catch (Exception ex) {
            Assert.fail("Could not get BSP Cohorts from BSP QA");
        }

        Assert.assertNotNull(rawCohorts);
        //Arbitrary sanity check.
        Assert.assertTrue( rawCohorts.size() > 1 );
    }

}
