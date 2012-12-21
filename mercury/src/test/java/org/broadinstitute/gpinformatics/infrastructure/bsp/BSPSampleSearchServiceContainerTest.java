package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test that BSPStartingSample can initialize BSPDto
 */
public class BSPSampleSearchServiceContainerTest extends ContainerTest {

    /**
     * Requires SEQUEL_DEPLOYMENT=DEV in DeploymentBuilder
     */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = false)
    public void testDto() {
        MercurySample bspStartingSample = new MercurySample("SM-12CO4", null, null/*, new PassBackedProjectPlan()*/);
//        String patientId = bspStartingSample.getPatientId();
//        Assert.assertEquals(patientId, "PT-2LK3", "Wrong patient ID");
    }

}
