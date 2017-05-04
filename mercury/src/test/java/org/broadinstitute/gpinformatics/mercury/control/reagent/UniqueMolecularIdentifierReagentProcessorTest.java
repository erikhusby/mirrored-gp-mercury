package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifierReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Test(groups = TestGroups.DATABASE_FREE)
public class UniqueMolecularIdentifierReagentProcessorTest {

    public static final String PLATE1_BARCODE = "000000012345";

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("UMIReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        UniqueMolecularIdentifierReagentProcessor umiProcessor =
                new UniqueMolecularIdentifierReagentProcessor("Sheet1");
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), umiProcessor);
            Assert.assertEquals(umiProcessor.getMessages().size(), 0);
            Assert.assertEquals(umiProcessor.getMapBarcodeToReagentDto().size(), 7);
            Assert.assertEquals(umiProcessor.getMapUmiToUmi().size(), 6);

            UniqueMolecularIdentifierReagentFactory factory = new UniqueMolecularIdentifierReagentFactory();
            MessageCollection messageCollection = new MessageCollection();

            Map<String, LabVessel> mapBarcodeToPlate = new HashMap<>();
            Map<String, UniqueMolecularIdentifierReagent> mapBarcodeToReagent = new HashMap<>();
            List<StaticPlate> staticPlates = factory.buildPlates(umiProcessor.getMapBarcodeToReagentDto(),
                    messageCollection, mapBarcodeToPlate, mapBarcodeToReagent);
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(staticPlates.size(), 7);

            StaticPlate staticPlate1 = staticPlates.get(0);
            Assert.assertEquals(staticPlate1.getLabel(), PLATE1_BARCODE);
            Assert.assertEquals(staticPlate1.getReagentContents().size(), 1);
            UniqueMolecularIdentifierReagent umi =
                    (UniqueMolecularIdentifierReagent) staticPlate1.getReagentContents().iterator().next();
            Assert.assertEquals(umi.getUmiLocation(), UniqueMolecularIdentifierReagent.UMILocation.INLINE_FIRST_READ);
            Assert.assertEquals(umi.getLength(), 6);
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}