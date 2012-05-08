package org.broadinstitute.sequel.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.infrastructure.bsp.*;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleSearchServiceTest  {

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testBasic() {
        BSPSampleSearchService service = new BSPSampleSearchServiceImpl(new QABSPConnectionParameters());
        final String TEST_SAMPLE_ID = "SM-12CO4";
        String [] sampleIDs = new String [] {TEST_SAMPLE_ID};
        List<String[]> data = service.runSampleSearch(Arrays.asList(sampleIDs), BSPSampleSearchColumn.SAMPLE_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.ROOT_SAMPLE);
        Assert.assertEquals(TEST_SAMPLE_ID, data.get(0)[0]);
    }


}