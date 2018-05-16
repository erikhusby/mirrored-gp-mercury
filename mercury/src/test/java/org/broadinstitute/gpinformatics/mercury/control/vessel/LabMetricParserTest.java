package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class LabMetricParserTest extends StubbyContainerTest {

    public LabMetricParserTest(){}

    @Inject
    private BarcodedTubeDao vesselDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserTransaction utx;

    // This following header, position, barcode, and quant data matches what is in GOOD_QUANT_UPLOAD_FILE,
    // except for 'Quantities' which is an INCORRECT header which we test for below.
    private final String[] headers = new String[]{"Location", "Barcode", "Quantities"};

    private final String[] positions =
            new String[]{"A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1",
                    "A2", "B2", "C2", "D2", "E2", "F2", "G2", "H2",
                    "A3", "B3", "C3", "D3", "E3", "F3", "G3", "H3",
                    "A4", "B4", "C4", "D4", "E4", "F4", "G4", "H4",
                    "A5", "B5", "C5", "D5", "E5", "F5", "G5", "H5",
                    "A6", "B6", "C6", "D6", "E6", "F6", "G6", "H6",
                    "A7", "B7", "C7", "D7", "E7", "F7", "G7", "H7",
                    "A8", "B8", "C8", "D8", "E8", "F8", "G8", "H8"
            };

    @SuppressWarnings("MagicNumber")
    private final double[] quants = new double[]{32.42, 54.22, 17.76, 16.22, 62.74, 99.11, 42.09, 28.04, 95.05, 41.21,
            71.66, 59.02, 50.44, 33.95, 38.44};

    private final String[] barcodes =
            new String[]{"SGMTEST2402938482", "SGMTEST2208428758", "SGMTEST3559709487", "SGMTEST3938342818",
                    "SGMTEST3585528276", "SGMTEST3132943337", "SGMTEST8815228500", "SGMTEST5936483766",
                    "SGMTEST4621329996", "SGMTEST9085949196", "SGMTEST4069756425", "SGMTEST3850486410",
                    "SGMTEST5761812024", "SGMTEST4047896363", "SGMTEST5142352881"};

    private static final String GOOD_QUANT_UPLOAD_FILE = "quant_upload_good.xlsx";

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null) {
            return;
        }

        utx.begin();

        for (String barcode : barcodes) {
            vesselDao.persist(new BarcodedTube(barcode));
        }
        vesselDao.flush();
        vesselDao.clear();
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null) {
            return;
        }
        utx.rollback();
    }

    @Test(groups = TestGroups.STUBBY)
    public void testQuantParser() throws InvalidFormatException, IOException, ValidationException {

        InputStream testSpreadSheetInputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(GOOD_QUANT_UPLOAD_FILE);

        Date startDate = new Date();
        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Date endDate = new Date();

        Collection<LabMetric> createdMetrics = processor.getMetrics();
        Assert.assertEquals(createdMetrics.size(), barcodes.length);
        Assert.assertTrue(processor.getMessages().isEmpty());

        for (LabMetric testMetric : createdMetrics) {
            int i = ArrayUtils.indexOf(barcodes, testMetric.getLabVessel().getLabel());
            Assert.assertTrue(i >= 0);
            Assert.assertEquals(MathUtils.scaleTwoDecimalPlaces(testMetric.getValue()), BigDecimal.valueOf(quants[i]));
            Assert.assertEquals(testMetric.getVesselPosition(), positions[i]);
            Assert.assertFalse(testMetric.getCreatedDate().before(startDate));
            Assert.assertFalse(testMetric.getCreatedDate().after(endDate));
            Assert.assertEquals(testMetric.getLabVessel().getLabel(), barcodes[i]);
        }
    }

    @Test
    public void testBadHeader() throws IOException, InvalidFormatException, ValidationException {
        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);

        InputStream fileInputStream = createQuantUploadInputStream();
        try {
            PoiSpreadsheetParser.processSingleWorksheet(fileInputStream, processor);
        } catch (ValidationException e) {
            // One error for an invalid header.
            Assert.assertEquals(e.getValidationMessages().size(), 1);
        }
        fileInputStream = createQuantUploadInputStream();
        try {
            PoiSpreadsheetParser.processSingleWorksheet(fileInputStream, processor);
        } catch (ValidationException e) {
            // Two errors, one for the previous error (which wasn't cleared) and one for the same error again.
            Assert.assertEquals(e.getValidationMessages().size(), 2);
        }
    }

    public InputStream createQuantUploadInputStream() {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        int currentSheetRow = 0;
        Row currentRow = sheet.createRow(currentSheetRow++);
        CreationHelper creationHelper = workbook.getCreationHelper();
        for (int i = 0; i < headers.length; i++) {
            currentRow.createCell(i).setCellValue(creationHelper.createRichTextString(headers[i]));
        }

        for (int i = 0; i < positions.length; ++i) {
            currentRow = sheet.createRow(currentSheetRow++);
            currentRow.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(positions[i]);

            if (i < barcodes.length) {
                currentRow.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(barcodes[i]);
                currentRow.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(quants[i]);
            }
        }

        InputStream inputStream;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            workbook.write(outputStream);

            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return inputStream;
    }
}
