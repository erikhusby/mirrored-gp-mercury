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

    @Inject
    private BSPSetVolumeConcentration bspSetVolumeConcentration;

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(enabled = true)
    public void testSetVolumeAndConcentration() {
        bspSetVolumeConcentration.setVolumeAndConcentration("SM-1234", 50.0F, 125.2F);
        String[] result = bspSetVolumeConcentration.getResult();
        Assert.assertTrue("There should be a result", result.length > 0);
    }
}
