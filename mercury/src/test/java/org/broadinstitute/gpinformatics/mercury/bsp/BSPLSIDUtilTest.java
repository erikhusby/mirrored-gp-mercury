package org.broadinstitute.gpinformatics.mercury.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPLSIDUtilTest {

    public void testIsBspLsidBareBspId() {
        assertThat(BSPLSIDUtil.isBspLsid("1234"), is(false));
    }

    public void testIsBspLsidSquidLsid() {
        assertThat(BSPLSIDUtil.isBspLsid("BROAD:SEQUENCING_SAMPLE:1234.0"), is(false));
    }

    public void testIsBspLsidBroadinstituteDotOrg() {
        assertThat(BSPLSIDUtil.isBspLsid("broadinstitute.org:bsp.prod.sample:1234"), is(true));
    }

    public void testIsBspLsidBroadDotMitDotEdu() {
        assertThat(BSPLSIDUtil.isBspLsid("broad.mit.edu:bsp.prod.sample:1234"), is(true));
    }

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

    public void testLsidToBspSampleId() {
        String  lsid = "broad.mit.edu:bsp.prod.sample:UP6R";

        String bspId = "SM-UP6R";

        String resultId = BSPLSIDUtil.lsidToBspSampleId(lsid);

        Assert.assertEquals(bspId, resultId);
    }
}
