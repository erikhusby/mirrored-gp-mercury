package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Test parsing of Varioskan file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class VarioskanParserTest {

    public static final String PLATE1_BARCODE = "000001234567";
    public static final String PLATE1_BARCODE_IN_SS = "1234567";
    public static final String PLATE2_BARCODE = "000002345678";
    public static final String PLATE2_BARCODE_IN_SS = "2345678";

    @Test
    public void testBasic() {
        InputStream testSpreadSheetInputStream = getSpreadsheet();
        try {
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheet(
                            VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);
            Assert.assertEquals(varioskanPlateProcessor.getPlateWellResults().size(), 192);
            Assert.assertEquals(varioskanPlateProcessor.getMessages().size(), 0);
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                testSpreadSheetInputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testCreateRun() {
        try {
            VesselEjb vesselEjb = new VesselEjb();
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            // get R2, just use raw POI API?
            Workbook workbook = WorkbookFactory.create(getSpreadsheet());
            parser.processRows(workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB), varioskanPlateProcessor);
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();

            Map<VesselPosition, BarcodedTube> mapPositionToTube = buildTubesAndTransfers(mapBarcodeToPlate,
                    PLATE1_BARCODE, PLATE2_BARCODE, "");

            VarioskanRowParser varioskanRowParser = new VarioskanRowParser(workbook);
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue = varioskanRowParser.getValues();
            MessageCollection messageCollection = new MessageCollection();
            LabMetricRun labMetricRun = vesselEjb.createVarioskanRunDaoFree(mapNameValueToValue,
                    LabMetric.MetricType.INITIAL_PICO, varioskanPlateProcessor, mapBarcodeToPlate, 101L,
                    messageCollection).getLeft();
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(labMetricRun.getLabMetrics().size(), 3 * 96);
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A01).getMetrics().iterator().next().getValue(),
                    new BigDecimal("3.34"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A02).getMetrics().iterator().next().getValue(),
                    new BigDecimal("0.87"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A03).getMetrics().iterator().next().getValue(),
                    new BigDecimal("1.37"));
        } catch (IOException | InvalidFormatException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<VesselPosition, BarcodedTube> buildTubesAndTransfers(Map<String, StaticPlate> mapBarcodeToPlate,
            String plate1Barcode, String plate2Barcode, String tubePrefix) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        StaticPlate staticPlate1 = new StaticPlate(plate1Barcode, StaticPlate.PlateType.Eppendorf96);
        mapBarcodeToPlate.put(staticPlate1.getLabel(), staticPlate1);
        StaticPlate staticPlate2 = new StaticPlate(plate2Barcode, StaticPlate.PlateType.Eppendorf96);
        mapBarcodeToPlate.put(staticPlate2.getLabel(), staticPlate2);

        LabEvent labEvent1 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.ALL96, null, labEvent1));
        LabEvent labEvent2 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 2L, 101L,
                "Bravo");
        labEvent2.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate2.getContainerRole(), SBSSection.ALL96, null, labEvent2));
        return mapPositionToTube;
    }

    public static InputStream getSpreadsheet() {
        InputStream testSpreadSheetInputStream = getTestResource("VarioskanOutput.xls");
        Assert.assertNotNull(testSpreadSheetInputStream);
        return testSpreadSheetInputStream;
    }

    public static InputStream getTestResource(String fileName) {
        InputStream testSpreadSheetInputStream = getResourceAsStream(fileName);
        if (testSpreadSheetInputStream == null) {
            testSpreadSheetInputStream = getResourceAsStream("testdata/" + fileName);
        }
        return testSpreadSheetInputStream;
    }

    public static InputStream getResourceAsStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }
}
