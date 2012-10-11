package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPUserListTest extends Arquillian {
    @Deployment
    public static WebArchive deployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    BSPUserList bspUserList;

    @Test
    public void testGetAllUsers() throws Exception {
        List<BspUser> users = bspUserList.getUsers();
        Assert.assertNotNull(users);
        Assert.assertTrue(!users.isEmpty());
        // This is an arbitrary sanity check; the actual database has about 2k users.
        Assert.assertTrue(users.size() > 1000);
    }

    @Test
    public void testFindUserById() throws Exception {
        List<BspUser> users = bspUserList.getUsers();
        BspUser user1 = users.get(0);
        BspUser user2 = bspUserList.getById(user1.getUserId());
        Assert.assertTrue(user1.equals(user2));
    }
}
