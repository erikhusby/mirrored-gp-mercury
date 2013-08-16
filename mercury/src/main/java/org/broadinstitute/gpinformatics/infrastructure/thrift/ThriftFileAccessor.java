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
import java.util.Scanner;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * ThriftFileAccessor provides support for {@link MockThriftService} (which is actually more of a stub) to return a
 * reasonably fully-fledged TZamboniRun without actually talking to a Squid Thrift server. The TZamboniRun returned
 * comes from a serialized form stored in a .thrift file (not an official use of the file extension, which normally is
 * used for a thrift service definition). The result is a snapshot of an actual result from thrift. Over time, the
 * result has been tweaked to contain more varied data to represent specific conditions that are used to test code that
 * works with the thrift result.
 */
public class ThriftFileAccessor {

    private static final String RUN_NAME = "120320_SL-HBN_0159_AFCC0GHCACXX";

    private static final String RUN_FILE_NAME = RUN_NAME + ".thrift";

    public static final File RUN_FILE = new File("src/test/resources/thrift/", RUN_FILE_NAME);

    /**
     * Use this method to fetch a fresh thrift result and serialize it to a file.
     */
    private static void writeRunFile() throws TException, TZIMSException, IOException {
        ThriftConfig qaThrift = ThriftConfig.produce(DEV);
        TZamboniRun runFetchedFromService = fetchRun(qaThrift);
        serializeRun(runFetchedFromService, RUN_FILE);
    }

    /**
     * This main method allows one to make changes to the serialized thrift file so that it contains data appropriate
     * for testing different conditions. Typically, if you need to test some variation of data or a new field is added,
     * you can modify the serialized thrift file with appropriate data and then use {@link MockThriftService} in a test
     * to verify that the code handles that situation properly.
     */
    public static void main(String[] args) {
        System.out.println("Perform updates to serialized thrift run? (y/N)");
        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine();
        if (response.equalsIgnoreCase("y")) {
            try {
                TZamboniRun run = deserializeRun();

                // Make changes to the run here using a separate method. See the bottom of this file for examples.

                ThriftFileAccessor.serializeRun(run, ThriftFileAccessor.RUN_FILE);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static TZamboniRun fetchRun(ThriftConfig thriftConfig) throws TZIMSException, TException {

        TTransport transport = new TSocket(thriftConfig.getHost(), thriftConfig.getPort());
        TProtocol protocol = new TBinaryProtocol(transport);
        LIMQueries.Client client = new LIMQueries.Client(protocol);
        transport.open();

        TZamboniRun zamboniRun = client.fetchRun(RUN_NAME);

        // TODO: Hey Zim, couldn't this be done once and serialized back out to the file? I thought that's how we were handling things like this.
        for (TZamboniLane lane : zamboniRun.getLanes()) {
            for (TZamboniLibrary lib : lane.getLibraries()) {
                if (lane.getLaneNumber() % 2 == 0) {
                    // we're overwriting some values here to make a more representative data set
                    lib.setAggregate(true);
                    lib.setLabMeasuredInsertSize(14.3);
                    lib.setPrecircularizationDnaSize(19.2);
                    lib.setDevExperimentDataIsSet(true);
                    lib.setDevExperimentData(new TZDevExperimentData("Dumy Experiment",
                            Arrays.asList(new String[]{"condition 1", "condition2"})));
                }
            }
        }
        transport.close();
        return zamboniRun;
    }

    private static void serializeRun(TZamboniRun zamboniRun, File fileToWrite) throws IOException, TException {
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
        InputStream inputStream;
        if (RUN_FILE.exists()) {
            inputStream = new BufferedInputStream(new FileInputStream(RUN_FILE));
        }
        else {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RUN_FILE.getName());
        }
        if (inputStream == null) {
            throw new RuntimeException("Cannot access cached zamboni file from " + RUN_FILE.getName()
                                       + ".  Were you expecting to connect to a live thrift service?");
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

    /*
     * Various methods used by previous runs of main() to affect a change in the serialized thrift result.
     */

    private static void addCustomAmplicons(TZamboniRun run) {
        int laneCounter = 0;
        int libraryCounter = 0;
        for (TZamboniLane lane : run.getLanes()) {
            if (laneCounter % 3 == 0) {
                for (TZamboniLibrary library : lane.getLibraries()) {
                    if (libraryCounter % 3 == 0) {
                        for (int i = 0; i < laneCounter / 2 + 1; i++) {
                            String customAmplicon = "TestCAT-" + laneCounter + "-" + libraryCounter + "-" + i;
                            System.out.println(
                                    lane.getLaneNumber() + ": " + library.getLibrary() + ": " + customAmplicon);
                            library.getCustomAmpliconSetNames().add(customAmplicon);
                        }
                    }
                    libraryCounter++;
                }
            }
            laneCounter++;
        }
    }

    private static void addNewRunDetails(TZamboniRun run) {
        run.setActualReadStructure("76T8B76T");
        run.setImagedAreaPerLaneMM2(1.23);
        run.setSetupReadStructure("76T8B8B76T");
        run.setLanesSequenced("2");
        run.setRunFolder("/full/path/to/run/folder");
    }
}
