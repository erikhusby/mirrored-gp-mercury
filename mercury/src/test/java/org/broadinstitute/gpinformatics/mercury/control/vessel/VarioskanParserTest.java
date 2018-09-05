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
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test parsing of Varioskan file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class VarioskanParserTest {

    public static final String VARIOSKAN_OUTPUT = "VarioskanOutput.xls";
    public static final String VARIOSKAN_384_OUTPUT = "Varioskan384Output.xls";
    public static final String VARIOSKAN_384_2CURVE_OUTPUT = "Varioskan384MultiCurveOutput.xls";
    public static final String VARIOSKAN_RIBO_OUTPUT = "VarioskanRiboOutput.xls";
    public static final int VARIOSKAN_SAMPLE_COUNT = 96;
    public static final int VARIOSKAN_384_SAMPLE_COUNT = 96;
    public static final int VARIOSKAN_RIBO_SAMPLE_COUNT = 95;
    public static final String PLATE1_BARCODE = "000001234567";
    public static final String PLATE1_BARCODE_IN_SS = "1234567";
    public static final String PLATE2_BARCODE = "000002345678";
    public static final String PLATE2_BARCODE_IN_SS = "2345678";
    public static final String RIBO_PLATE_BARCODE = "000111222333";
    public static final String RIBO_PLATE_BARCODE_IN_SS = "111222333";

    @Test
    public void testMultipleCurves() {
        InputStream testSpreadSheetInputStream = getSpreadsheet(VARIOSKAN_384_2CURVE_OUTPUT);
        try {
            int numSamples = 10;
            Workbook workbook = WorkbookFactory.create(testSpreadSheetInputStream);
            VarioskanPlateProcessorTwoCurve plateProcessorTwoCurve = new VarioskanPlateProcessorTwoCurve(workbook);
            Map<String, StaticPlate.TubeFormationByWellCriteria.Result> mapBarcodeToResult = new HashMap<>();

            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            VarioskanParserTest.buildTriplicateTubesAndTransfers(mapBarcodeToPlate, "Dil1", "000002108020", "Abs");
            VarioskanParserTest.buildTriplicateTubesAndTransfers(mapBarcodeToPlate, "Dil2", "000002107920", "Abs");
            for (StaticPlate staticPlate: mapBarcodeToPlate.values()) {
                mapBarcodeToResult.put(staticPlate.getLabel(), staticPlate.nearestFormationAndTubePositionByWell());
            }

            List<VarioskanPlateProcessor.PlateWellResult> plateWellResults =
                    plateProcessorTwoCurve.processMultipleCurves(LabMetric.MetricType.INITIAL_PICO);

            Assert.assertEquals(plateWellResults.size(), numSamples * 3);
            Assert.assertEquals(plateProcessorTwoCurve.getMessages().size(), 0);
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
    public void testDuplicatePicoSpreadsheet() {
        InputStream testSpreadSheetInputStream = getSpreadsheet(VARIOSKAN_OUTPUT);
        try {
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, LabMetric.MetricType.INITIAL_PICO);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheet(
                            VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);
            // Spreadsheet has quants in two 96 well microfluor plates.
            Assert.assertEquals(varioskanPlateProcessor.getPlateWellResults().size(), VARIOSKAN_SAMPLE_COUNT * 2);
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
    public void testTriplicatePicoSpreadsheet() {
        InputStream testSpreadSheetInputStream = getSpreadsheet(VARIOSKAN_384_OUTPUT);
        try {
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, LabMetric.MetricType.INITIAL_PICO);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheet(
                            VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);
            // Spreadsheet has quants in three sections of one 384 well microfluor plate.
            Assert.assertEquals(varioskanPlateProcessor.getPlateWellResults().size(), VARIOSKAN_384_SAMPLE_COUNT * 3);
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
    public void testDuplicateRibo384Spreadsheet() {
        InputStream testSpreadSheetInputStream = getSpreadsheet(VARIOSKAN_RIBO_OUTPUT);
        try {
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, LabMetric.MetricType.INITIAL_PICO);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheet(
                            VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);
            // Spreadsheet has quants in two sections of one 384 well microfluor plate.
            Assert.assertEquals(varioskanPlateProcessor.getPlateWellResults().size(), VARIOSKAN_RIBO_SAMPLE_COUNT * 2);
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
    public void testCreatePicoRun() throws ParseException {
        try {
            VesselEjb vesselEjb = new VesselEjb();
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, LabMetric.MetricType.INITIAL_PICO);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            // get R2, just use raw POI API?
            Workbook workbook = WorkbookFactory.create(getSpreadsheet(VARIOSKAN_OUTPUT));
            parser.processRows(workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB), varioskanPlateProcessor);
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();

            Map<VesselPosition, BarcodedTube> mapPositionToTube = buildPicoTubesAndTransfers(mapBarcodeToPlate,
                    PLATE1_BARCODE, PLATE2_BARCODE, "");

            VarioskanRowParser varioskanRowParser = new VarioskanRowParser(workbook);
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue = varioskanRowParser.getValues();
            MessageCollection messageCollection = new MessageCollection();
            StaticPlate.TubeFormationByWellCriteria.Result result =
                    mapBarcodeToPlate.values().iterator().next().nearestFormationAndTubePositionByWell();
            Map<String, StaticPlate.TubeFormationByWellCriteria.Result> mapBarcodeToResult = new HashMap<>();
            for (Map.Entry<String, StaticPlate> entry: mapBarcodeToPlate.entrySet()) {
                mapBarcodeToResult.put(entry.getValue().getLabel(), result);
            }

            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
            Date runStarted = simpleDateFormat.parse(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_STARTED));
            LabMetricRun run = new LabMetricRun(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME),
                    runStarted, LabMetric.MetricType.INITIAL_PICO);
            String r2 = mapNameValueToValue.get(VarioskanRowParser.NameValue.CORRELATION_COEFFICIENT_R2);
            run.getMetadata().add(new Metadata(Metadata.Key.CORRELATION_COEFFICIENT_R2, r2));
            boolean runFailed = false;
            if (new BigDecimal(r2).compareTo(new BigDecimal("0.97")) == -1) {
                runFailed = true;
            }
            run.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                    mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_NAME)));
            run.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_SERIAL_NUMBER,
                    mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_SERIAL_NUMBER)));

            LabMetricRun labMetricRun = vesselEjb.createVarioskanRunDaoFree(run, runStarted,
                    LabMetric.MetricType.INITIAL_PICO, varioskanPlateProcessor.getPlateWellResults(), mapBarcodeToPlate,
                    101L, messageCollection, mapBarcodeToResult, runFailed);
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertFalse(result.getWellToTubePosition().isEmpty());
            Assert.assertTrue(result.getLabEventMetadata().isEmpty());
            Assert.assertEquals(labMetricRun.getLabMetrics().size(), 3 * VARIOSKAN_SAMPLE_COUNT);
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A01).getMetrics().iterator().next().getValue(),
                    new BigDecimal("3.34"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A02).getMetrics().iterator().next().getValue(),
                    new BigDecimal("0.87"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A03).getMetrics().iterator().next().getValue(),
                    new BigDecimal("1.36"));
        } catch (IOException | InvalidFormatException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<VesselPosition, BarcodedTube> buildPicoTubesAndTransfers(
            Map<String, StaticPlate> mapBarcodeToPlate, String plate1Barcode, String plate2Barcode, String tubePrefix) {
        return buildPicoTubesAndTransfers(96, mapBarcodeToPlate, plate1Barcode, plate2Barcode, tubePrefix);
    }

    public static Map<VesselPosition, BarcodedTube> buildPicoTubesAndTransfers(int numSamples,
            Map<String, StaticPlate> mapBarcodeToPlate, String plate1Barcode, String plate2Barcode, String tubePrefix) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        int counter = 0;
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
            counter++;
            if (counter >= numSamples)
                break;
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        Assert.assertEquals(tubeFormation.getContainerRole().getContainedVessels().size(), numSamples);

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

    @Test
    public void testCreateRiboRun() throws ParseException {
        try {
            VesselEjb vesselEjb = new VesselEjb();
            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, LabMetric.MetricType.PLATING_RIBO);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());

            Workbook workbook = WorkbookFactory.create(getTestResource("VarioskanRiboOutput.xls"));
            parser.processRows(workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB), varioskanPlateProcessor);
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();

            Map<VesselPosition, BarcodedTube> mapPositionToTube = buildRiboTubesAndTransfers(mapBarcodeToPlate,
                    RIBO_PLATE_BARCODE, "");

            VarioskanRowParser varioskanRowParser = new VarioskanRowParser(workbook);
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue = varioskanRowParser.getValues();
            MessageCollection messageCollection = new MessageCollection();
            StaticPlate.TubeFormationByWellCriteria.Result result =
                    mapBarcodeToPlate.values().iterator().next().nearestFormationAndTubePositionByWell();
            Map<String, StaticPlate.TubeFormationByWellCriteria.Result> mapBarcodeToResult = new HashMap<>();
            mapBarcodeToResult.put(mapBarcodeToPlate.values().iterator().next().getLabel(), result);

            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
            Date runStarted = simpleDateFormat.parse(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_STARTED));
            LabMetricRun run = new LabMetricRun(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME),
                    runStarted, LabMetric.MetricType.INITIAL_PICO);
            String r2 = mapNameValueToValue.get(VarioskanRowParser.NameValue.CORRELATION_COEFFICIENT_R2);
            run.getMetadata().add(new Metadata(Metadata.Key.CORRELATION_COEFFICIENT_R2, r2));
            run.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                    mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_NAME)));
            run.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_SERIAL_NUMBER,
                    mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_SERIAL_NUMBER)));
            boolean runFailed = false;
            if (new BigDecimal(r2).compareTo(new BigDecimal("0.97")) == -1) {
                runFailed = true;
            }
            LabMetricRun labMetricRun = vesselEjb.createVarioskanRunDaoFree(run, runStarted,
                    LabMetric.MetricType.PLATING_RIBO, varioskanPlateProcessor.getPlateWellResults(), mapBarcodeToPlate,
                    101L, messageCollection, mapBarcodeToResult, runFailed);
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertFalse(result.getWellToTubePosition().isEmpty());
            Assert.assertTrue(result.getLabEventMetadata().isEmpty());
            Assert.assertEquals(labMetricRun.getLabMetrics().size(), 3 * VARIOSKAN_RIBO_SAMPLE_COUNT);
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A02).getMetrics().iterator().next().getValue(),
                    new BigDecimal("3.38"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A03).getMetrics().iterator().next().getValue(),
                    new BigDecimal("3.28"));
            Assert.assertEquals(mapPositionToTube.get(VesselPosition.A04).getMetrics().iterator().next().getValue(),
                    new BigDecimal("3.11"));
        } catch (IOException | InvalidFormatException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<VesselPosition, BarcodedTube> buildRiboTubesAndTransfers(
            Map<String, StaticPlate> mapBarcodeToPlate, String plate1Barcode, String tubePrefix) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        Assert.assertEquals(tubeFormation.getContainerRole().getContainedVessels().size(), 96);

        StaticPlate staticPlate1 = new StaticPlate(plate1Barcode, StaticPlate.PlateType.Eppendorf384);
        mapBarcodeToPlate.put(staticPlate1.getLabel(), staticPlate1);

        LabEvent labEvent1 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A2, null, labEvent1));
        LabEvent labEvent2 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 2L, 101L,
                "Bravo");
        labEvent2.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_B1, null, labEvent2));
        return mapPositionToTube;
    }

    public static Map<VesselPosition, BarcodedTube> buildTriplicateNoDilutionTubesAndTransfers(
            Map<String, StaticPlate> mapBarcodeToPlate, String plateBarcode, String tubePrefix) {

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        Assert.assertEquals(tubeFormation.getContainerRole().getContainedVessels().size(), 96);

        StaticPlate microfluorPlate = new StaticPlate(plateBarcode, StaticPlate.PlateType.Eppendorf384);
        mapBarcodeToPlate.put(microfluorPlate.getLabel(), microfluorPlate);

        LabEvent labEvent1 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, microfluorPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A1, null, labEvent1));
        LabEvent labEvent2 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 2L, 101L,
                "Bravo");
        labEvent2.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, microfluorPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A2, null, labEvent2));
        LabEvent labEvent3 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 3L, 101L,
                "Bravo");
        labEvent3.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, microfluorPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_B1, null, labEvent3));
        return mapPositionToTube;
    }

    public static Map<VesselPosition, BarcodedTube> buildTriplicateTubesAndTransfers(
            Map<String, StaticPlate> mapBarcodeToPlate, String dilutionBarcode, String microfluorBarcode,
            String tubePrefix) {

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        Assert.assertEquals(tubeFormation.getContainerRole().getContainedVessels().size(), 96);

        StaticPlate dilutionPlate = new StaticPlate(dilutionBarcode, StaticPlate.PlateType.Eppendorf384);
        mapBarcodeToPlate.put(dilutionPlate.getLabel(), dilutionPlate);

        LabEvent labEvent1 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, dilutionPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A1, null, labEvent1));
        LabEvent labEvent2 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 2L, 101L,
                "Bravo");
        labEvent2.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, dilutionPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_A2, null, labEvent2));
        LabEvent labEvent3 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 3L, 101L,
                "Bravo");
        labEvent3.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, dilutionPlate.getContainerRole(), SBSSection.P384_96TIP_1INTERVAL_B1, null, labEvent3));

        StaticPlate microfluorPlate = new StaticPlate(microfluorBarcode, StaticPlate.PlateType.Eppendorf384);
        mapBarcodeToPlate.put(microfluorPlate.getLabel(), microfluorPlate);

        LabEvent labEvent4 = new LabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER, new Date(), "BATMAN", 4L, 101L,
                "Bravo");
        labEvent4.getSectionTransfers().add(new SectionTransfer(dilutionPlate.getContainerRole(), SBSSection.ALL384,
                null, microfluorPlate.getContainerRole(), SBSSection.ALL384, null, labEvent4));

        return mapPositionToTube;
    }

    public static InputStream getSpreadsheet(String filename) {
        InputStream testSpreadSheetInputStream = getTestResource(filename);
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
