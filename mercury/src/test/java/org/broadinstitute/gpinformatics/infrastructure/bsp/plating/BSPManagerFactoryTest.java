package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.users.UserManager;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPManagerFactoryTest {

    private BSPManagerFactory bspManagerFactory=BSPManagerFactoryProducer.testInstance();

    public void testGetProjectManagers() {
        Assert.assertNotNull(bspManagerFactory);

        UserManager userManager = bspManagerFactory.createUserManager();
        Assert.assertNotNull(userManager);

        List<BspUser> users = userManager.getUsers();
        Assert.assertNotNull(users);

        Assert.assertTrue(users.size() > 10);
    }
}
