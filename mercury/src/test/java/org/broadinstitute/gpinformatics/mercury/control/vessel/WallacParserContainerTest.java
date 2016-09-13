package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the Wallac upload with persistence.
 */
@Test(groups = TestGroups.STANDARD)
public class WallacParserContainerTest extends Arquillian {
    public static final String WALLAC_OUTPUT = "Wallac96WellOutput.xls";

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final String PLATE_1_BARCODE = "2408120";
    public static final String PLATE_2_BARCODE = "2408020";

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence() throws Exception {
        // Replace plate barcodes with timestamps, to avoid unique constraints
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String plate1Barcode = timestamp + "01";
        String plate2Barcode = timestamp + "02";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Pair<LabMetricRun, String> pair1 = makeWallac96Run(plate1Barcode, plate2Barcode, timestamp,
                messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS);

        Assert.assertTrue(StringUtils.isNotBlank(pair1.getRight()));
        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
        Assert.assertNotNull(pair1.getLeft());
        Assert.assertEquals(pair1.getLeft().getLabMetrics().size(), 96 * 3);

        // Should fail the pico redo due to previous quants of the same type.
        messageCollection.clearAll();
        Pair<LabMetricRun, String> pair2 = makeWallac96Run(plate1Barcode, plate2Barcode, timestamp + "2",
                messageCollection, !ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertTrue(messageCollection.hasErrors());
        Assert.assertTrue(messageCollection.getErrors().get(0).contains("Pond Pico was previously done on tubes"));
        Assert.assertNull(pair2);

        // Should accept the pico redo when told to, despite previous quants.
        messageCollection.clearAll();
        Pair<LabMetricRun, String> pair3 = makeWallac96Run(plate1Barcode, plate2Barcode, timestamp + "3",
                messageCollection, ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertTrue(StringUtils.isNotBlank(pair3.getRight()));
        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
        Assert.assertNotNull(pair3.getLeft());
        Assert.assertEquals(pair3.getLeft().getLabMetrics().size(), 96 * 3);
    }

    private Pair<LabMetricRun, String> makeWallac96Run(String plate1Barcode, String plate2Barcode,
                                                       String namePrefix, MessageCollection messageCollection,
                                                       boolean acceptRePico, boolean persistVessels)
            throws Exception {

        Workbook workbook = WorkbookFactory.create(VarioskanParserTest.getSpreadsheet(WALLAC_OUTPUT));
        Sheet curveSheet = workbook.getSheet(WallacRowParser.MEASURE_DETAILS_TAB);
        String runStartTime = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm:ss a").format(new Date());
        for (int i = 0; i <= curveSheet.getLastRowNum(); i++) {
            Row row = curveSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(1);
                if (cell != null) {
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.equals(PLATE_1_BARCODE)) {
                        cell.setCellValue(plate1Barcode);
                    } else if (cellValue.equals(PLATE_2_BARCODE)) {
                        cell.setCellValue(plate2Barcode);
                    } else if (cellValue.equals("Monday, August 06, 2012 3:14:08 PM")) {
                        cell.setCellValue(runStartTime);
                    }
                }
            }
        }
        String runName = namePrefix + "Wallac96Run";
        File tempFile = File.createTempFile("Wallac96", ".xls");
        System.out.println(tempFile.getPath());
        workbook.write(new FileOutputStream(tempFile));
        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.buildPicoTubesAndTransfers(
                mapBarcodeToPlate, plate1Barcode, plate2Barcode, namePrefix);
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }
        return vesselEjb.createWallacRun(new FileInputStream(tempFile), runName, LabMetric.MetricType.POND_PICO,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
    }
}
