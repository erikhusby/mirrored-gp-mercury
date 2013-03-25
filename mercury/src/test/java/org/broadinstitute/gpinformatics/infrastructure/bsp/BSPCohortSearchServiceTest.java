package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class BSPCohortSearchServiceTest  extends ContainerTest {

    @Test(groups = EXTERNAL_INTEGRATION, enabled = false)
    public void testBasic() {
        Set<Cohort> rawCohorts = null;
        try {
            BSPCohortSearchService cohortSearchService = BSPCohortSearchServiceProducer.testInstance();
            rawCohorts = cohortSearchService.getAllCohorts();
        } catch (Exception ex) {
            Assert.fail("Could not get BSP Cohorts from BSP QA");
        }

        Assert.assertNotNull(rawCohorts);
        //Arbitrary sanity check.
        Assert.assertTrue( rawCohorts.size() > 1 );
    }

}
