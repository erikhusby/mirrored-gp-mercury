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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test creation of plates
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

    @BeforeTest
    private void beforeTest() {
        if (CollectionUtils.isEmpty(misNames)) {
            misNames = staticPlateDao.findAll(MolecularIndexingScheme.class).stream().
                    map(MolecularIndexingScheme::getName).
                    filter(name -> name.startsWith("Illumina") && name.contains("P5") && name.contains("P7")).
                    limit(400).
                    collect(Collectors.toList());
        }
    }

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

    @Test
    public void testDefineAndInstantiateIndexPlate() throws Exception {
        String identifier = dateFormat.format(new Date());

        // Makes a spreadsheet consisting of a header and 384 rows of plate well positions and mis names.
        VesselGeometry vesselGeometry = VesselGeometry.G24x16;
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
        String plateName = "plateName" + identifier;
        indexedPlateFactory.makeIndexPlateDefinition(plateName, spreadsheet, vesselGeometry,
                IndexPlateDefinition.ReagentType.ADAPTER, false, messageCollection);
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
        Map<String, Map<String, String>> queryResult = indexPlatesForBarcode(zeroFilledBarcode);
        Assert.assertEquals(queryResult.keySet().size(), 1);
        // Positions and mis names should match.
        Assert.assertEquals(queryResult.get(zeroFilledBarcode).entrySet().stream().
                        map(mapEntry -> mapEntry.getKey() + " " + mapEntry.getValue()).
                        sorted().collect(Collectors.joining(" ")),
                cellGrid.subList(1, cellGrid.size()).stream().flatMap(List::stream).
                        collect(Collectors.joining(" ")));
    }

    /**
     * Returns plate barcode, well name, and mis name for the given plate barcode.
     */
    private Map<String, Map<String, String>> indexPlatesForBarcode(String plateBarcode) {
        Map<String, Map<String, String>> barcodePositionMisname = new HashMap<>();
        String queryString =
                "select lv.label, lm.mapkey, mis.name " +
                        "from lab_vessel lv " +
                        "join lv_map_position_to_vessel lm on lm.lab_vessel = lv.lab_vessel_id " +
                        "join lab_vessel well on well.lab_vessel_id = lm.map_position_to_vessel " +
                        "join lv_reagent_contents lr on lr.lab_vessel = well.lab_vessel_id " +
                        "join reagent r on r.reagent_id = lr.reagent_contents " +
                        "join molecular_indexing_scheme mis on " +
                        "      mis.molecular_indexing_scheme_id = r.molecular_indexing_scheme " +
                        "where lv.label = '" + plateBarcode + "' " +
                        "order by 1, 2";
        List<Object[]> resultRows = staticPlateDao.getEntityManager().createNativeQuery(queryString).getResultList();
        for (Object[] resultRow : resultRows) {
            String barcode = (String) resultRow[0];
            Map<String, String> positionMisname = barcodePositionMisname.get(barcode);
            if (positionMisname == null) {
                positionMisname = new HashMap<>();
                barcodePositionMisname.put(barcode, positionMisname);
            }
            positionMisname.put((String) resultRow[1], (String) resultRow[2]);
        }
        return barcodePositionMisname;
    }

    private InputStream excelSpreadsheet(List<List<String>> cellGrid) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet1 = workbook.createSheet("sheet 1");
        int rowIndex = 0;
        for (List<String> rowValues : cellGrid) {
            Row row = sheet1.createRow(rowIndex++);
            int colIndex = 0;
            for (String value : rowValues) {
                Cell cell = row.createCell(colIndex++);
                cell.setCellValue(value);
            }
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        return new ByteArrayInputStream(stream.toByteArray());
    }

}
