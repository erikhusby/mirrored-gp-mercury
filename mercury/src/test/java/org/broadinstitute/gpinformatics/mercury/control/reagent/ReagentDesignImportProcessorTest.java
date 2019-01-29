package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReagentDesignImportProcessorTest {

    private static String IMPORT_CSV = "Probe Tube CSV.csv";

    public void testBasic() {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(IMPORT_CSV);
        try {
            ReagentDesignImportProcessor processor = new ReagentDesignImportProcessor();
            MessageCollection messageCollection = new MessageCollection();
            List<ReagentDesignImportProcessor.ReagentImportDto> dtos = processor.parse(testSpreadSheet,
                    messageCollection);
            assertEquals(messageCollection.getErrors().size(), 0);
            assertEquals(dtos.size(), 1);
        } finally {
            try {
                testSpreadSheet.close();
            } catch (IOException ignored) {
            }
        }
    }
}