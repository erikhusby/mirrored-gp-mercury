package org.broadinstitute.sequel.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BSPSampleSearchServiceTest {

    @Inject
    private BSPSampleSearchService service;


    @SuppressWarnings("unused")
    private static final Log _logger = LogFactory.getLog(BSPSampleSearchServiceTest.class);


    @Test(enabled = false)
    public void testBasic() {
        Assert.assertNotNull(service);

        final String TEST_SAMPLE_ID = "SM-12CO4";
        String [] sampleIDs = new String [] {TEST_SAMPLE_ID};

        List<String[]> data = service.runSampleSearch(Arrays.asList(sampleIDs), BSPSampleSearchColumn.SAMPLE_ID);

        Assert.assertEquals(TEST_SAMPLE_ID, data.get(0)[0]);
    }


    @Test(enabled = false)
    public void testLsidsToIds() {
        String [] lsids = {
                "broadinstitute.org:bsp.prod.sample:UP6R",
                "broad.mit.edu:bsp.prod.sample:192P",
        };

        String [] ids = {
                "UP6R",
                "192P"
        };

        Map<String, String> map = service.lsidsToBareIds(Arrays.asList(lsids));

        Assert.assertEquals(2, map.size());

        for (int i = 0; i < lsids.length; i++) {
            Assert.assertTrue(map.containsKey(lsids[i]));
            Assert.assertEquals(ids[i], map.get(lsids[i]));
        }
    }
}