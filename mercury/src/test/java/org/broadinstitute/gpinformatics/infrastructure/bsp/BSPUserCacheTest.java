package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

@Test
public class BSPUserCacheTest extends ContainerTest {
    @TestInstance
    @Inject
    BSPUserCache bspUserCache;

    @Test
    public void testGetAllUsers() throws Exception {
        Assert.assertTrue(false);
        List<BspUser> users = bspUserCache.getUsers();
        Assert.assertNotNull(users);
        Assert.assertTrue(!users.isEmpty());
        // This is an arbitrary sanity check; the actual database has about 2.5k users.
        Assert.assertTrue(users.size() > 10);
    }

    @Test
    public void testFindResearchProjectById() throws Exception {
        Assert.assertTrue(false);
        List<BspUser> users = bspUserCache.getUsers();
        BspUser user1 = users.get(0);
        BspUser user2 = bspUserCache.getById(user1.getUserId());
        Assert.assertFalse(user1.equals(user2));
    }
}
