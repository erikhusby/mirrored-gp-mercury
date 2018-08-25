package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Triple;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.TubeFormationByWellCriteria.Result;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric.MetricType.INITIAL_PICO;

/**
 * Test the Varioskan upload with persistence.
 * Make this single threaded to avoid "A previous upload has the same Run Started timestamp." errors
 * Each varioscan run preceded by a 2 second sleep because granularity on LabMetricRun is 1 second
 */
@Test(groups = TestGroups.STANDARD, singleThreaded = true)
@Dependent // To support injection into PicoToBspContainerTest
public class VarioskanParserContainerTest extends Arquillian {
    private static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");
    private static final float A01_384_VALUE = 44.229f; // From Varioskan384Output.xls

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence96InDuplicate() throws Exception {

        // Sleep to avoid "A previous upload has the same Run Started timestamp." errors
        // Granularity on LabMetricRun is 1 second
        Thread.sleep(2000L);

        // Replace plate barcodes with timestamps, to avoid unique constraints
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String plate1Barcode = timestamp + "01";
        String plate2Barcode = timestamp + "02";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple1 = makeVarioskanRun(plate1Barcode, plate2Barcode,
                timestamp, messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS);

        Assert.assertNotNull(triple1.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple1.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple1.getLeft());
        Assert.assertEquals(triple1.getLeft().getLabMetrics().size(), VarioskanParserTest.VARIOSKAN_SAMPLE_COUNT * 3);

        // Should fail the pico redo due to previous quants of the same type.
        messageCollection.clearAll();
        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple2 = makeVarioskanRun(plate1Barcode, plate2Barcode,
                timestamp + "2", messageCollection, !ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertTrue(messageCollection.hasErrors());
        Assert.assertTrue(messageCollection.getErrors().get(0).contains("Initial Pico was previously done"), "Wrong expected error: " + messageCollection.getErrors().get(0));
        Assert.assertNull(triple2);

        // Should accept the pico redo when told to, despite previous quants.
        messageCollection.clearAll();
        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple3 = makeVarioskanRun(plate1Barcode, plate2Barcode,
                timestamp + "3", messageCollection, ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertNotNull(triple3.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple3.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple3.getLeft());
        Assert.assertEquals(triple3.getLeft().getLabMetrics().size(), VarioskanParserTest.VARIOSKAN_SAMPLE_COUNT * 3);

    }


    @Test
    public void testPersistenceRibo384InDuplicate() throws Exception {
        // Replace plate barcodes with timestamps, to avoid unique constraints
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String plateBarcode = timestamp + "01";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple1 = make384RiboVarioskanRun(plateBarcode, timestamp,
                messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS);

        Assert.assertNotNull(triple1.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple1.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple1.getLeft());
        Assert.assertEquals(triple1.getLeft().getLabMetrics().size(),
                VarioskanParserTest.VARIOSKAN_RIBO_SAMPLE_COUNT * 3);

        // Should fail the pico redo due to previous quants of the same type.
        messageCollection.clearAll();
        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>>
                triple2 = make384RiboVarioskanRun(plateBarcode, timestamp + "2",
                messageCollection, !ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertTrue(messageCollection.hasErrors());
        Assert.assertTrue(messageCollection.getErrors().get(0).contains("Initial Ribo was previously done"), "Wrong expected error: " + messageCollection.getErrors().get(0));
        Assert.assertNull(triple2);

        // Should accept the pico redo when told to, despite previous quants.
        messageCollection.clearAll();
        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>>
                triple3 = make384RiboVarioskanRun(plateBarcode, timestamp + "3",
                messageCollection, ACCEPT_PICO_REDO, !PERSIST_VESSELS);

        Assert.assertNotNull(triple3.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple3.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple3.getLeft());
        Assert.assertEquals(triple3.getLeft().getLabMetrics().size(),
                VarioskanParserTest.VARIOSKAN_RIBO_SAMPLE_COUNT * 3);
    }

    @Test
    public void test384TriplicateNoDilution() throws Exception {
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String plateBarcode = timestamp + "03";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple = triplicateNoDilution(plateBarcode, timestamp,
                messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS);

        Assert.assertNotNull(triple.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple.getLeft());
        // For each sample, expects one metric for each microflour well plus a metric for the average.
        int metricCount = VarioskanParserTest.VARIOSKAN_384_SAMPLE_COUNT * 4;
        Assert.assertEquals(triple.getLeft().getLabMetrics().size(), metricCount);
    }

    @Test
    public void test384Triplicate() throws Exception {
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String dilutionBarcode = timestamp + "04";
        String microfluorBarcode = timestamp + "05";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple = triplicateDilution(dilutionBarcode,
                microfluorBarcode, timestamp, messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS);

        Assert.assertNotNull(triple.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple.getLeft());
        // For each sample, expects one metric for each microflour well plus a metric for the average.
        int metricCount = VarioskanParserTest.VARIOSKAN_384_SAMPLE_COUNT * 4;
        Assert.assertEquals(triple.getLeft().getLabMetrics().size(), metricCount);
    }

    @Test
    public void test384TwoCurveTriplicate() throws Exception {
        int numSamples = 10;
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String dilutionBarcode = timestamp + "04";
        String microfluorBarcode = timestamp + "05";
        MessageCollection messageCollection = new MessageCollection();
        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        Thread.sleep(2000L);
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triple = triplicateDilution(dilutionBarcode,
                microfluorBarcode, timestamp, messageCollection, ACCEPT_PICO_REDO, PERSIST_VESSELS,
                VarioskanParserTest.VARIOSKAN_384_2CURVE_OUTPUT);

        Assert.assertNotNull(triple.getMiddle().get(0).getTubeFormation());
        Assert.assertTrue(CollectionUtils.isNotEmpty(triple.getRight()));
        Assert.assertFalse(messageCollection.hasErrors(), "Errors exist: " + StringUtils
                .join(messageCollection.getErrors(), ","));
        Assert.assertFalse(messageCollection.hasWarnings(), "Warnings exist: " + StringUtils
                .join(messageCollection.getWarnings(), ","));
        Assert.assertNotNull(triple.getLeft());
        // For each sample, expects one metric for each microflour well plus a metric for the average.
        int metricCount = 35; //5 NaNs in HS should be skipped so not the full 40
        Set<LabMetric> labMetrics = triple.getLeft().getLabMetrics();
        Assert.assertEquals(triple.getLeft().getLabMetrics().size(), metricCount);
    }

    private Triple<LabMetricRun, List<Result>, Set<StaticPlate>> make384RiboVarioskanRun(String plateBarcode,
                                                                                         String namePrefix, MessageCollection messageCollection, boolean acceptRePico, boolean persistVessels)
            throws Exception {

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.buildRiboTubesAndTransfers(
                mapBarcodeToPlate, plateBarcode, namePrefix);
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }
        BufferedInputStream inputStream = makeVarioskanSpreadsheet(new String[]{plateBarcode},
                VarioskanParserTest.VARIOSKAN_RIBO_OUTPUT, namePrefix);
        return vesselEjb.createVarioskanRun(inputStream, LabMetric.MetricType.INITIAL_RIBO,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
    }

    private Triple<LabMetricRun, List<Result>, Set<StaticPlate>> makeVarioskanRun(String plate1Barcode, String plate2Barcode,
                                                                                  String namePrefix, MessageCollection messageCollection, boolean acceptRePico, boolean persistVessels)
            throws Exception {

        BufferedInputStream quantStream = makeVarioskanSpreadsheet(new String[]{plate1Barcode, plate2Barcode},
                VarioskanParserTest.VARIOSKAN_OUTPUT, namePrefix);

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.buildPicoTubesAndTransfers(
                mapBarcodeToPlate, plate1Barcode, plate2Barcode, namePrefix);
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }
        return vesselEjb.createVarioskanRun(quantStream, INITIAL_PICO,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
    }

    private Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triplicateNoDilution(String plateBarcode, String namePrefix,
                                                                                      MessageCollection messageCollection, boolean acceptRePico, boolean persistVessels) throws Exception {

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.
                buildTriplicateNoDilutionTubesAndTransfers(mapBarcodeToPlate, plateBarcode, namePrefix);
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }
        BufferedInputStream inputStream = makeVarioskanSpreadsheet(new String[]{plateBarcode},
                VarioskanParserTest.VARIOSKAN_384_OUTPUT, namePrefix);
        return vesselEjb.createVarioskanRun(inputStream, INITIAL_PICO,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
    }

    private Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triplicateDilution(String dilutionBarcode,
                                                                                    String microfluorBarcode, String namePrefix, MessageCollection messageCollection, boolean acceptRePico,
                                                                                    boolean persistVessels) throws Exception {
        return triplicateDilution(dilutionBarcode, microfluorBarcode, namePrefix, messageCollection, acceptRePico,
                persistVessels, VarioskanParserTest.VARIOSKAN_384_OUTPUT);
    }

    private Triple<LabMetricRun, List<Result>, Set<StaticPlate>> triplicateDilution(String dilutionBarcode,
                                                                                    String microfluorBarcode, String namePrefix, MessageCollection messageCollection, boolean acceptRePico,
                                                                                    boolean persistVessels,
                                                                                    String filename) throws Exception {

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = VarioskanParserTest.
                buildTriplicateTubesAndTransfers(mapBarcodeToPlate, dilutionBarcode, microfluorBarcode, namePrefix);
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }
        BufferedInputStream inputStream = makeVarioskanSpreadsheet(new String[]{microfluorBarcode},
                filename, namePrefix);
        return vesselEjb.createVarioskanRun(inputStream, INITIAL_PICO,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
    }


    /** Makes a pico spreadsheet with one or more microfluor plates of either 96 or 384 wells. */
    public BufferedInputStream makeVarioskanSpreadsheet(String[] plateBarcodes, String spreadsheetName,
                                                        String namePrefix) throws Exception {

        Workbook workbook = WorkbookFactory.create(VarioskanParserTest.getSpreadsheet(spreadsheetName));
        Sheet curveSheet = workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
        List<Row> toBeRemoved = new ArrayList<>();
        for (int i = 1; i <= curveSheet.getLastRowNum(); i++) {
            Row row = curveSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.equals(VarioskanParserTest.PLATE1_BARCODE_IN_SS) ||
                        cellValue.equals(VarioskanParserTest.RIBO_PLATE_BARCODE_IN_SS)) {
                        cell.setCellValue(plateBarcodes[0]);
                    } else if (cellValue.equals(VarioskanParserTest.PLATE2_BARCODE_IN_SS)) {
                        if (plateBarcodes.length > 1) {
                            cell.setCellValue(plateBarcodes[1]);
                        } else {
                            toBeRemoved.add(row);
                        }
                    }
                }
            }
        }
        // If there is only one plate barcode, removes the rows for PLATE2_BARCODE_IN_SS.
        for (Row row : toBeRemoved) {
            curveSheet.removeRow(row);
        }

        // Replace run name with timestamp, to avoid unique constraints
        Sheet generalSheet = workbook.getSheet(VarioskanRowParser.GENERAL_INFO_TAB);
        for (int i = 1; i <= generalSheet.getLastRowNum(); i++) {
            Row row = generalSheet.getRow(i);
            if (row != null) {
                Cell nameCell = row.getCell(VarioskanRowParser.NAME_COLUMN);
                if (nameCell != null && nameCell.getStringCellValue().equals(
                        VarioskanRowParser.NameValue.RUN_NAME.getFieldName())) {
                    Cell valueCell = row.getCell(VarioskanRowParser.VALUE_COLUMN);
                    valueCell.setCellValue(namePrefix + " Mike Test");
                } else if (nameCell != null && nameCell.getStringCellValue().equals(
                        VarioskanRowParser.NameValue.RUN_STARTED.getFieldName())) {
                    Cell valueCell = row.getCell(VarioskanRowParser.VALUE_COLUMN);
                    valueCell.setCellValue(new SimpleDateFormat(
                            VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat()).format(new Date()));
                }
            }
        }

        File tempFile = File.createTempFile("Varioskan", ".xls");
        workbook.write(new FileOutputStream(tempFile));
        return new BufferedInputStream(FileUtils.openInputStream(tempFile));
    }
}
