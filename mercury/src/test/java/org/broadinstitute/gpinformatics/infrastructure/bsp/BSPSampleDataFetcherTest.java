package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.testng.annotations.Test;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleDataFetcherTest {

    /**
     * BSP sometimes sends shorter result arrays when there are null fields
     *
     * mlc disabling this since neither the collaborator sample name nor LSID looks to actually be null in BSP
     */
    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void test_NPE_on_sample_with_multiple_matches_and_some_null_data() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceImpl(new BSPConfigProducer().produce(Deployment.QA)));
        BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-2NQU6");

        Assert.assertNull(bspSampleDTO.getCollaboratorsSampleName());
        Assert.assertNull(bspSampleDTO.getSampleLsid());
    }
}
