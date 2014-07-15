package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPUserListTest  {
    private static final String TEST_BADGE_ID = "bsptestuser_badge_id_1234";
    private static final String BSP_TEST_USER = "tester";

    BSPUserList bspUserList=new BSPUserList(BSPManagerFactoryProducer.testInstance());

    public void testGetAllUsers() throws Exception {
        Collection<BspUser> users = bspUserList.getUsers().values();
        Assert.assertNotNull(users);
        Assert.assertTrue(!users.isEmpty());
        Assert.assertTrue(users.size() > 400);
    }

    public void testFindUserById() throws Exception {
        Collection<BspUser> users = bspUserList.getUsers().values();
        Assert.assertTrue(!users.isEmpty());
        BspUser user1 = users.iterator().next();
        BspUser user2 = bspUserList.getById(user1.getUserId());
        Assert.assertTrue(user1.equals(user2));
    }

    public void testFindUserByBadgeId() throws Exception {
        BspUser user = bspUserList.getByBadgeId(TEST_BADGE_ID);
        Assert.assertNotNull(user, "Could not find test user!!");
        Assert.assertTrue(user.getUsername().equals(BSP_TEST_USER), "user is not test user!");
    }

    public void testHasBadgeId() throws Exception {
        BspUser user = bspUserList.getByUsername(BSP_TEST_USER);
        Assert.assertNotNull(user, "Could not find test user!!");
        Assert.assertTrue(user.getUsername().equals(BSP_TEST_USER), "user is not test user!");
        Assert.assertNotNull(user.getBadgeNumber(), "test user should have badgeId");
        Assert.assertTrue(user.getBadgeNumber().equals(TEST_BADGE_ID), "test user should have badgeId");
    }


    /**
     * Test the performance of BSPUserList.getByBadgeId(). Like the existing implementation of getById(), the current
     * implementation of getByBadgeId() is rather inefficient. However, it's good enough, as long as it consistently
     * returns a result in 50ms or less. This test assumes that querying on a badge ID that does not exist is the worst
     * case for performance.
     *
     * TODO: consider moving this to LimsQueryResourceTest instead, since the overall performance of the service is probably a more meaningful measure
     */
    public void testGetByBadgeIdPerformance() {
        long start = System.nanoTime();
        BspUser user = bspUserList.getByBadgeId("BSPUserListTest.testGetByBadgeIdPerformance");
        long end = System.nanoTime();
        long durationNano = end - start;
        double durationMilli = durationNano / 1000000.0;
        Assert.assertTrue(user == null, "User should not have been found");
        Assert.assertTrue(durationMilli < 50, "Query for unknown badge ID should take less than 200ms. Actually took " +
                durationMilli + "ms");
    }
}
