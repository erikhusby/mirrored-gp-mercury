package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSetVolumeConcentrationTest extends Arquillian {

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private BSPConfig bspConfig;

    @Test(enabled = true)
    public void testSetVolumeAndConcentration() {
        BSPSetVolumeConcentration bspSetVolumeConcentration = new BSPSetVolumeConcentration(bspConfig);
        bspSetVolumeConcentration.setVolumeAndConcentration("SM-1234", 50.0D, 125.2D);
        String[] result = bspSetVolumeConcentration.getResult();
        Assert.assertTrue("There should be a result", result != null && result.length > 0);
        Assert.assertTrue("Should have received update result", result[0].startsWith("updated volume and concentration for"));
    }
}
