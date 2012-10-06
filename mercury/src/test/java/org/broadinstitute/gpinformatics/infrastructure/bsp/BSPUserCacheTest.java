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

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

@Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPUserCacheTest extends Arquillian {


    @Deployment
    public static WebArchive deployment() {
        // QA BSP config not working for me, yaml parameters might be off
        return DeploymentBuilder.buildMercuryWar(PROD);
    }

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
    // test currently works but the method name seems a tad off
    public void testFindResearchProjectById() throws Exception {
        List<BspUser> users = bspUserCache.getUsers();
        BspUser user1 = users.get(0);
        BspUser user2 = bspUserCache.getById(user1.getUserId());
        Assert.assertTrue(user1.equals(user2));
    }
}
