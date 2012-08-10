package org.broadinstitute.sequel.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.sequel.boundary.zims.IlluminaRunResource;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.thrift.LiveThriftService;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfigProducer;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConnection;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftServiceProducer;
import org.testng.annotations.Test;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleDataFetcherTest {

    /**
     * BSP sometimes sends shorter result arrays when there are null fields
     */
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_NPE_on_sample_with_multiple_matches_and_some_null_data() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceImpl(new BSPConfigProducer().produce(Deployment.QA)));
       BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-2NQU6");

        Assert.assertNull(bspSampleDTO.getCollaboratorsSampleName());
        Assert.assertNull(bspSampleDTO.getSampleLsid());
    }
}
