package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * This is an example of a "real live" integration test.
 */
public class BSPSampleSearchServiceTest {

    @Test(groups = EXTERNAL_INTEGRATION, enabled = true)
    public void testBasic() {

        BSPSampleSearchService service = BSPSampleSearchServiceProducer.qaInstance();

        List<String> sampleIDs = new ArrayList<String>() {{
            add("SM-12CO4");
        }};

        List<Map<BSPSampleSearchColumn, String>> data =
            service.runSampleSearch(
                sampleIDs,
                BSPSampleSearchColumn.SAMPLE_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.ROOT_SAMPLE);
        Assert.assertEquals("SM-12CO4", data.get(0).get(BSPSampleSearchColumn.SAMPLE_ID));

        sampleIDs = new ArrayList<String>() {{
            add("SM-12MD2");
        }};

        data = service.runSampleSearch(sampleIDs, BSPSampleSearchColumn.SAMPLE_TYPE, BSPSampleSearchColumn.LSID);
        Assert.assertEquals(BSPSampleDTO.TUMOR_IND, data.get(0).get(BSPSampleSearchColumn.SAMPLE_TYPE));
    }
}