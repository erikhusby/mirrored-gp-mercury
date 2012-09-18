package org.broadinstitute.gpinformatics.mercury.integration.bsp;

import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.project.PassBackedProjectPlan;
import org.broadinstitute.gpinformatics.mercury.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.mercury.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test that BSPStartingSample can initialize BSPDto
 */
public class BSPSampleSearchServiceContainerTest extends ContainerTest {

    /**
     * Requires SEQUEL_DEPLOYMENT=DEV in DeploymentBuilder
     */
    @Test(groups = EXTERNAL_INTEGRATION, enabled = false)
    public void testDto() {
        BSPStartingSample bspStartingSample = new BSPStartingSample("SM-12CO4", new PassBackedProjectPlan());
        String patientId = bspStartingSample.getPatientId();
        Assert.assertEquals(patientId, "PT-2LK3", "Wrong patient ID");
    }

}
