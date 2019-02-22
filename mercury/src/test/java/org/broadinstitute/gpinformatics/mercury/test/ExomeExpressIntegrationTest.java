package org.broadinstitute.gpinformatics.mercury.test;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingJaxbBuilder;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Implementation of GPLIM-1070.  This test is run from its main method.  Before sending each group of messages, the
 * test it waits for the user to hit Enter, allowing the user to interact with the Mercury UI between messages.
 * <p/>
 * If you are hitting a local webservice, You will need to have directory named
 * /seq/lims/mercury/dev/samplereceipt/inbox which is readable-writable
 */
@SuppressWarnings({"OverlyCoupledMethod", "UseOfSystemOutOrSystemErr"})
public class ExomeExpressIntegrationTest {

    // Make barcodes unique, so the test can be run multiple times.
    private final SimpleDateFormat testSuffixDateFormat = new SimpleDateFormat("MMddHHmmss");

    @SuppressWarnings("FeatureEnvy")
    public void testAll(String sampleFileName) {
        try {
            URL baseUrl = new URL("https", "localhost", 8443, "/Mercury");
            String testSuffix = testSuffixDateFormat.format(new Date());

            // load reagents with ImportFromSquidTest.

            // get list of samples and tube barcodes.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(sampleFileName));
            Map<String, String> sampleBarcodeMap = new HashMap<>();
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                if (!line.trim().isEmpty()) {
                    final String[] lineArray = StringUtils.split(line);
                    assert (lineArray.length == 2);
                    sampleBarcodeMap.put(lineArray[0], lineArray[1]);
                }
            }

            Scanner scanner = new Scanner(System.in);
            System.out.println("Using samples");
            for (String sampleName : sampleBarcodeMap.keySet()) {
                System.out.println(sampleName + "\t" + sampleBarcodeMap.get(sampleName));
            }

            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

            System.out.print("Enter export rack barcode: ");
            String exportRackBarcode = scanner.nextLine();

            System.out.println(
                    "About to send LC messages.  Press y to skip end repair.  To include everything, just hit enter.");
            String line = scanner.nextLine();
            boolean shouldSkipEndRepair = line.contains("y");
            // LC messages.
            // Reconstruct the factory, to update the time.
            bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
            ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(
                    bettaLimsMessageTestFactory,
                    new ArrayList<>(sampleBarcodeMap.values()),
                    testSuffix, exportRackBarcode).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                    bettaLimsMessageTestFactory, testSuffix, shearingJaxbBuilder.getShearCleanPlateBarcode(),
                    "000002453323", null, sampleBarcodeMap.size(),
                    LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY,
                    Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                    Arrays.asList(Triple.of("PEG", "0009753352", 2), Triple.of("70% Ethanol", "LCEtohTest", 3),
                            Triple.of("EB", "0009753452", 4), Triple.of("SPRI", "LCSpriTest", 5)),
                    Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6)),
                    LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();

            for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
                boolean willSkipEndRepair = false;
                if (shouldSkipEndRepair) {
                    for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
                        if ("EndRepair".equals(plateEventType.getEventType())) {
                            willSkipEndRepair = true;
                        }
                    }
                }
                if (!willSkipEndRepair) {
                    sendMessage(baseUrl, bettaLIMSMessage);
                } else {
                    System.out.println("Skipped end repair.");
                }
            }

            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(
                    bettaLimsMessageTestFactory, testSuffix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                    libraryConstructionJaxbBuilder.getPondRegTubeBarcodes(), "0102692378").invoke();
            for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, testSuffix,
                    Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                    Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                    true, QtpJaxbBuilder.PcrType.VIIA_7);
            qtpJaxbBuilder.invokeToQuant();
            qtpJaxbBuilder.invokePostQuant();
            for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }

            System.out.println("About to perform denature to Flowcell Transfer.  Press 'y' if running in Parallel?");
            String parallelIndicator = scanner.nextLine();
            boolean parallelFlag = parallelIndicator.contains("y");

            String fctName = "";
            if (parallelFlag) {
                System.out.println("Enter the FCT Ticket Name");
                fctName = scanner.nextLine();
            }

            HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageTestFactory,
                    testSuffix, Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                    qtpJaxbBuilder.getDenatureRackBarcode(), fctName, ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                    BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2);

            hiSeq2500JaxbBuilder.invoke();

            for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }

            System.out.println("Transfer from denature tube: " + qtpJaxbBuilder.getDenatureTubeBarcode());
            String flowcellBarcode = hiSeq2500JaxbBuilder.getFlowcellBarcode();
            System.out.println("Using flowcell: " + flowcellBarcode);

            // User checks chain of custody, activity stream.
            // User does denature to flowcell transfer in UI.
            // User checks chain of custody, activity stream.

            System.out.println("Press enter to register run");
            scanner.nextLine();
            // Run registration web service call.
            File runFile = File.createTempFile("RunDir", ".txt");
            String runFilePath = runFile.getAbsolutePath();
            String runName = "Run" + testSuffix;
            SolexaRunBean solexaRunBean = new SolexaRunBean(flowcellBarcode, runName, new Date(), "SL-HAL",
                    runFilePath, null);
            System.out.println("Registering run " + runName + " with path " + runFilePath);
            System.out.println("URL to preview the run will be " + baseUrl.toExternalForm()
                               + "/rest/IlluminaRun/queryMercury?runName=" + runFile.getName());
            ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            JerseyUtils.acceptAllServerCertificates(clientBuilder);

            clientBuilder.build().target(baseUrl.toExternalForm() + "/rest/solexarun")
                    .request(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .post(Entity.xml(solexaRunBean), String.class);

            // User checks chain of custody, activity stream.
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(URL baseUrl, BettaLIMSMessage bean) {
        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        ClientBuilder.newClient(clientConfig).target(baseUrl + "/rest/bettalimsmessage")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(bean), String.class);

    }

    /**
     * Runs the test.
     *
     * @param args path to file that was output by BSP CreateKitTest.createKit.
     *             The file is in the form: sample_name\tbarcode
     */
    public static void main(String[] args) {
        new ExomeExpressIntegrationTest().testAll(args[0]);
    }
}
