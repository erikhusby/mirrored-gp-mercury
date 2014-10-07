package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ControlReagentProcessorTest {

    public static final String POSITIVE_CONTROL_ID = "NA12878";
    public static final String NEGATIVE_CONTROL_ID = "NO_TEMPLATE_CONTROL";
    public static final String TUBE1_BARCODE = "12345";
    public static final String TUBE2_BARCODE = "23456";
    public static final String TUBE3_BARCODE = "34567";

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("ControlReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        ControlReagentProcessor controlReagentProcessor = new ControlReagentProcessor("Sheet1");
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), controlReagentProcessor);
            Assert.assertEquals(controlReagentProcessor.getMessages().size(), 0);
            Assert.assertEquals(controlReagentProcessor.getMapControlToControl().size(), 2);
            Assert.assertEquals(controlReagentProcessor.getMapTubeBarcodeToControl().size(), 3);

            ControlReagentFactory controlReagentFactory = new ControlReagentFactory();
            MessageCollection messageCollection = new MessageCollection();
            List<Control> controls = new ArrayList<>();
            controls.add(new Control(POSITIVE_CONTROL_ID, Control.ControlType.POSITIVE));
            controls.add(new Control(NEGATIVE_CONTROL_ID, Control.ControlType.NEGATIVE));
            Map<String, BarcodedTube> mapBarcodeToTube = new HashMap<>();
            Map<String, ControlReagent> mapLotToControl = new HashMap<>();
            List<BarcodedTube> barcodedTubes = controlReagentFactory.buildTubes(
                    controlReagentProcessor.getMapTubeBarcodeToControl(), messageCollection, controls,
                    mapBarcodeToTube, mapLotToControl);

            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(barcodedTubes.size(), 3);

            BarcodedTube tube1 = barcodedTubes.get(0);
            Assert.assertEquals(tube1.getLabel(), TUBE1_BARCODE);
            Assert.assertEquals(tube1.getReagentContents().size(), 1);
            ControlReagent positiveControl = (ControlReagent) tube1.getReagentContents().iterator().next();
            Assert.assertEquals(positiveControl.getControl().getCollaboratorSampleId(), POSITIVE_CONTROL_ID);
            Assert.assertEquals(positiveControl.getLot(), "SK-1234");
            Assert.assertEquals(positiveControl.getExpiration(), new GregorianCalendar(2014, 11, 31).getTime());

            BarcodedTube tube2 = barcodedTubes.get(1);
            Assert.assertEquals(tube2.getLabel(), TUBE2_BARCODE);
            Assert.assertEquals(tube2.getReagentContents().size(), 1);
            Assert.assertEquals(tube1.getReagentContents().iterator().next(), positiveControl);

            BarcodedTube tube3 = barcodedTubes.get(2);
            Assert.assertEquals(tube3.getLabel(), TUBE3_BARCODE);
            Assert.assertEquals(tube2.getReagentContents().size(), 1);
            ControlReagent negativeControl = (ControlReagent) tube3.getReagentContents().iterator().next();
            Assert.assertEquals(negativeControl.getControl().getCollaboratorSampleId(), NEGATIVE_CONTROL_ID);
            Assert.assertEquals(negativeControl.getLot(), "SK-2345");

        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}