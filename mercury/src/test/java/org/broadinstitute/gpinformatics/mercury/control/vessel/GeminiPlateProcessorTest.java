package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Test(groups = TestGroups.DATABASE_FREE)
public class GeminiPlateProcessorTest {
    public static final String INITIAL_PICO = "GeminiInitialPico.xls";
    public static final String PLATING_PICO = "GeminiPlatingPico.xls";
    public static final String DUPLICATE_96_PICO = "Gemini96DuplicatePond.xls";
    public static final String NEXOME_PICO = "GeminiNexomePico.xls";
    public static final String NEXOME_4_PLATE_PICO = "GeminiNexome4Plate.xlsx";

    @Test
    public void testPlatingPicoGemini() {
        InputStream inputStream = VarioskanParserTest.getTestResource(PLATING_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Pair<GeminiPlateProcessor.GeminiRunInfo, List<GeminiPlateProcessor>> pair =
                    GeminiPlateProcessor.parse(workbook, PLATING_PICO);
            GeminiPlateProcessor.GeminiRunInfo left = pair.getLeft();
            List<GeminiPlateProcessor> right = pair.getRight();
            Assert.assertEquals(left.getRunName(), PLATING_PICO);
            Assert.assertEquals(right.size(), 1);
            Assert.assertEquals(right.iterator().next().getPlateWellResults().size(), 288);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInitialPico() {
        InputStream inputStream = VarioskanParserTest.getTestResource(INITIAL_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Pair<GeminiPlateProcessor.GeminiRunInfo, List<GeminiPlateProcessor>> parser =
                    GeminiPlateProcessor.parse(workbook, INITIAL_PICO);
            List<GeminiPlateProcessor> reads = parser.getRight();
            GeminiPlateProcessor.GeminiRunInfo runInfo = parser.getLeft();
            Assert.assertEquals(reads.size(), 1);
            for (GeminiPlateProcessor plateProcessor: reads) {
                Assert.assertEquals(plateProcessor.getMessages().size(), 0);
            }

            // One 'Read' but two pico barcodes
            Map<String, List<VarioskanPlateProcessor.PlateWellResult>> mapBarcodeToPlateWellResults =
                    reads.get(0).getPlateWellResults().stream()
                            .collect(Collectors.groupingBy(VarioskanPlateProcessor.PlateWellResult::getPlateBarcode));

            Assert.assertEquals(mapBarcodeToPlateWellResults.size(), 2);

            List<VarioskanPlateProcessor.PlateWellResult> firstPlateWellResults =
                    mapBarcodeToPlateWellResults.get("000003285220");
            Assert.assertEquals(firstPlateWellResults.size(), 96 * 3);

            List<VarioskanPlateProcessor.PlateWellResult> secondPlateWellResults =
                    mapBarcodeToPlateWellResults.get("000003285020");
            Assert.assertEquals(secondPlateWellResults.size(), 96 * 3);
            Assert.assertEquals(runInfo.getRunName(), INITIAL_PICO  );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDuplicatePico() {
        InputStream inputStream = VarioskanParserTest.getTestResource(DUPLICATE_96_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Pair<GeminiPlateProcessor.GeminiRunInfo, List<GeminiPlateProcessor>> pair =
                    GeminiPlateProcessor.parse(workbook, DUPLICATE_96_PICO);
            GeminiPlateProcessor.GeminiRunInfo left = pair.getLeft();
            Assert.assertEquals(left.getRunName(), DUPLICATE_96_PICO);
            List<GeminiPlateProcessor> right = pair.getRight();
            Assert.assertEquals(right.size(), 1);
            GeminiPlateProcessor plateProcessor = right.get(0);
            Assert.assertEquals(plateProcessor.getPlateWellResults().size(), 192);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNexomePico() {
        InputStream inputStream = VarioskanParserTest.getTestResource(NEXOME_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Pair<GeminiPlateProcessor.GeminiRunInfo, List<GeminiPlateProcessor>> pair =
                    GeminiPlateProcessor.parse(workbook, NEXOME_PICO);
            GeminiPlateProcessor.GeminiRunInfo left = pair.getLeft();
            Assert.assertEquals(left.getRunName(), NEXOME_PICO);
            List<GeminiPlateProcessor> right = pair.getRight();
            Assert.assertEquals(right.size(), 2);
            GeminiPlateProcessor plateProcessor = right.get(0);
            Assert.assertEquals(plateProcessor.getMessages().size(), 0);
            Assert.assertEquals(plateProcessor.getPlateWellResults().size(), 192);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNexome4PlatePico() {
        InputStream inputStream = VarioskanParserTest.getTestResource(NEXOME_4_PLATE_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Pair<GeminiPlateProcessor.GeminiRunInfo, List<GeminiPlateProcessor>> pair =
                    GeminiPlateProcessor.parse(workbook, NEXOME_4_PLATE_PICO);
            GeminiPlateProcessor.GeminiRunInfo left = pair.getLeft();
            Assert.assertEquals(left.getRunName(), NEXOME_4_PLATE_PICO);
            List<GeminiPlateProcessor> right = pair.getRight();
            Assert.assertEquals(right.size(), 4);
            GeminiPlateProcessor plateProcessor = right.get(0);
            Assert.assertEquals(plateProcessor.getMessages().size(), 0);
            Assert.assertEquals(plateProcessor.getPlateWellResults().size(), 192);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}