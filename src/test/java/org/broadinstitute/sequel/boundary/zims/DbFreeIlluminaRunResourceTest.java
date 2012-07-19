package org.broadinstitute.sequel.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import junit.framework.Assert;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.sequel.infrastructure.jmx.ZimsCacheControl;
import org.broadinstitute.sequel.infrastructure.thrift.MockThriftService;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * Instead of spending so much time waiting for the thrift server
 * (that's left to {@link IlluminaRunResourceTest}), this test
 * loads a pre-serialized thrift run from local disk, converts it
 * to {@link ZimsIlluminaRun}, and does some basic assertions.
 */
public class DbFreeIlluminaRunResourceTest {

    
    @Test(groups = DATABASE_FREE)
    public void test_known_good_run() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        ZimsIlluminaRun runBean = new IlluminaRunResource(
                new MockThriftService(),
                new ZimsCacheControl(),
                new BSPSampleDataFetcher(new BSPSampleSearchServiceStub())
        ).getRun(thriftRun,new HashMap<String, BSPSampleDTO>());
        IlluminaRunResourceTest.doAssertions(thriftRun,runBean);
    }

    @Test(groups = DATABASE_FREE)
    public void test_empty_reads_refreshes_cache() throws Exception {
        EmptyReadsThriftService emptyReadsThriftService = new EmptyReadsThriftService();
        emptyReadsThriftService.setDoEmptyReads(true);
        ZimsIlluminaRun runBean = null;
        final IlluminaRunResource runResource  = new IlluminaRunResource(emptyReadsThriftService,new ZimsCacheControl(), new BSPSampleDataFetcher(new BSPSampleSearchServiceStub()));
        runBean = runResource.getRun("whatever");
        Assert.assertTrue(runBean.getReads().isEmpty());
        // run should now be cached.  update the reads and it should be re-fetched
        emptyReadsThriftService.setDoEmptyReads(false);
        runBean = runResource.getRun("whatever");
        Assert.assertFalse(runBean.getReads().isEmpty());
    }

    private final class EmptyReadsThriftService implements ThriftService {

        private boolean emptyReads = false;

        public void setDoEmptyReads(boolean doEmptyReads) {
            this.emptyReads = doEmptyReads;
        }

        @Override
        public TZamboniRun fetchRun(String runName) throws TZIMSException, TException {
            TZamboniRun thriftRun = null;
            try {
                thriftRun = ThriftFileAccessor.deserializeRun();
            }
            catch(Exception e) {
                throw new RuntimeException("Couldn't read run file",e);
            }
            if (emptyReads) {
                thriftRun.getReads().clear();
            }
            return thriftRun;
        }
    }
    
    


    

}
