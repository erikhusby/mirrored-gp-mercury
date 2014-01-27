package org.broadinstitute.gpinformatics.mercury.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BSPLSIDUtilTest {

    @Test(groups = DATABASE_FREE)
    public void testIsBspLsidBareBspId() {
        assertThat(BSPLSIDUtil.isBspLsid("1234"), is(false));
    }

    @Test(groups = DATABASE_FREE)
    public void testIsBspLsidSquidLsid() {
        assertThat(BSPLSIDUtil.isBspLsid("BROAD:SEQUENCING_SAMPLE:1234.0"), is(false));
    }

    @Test(groups = DATABASE_FREE)
    public void testIsBspLsidBroadinstituteDotOrg() {
        assertThat(BSPLSIDUtil.isBspLsid("broadinstitute.org:bsp.prod.sample:1234"), is(true));
    }

    @Test(groups = DATABASE_FREE)
    public void testIsBspLsidBroadDotMitDotEdu() {
        assertThat(BSPLSIDUtil.isBspLsid("broad.mit.edu:bsp.prod.sample:1234"), is(true));
    }

    @Test(groups = DATABASE_FREE)
    public void testLsidsToIds() {
        String [] lsids = {
                "broadinstitute.org:bsp.prod.sample:UP6R",
                "broad.mit.edu:bsp.prod.sample:192P",
        };

        String [] ids = {
                "UP6R",
                "192P"
        };

        Map<String, String> map = BSPLSIDUtil.lsidsToBareIds(Arrays.asList(lsids));

        Assert.assertEquals(2, map.size());

        for (int i = 0; i < lsids.length; i++) {
            Assert.assertTrue(map.containsKey(lsids[i]));
            Assert.assertEquals(ids[i], map.get(lsids[i]));
        }
    }
}
