package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(enabled = false, groups = {TestGroups.STANDARD})
public class BSPManagerFactoryTest extends Arquillian {

    @Inject
    private BSPManagerFactory bspManagerFactory;


    @Deployment
    public static WebArchive deployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public void testGetProjectManagers() {
        Assert.assertNotNull(bspManagerFactory);

        UserManager userManager = bspManagerFactory.createUserManager();
        Assert.assertNotNull(userManager);

        List<BspUser> users = userManager.getUsers();
        Assert.assertNotNull(users);

        Assert.assertTrue(users.size() > 10);
    }
}