package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the Varioskan upload with persistence.
 */
@Test(groups = TestGroups.STANDARD)
public class VarioskanParserContainerTest extends Arquillian {

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence() {
        InputStream spreadsheet = VarioskanParserTest.getSpreadsheet();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());
        String plate1Barcode = timestamp + "1";
        String plate2Barcode = timestamp + "2";
        try {
            Workbook workbook = WorkbookFactory.create(spreadsheet);
            Sheet sheet = workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            if (cell.toString().equals(VarioskanParserTest.PLATE1_BARCODE_IN_SS)) {
                                cell.setCellValue(plate1Barcode);
                            } else if (cell.toString().equals(VarioskanParserTest.PLATE2_BARCODE_IN_SS)) {
                                cell.setCellValue(plate2Barcode);
                            }
                        }
                    }
                }
            }
            File tempFile = File.createTempFile("Varioskan", ".xls");
            workbook.write(new FileOutputStream(tempFile));
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.buildTubesAndTransfers(
                    mapBarcodeToPlate, plate1Barcode, plate2Barcode, timestamp);
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
            LabMetricRun labMetricRun = vesselEjb.createVarioskanRun(new FileInputStream(tempFile),
                    LabMetric.MetricType.INITIAL_PICO, BSPManagerFactoryStub.QA_DUDE_USER_ID);

            Assert.assertEquals(labMetricRun.getLabMetrics().size(), 96 * 3);
        } catch (InvalidFormatException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}
