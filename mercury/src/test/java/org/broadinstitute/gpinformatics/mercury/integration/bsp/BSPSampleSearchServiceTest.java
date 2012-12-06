package org.broadinstitute.gpinformatics.mercury.integration.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceProducer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * This is an example of a "real live" integration test.
 */
public class BSPSampleSearchServiceTest {

    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testBasic() {
        BSPSampleSearchService service = BSPSampleSearchServiceProducer.qaInstance();

        String TEST_SAMPLE_ID = "SM-12CO4";
        String [] sampleIDs = new String [] {TEST_SAMPLE_ID};
        List<String[]> data = service.runSampleSearch(Arrays.asList(sampleIDs), BSPSampleSearchColumn.SAMPLE_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.ROOT_SAMPLE);
        Assert.assertEquals(TEST_SAMPLE_ID, data.get(0)[0]);

        TEST_SAMPLE_ID = "SM-12MD2";
        sampleIDs = new String [] {TEST_SAMPLE_ID};
        data = service.runSampleSearch(Arrays.asList(sampleIDs),BSPSampleSearchColumn.SAMPLE_TYPE);
        Assert.assertEquals(BSPSampleDTO.TUMOR_IND,data.get(0)[0]);

    }

}