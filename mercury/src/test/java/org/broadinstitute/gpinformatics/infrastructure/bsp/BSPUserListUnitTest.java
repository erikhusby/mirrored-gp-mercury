package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author breilly
 */
@Test(groups = DATABASE_FREE)
public class BSPUserListUnitTest {

    private BSPUserList bspUserList;

    @BeforeMethod
    public void setUp() throws Exception {
        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        bspUserList.refreshCache();
    }

    public void testFindByBadgeId() {
        BspUser user = bspUserList.getByBadgeId("Test9382");
        assertThat(user, notNullValue());
        assertThat(user.getUsername(), equalTo("QADudeTest"));
    }

    public void testFindByBadgeIdNotFound() {
        BspUser user = bspUserList.getByBadgeId("unknown");
        assertThat(user, nullValue());
    }
}
