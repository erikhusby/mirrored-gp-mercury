package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Test(groups = TestGroups.DATABASE_FREE)
public class ControlReagentProcessorTest {

    public void testBasic() {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ControlReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        ControlReagentProcessor controlReagentProcessor = new ControlReagentProcessor("Sheet1");
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), controlReagentProcessor);
            Assert.assertEquals(controlReagentProcessor.getMessages().size(), 0);
            Assert.assertEquals(controlReagentProcessor.getMapControlToControl().size(), 2);
            Assert.assertEquals(controlReagentProcessor.getMapTubeBarcodeToControl().size(), 3);
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}