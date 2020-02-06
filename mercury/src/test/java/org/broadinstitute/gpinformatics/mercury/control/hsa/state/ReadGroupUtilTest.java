package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReadGroupUtilTest {

    @Test
    public void testCreateSampleSheetId() {
        String sampleKey = "0.65X_POOL_SPRI_1";
        String ssId = ReadGroupUtil.convertSampleKeyToSampleSheetId(sampleKey);
        String rgId = ReadGroupUtil.createSampleSheetId("HM7L3DSXX", 3, sampleKey);
        Assert.assertEquals(rgId, "HM7L3DSXX_3_0_65X_POOL_SPRI_1");
        String ssIdParsed = ReadGroupUtil.parseSampleIdFromRgSampleSheet(rgId);
        Assert.assertEquals(ssId, ssIdParsed);
        Assert.assertEquals(rgId, "HM7L3DSXX_3_0_65X_POOL_SPRI_1");
    }
}