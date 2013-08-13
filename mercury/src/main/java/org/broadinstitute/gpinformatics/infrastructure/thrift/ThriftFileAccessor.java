package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.*;
import java.util.Arrays;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class ThriftFileAccessor {

    private static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX";

    private static final String RUN_FILE_NAME = RUN_NAME + ".thrift";

    public static final File RUN_FILE = new File("src/test/resources/thrift/",RUN_FILE_NAME);

    /**
     * Use this method to update the thrift run
     * object.
     * @throws Exception
     */
    private static void writeRunFile() throws Exception {
        ThriftConfig qaThrift = ThriftConfig.produce(DEV);
        TZamboniRun runFetchedFromService = fetchRun(qaThrift);
        serializeRun(runFetchedFromService,RUN_FILE);
    }

    public static void main(String[] args) {
        try {
            TZamboniRun updateRun = deserializeRun();
            // change it
            /*
            for (TZamboniLane zamboniLane : updateRun.getLanes()) {
                for (TZamboniLibrary zamboniLibrary : zamboniLane.getLibraries()) {
                    if (zamboniLibrary.getWorkRequestId() == 29225) {
                        zamboniLibrary.setPdoKey("PDO-36");
                        System.out.println("updated pdo");
                        zamboniLibrary.setLcset("LCSET-999999");
                    }
                }
            }
            updateRun.setActualReadStructure("76T8B76T");
            updateRun.setImagedAreaPerLaneMM2(1.23);
            updateRun.setSetupReadStructure("76T8B8B76T");
            updateRun.setLanesSequenced("2");
            updateRun.setRunFolder("/full/path/to/run/folder");
            ThriftFileAccessor.serializeRun(updateRun,ThriftFileAccessor.RUN_FILE);
            */
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private static TZamboniRun fetchRun(ThriftConfig thriftConfig) throws TZIMSException, TException {

        TTransport transport = new TSocket(thriftConfig.getHost(), thriftConfig.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        transport.open();

        TZamboniRun zamboniRun = client.fetchRun(RUN_NAME);
        for (TZamboniLane lane : zamboniRun.getLanes()) {
            for (TZamboniLibrary lib : lane.getLibraries()) {
                if (lane.getLaneNumber() % 2 == 0) {
                    // we're overwriting some values here to make a more
                    // representative data set
                    lib.setAggregate(true);
                    lib.setLabMeasuredInsertSize(14.3);
                    lib.setPrecircularizationDnaSize(19.2);
                    lib.setDevExperimentDataIsSet(true);
                    lib.setDevExperimentData(new TZDevExperimentData("Dumy Experiment",Arrays.asList(new String[]{"condition 1","condition2"})));
                }
            }
        }
        transport.close();
        return zamboniRun;
    }


    private static void serializeRun(TZamboniRun zamboniRun,File fileToWrite) throws IOException,TException {
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

    public static TZamboniRun deserializeRun() throws IOException {
        /*
        try {
           writeRunFile();
       }
       catch(Exception e) {
           throw new RuntimeException("Failed to rewrite run file to local disk",e);
       }
       */
       InputStream inputStream = null;
        if (RUN_FILE.exists()) {
            inputStream = new BufferedInputStream(new FileInputStream(RUN_FILE));
        }
        else {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RUN_FILE.getName());
            // TODO: obtain the servlet context and use that instead
//            inputStream = servletContext.getResourceAsStream(RUN_FILE.getName());
        }
        if (inputStream == null) {
            throw new RuntimeException("Cannot access cached zamboni file from " + RUN_FILE.getName() +  ".  Were you expecting to connect to a live thrift service?");
        }
        TBinaryProtocol thriftReader = new TBinaryProtocol(new TIOStreamTransport(inputStream));
        TZamboniRun zamboniRun = new TZamboniRun();
        try {
            zamboniRun.read(thriftReader);
        } catch (TException e) {
            throw new RuntimeException("Error reading thrift file " + RUN_FILE, e);
        }
        inputStream.close();

        return zamboniRun;
    }

    private static void addCustomAmplicons(TZamboniRun run) {
        int laneCounter = 0;
        int libraryCounter = 0;
        for (TZamboniLane lane : run.getLanes()) {
            if (laneCounter % 3 == 0) {
                for (TZamboniLibrary library : lane.getLibraries()) {
                    if (libraryCounter % 3 == 0) {
                        for (int i = 0; i < laneCounter / 2 + 1; i++) {
                            String customAmplicon = "TestCAT-" + laneCounter + "-" + libraryCounter + "-" + i;
                            System.out.println(lane.getLaneNumber() + ": " + library.getLibrary() + ": " + customAmplicon);
                            library.getCustomAmpliconSetNames().add(customAmplicon);
                        }
                    }
                    libraryCounter++;
                }
            }
            laneCounter++;
        }
    }
}
