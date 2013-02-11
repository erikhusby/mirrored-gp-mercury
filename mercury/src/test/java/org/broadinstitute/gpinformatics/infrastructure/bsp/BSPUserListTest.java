package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPUserListTest extends Arquillian {
    private final String TEST_BADGE_ID = "bsptestuser_badge_id_1234";
    private final String BSP_TEST_USER = "tester";

    @Deployment
    public static WebArchive deployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    BSPUserList bspUserList;

    @Test
    public void testGetAllUsers() throws Exception {
        Collection<BspUser> users = bspUserList.getUsers().values();
        Assert.assertNotNull(users);
        Assert.assertTrue(!users.isEmpty());
        // This is an arbitrary sanity check; the actual database has about 2k users.
        Assert.assertTrue(users.size() > 1000);
    }

    @Test
    public void testFindUserById() throws Exception {
        Collection<BspUser> users = bspUserList.getUsers().values();
        Assert.assertTrue(!users.isEmpty());
        BspUser user1 = users.iterator().next();
        BspUser user2 = bspUserList.getById(user1.getUserId());
        Assert.assertTrue(user1.equals(user2));
    }

    @Test
    public void testFindUserByBadgeId() throws Exception {
        BspUser user = bspUserList.getByBadgeId(TEST_BADGE_ID);
        Assert.assertNotNull(user, "Could not find test user!!");
        Assert.assertTrue(user.getUsername().equals(BSP_TEST_USER), "user is not test user!");
    }

    @Test
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
    @Test
    public void testGetByBadgeIdPerformance() {
        long start = System.nanoTime();
        BspUser user = bspUserList.getByBadgeId("BSPUserListTest.testGetByBadgeIdPerformance");
        long end = System.nanoTime();
        long durationNano = end - start;
        double durationMilli = durationNano / 1000000.0;
        Assert.assertTrue("User should not have been found", user == null);
        Assert.assertTrue("Query for unknown badge ID should take less than 200ms. Actually took " +
                durationMilli + "ms", durationMilli < 50);
    }
}
