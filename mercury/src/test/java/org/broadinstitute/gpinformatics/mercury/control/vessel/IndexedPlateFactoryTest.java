package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.IndexPlateDefinitionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sun.plugin2.ipc.IPCFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test creation of index plates
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class IndexedPlateFactoryTest extends StubbyContainerTest {
    private List<String> misNames;
    private FastDateFormat dateFormat = FastDateFormat.getInstance("MMddHHmmss");

    public IndexedPlateFactoryTest() {
    }

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private IndexPlateDefinitionDao indexPlateDefinitionDao;

    @Inject
    private UserBean userBean;

    @Test
    public void testParseFile() {
        Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseStream(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("DuplexCOAforBroad.xlsx"),
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        Assert.assertEquals(mapBarcodeToPlate.size(), 50, "Wrong number of plates");
    }

    @Test
    public void testParse384WellFile() throws IOException, InvalidFormatException {
        File tempFile = makeUploadFile();
        FileInputStream fis = new FileInputStream(tempFile);
        Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
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
            if (!foundHeader) {
                foundHeader = true;
                continue;
            }
            if (row != null) {
                Cell broadBarcodeCell = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
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

    @BeforeMethod
    public void beforeMethod() {
        if (userBean != null) {
            userBean.loginTestUser();
        }
        if (staticPlateDao != null && CollectionUtils.isEmpty(misNames)) {
            misNames = staticPlateDao.findAll(MolecularIndexingScheme.class).stream().
                    map(MolecularIndexingScheme::getName).
                    filter(name -> name.startsWith("Illumina") && name.contains("P5") && name.contains("P7")).
                    limit(400).
                    collect(Collectors.toList());
        }
    }

    @Test
    public void testDefineAndInstantiateIndexPlate() throws Exception {
        String identifier = dateFormat.format(new Date());
        String salesOrderNumber = "salesOrder" + identifier;
        String plateName = "plateName" + identifier;
        IndexPlateDefinition.ReagentType reagentType = IndexPlateDefinition.ReagentType.ADAPTER;
        VesselGeometry vesselGeometry = VesselGeometry.G24x16;

        // Makes a spreadsheet consisting of a header and rows for each plate well position and mis name.
        MessageCollection messageCollection = new MessageCollection();
        List<List<String>> cellGrid = new ArrayList<>();
        Iterator<String> vesselPositionIterator = vesselGeometry.getPositionNames();
        Iterator<String> misNameIterator = misNames.iterator();
        cellGrid.add(Arrays.asList("Position", "Index Name"));
        while (vesselPositionIterator.hasNext()) {
            cellGrid.add(Arrays.asList(vesselPositionIterator.next(), misNameIterator.next()));
        }
        InputStream stream = excelSpreadsheet(cellGrid);
        List<List<String>> spreadsheet = indexedPlateFactory.parseSpreadsheet(stream, 2, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertEquals(spreadsheet.stream().flatMap(List::stream).collect(Collectors.joining(" ")),
                cellGrid.stream().flatMap(List::stream).collect(Collectors.joining(" ")));

        // Creates index plate definition.
        messageCollection.clearAll();
        indexedPlateFactory.makeIndexPlateDefinition(plateName, spreadsheet, vesselGeometry,
                reagentType, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        String selectedPlateName = indexedPlateFactory.plateNameToSelectionName(plateName, reagentType);
        List<String> plateNameSelection = new ArrayList<>();
        indexedPlateFactory.findPlateDefinitionNames(plateNameSelection);
        Assert.assertTrue(plateNameSelection.contains(selectedPlateName), "Missing " + selectedPlateName);

        // Instantiates an index plate from the plate definition.
        messageCollection.clearAll();
        String plateBarcode = identifier;
        List<List<String>> barcodeSpreadsheet = Arrays.asList(Arrays.asList("Barcodes"), Arrays.asList(plateBarcode));
        indexedPlateFactory.makeIndexPlate(selectedPlateName, barcodeSpreadsheet, salesOrderNumber, false,
                messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Verifies the plate barcode is leading zero filled to 12 digits.
        String zeroFilledBarcode = String.format("%012d", Long.parseLong(plateBarcode));
        StaticPlate staticPlate = staticPlateDao.findByBarcode(zeroFilledBarcode);
        Assert.assertNotNull(staticPlate);
        Assert.assertEquals(staticPlate.getSalesOrderNumber(), salesOrderNumber);
        Assert.assertEquals(staticPlate.getIndexPlateDefinition().getDefinitionName(), plateName);
        for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
            Collection<SampleInstanceV2> instances = staticPlate.getContainerRole().
                    getSampleInstancesAtPositionV2(VesselPosition.getByName(row.get(0)));
            Assert.assertEquals(instances.size(), 1);
            Assert.assertEquals(instances.iterator().next().getMolecularIndexingScheme().getName(), row.get(1));
        }
    }

    @Test
    public void testMultipleIndexPlateInstances() {
        final String identifier = dateFormat.format(new Date());
        final String salesOrderNumber = "salesOrder " + identifier;
        final String plateName = "plateName" + identifier;
        final int numberOfPlates = 5;
        final MessageCollection messageCollection = new MessageCollection();
        final IndexPlateDefinition.ReagentType reagentType = IndexPlateDefinition.ReagentType.ADAPTER;
        final VesselGeometry vesselGeometry = VesselGeometry.G12x8;

        // Makes an index plate definition.
        indexedPlateFactory.makeIndexPlateDefinition(plateName,
                Arrays.asList(Arrays.asList("B02", misNames.get(302)), Arrays.asList("C03", misNames.get(303))),
                vesselGeometry, reagentType, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        String selectedPlateName = indexedPlateFactory.plateNameToSelectionName(plateName, reagentType);

        // Instantiates a number of index plates from the definition.
        messageCollection.clearAll();
        List<List<String>> plateBarcodes = IntStream.range(0, numberOfPlates).
                mapToObj(i -> Collections.singletonList("0" + identifier + i)).
                collect(Collectors.toList());
        indexedPlateFactory.makeIndexPlate(selectedPlateName, plateBarcodes.subList(0, numberOfPlates - 1),
                salesOrderNumber, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        // Does the last barcode in another call.
        indexedPlateFactory.makeIndexPlate(selectedPlateName,
                plateBarcodes.subList(numberOfPlates - 1, numberOfPlates), salesOrderNumber, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        final IndexPlateDefinition plateDefinition = indexPlateDefinitionDao.findByName(plateName);
        Assert.assertNotNull(plateDefinition, "def for " + plateName);
        Assert.assertEquals(plateDefinition.getDefinitionName(), plateName);
        Assert.assertEquals(plateDefinition.getVesselGeometry(), vesselGeometry);
        Assert.assertEquals(plateDefinition.getReagentType(), reagentType);
        Assert.assertEquals(plateDefinition.getPlateInstances().stream().
                        map(LabVessel::getLabel).sorted().collect(Collectors.joining(" ")),
                plateBarcodes.stream().flatMap(List::stream).sorted().collect(Collectors.joining(" ")));

        plateBarcodes.stream().flatMap(List::stream).forEach(barcode -> {
            StaticPlate staticPlate = staticPlateDao.findByBarcode(barcode);
            Assert.assertNotNull(staticPlate, "missing " + barcode);
            Assert.assertEquals(staticPlate.getIndexPlateDefinition(), plateDefinition, "for barcode " + barcode);
            Assert.assertEquals(staticPlate.getSalesOrderNumber(), salesOrderNumber, "for barcode " + barcode);
        });
    }

    @Test
    public void testRenameAndDelete() {
        final String identifier = dateFormat.format(new Date());
        final String plateName1 = "plateName1_" + identifier;
        final String plateName2 = "plateName2_" + identifier;
        final String plateName3 = "plateName3_" + identifier;
        final MessageCollection messageCollection = new MessageCollection();

        indexedPlateFactory.makeIndexPlateDefinition(plateName1, Arrays.asList(Arrays.asList("A1", misNames.get(99))),
                VesselGeometry.G12x8, IndexPlateDefinition.ReagentType.ADAPTER, false, messageCollection);

        indexedPlateFactory.makeIndexPlateDefinition(plateName2, Arrays.asList(Arrays.asList("A1", misNames.get(99))),
                VesselGeometry.G12x8, IndexPlateDefinition.ReagentType.ADAPTER, false, messageCollection);

        final String selectedPlateName1 = indexedPlateFactory.plateNameToSelectionName(plateName1,
                IndexPlateDefinition.ReagentType.ADAPTER);
        final String selectedPlateName2 = indexedPlateFactory.plateNameToSelectionName(plateName2,
                IndexPlateDefinition.ReagentType.ADAPTER);

        // Rename should fail since new plate name is in use.
        indexedPlateFactory.renameDefinition(selectedPlateName1, plateName2, messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.IN_USE, IndexedPlateFactory.DEFINITION_SUFFIX, plateName2)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Delete should succeed.
        indexedPlateFactory.deleteDefinition(selectedPlateName2, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertNull(indexPlateDefinitionDao.findByName(plateName2), "Failed to delete " + plateName2);

        // Rename should succeed.
        indexedPlateFactory.renameDefinition(selectedPlateName1, plateName2, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Rename should fail since plate definition doesn't exist any more.
        indexedPlateFactory.renameDefinition(selectedPlateName1, plateName3, messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.NOT_FOUND, IndexedPlateFactory.DEFINITION_SUFFIX, plateName1)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Delete should fail since plate definition doesn't exist.
        indexedPlateFactory.deleteDefinition(selectedPlateName1, messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.NOT_FOUND, IndexedPlateFactory.DEFINITION_SUFFIX, plateName1)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Instantiate the plate definition should fail since plate definition doesn't exist.
        String plateBarcode = String.format("%012d", Long.parseLong(identifier));
        indexedPlateFactory.makeIndexPlate(selectedPlateName1, Arrays.asList(Arrays.asList(plateBarcode)), "", false,
                messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.NOT_FOUND, IndexedPlateFactory.DEFINITION_SUFFIX, plateName1)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Instantiate should succeed with plateName2.
        indexedPlateFactory.makeIndexPlate(selectedPlateName2, Arrays.asList(Arrays.asList(plateBarcode)), "", false,
                messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        StaticPlate plate2 = staticPlateDao.findByBarcode(plateBarcode);
        Assert.assertNotNull(plate2);
        Assert.assertTrue(indexPlateDefinitionDao.findByName(plateName2).getPlateInstances().contains(plate2));

        // Delete should fail since plate definition is in use.
        indexedPlateFactory.deleteDefinition(selectedPlateName2, messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.CANNOT_REMOVE, IndexedPlateFactory.DEFINITION_SUFFIX, plateName2)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Deleting the plate instances should fail since one of the plates doesn't exist.
        indexedPlateFactory.deleteInstances(Arrays.asList("0-0-0-0-0-00", plateBarcode), messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.NOT_FOUND, "", "0-0-0-0-0-00")),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Deleting the plate instance should succeed.
        indexedPlateFactory.deleteInstances(Collections.singletonList(plateBarcode), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertNull(staticPlateDao.findByBarcode(plateBarcode), "Failed to delete " + plateBarcode);

        // And now deleting the plate definition should succeed.
        indexedPlateFactory.deleteDefinition(selectedPlateName2, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertNull(indexPlateDefinitionDao.findByName(plateName2), "Failed to delete " + plateName2);
    }

    @Test
    public void testRemakeAndDelete() {
        final String identifier = dateFormat.format(new Date());
        final String plateName1 = "plateName1_" + identifier;
        final String plateName2 = "plateName2_" + identifier;
        final MessageCollection messageCollection = new MessageCollection();

        final String selectedPlateName1 = indexedPlateFactory.plateNameToSelectionName(plateName1,
                IndexPlateDefinition.ReagentType.ADAPTER);
        final String selectedPlateName2 = indexedPlateFactory.plateNameToSelectionName(plateName2,
                IndexPlateDefinition.ReagentType.PRIMER);


        // Makes the first plate definition.
        indexedPlateFactory.makeIndexPlateDefinition(plateName1, Arrays.asList(Arrays.asList("J18", misNames.get(99))),
                VesselGeometry.G24x16, IndexPlateDefinition.ReagentType.ADAPTER, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertNotNull(indexPlateDefinitionDao.findByName(plateName1));
        // Remakes the definition. This fails when "Replace Existing" is unset.
        indexedPlateFactory.makeIndexPlateDefinition(plateName1, Arrays.asList(Arrays.asList("A01", misNames.get(0))),
                VesselGeometry.G12x8, IndexPlateDefinition.ReagentType.PRIMER, false, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().get(0),
                String.format(IndexedPlateFactory.NEEDS_OVERWRITE, IndexedPlateFactory.DEFINITION_SUFFIX),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();
        // The original definition still exists.
        Assert.assertEquals(indexPlateDefinitionDao.findByName(plateName1).getVesselGeometry(), VesselGeometry.G24x16);
        // Remake succeeds when "Replace Existing" is set.
        indexedPlateFactory.makeIndexPlateDefinition(plateName1, Arrays.asList(Arrays.asList("A01", misNames.get(0))),
                VesselGeometry.G12x8, IndexPlateDefinition.ReagentType.ADAPTER, true, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertEquals(indexPlateDefinitionDao.findByName(plateName1).getVesselGeometry(), VesselGeometry.G12x8);

        // Makes the second plate definition.
        indexedPlateFactory.makeIndexPlateDefinition(plateName2, Arrays.asList(Arrays.asList("N03", misNames.get(90))),
                VesselGeometry.G24x16, IndexPlateDefinition.ReagentType.PRIMER, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Instantiates an index plate from the 2nd plate definition then remakes the plate (using the same
        // plate barcode) from the 1st plate definition. This is allowed as long as the index plate is unused.
        String plateBarcode = String.format("%012d", Long.parseLong(identifier));
        List<List<String>> barcodeSpreadsheet = Arrays.asList(Arrays.asList("Barcodes"), Arrays.asList(plateBarcode));
        indexedPlateFactory.makeIndexPlate(selectedPlateName2, barcodeSpreadsheet, identifier, false,
                messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        // With "replace existing" checkbox unset the remake fails.
        indexedPlateFactory.makeIndexPlate(selectedPlateName1, barcodeSpreadsheet, identifier, false,
                messageCollection);
        Assert.assertEquals(messageCollection.getErrors().get(0),
                String.format(IndexedPlateFactory.NEEDS_OVERWRITE, ""),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();
        // This remake succeeds.
        indexedPlateFactory.makeIndexPlate(selectedPlateName1, barcodeSpreadsheet, "a test", true, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        StaticPlate staticPlate = staticPlateDao.findByBarcode(plateBarcode);
        Assert.assertNotNull(staticPlate, "missing " + plateBarcode);
        messageCollection.clearAll();

        // Tests finding the instances for the plate definition.
        Pair<String, String> unusedAndInUse = indexedPlateFactory.findInstances(selectedPlateName1, messageCollection);
        Assert.assertTrue(unusedAndInUse.getLeft().contains(staticPlate.getLabel()));
        Assert.assertTrue(unusedAndInUse.getRight().isEmpty());

        // Adds a transfer to a new plate so that this plate will be treated as being in use.
        StaticPlate targetPlate = new StaticPlate("dummy" + identifier, staticPlate.getPlateType());
        staticPlate.getContainerRole().getSectionTransfersFrom().add(new SectionTransfer(
                staticPlate.getContainerRole(), SBSSection.ALL96, null,
                targetPlate.getContainerRole(), SBSSection.ALL96, null,
                new LabEvent(LabEventType.INDEXED_ADAPTER_LIGATION, new Date(), "test", 0L, 0L, "test")));
        staticPlateDao.persist(staticPlate);
        Assert.assertFalse(CollectionUtils.isEmpty(staticPlate.getTransfersFrom()));
        messageCollection.clearAll();

        // Deleting the instance should fail since the plate is in use.
        indexedPlateFactory.deleteInstances(Collections.singletonList(plateBarcode), messageCollection);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(IndexedPlateFactory.CANNOT_REMOVE, "", plateBarcode)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertNotNull(staticPlateDao.findByBarcode(plateBarcode), "Failed to delete " + plateBarcode);
        messageCollection.clearAll();

        // Remaking the instance from a new definition should fail since the plate is in use.
        indexedPlateFactory.makeIndexPlate(selectedPlateName2, barcodeSpreadsheet, identifier, true, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().get(0),
                String.format(IndexedPlateFactory.IN_USE, "", plateBarcode),
                StringUtils.join(messageCollection.getErrors(), "; "));

        // Tests finding the instances for the plate definition.
        unusedAndInUse = indexedPlateFactory.findInstances(selectedPlateName1, messageCollection);
        Assert.assertTrue(unusedAndInUse.getLeft().isEmpty());
        Assert.assertTrue(unusedAndInUse.getRight().contains(staticPlate.getLabel()));
    }


    @Test
    public void testEmptySpreadsheetCreate() {
        final String name = dateFormat.format(new Date());
        final MessageCollection messageCollection = new MessageCollection();

        // Plate definition with no content.
        indexedPlateFactory.makeIndexPlateDefinition(name, Collections.emptyList(),
                VesselGeometry.G24x16, IndexPlateDefinition.ReagentType.ADAPTER, false, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().get(0), String.format(IndexedPlateFactory.SPREADSHEET_EMPTY),
                StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Plate instance with no content.
        String selectedName = indexedPlateFactory.plateNameToSelectionName(name,
                IndexPlateDefinition.ReagentType.ADAPTER);
        indexedPlateFactory.makeIndexPlate(selectedName, Collections.emptyList(), name, false, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().get(0), String.format(IndexedPlateFactory.SPREADSHEET_EMPTY),
                StringUtils.join(messageCollection.getErrors(), "; "));

    }


    /** Returns a stream to an excel spreadsheet that contains the cell grid. */
    public InputStream excelSpreadsheet(List<List<String>> cellGrid) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet1 = workbook.createSheet("sheet 1");
        for (int rowIndex = 0; rowIndex < cellGrid.size(); ++rowIndex) {
            Row row = sheet1.createRow(rowIndex);
            for (int colIndex = 0; colIndex < cellGrid.get(rowIndex).size(); ++colIndex) {
                Cell cell = row.createCell(colIndex);
                cell.setCellValue(cellGrid.get(rowIndex).get(colIndex));
            }
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        return new ByteArrayInputStream(stream.toByteArray());
    }
}
