package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.inject.Inject;
import java.io.*;
import java.util.*;

/**
 *
 */
public class BillingTrackerImporter {

    @Inject
    Log log;

    private ProductOrderDao productOrderDao;

    private static final String[] fixedHeaders = SampleLedgerExporter.FIXED_HEADERS;
    private static final Map<String,Integer> headerColumnIndices = new HashMap<String, Integer>();
    static {
        for (int i = 0; i < fixedHeaders.length; i++) {
            headerColumnIndices.put(fixedHeaders[i], i);
        }
    }
    private final static int SAMPLE_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.SAMPLE_ID_HEADING);
    private final static int PDO_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.ORDER_ID_HEADING);
    private final static int SORT_COLUMN_COL_POS = headerColumnIndices.get( SampleLedgerExporter.SORT_COLUMN_HEADING);
    private final static int WORK_COMPLETE_DATE_COL_POS = headerColumnIndices.get( SampleLedgerExporter.WORK_COMPLETE_DATE_HEADING);
    private final static int numberOfHeaderRows = 2;


    public BillingTrackerImporter(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }


    public Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> parseFileForSummaryMap(
            InputStream inputStream) throws IOException {

        Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> trackerSummaryMap =
                new HashMap<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>>();

        Workbook workbook;
        try {
            workbook = WorkbookFactory.create(inputStream);

            checkSampleOrdering(workbook);

            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {

                Sheet sheet = workbook.getSheetAt(i);
                String productPartNumberStr = sheet.getSheetName();

                List<TrackerColumnInfo> trackerHeaderList = parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

                // Get a map (by PDOId) of a map of OrderBillSummaryStat objects (by BillableRef) for this sheet.
                Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap =
                        parseSheetForSummaryMap(sheet, trackerHeaderList);
                trackerSummaryMap.put(productPartNumberStr, sheetSummaryMap);

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return trackerSummaryMap;

    }

    void checkSampleOrdering(Workbook workbook) {
        Cell sortCell = null;

        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            double expectedSortColValue = 1;

            Sheet sheet = workbook.getSheetAt(i);
            String productPartNumberStr = sheet.getSheetName();

            Iterator<Row> rows = sheet.rowIterator();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (row.getRowNum() == 0) {
                    row = skipHeaderRows(rows, row);
                }

                sortCell = row.getCell(SORT_COLUMN_COL_POS);
                if (sortCell == null) {
                    // Break out of this loop since there is no sort num value for this row.
                    // Assuming at the end of the valued rows.
                    break;
                }

                if (!isNonNullNumericCell(sortCell)) {
                    throw new RuntimeException("Row " + (row.getRowNum() + 1) +
                                               " of spreadsheet tab " + productPartNumberStr
                                               + " has a non-numeric value is the " +
                                               SampleLedgerExporter.SORT_COLUMN_HEADING
                                               + " cell. Please correct and ensure the spreadsheet is ordered by the " +
                                               SampleLedgerExporter.SORT_COLUMN_HEADING + " column heading.");
                }
                double sortCellVal = sortCell.getNumericCellValue();

                String currentSampleName = row.getCell(SAMPLE_ID_COL_POS).getStringCellValue();
                if (!(String.valueOf(sortCellVal)).equals(String.valueOf(expectedSortColValue))) {
                    throw new RuntimeException("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                               " of spreadsheet tab " + productPartNumberStr
                                               + " is not in the expected position. Please re-order the spreadsheet by the "
                                               + SampleLedgerExporter.SORT_COLUMN_HEADING + " column heading.");
                }
                expectedSortColValue = expectedSortColValue + 1;
            }

            if (sortCell == null) {
                // Break out of this loop since there is no sort num value for this row.
                // Assuming at the end of the valued rows.
                break;
            }
        }
    }

    Map<String, Map<BillableRef, OrderBillSummaryStat>> parseSheetForSummaryMap(Sheet sheet, List<TrackerColumnInfo> trackerColumnInfos) {
        ProductOrder productOrder = null;
        Product product = null;
        List<ProductOrderSample> samples = null;

        String primaryProductPartNumber = sheet.getSheetName();
        int maxNumberOfProductsInSheet = trackerColumnInfos.size();
        String currentPdoId = "";

        // A map (by PDO) of maps ( by PPN) of OrderBillSummaryStat objects
        Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap =
                new HashMap<String, Map<BillableRef, OrderBillSummaryStat>>();

        int sampleIndexInOrder = 0;

        Iterator<Row> rows = sheet.rowIterator();
        while (rows.hasNext()) {
            Row row = rows.next();

            if (row.getRowNum() == 0) {
                row = skipHeaderRows(rows, row);
            }

            Cell pdoCell = row.getCell(PDO_ID_COL_POS);
            if (pdoCell == null) {
                // Break out of this loop since there is no PDO for this row. Assuming at the end of the valued rows.
                break;
            }

            String rowPdoIdStr = pdoCell.getStringCellValue();
            String currentSampleName = row.getCell(SAMPLE_ID_COL_POS).getStringCellValue();
            Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap  = sheetSummaryMap.get(rowPdoIdStr);

            // For a newly found PdoId create a new map for it and add it to the sheet summary map
            if (!currentPdoId.equalsIgnoreCase(rowPdoIdStr) && pdoSummaryStatsMap == null) {
                pdoSummaryStatsMap = new HashMap<BillableRef, OrderBillSummaryStat>(maxNumberOfProductsInSheet);
                sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);
                currentPdoId = rowPdoIdStr;
                sampleIndexInOrder = 0;

                // Find the order in the DB
                productOrder = productOrderDao.findByBusinessKey(currentPdoId);
                if (productOrder == null) {
                    throw new RuntimeException("Product Order " + currentPdoId + " on row " + (row.getRowNum() + 1) +
                                               " of sheet " + primaryProductPartNumber
                                               + " is not found in the database.");
                }

                product = productOrder.getProduct();
                samples = productOrder.getSamples();

            }

            // TODO hmc We are assuming ( for now ) that the order is the same
            // in the spreadsheet as returned in the productOrder !
            if (sampleIndexInOrder >= samples.size()) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples." );
            }

            ProductOrderSample productOrderSample = samples.get(sampleIndexInOrder++);
            if (!productOrderSample.getSampleName().equals(currentSampleName)) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                           " of spreadsheet " + primaryProductPartNumber +
                                           " is in different position than expected. Expected value from Order is "
                                           + productOrderSample.getSampleName());
            }

            parseRowForSummaryMap(row, pdoSummaryStatsMap, trackerColumnInfos, product);
            sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);
        }

        return sheetSummaryMap;
    }

    private void parseRowForSummaryMap(
            Row row, Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap,
            List<TrackerColumnInfo> trackerColumnInfos, Product product) {


        for (int priceItemIndex = 0; priceItemIndex < trackerColumnInfos.size(); priceItemIndex++) {
            double newQuantity;
            double previouslyBilledQuantity = 0;
            BillableRef billableRef = trackerColumnInfos.get(priceItemIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = fixedHeaders.length + (priceItemIndex * 2);

            // Get the AlreadyBilled cell
            Cell billedCell = row.getCell(currentBilledPosition);
            if (isNonNullNumericCell(billedCell)) {
                previouslyBilledQuantity = billedCell.getNumericCellValue();
                //TODO hmc need the AlreadyBilled validation check here per GPLIM-451.
                // Check billedQuantity parsed against that which is already billed for this POS and PriceItem - should match
                //TODO Sum the already billed amount for this sample
            }

            // Get the newQuantity cell value
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if (isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();

                if (newQuantity < 0) {
                    throw new RuntimeException(
                            String.format("Found negative new quantity '%f' for sample %s in %s, price item '%s', in Product sheet %s",
                                newQuantity, row.getCell(SAMPLE_ID_COL_POS), row.getCell(PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber()));
                }

                //TODO Get the actual value from the DB for this POS to calculate the delta  !!!!!!!!!

                double delta = newQuantity - previouslyBilledQuantity;

                if ( delta != 0 ) {

                    Cell cell = row.getCell(WORK_COMPLETE_DATE_COL_POS);
                    if (cell == null || cell.getDateCellValue() == null) {
                        throw new RuntimeException(String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product sheet %s",
                                SampleLedgerExporter.WORK_COMPLETE_DATE_HEADING, row.getCell(SAMPLE_ID_COL_POS), row.getCell(PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber()));
                    }

                    OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(billableRef);
                    if (orderBillSummaryStat == null) {
                        // create a new stat obj and add it to the map
                        orderBillSummaryStat = new OrderBillSummaryStat();
                        pdoSummaryStatsMap.put(billableRef, orderBillSummaryStat);
                    }

                    orderBillSummaryStat.applyDelta(delta);
                }
            }
        } // end of for each product loop
    }



    private boolean isNonNullNumericCell(Cell cell) {
        return (cell != null ) && ( Cell.CELL_TYPE_NUMERIC == cell.getCellType());
    }


    private Row skipHeaderRows(Iterator<Row> rit,  Row row) {
        Row newRow=row;
        // skip the 3 header rows
        for (int i = 0; i < numberOfHeaderRows; i++) {
            newRow = rit.next();
        }
        return newRow;
    }


    List<TrackerColumnInfo> parseTrackerSheetHeader(Row headerRow, String primaryProductPartNumber) {
        int numFixedHeaders = fixedHeaders.length;

        // Check header names. Should match what we write.
        Iterator<Cell> cells = headerRow.cellIterator();
        for (String fixedHeader : fixedHeaders) {
            Cell cell = cells.next();
            if ((cell == null)
                || StringUtils.isBlank(cell.getStringCellValue())
                || !cell.getStringCellValue().equals(fixedHeader)) {
                String cellValFound = (cell == null) ? "" : cell.getStringCellValue();
                throw new RuntimeException("Tracker Sheet Header mismatch.  Expected : " +
                                           fixedHeader + " but found " + cellValFound);
            }
        }

        List<String> columnHeaders = new ArrayList<String>();
        for (Iterator<Cell> cit = headerRow.cellIterator(); cit.hasNext(); ) {
            Cell cell = cit.next();
            if ((cell != null) && StringUtils.isNotBlank(cell.getStringCellValue())) {
                columnHeaders.add(cell.getStringCellValue());
            }
        }

        // Check for minimum columns.
        if (columnHeaders.size() < (numFixedHeaders + 1)) {
            throw new RuntimeException("Tracker Sheet Header mismatch.  Expected at least <" +
                                       (numFixedHeaders + 1) + "> non-null header columns but found only "
                                       + columnHeaders.size()
                                       + " in tab " + primaryProductPartNumber);
        }

        // Check for primary product part number in the header row.
        Cell primaryProductHeaderCell = headerRow.getCell(numFixedHeaders);
        if (!primaryProductHeaderCell.getStringCellValue().contains(primaryProductPartNumber)) {
            throw new RuntimeException("Tracker Sheet Header mismatch.  Expected Primary Product PartNumber <" +
                                       primaryProductPartNumber + "> in the first row and cell position "
                                       + primaryProductHeaderCell.getColumnIndex());
        }

        //Derive the list of TrackerColumnInfo objects, and skip the comments and billing errors at the end
        int totalProductsHeaders = columnHeaders.size() - numFixedHeaders - 2;

        List<TrackerColumnInfo> result = new ArrayList<TrackerColumnInfo>();
        int mergedCellAddOn = 0;
        for (int i = 0; i < totalProductsHeaders; i++) {
            Cell cell = headerRow.getCell(i + numFixedHeaders + mergedCellAddOn);
            if (StringUtils.isNotBlank(cell.getStringCellValue())) {
                BillableRef billableRef = extractBillableRefFromHeader(cell.getStringCellValue());
                TrackerColumnInfo columnInfo = new TrackerColumnInfo(billableRef, i + numFixedHeaders);
                result.add(columnInfo);
                mergedCellAddOn++;
            }
        }

        return result;
    }

    BillableRef extractBillableRefFromHeader(String cellValueStr) {
        String productPartNumber = "";
        String priceItemName = "";

        if (StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0) || (startPos < 0) || !(startPos + 1 < endPos)) {
            throw new RuntimeException(
                    "Tracker Sheet Header Format Error.  Could not find product partNumber "
                    + "in column header. Column header contained: <"
                    + cellValueStr + ">");
        }
        productPartNumber = cellValueStr.substring(startPos + 1, endPos);
        // Substring from char position 0 to position before separating space char.
        priceItemName = cellValueStr.substring(0, startPos - 1);

        return new BillableRef(productPartNumber, priceItemName);
    }


    public File copyFromStreamToTempFile(InputStream is) throws IOException {

        Date now = new Date();
        File tempFile = File.createTempFile("BillingTrackerTempFile_" + now.getTime(), ".xls");

        OutputStream out = new FileOutputStream(tempFile);

        try {
            IOUtils.copy(is, out);

            log.info("New file created!");
        } catch (IOException e) {
            log.error(e);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return tempFile.getAbsoluteFile();
    }


    static class TrackerColumnInfo {
        private final BillableRef billableRef;
        private final int columnIndex;

        private TrackerColumnInfo(BillableRef billableRef, int columnIndex) {
            this.billableRef = billableRef;
            this.columnIndex = columnIndex;
        }

        public BillableRef getBillableRef() {
            return billableRef;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TrackerColumnInfo)) {
                return false;
            }

            TrackerColumnInfo that = (TrackerColumnInfo) o;

            return columnIndex == that.columnIndex && billableRef.equals(that.billableRef);
        }

        @Override
        public int hashCode() {
            int result = billableRef.hashCode();
            result = 31 * result + columnIndex;
            return result;
        }
    }
}
