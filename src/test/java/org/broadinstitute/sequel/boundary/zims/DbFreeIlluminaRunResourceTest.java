package org.broadinstitute.sequel.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.thrift.QAThriftConfiguration;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftFileAccessor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.util.HashMap;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

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
        ZimsIlluminaRun runBean = new IlluminaRunResource().getRun(thriftRun,new HashMap<String, BSPSampleDTO>());
        IlluminaRunResourceTest.doAssertions(thriftRun,runBean);
    }
    
    


    

}
