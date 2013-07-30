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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class LabMetricParserTest extends ContainerTest {

    @Inject
    private TwoDBarcodedTubeDAO vesselDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserTransaction utx;
 //   private Map<String, LabVessel> barcodeToTubeMap = new HashMap<>();
    private Map<String, Double> barcodeToQuant = new HashMap<>();
    private InputStream resourceFile;
    private String[] positions;
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

        positions = new String[]{"A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10", "A11", "A12",
                "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10", "B11", "B12"};

        barcodeToQuant.put("SGMTEST0150674728", 71.66d);
        barcodeToQuant.put("SGMTEST0150674722", 59.02d);
        barcodeToQuant.put("SGMTEST0150674727", 50.44d);
        barcodeToQuant.put("SGMTEST0150674703", 33.95d);
        barcodeToQuant.put("SGMTEST0150674743", 38.44d);

        for (Map.Entry<String, Double> quantEntry : barcodeToQuant.entrySet()) {
            TwoDBarcodedTube testTube = new TwoDBarcodedTube(quantEntry.getKey());
            vesselDao.persist(testTube);
        }

        String GOOD_QUANT_UPLOAD_FILE = "quant_upload_good.xlsx";
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

        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processor);
        parser.processUploadFile(resourceFile);

        Collection<LabMetric> createdMetrics = processor.getMetrics();
        Assert.assertEquals(createdMetrics.size(), barcodeToQuant.size());

        for(LabMetric testMetric:createdMetrics) {
            Assert.assertEquals(testMetric.getValue().setScale(2),
                    BigDecimal.valueOf(barcodeToQuant.get(testMetric.getLabVessel().getLabel())));
        }
    }

    @Test
    public void testBadHeader() throws IOException, InvalidFormatException, ValidationException {
        LabMetricProcessor processor = new LabMetricProcessor(labVesselDao, LabMetric.MetricType.ECO_QPCR);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processor);

        File quantUploadFile = createQuantUploadFile(Arrays.asList("Location","Barcode","Quants"),barcodeToQuant);
        InputStream fileInputStream = new FileInputStream(quantUploadFile);
        try {
            parser.processUploadFile(fileInputStream);
        } catch (ValidationException e) {
            Assert.assertEquals(e.getValidationMessages().size(), 1);
        }
        quantUploadFile = createQuantUploadFile(Arrays.asList("Location","Barcodes","Quants"),barcodeToQuant);
        fileInputStream = new FileInputStream(quantUploadFile);
        try {
            parser.processUploadFile(fileInputStream);
        } catch (ValidationException e) {
            Assert.assertEquals(e.getValidationMessages().size(), 2);
        }
    }

    public File createQuantUploadFile(List<String> headers, Map<String, Double> barcodeToQuant) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        int currentSheetRow = 0;
        HSSFRow currentRow = sheet.createRow(currentSheetRow);
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            currentRow.createCell(i).setCellValue(new HSSFRichTextString(header));
        }

        currentSheetRow++;


        for (Map.Entry<String, Double> currBarcode : barcodeToQuant.entrySet()) {
            currentRow = sheet.createRow(currentSheetRow);
            String barcode = currBarcode.getKey();
            Double quantValue = currBarcode.getValue();

            currentRow.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(positions[currentSheetRow]);
            currentRow.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(barcode);
            currentRow.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(quantValue);
            currentSheetRow++;
        }
        while (currentSheetRow < positions.length) {
            currentRow = sheet.createRow(currentSheetRow);
            currentRow.createCell(0).setCellValue(positions[currentSheetRow]);
            currentSheetRow++;
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
