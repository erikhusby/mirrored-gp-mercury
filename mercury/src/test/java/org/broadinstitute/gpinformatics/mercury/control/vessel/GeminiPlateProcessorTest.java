package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class GeminiPlateProcessorTest {
    public static final String INITIAL_PICO = "GeminiInitialPico.xls";
    public static final String PLATING_PICO = "GeminiPlatingPico.xls";

    @Test
    public void testPlatingPicoGemini() {
        InputStream inputStream = VarioskanParserTest.getTestResource(PLATING_PICO);
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            GeminiPlateProcessor.parse(workbook, PLATING_PICO);
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
            Assert.assertEquals(reads.size(), 2);
            for (GeminiPlateProcessor plateProcessor: reads) {
                Assert.assertEquals(plateProcessor.getMessages().size(), 0);
            }
            GeminiPlateProcessor firstPlate = reads.iterator().next();
            Assert.assertEquals(firstPlate.getPlateWellResults().size(), 96 * 3);
            Assert.assertEquals(runInfo.getRunName(), INITIAL_PICO  );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}