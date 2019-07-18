package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.bsp.client.location.LabLocation;
import org.broadinstitute.bsp.client.printing.LabelData;
import org.broadinstitute.bsp.client.printing.PaperType;
import org.broadinstitute.bsp.client.printing.PrintingManager;
import org.broadinstitute.bsp.client.printing.PrintingMessage;

/**
 * This class is a factory for PrintingMessage objects.  It is intended to facilitate building messages in test cases.
 */
public class PrintingMessageTestFactory {

    private PrintingManager printingManager = new PrintingManager();

    public PrintingMessage createPrintingMessage(String barcodeValue, String optionalValue, PaperType paperType,
                                                 LabLocation labLocation) throws Exception {

        return printingManager.createPrintingMessage(new LabelData(barcodeValue, optionalValue, null), paperType, labLocation);
    }
}
