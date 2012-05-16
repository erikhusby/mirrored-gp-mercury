package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.*;

public class ThriftFileAccessor {

    private static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX";

    private static final File RUN_FILE = new File("src/test/data/thrift/",RUN_NAME + ".thrift");

    /**
     * Use this method to update the thrift run
     * object.
     * @throws Exception
     */
    private void writeRunFile() throws Exception {
        ThriftConfiguration qaThrift = new QAThriftConfiguration();
        TZamboniRun runFetchedFromService = fetchRun(qaThrift,"foo");
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

    public static TZamboniRun deserializeRun() throws IOException,TException {
        if (!RUN_FILE.exists()) {
            throw new RuntimeException("File " + RUN_FILE.getAbsolutePath() + " does not exist.");
        }
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(RUN_FILE));
        TBinaryProtocol thriftReader = new TBinaryProtocol(new TIOStreamTransport(inputStream));
        TZamboniRun zamboniRun = new TZamboniRun();
        zamboniRun.read(thriftReader);
        inputStream.close();

        return zamboniRun;
    }
}
