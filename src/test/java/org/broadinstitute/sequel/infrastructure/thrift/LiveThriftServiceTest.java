package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author breilly
 */
public class LiveThriftServiceTest {

    private LiveThriftService thriftService;

    @BeforeMethod
    public void setUp() throws Exception {
        thriftService = new LiveThriftService(ThriftConfigProducer.produce(Deployment.TEST));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFetchRun() throws Exception {
        TZamboniRun run = thriftService.fetchRun("120320_SL-HBN_0159_AFCC0GHCACXX");
        assertThat(run, not(nullValue()));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, expectedExceptions = TZIMSException.class)
    public void testFetchRunInvalid() throws Exception {
        thriftService.fetchRun("invalid_run");
    }
}
