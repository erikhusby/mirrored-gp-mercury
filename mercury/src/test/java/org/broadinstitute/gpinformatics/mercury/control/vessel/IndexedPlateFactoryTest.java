package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

    @Test(enabled = true)
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
        if (staticPlateDao != null && CollectionUtils.isEmpty(misNames)) {
            misNames = staticPlateDao.findAll(MolecularIndexingScheme.class).stream().
                    map(MolecularIndexingScheme::getName).
                    filter(name -> name.startsWith("Illumina") && name.contains("P5") && name.contains("P7")).
                    limit(400).
                    collect(Collectors.toList());
        }
    }

    @Test
    public void testMultipleIndexPlateInstances() throws Exception {
        String identifier = dateFormat.format(new Date());
        // Makes an index plate definition with two wells.
        String plateName = "plateName" + identifier;
        IndexPlateDefinition.ReagentType reagentType = IndexPlateDefinition.ReagentType.PRIMER;
        VesselGeometry vesselGeometry = VesselGeometry.G12x8;
        MessageCollection messageCollection = new MessageCollection();
        List<List<String>> cellGrid = Arrays.asList(Arrays.asList("B02", misNames.get(302)),
                Arrays.asList("C03", misNames.get(303)));
        indexedPlateFactory.makeIndexPlateDefinition(plateName, cellGrid, vesselGeometry,
                reagentType, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        // Instantiates a number of index plates from the definition.
        messageCollection.clearAll();
        final int numberOfPlates = 5;
        List<List<String>> plateBarcodes = IntStream.range(0, numberOfPlates).
                mapToObj(i -> Collections.singletonList("0" + identifier + i)).
                collect(Collectors.toList());
        indexedPlateFactory.makeIndexPlate(plateName, plateBarcodes, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        final IndexPlateDefinition plateDefinition = staticPlateDao.findSingle(IndexPlateDefinition.class,
                IndexPlateDefinition_.definitionName, plateName);
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

            // Ugly but it works.
            Query query = staticPlateDao.getEntityManager().createNativeQuery(
                    "select definition_id from index_plate_instance where lab_vessel = ?");
            query.setParameter(1, staticPlate.getLabVesselId());
            BigDecimal id = (BigDecimal)query.getSingleResult();
            Assert.assertNotNull(id);
            Assert.assertEquals(staticPlateDao.findById(IndexPlateDefinition.class, id.longValueExact()),
                    plateDefinition, "for barcode " + barcode);

            // XXX should work but doesn't
            //Assert.assertEquals(staticPlate.getIndexPlateDefinition(), plateDefinition, "for barcode " + barcode);
        });
    }

    @Test
    public void testDefineAndInstantiateIndexPlate() throws Exception {
        String identifier = dateFormat.format(new Date());

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

        // Creates an index plate definition.
        messageCollection.clearAll();
        indexedPlateFactory.makeIndexPlateDefinition(plateName, spreadsheet, vesselGeometry,
                reagentType, false, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // The definition should now be available.
        Assert.assertTrue(indexedPlateFactory.findPlateDefinitionNames().contains(plateName), "missing " + plateName);

        // Instantiates an index plate.
        messageCollection.clearAll();
        String plateBarcode = identifier;
        indexedPlateFactory.makeIndexPlate(plateName,
                Arrays.asList(Arrays.asList("Barcode"), Arrays.asList(identifier)),
                messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        String zeroFilledBarcode = String.format("%012d", Long.parseLong(plateBarcode));
        StaticPlate staticPlate = staticPlateDao.findByBarcode(zeroFilledBarcode);
        for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
            Collection<SampleInstanceV2> instances = staticPlate.getContainerRole().
                    getSampleInstancesAtPositionV2(VesselPosition.getByName(row.get(0)));
            Assert.assertEquals(instances.size(), 1);
            Assert.assertEquals(instances.iterator().next().getMolecularIndexingScheme().getName(), row.get(1));
        }
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
