package org.broadinstitute.sequel.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.jmx.ZimsCacheControl;
import org.broadinstitute.sequel.infrastructure.thrift.MockThriftService;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftFileAccessor;
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
                new ZimsCacheControl()
        ).getRun(thriftRun,new HashMap<String, BSPSampleDTO>());
        IlluminaRunResourceTest.doAssertions(thriftRun,runBean);
    }
    
    


    

}
