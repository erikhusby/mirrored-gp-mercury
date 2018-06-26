package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Test creation of plates
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class IndexedPlateFactoryTest extends StubbyContainerTest {

    public IndexedPlateFactoryTest(){}

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Test(enabled = true)
    public void testParseFile() {
        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseStream(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("DuplexCOAforBroad.xlsx"),
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        Assert.assertEquals(mapBarcodeToPlate.size(), 50, "Wrong number of plates");
    }

    @Test
    public void testParse384WellFile() throws IOException, InvalidFormatException {
        File tempFile = makeUploadFile();
        FileInputStream fis = new FileInputStream(tempFile);
        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_FP, fis);
        Assert.assertEquals(mapBarcodeToPlate.size(), 1, "Wrong number of plates");
    }

    private File makeUploadFile() throws IOException, InvalidFormatException {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String plateBarcode = timestamp + "_DualIndexPlate";
        InputStream inputStream = VarioskanParserTest.getSpreadsheet("DualIndex384WellManifest.xlsx");
        Workbook wb = WorkbookFactory.create(inputStream);
        Sheet sheet = wb.getSheetAt(0);
        boolean foundHeader = false;
        for (Row row : sheet) {
            if (!foundHeader)
            {
                foundHeader = true;
                continue;
            }
            if (row != null) {
                Cell broadBarcodeCell = row.getCell(3, Row.RETURN_BLANK_AS_NULL);
                if (broadBarcodeCell != null) {
                    broadBarcodeCell.setCellValue(plateBarcode);
                }
            }
        }

        File tempFile = File.createTempFile("DualIndexSheet", ".xlsx");
        FileOutputStream out = new FileOutputStream(tempFile);
        wb.write(out);
        out.close();
        return tempFile;
    }
}
