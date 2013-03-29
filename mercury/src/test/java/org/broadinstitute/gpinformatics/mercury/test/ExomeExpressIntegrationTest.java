package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.*;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleImportBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptBean;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

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
import java.util.List;
import java.util.Scanner;

/**
 * Implementation of GPLIM-1070.  This test is run from its main method.  Before sending each group of messages, the
 * test it waits for the user to hit Enter, allowing the user to interact with the Mercury UI between messages.
 *
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
            List<String> sampleIds = new ArrayList<String>();
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                if (!line.trim().isEmpty()) {
                    sampleIds.add(line);
                }
            }

            Scanner scanner = new Scanner(System.in);
            System.out.println("Using samples");
            for (String sampleId : sampleIds) {
                System.out.println(sampleId);
            }

            System.out.println("Press enter to send receipt message");
            scanner.nextLine();
            // Send receipt message
            List<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
            List<String> tubeBarcodes = new ArrayList<String>();
            int j = 1;
            for (String sampleId : sampleIds) {
                String manufacturerBarcode = "0" + testSuffix + j;
                tubeBarcodes.add(manufacturerBarcode);
                parentVesselBeans.add(new ParentVesselBean(manufacturerBarcode, sampleId, "Matrix Tube [0.75mL]", null));
                j++;
            }
            SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-" + testSuffix,
                    parentVesselBeans, "jowalsh");
            WebResource resource = Client.create().resource(baseUrl.toExternalForm() + "/rest/samplereceipt");
            resource.type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(sampleReceiptBean)
                    .post(String.class);

            // User creates PDO in UI.
            // User checks bucket.
            // User creates LCSET from bucket.

            System.out.println("Press enter to send dilution and plating");
            scanner.nextLine();
            // dilution.
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
            String dilutionTargetRackBarcode = "DilutionTarget" + testSuffix;
            List<String> dilutionTargetTubeBarcodes = new ArrayList<String>();
            List<String> platingTargetTubeBarcodes = new ArrayList<String>();
            for (int i = 0; i < tubeBarcodes.size(); i++) {
                dilutionTargetTubeBarcodes.add(testSuffix + i);
            }
            PlateTransferEventType dilutionTransferEvent = bettaLimsMessageTestFactory.buildRackToRack(
                    LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName(), "DilutionSource" + testSuffix, tubeBarcodes,
                    dilutionTargetRackBarcode, dilutionTargetTubeBarcodes);
            BettaLIMSMessage dilutionTransferMessage = new BettaLIMSMessage();
            dilutionTransferMessage.getPlateTransferEvent().add(dilutionTransferEvent);
            sendMessage(baseUrl, dilutionTransferMessage);
            bettaLimsMessageTestFactory.advanceTime();

            // plating aliquot.
            for (int i = 0; i < tubeBarcodes.size(); i++) {
                platingTargetTubeBarcodes.add("1" + testSuffix + i);
            }
            PlateTransferEventType platingTransfer = bettaLimsMessageTestFactory.buildRackToRack(
                    LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName(), dilutionTargetRackBarcode,
                    dilutionTargetTubeBarcodes, "PlatingTarget" + testSuffix, platingTargetTubeBarcodes);
            BettaLIMSMessage platingTransferMessage = new BettaLIMSMessage();
            platingTransferMessage.getPlateTransferEvent().add(platingTransfer);
            sendMessage(baseUrl, platingTransferMessage);
            bettaLimsMessageTestFactory.advanceTime();

            // User checks chain of custody.
            // User checks LCSET LIMS Activity Stream.

            System.out.println("Press enter to send export");
            scanner.nextLine();
            // export message.
            // Reconstruct the factory, to update the time.
            bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
            parentVesselBeans = new ArrayList<ParentVesselBean>();
            ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<ChildVesselBean>();
            // Need a 4 character base 36 ID.
            @SuppressWarnings("NumericCastThatLosesPrecision")
            String partialSampleId = Integer.toString((int) (System.currentTimeMillis() % 1600000L), 36).toUpperCase();
            for (int i = 0; i < tubeBarcodes.size(); i++) {
                childVesselBeans.add(new ChildVesselBean(platingTargetTubeBarcodes.get(i), "SM-" + partialSampleId + (i + 1),
                        "tube", bettaLimsMessageTestFactory.buildWellName(i + 1)));
            }
            String exportRackBarcode = "EX-" + testSuffix;
            parentVesselBeans.add(new ParentVesselBean(exportRackBarcode, null, "Rack", childVesselBeans));
            SampleImportBean sampleImportBean = new SampleImportBean("BSP", "EX-" + testSuffix, new Date(),
                    parentVesselBeans, "jowalsh");
            resource = Client.create().resource(baseUrl.toExternalForm() + "/rest/sampleimport");
            resource.type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(sampleImportBean)
                    .post(String.class);

            // User checks chain of custody, activity stream.

            System.out.println("About to send LC messages.  Press y to skip end repair.  To include everything, just hit enter.");
            String line = scanner.nextLine();
            boolean shouldSkipEndRepair = line.contains("y");
            // LC messages.
            // Reconstruct the factory, to update the time.
            bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
            LabEventTest.ShearingJaxbBuilder shearingJaxbBuilder = new LabEventTest.ShearingJaxbBuilder(
                    bettaLimsMessageTestFactory,
                    platingTargetTubeBarcodes, testSuffix, exportRackBarcode).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            LabEventTest.LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LabEventTest.LibraryConstructionJaxbBuilder(
                    bettaLimsMessageTestFactory, testSuffix, shearingJaxbBuilder.getShearCleanPlateBarcode(), "000002453323",
                    sampleIds.size()).invoke();

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
                }
                else {
                    System.out.println("Skipped end repair.");
                }
            }

            LabEventTest.HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new LabEventTest.HybridSelectionJaxbBuilder(
                    bettaLimsMessageTestFactory, testSuffix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                    libraryConstructionJaxbBuilder.getPondRegTubeBarcodes(), "0102692378").invoke();
            for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }
            LabEventTest.QtpJaxbBuilder qtpJaxbBuilder = new LabEventTest.QtpJaxbBuilder(bettaLimsMessageTestFactory, testSuffix,
                    Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                    Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                    WorkflowName.EXOME_EXPRESS).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
                sendMessage(baseUrl, bettaLIMSMessage);
            }

            System.out.println("Transfer from denature tube: " + qtpJaxbBuilder.getDenatureTubeBarcode());
            System.out.print("Enter flowcell barcode: ");
            String flowcellBarcode = scanner.nextLine();

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
            System.out.println("URL to preview the run will be " + baseUrl.toExternalForm() + "/rest/IlluminaRun/queryMercury?runName=" + runFile.getName());
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
     * @param args path to file that was output by BSP CreateKitTest.createKit.
     */
    public static void main(String[] args) {
        new ExomeExpressIntegrationTest().testAll(args[0]);
    }
}
