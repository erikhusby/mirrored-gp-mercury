package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabMetricParserTest extends ContainerTest {

    @Inject
    private TwoDBarcodedTubeDao vesselDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserTransaction utx;

    private List<String> headers;
    private List<String> barcodes;
    private List<Double> quants;
    private List<String> positions;
    private InputStream resourceFile;
    private final String GOOD_QUANT_UPLOAD_FILE = "quant_upload_good.xlsx";

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null) {
            return;
        }
        if (utx == null) {
            return;
        }

        utx.begin();

        // This following header, position, barcode, and quant data matches what is in GOOD_QUANT_UPLOAD_FILE.
        headers = Arrays.asList("Location", "Barcode", "Quants");
        positions = Arrays.asList(
                new String[]{"A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1",
                        "A2", "B2", "C2", "D2", "E2", "F2", "G2", "H2",
                        "A3", "B3", "C3", "D3", "E3", "F3", "G3", "H3",
                        "A4", "B4", "C4", "D4", "E4", "F4", "G4", "H4",
                        "A5", "B5", "C5", "D5", "E5", "F5", "G5", "H5",
                        "A6", "B6", "C6", "D6", "E6", "F6", "G6", "H6",
                        "A7", "B7", "C7", "D7", "E7", "F7", "G7", "H7",
                        "A8", "B8", "C8", "D8", "E8", "F8", "G8", "H8"
                });

        barcodes = Arrays.asList(
                new String[]{"SGMTEST2402938482", "SGMTEST2208428758", "SGMTEST3559709487", "SGMTEST3938342818",
                        "SGMTEST3585528276", "SGMTEST3132943337", "SGMTEST8815228500", "SGMTEST5936483766",
                        "SGMTEST4621329996", "SGMTEST9085949196", "SGMTEST4069756425", "SGMTEST3850486410",
                        "SGMTEST5761812024", "SGMTEST4047896363", "SGMTEST5142352881"});

        quants = Arrays.asList(
                new Double[]{32.42d, 54.22d, 17.76d, 16.22d, 62.74d, 99.11d, 42.09d, 28.04d, 95.05d, 41.21d, 71.66d,
                        59.02d, 50.44d, 33.95d, 38.44d});

        for (String barcode : barcodes) {
            TwoDBarcodedTube testTube = new TwoDBarcodedTube(barcode);
            vesselDao.persist(testTube);
        }
        vesselDao.flush();
        vesselDao.clear();

        resourceFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(GOOD_QUANT_UPLOAD_FILE);
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (vesselDao == null) {
            return;
        }
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testQuantParser() throws InvalidFormatException, IOException, ValidationException {
        Date startDate = new Date();
        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processor);
        parser.processUploadFile(resourceFile);
        Date endDate = new Date();

        Collection<LabMetric> createdMetrics = processor.getMetrics();
        Assert.assertEquals(createdMetrics.size(), barcodes.size());

        for (LabMetric testMetric : createdMetrics) {
            int idx = barcodes.indexOf(testMetric.getLabVessel().getLabel());
            Assert.assertTrue(idx >= 0);
            Assert.assertEquals(testMetric.getValue().setScale(2), BigDecimal.valueOf(quants.get(idx)));
            Assert.assertEquals(testMetric.getVesselPosition(), positions.get(idx));
            Assert.assertFalse(testMetric.getCreatedDate().before(startDate));
            Assert.assertFalse(testMetric.getCreatedDate().after(endDate));
        }
    }

    @Test
    public void testBadHeader() throws IOException, InvalidFormatException, ValidationException {
        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processor);

        File quantUploadFile = createQuantUploadFile(headers, positions, barcodes, quants);
        InputStream fileInputStream = new FileInputStream(quantUploadFile);
        try {
            parser.processUploadFile(fileInputStream);
        } catch (ValidationException e) {
            Assert.assertEquals(e.getValidationMessages().size(), 1);
        }
        quantUploadFile = createQuantUploadFile(headers, positions, barcodes, quants);
        fileInputStream = new FileInputStream(quantUploadFile);
        try {
            parser.processUploadFile(fileInputStream);
        } catch (ValidationException e) {
            Assert.assertEquals(e.getValidationMessages().size(), 2);
        }
    }

    public File createQuantUploadFile(List<String> headers, List<String> positions, List<String> barcodes,
                                      List<Double> quants) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        int currentSheetRow = 0;
        HSSFRow currentRow = sheet.createRow(currentSheetRow);
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            currentRow.createCell(i).setCellValue(new HSSFRichTextString(header));
        }

        for (int idx = 0; idx < positions.size(); ++idx) {
            currentSheetRow++;
            String position = positions.get(idx);
            currentRow = sheet.createRow(currentSheetRow);
            currentRow.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(position);

            if (idx < barcodes.size()) {
                currentRow.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(barcodes.get(idx));
                currentRow.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(quants.get(idx));
            }
        }

        File quantFile = null;
        try {
            quantFile = File.createTempFile("quant", ".xls");
            FileOutputStream quantFileOut = new FileOutputStream(quantFile);
            workbook.write(quantFileOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return quantFile;
    }
}
