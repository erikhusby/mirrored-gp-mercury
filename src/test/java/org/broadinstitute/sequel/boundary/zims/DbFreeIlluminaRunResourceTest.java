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

    private final File RUN_FILE = new File("src/test/data/thrift/",IlluminaRunResourceTest.RUN_NAME + ".thrift");
    
    @Test(groups = DATABASE_FREE)
    public void test_known_good_run() throws Exception {  
        TZamboniRun thriftRun = deserializeRun(RUN_FILE);
        ZimsIlluminaRun runBean = new IlluminaRunResource().getRun(thriftRun,new HashMap<String, BSPSampleDTO>());
        IlluminaRunResourceTest.doAssertions(thriftRun,runBean);
    }

    /**
     * Use this method to update the thrift run
     * object.
     * @throws Exception
     */
    private void writeRunFile() throws Exception {
        ThriftConfiguration qaThrift = new QAThriftConfiguration();
        TZamboniRun runFetchedFromService = fetchRun(qaThrift,IlluminaRunResourceTest.RUN_NAME);
        serializeRun(runFetchedFromService,RUN_FILE);
    }
    
    
    
    private TZamboniRun fetchRun(ThriftConfiguration thriftConfiguration,
                                 String runName) throws TZIMSException, TException {

        TTransport transport = new TSocket(thriftConfiguration.getHost(), thriftConfiguration.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        transport.open();
        
        TZamboniRun zamboniRun = client.fetchRun(runName);
        transport.close();
        return zamboniRun;
    }
    
    private void serializeRun(TZamboniRun zamboniRun,File fileToWrite) throws IOException,TException {
        if (!fileToWrite.exists()) {
            if (!fileToWrite.createNewFile()) {
                throw new RuntimeException("Could not create file " + fileToWrite.getAbsolutePath());
            }
        }
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileToWrite));
        TBinaryProtocol thriftWriter = new TBinaryProtocol(new TIOStreamTransport(outputStream));
        zamboniRun.write(thriftWriter);
        outputStream.flush();
        outputStream.close();        
    }
    
    private TZamboniRun deserializeRun(File fileToRead) throws IOException,TException  {
        if (!fileToRead.exists()) {
            throw new RuntimeException("File " + fileToRead.getAbsolutePath() + " does not exist.");
        }      
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileToRead));
        TBinaryProtocol thriftReader = new TBinaryProtocol(new TIOStreamTransport(inputStream));
        TZamboniRun zamboniRun = new TZamboniRun();
        zamboniRun.read(thriftReader);
        inputStream.close();
        
        return zamboniRun;
    }
}
