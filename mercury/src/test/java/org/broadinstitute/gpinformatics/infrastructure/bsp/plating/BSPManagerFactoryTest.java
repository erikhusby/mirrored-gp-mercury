package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;


@Test(enabled = false)
public class BSPManagerFactoryTest extends Arquillian {

    @Inject
    private BSPManagerFactory bspManagerFactory;


    @Deployment
    public static WebArchive deployment() {
        // QA BSP config not working for me, yaml parameters might be off
        return DeploymentBuilder.buildMercuryWar(PROD);
    }


    public void testGetProjectManagers() {

        Assert.assertNotNull(bspManagerFactory);

        UserManager userManager = bspManagerFactory.createUserManager();
        Assert.assertNotNull(userManager);

        List<BspUser> projectManagers = userManager.getProjectManagers();
        Assert.assertNotNull(projectManagers);

        Assert.assertTrue(projectManagers.size() > 10);

    }

}
