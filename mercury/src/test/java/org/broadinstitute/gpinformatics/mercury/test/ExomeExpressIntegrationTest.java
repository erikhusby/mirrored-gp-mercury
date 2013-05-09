package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingJaxbBuilder;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            URL baseUrl = new URL("http", "localhost", 8080, "/Mercury");
            String testSuffix = testSuffixDateFormat.format(new Date());

            // load reagents with ImportFromSquidTest.

            // get list of samples and tube barcodes.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(sampleFileName));
            Map<String, String> sampleBarcodeMap = new HashMap<String, String>();
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
                    new ArrayList<String>(sampleBarcodeMap.values()),
                    testSuffix, exportRackBarcode).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                    bettaLimsMessageTestFactory, testSuffix, shearingJaxbBuilder.getShearCleanPlateBarcode(),
                    "000002453323",
                    sampleBarcodeMap.size()).invoke();

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
                    WorkflowName.EXOME_EXPRESS).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder =
                    new HiSeq2500JaxbBuilder(bettaLimsMessageTestFactory, testSuffix,
                            qtpJaxbBuilder.getDenatureTubeBarcode()).invoke();
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
            Client.create().resource(baseUrl.toExternalForm() + "/rest/solexarun")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(solexaRunBean)
                    .post(String.class);

            // User checks chain of custody, activity stream.
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(URL baseUrl, BettaLIMSMessage bean) {
        Client.create().resource(baseUrl + "/rest/bettalimsmessage")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(bean)
                .post(String.class);

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
