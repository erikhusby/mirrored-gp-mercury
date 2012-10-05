package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPUserCacheTest extends ContainerTest {
    @Inject
    BSPUserCache bspUserCache;

    @Test
    public void testGetAllUsers() throws Exception {
        List<BspUser> users = bspUserCache.getUsers();
        Assert.assertNotNull(users);
        Assert.assertTrue(!users.isEmpty());
        // This is an arbitrary sanity check; the actual database has about 2.5k users.
        Assert.assertTrue(users.size() > 10);
    }

    @Test
    public void testFindResearchProjectById() throws Exception {
        List<BspUser> users = bspUserCache.getUsers();
        BspUser user1 = users.get(0);
        BspUser user2 = bspUserCache.getById(user1.getUserId());
        Assert.assertTrue(user1.equals(user2));
    }
}
