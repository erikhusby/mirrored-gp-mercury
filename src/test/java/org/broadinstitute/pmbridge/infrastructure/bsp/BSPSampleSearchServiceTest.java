package org.broadinstitute.pmbridge.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.WeldBooter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;


import java.util.Arrays;
import java.util.List;

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
        Assert.assertEquals(data.get(0)[0],TEST_SAMPLE_ID);
        Assert.assertEquals(data.get(0)[2], "SM-4ELX");

    }
}