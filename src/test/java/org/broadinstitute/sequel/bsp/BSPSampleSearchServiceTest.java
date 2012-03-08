package org.broadinstitute.sequel.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.TestUtilities;
import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.sequel.TestGroups.BOOT_WELD;

public class BSPSampleSearchServiceTest extends WeldBooter {

    @SuppressWarnings("unused")
    private static final Log _logger = LogFactory.getLog(BSPSampleSearchServiceTest.class);

    BSPSampleSearchService service;

    @BeforeClass
    private void init() {
        service = weldUtil.getFromContainer(BSPSampleSearchService.class);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testBasic() {
        final String TEST_SAMPLE_ID = "SM-12CO4";
        String [] sampleIDs = new String [] {TEST_SAMPLE_ID};
        List<String[]> data = service.runSampleSearch(Arrays.asList(sampleIDs), BSPSampleSearchColumn.SAMPLE_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.ROOT_SAMPLE);
        Assert.assertEquals(TEST_SAMPLE_ID, data.get(0)[0]);
    }


    @Test(groups = EXTERNAL_INTEGRATION)
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