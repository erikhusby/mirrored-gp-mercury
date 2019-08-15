package org.broadinstitute.gpinformatics.mercury.boundary.printing;

import org.broadinstitute.bsp.client.location.LabLocation;
import org.broadinstitute.bsp.client.printing.BarcodeType;
import org.broadinstitute.bsp.client.printing.LabelDetail;
import org.broadinstitute.bsp.client.printing.PaperType;
import org.broadinstitute.bsp.client.printing.PrintingManager;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test process for triggering printing.
 */
@Test(groups = TestGroups.STANDARD)
public class PrintingMessageBeanTest extends Arquillian {

    @Inject
    PrintingManagerImpl printingManagerImpl;

    // Required to get the correct configuration for running JMS queue tests on the bamboo server.  In that case,
    // we can't use localhost.
    //
    // NOTE: To run locally, you must change this to DEV.  Make sure you change it back before checking in!
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(enabled = true)
    public void testCreatingAndSendingPrintingMessage() {
        PaperType testPaper = PaperType.SAMPLE_WRAP_AROUND;
        String testBarcode = "SM-1234";

        Assert.assertFalse(printingManagerImpl.getPrinterAndLabelDetailByLabLocationAndPaper().isEmpty(), "Unable to load printer settings.");

        // Get the expected paper type we'll use for the test and ensure it had the coded data in it.
        Map<PaperType, PrintingManager.PrinterLabelDetail> labelDetailPerPaperTypeInLabMap =
                printingManagerImpl.getPrinterAndLabelDetailByLabLocationAndPaper().get(LabLocation.LAB_1182);
        LabelDetail labelDetailByPaperType = labelDetailPerPaperTypeInLabMap.get(testPaper).getLabelDetail();
        Assert.assertTrue(labelDetailByPaperType.getZplScript().contains("#root[\"labelData\"]"));
        // Check to ensure the test barcode isn't magically in the script
        Assert.assertFalse(labelDetailByPaperType.getZplScript().contains(testBarcode));

        try {
            printingManagerImpl
                    .print(Collections.singletonList(testBarcode), PaperType.SAMPLE_WRAP_AROUND, BarcodeType.LINEAR,
                            LabLocation.LAB_1182);
        } catch (Exception exception) {
            Assert.fail("Unexpected failure when attempting to create a printer message.", exception);
        }
    }

   /**
     * Creates several printer message and adds them to the queue using the JMS API (local configuration).
     */
    @Test(enabled = true)
    public void testSendingMultiplePrintingMessages() {
        try {
            Assert.assertFalse(printingManagerImpl.getPrinterAndLabelDetailByLabLocationAndPaper().isEmpty(), "Unable to load printer settings.");

            printingManagerImpl.print(Arrays.asList("SM-223344", "SM-33223"), PaperType.SAMPLE_WRAP_AROUND, BarcodeType.LINEAR, LabLocation.LAB_1182);
            printingManagerImpl.print(Arrays.asList("CO-112233"), PaperType.CONTAINER, BarcodeType.LINEAR, LabLocation.LAB_1182);
        } catch (Exception exception) {
            Assert.fail("Unexpected exception when printing.", exception);
        }
    }
}
