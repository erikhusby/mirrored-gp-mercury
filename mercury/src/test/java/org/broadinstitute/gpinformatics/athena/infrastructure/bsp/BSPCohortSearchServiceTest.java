package org.broadinstitute.gpinformatics.athena.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfigProducer;
import org.testng.annotations.Test;

import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.QA;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class BSPCohortSearchServiceTest {

    @Test(groups = EXTERNAL_INTEGRATION, enabled = false)
    public void testBasic() {
        Set<Cohort> rawCohorts = null;
        try {
            BSPConfig bspConfig = BSPConfigProducer.getConfig(QA);
            BSPCohortSearchService cohortSearchService  = new BSPCohortSearchServiceImpl( bspConfig );
            rawCohorts = cohortSearchService.getAllCohorts();
        } catch (Exception ex) {
            Assert.fail("Could not get BSP Cohorts from BSP QA");
        }

        Assert.assertNotNull(rawCohorts);
        //Arbitrary sanity check.
        Assert.assertTrue( rawCohorts.size() > 1 );
    }

}