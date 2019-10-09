package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.TubeFormationByWellCriteria.Result;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.UploadQuantsActionBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric.MetricType.POND_PICO;

/**
 * Test the Gemini upload with persistence.
 * Make this single threaded to avoid "A previous upload has the same Run Started timestamp." errors
 * Each Gemini run preceded by a 2 second sleep because granularity on LabMetricRun is 1 second
 */
@Test(groups = TestGroups.STANDARD, singleThreaded = true)
public class GeminiParserContainerTest extends Arquillian {
    private SimpleDateFormat sdf = new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
    private static final String PLATE_DUPLICATE_START = "Original Filename: Plating Pico; Date Last Saved: ";
    // This boolean controls whether the test will send the Gemini quants to BSP.
    private static final boolean SEND_TO_BSP = false;

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UploadQuantsActionBean uploadQuantsActionBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence96InDuplicate() throws Exception {
        Thread.sleep(2000L);

        String timestamp = sdf.format(new Date());
        String plate1Barcode = System.currentTimeMillis() + "01";
        String plate2Barcode = System.currentTimeMillis() + "02";

        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;
        final Replicate replicate = Replicate.Duplicate;

        MessageCollection messageCollection = new MessageCollection();

        final String baseFile = GeminiPlateProcessorTest.DUPLICATE_96_PICO;
        makeGeminiRun(baseFile, replicate,
                timestamp, messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS, plate1Barcode, plate2Barcode);
        Assert.assertEquals(StringUtils.join(messageCollection.getErrors(), "; "), "");
        // Informational message from BSP saying it doesn't know the tube barcodes.
        if (SEND_TO_BSP) {
            Assert.assertTrue(messageCollection.getInfos().get(0).contains("96 are not in BSP"),
                    StringUtils.join(messageCollection.getInfos(), "; "));
        }
    }

    /**
     * Nexome Pond Pico will be 2 library Plates to 1 384 Pico Plate in Duplicate
     */
    @Test
    public void testNexomePondpico() throws Exception {
        Thread.sleep(2000L);

        String timestamp = sdf.format(new Date());
        String plate1Barcode = System.currentTimeMillis() + "01";

        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_PICO_REDO = true;

        MessageCollection messageCollection = new MessageCollection();

        // Plate Barcode shows up twice in a row
        final String baseFile = GeminiPlateProcessorTest.NEXOME_PICO;
        LabMetricRun labMetricRun = makeGeminiRun(baseFile, Replicate.Nexome,
                timestamp, messageCollection, !ACCEPT_PICO_REDO, PERSIST_VESSELS, plate1Barcode, plate1Barcode);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);
        if (SEND_TO_BSP) {
            // Should not even have attempted to send Nexome wells to BSP.
            Assert.assertFalse(StringUtils.join(messageCollection.getInfos()).contains("BSP"),
                    StringUtils.join(messageCollection.getInfos(), "; "));
        }
        Assert.assertEquals(labMetricRun.getLabMetrics().size(),  576);
    }

    private LabMetricRun makeGeminiRun(String baseFile, Replicate replicate, String timestamp,
            MessageCollection messageCollection, boolean acceptRePico, boolean persistVessels, String... plateBarcode)
            throws Exception {

        BufferedInputStream quantStream = makeGeminiSpreadsheet(baseFile, plateBarcode, timestamp, replicate);

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        List<Map<VesselPosition, BarcodedTube>> mapPositionToTube = new ArrayList<>();
        List<Collection<?>> labVessels = null;
        if (replicate == Replicate.Duplicate) {
            mapPositionToTube.add(VarioskanParserTest.buildPicoTubesAndTransfers(
                    mapBarcodeToPlate, plateBarcode[0], plateBarcode[1], System.currentTimeMillis() + ""));
            labVessels = mapPositionToTube.stream().map(Map::values).collect(Collectors.toList());
        } else if (replicate == Replicate.Nexome) {
            List<Map<VesselPosition, LabVessel>> mapList = VarioskanParserTest.buildPicoPlateWellsAndTransfers( 96,
                    mapBarcodeToPlate, plateBarcode[0], System.currentTimeMillis() + "");
            labVessels = mapList.stream().map(Map::values).collect(Collectors.toList());
        }
        if (persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            for (Collection<?> list: labVessels) {
                labVesselDao.persistAll(list);
            }
        }
        String runName = "Gemini Run " + timestamp;
        Triple<LabMetricRun, List<Result>, Map<String, String>> triple = vesselEjb.createGeminiRun(quantStream,
                runName, POND_PICO, BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptRePico);
        Map<String, String> tubeBarcodeToQuant = triple.getRight();
        mapPositionToTube.stream().
                flatMap(map -> map.values().stream()).
                map(LabVessel::getLabel).
                sorted().distinct().
                forEach(tubeBarcode -> {
                    Assert.assertTrue(tubeBarcodeToQuant.containsKey(tubeBarcode), "Missing " + tubeBarcode);
                    Assert.assertTrue(StringUtils.isNotBlank(tubeBarcodeToQuant.get(tubeBarcode)),
                            "Missing quant for " + tubeBarcode);
                });
        if (SEND_TO_BSP && triple.getMiddle() != null && tubeBarcodeToQuant != null) {
            // SendS tube quants to BSP. Since these are not in BSP they'll just be ignored.
            uploadQuantsActionBean.getUserBean().loginOSUser();
            uploadQuantsActionBean.setTubeFormationLabels(triple.getMiddle().stream()
                    .map(r -> r.getTubeFormation().getLabel()).collect(Collectors.toList()));
            Map<String, String> tubeBarcodeToQuantValue = triple.getRight();
            uploadQuantsActionBean.sendTubeQuantsToBsp(tubeBarcodeToQuantValue, messageCollection);
        }
        return triple.getLeft();
    }

    /** Makes a pico spreadsheet with one or more microfluor plates of either 96 or 384 wells. */
    private BufferedInputStream makeGeminiSpreadsheet(String baseFile, String[] inputPlateBarcode,
                                                     String newTime, Replicate replicate) throws Exception {
        String[] plateBarcodes = ArrayUtils.clone(inputPlateBarcode);
        int barcodeIndex = 0;
        String currPlateBarcode = null;
        if (replicate == Replicate.Duplicate) {
            plateBarcodes[0] = plateBarcodes[0] + "," + plateBarcodes[1];
        }
        Workbook workbook = WorkbookFactory.create(VarioskanParserTest.getSpreadsheet(baseFile));
        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.startsWith("Group: 00")) {
                        currPlateBarcode = plateBarcodes[barcodeIndex++];
                        cell.setCellValue("Group: " + currPlateBarcode);
                    } else if (cellValue.startsWith("Original Filename")) {
                        cell.setCellValue(PLATE_DUPLICATE_START + newTime);
                    }
                }
            }
        }

        File tempFile = File.createTempFile("Gemini", ".xls");
        workbook.write(new FileOutputStream(tempFile));
        return new BufferedInputStream(FileUtils.openInputStream(tempFile));
    }

    private enum Replicate {
        Duplicate, Triplicate, Nexome
    }
}
