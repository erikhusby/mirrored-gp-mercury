package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class BillingTrackerImporter {

    private static final Log logger = LogFactory.getLog(BillingTrackerImporter.class);
    private ProductOrderDao productOrderDao;

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

                List<TrackerColumnInfo> trackerHeaderList = BillingTrackerUtils.parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

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
                    row = BillingTrackerUtils.skipHeaderRows(rows, row);
                }

                sortCell = row.getCell(BillingTrackerUtils.SORT_COLUMN_COL_POS);
                if (sortCell == null) {
                    // Break out of this loop since there is no sort num value for this row.
                    // Assuming at the end of the valued rows.
                    break;
                }

                if (!BillingTrackerUtils.isNonNullNumericCell( sortCell)) {
                    throw new RuntimeException("Row " + (row.getRowNum() + 1) +
                                               " of spreadsheet tab " + productPartNumberStr
                                               + " has a non-numeric value is the " +
                                               SampleLedgerExporter.SORT_COLUMN_HEADING
                                               + " cell. Please correct and ensure the spreadsheet is ordered by the " +
                                               SampleLedgerExporter.SORT_COLUMN_HEADING + " column heading.");
                }
                double sortCellVal = sortCell.getNumericCellValue();

                String currentSampleName = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS).getStringCellValue();
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
        Map<TrackerColumnInfo, PriceItem> priceItemMap = null;
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
                row = BillingTrackerUtils.skipHeaderRows(rows, row);
            }

            Cell pdoCell = row.getCell(BillingTrackerUtils.PDO_ID_COL_POS);
            if (pdoCell == null) {
                // Break out of this loop since there is no PDO for this row. Assuming at the end of the valued rows.
                break;
            }

            String rowPdoIdStr = pdoCell.getStringCellValue();
            String currentSampleName = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS).getStringCellValue();
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
                if (priceItemMap == null) {
                    priceItemMap = BillingTrackerUtils.createPriceItemMapForSheet(trackerColumnInfos, product);
                }

            }

            // TODO hmc We are assuming ( for now ) that the order is the same
            // in the spreadsheet as returned in the productOrder !
            if (sampleIndexInOrder >= samples.size()) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples." );
            }

            ProductOrderSample productOrderSample = samples.get(sampleIndexInOrder);
            if (!productOrderSample.getSampleName().equals(currentSampleName)) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                           " of spreadsheet " + primaryProductPartNumber +
                                           " is in different position than expected. Expected value from Order is "
                                           + productOrderSample.getSampleName());
            }

            parseRowForSummaryMap(row, pdoSummaryStatsMap, trackerColumnInfos, product, productOrderSample, priceItemMap);
            sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);

            sampleIndexInOrder++;
        }

        return sheetSummaryMap;
    }

    private void parseRowForSummaryMap(
            Row row, Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap,
            List<TrackerColumnInfo> trackerColumnInfos, Product product, ProductOrderSample productOrderSample,
            Map<TrackerColumnInfo, PriceItem> priceItemMap ) {

        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = ProductOrderSample.getLedgerQuantities(productOrderSample);

        for (int priceItemIndex = 0; priceItemIndex < trackerColumnInfos.size(); priceItemIndex++) {
            double newQuantity;
            double previouslyBilledQuantity = 0;
            BillableRef billableRef = trackerColumnInfos.get(priceItemIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = BillingTrackerUtils.fixedHeaders.length + (priceItemIndex * 2);

            // Get the AlreadyBilled cell
            Cell billedCell = row.getCell(currentBilledPosition);
            if (BillingTrackerUtils.isNonNullNumericCell(billedCell)) {
                // Check the already billed amount against the DB.
                previouslyBilledQuantity = billedCell.getNumericCellValue();
                PriceItem priceItem = priceItemMap.get( trackerColumnInfos.get(priceItemIndex) );
                // Check billedQuantity parsed against that which is already billed for this POS and PriceItem - should match
                ProductOrderSample.LedgerQuantities quantities = billCounts.get(priceItem);
                if ((quantities != null ) && (quantities.getBilled() != previouslyBilledQuantity)) {
                    throw new RuntimeException(
                            String.format("Found a different billed quantity '%f' in the database for sample in %s in %s, price item '%s', in Product sheet %s. " +
                                    "The billed quantity in the spreadsheet is '%f', please download a recent copy of the BillingTracker spreadsheet.",
                                    quantities.getBilled(), row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber(), previouslyBilledQuantity ));
                }
            }

            // Get the newQuantity cell value
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if ( BillingTrackerUtils.isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();
                if (newQuantity < 0) {
                    throw new RuntimeException(
                            String.format("Found negative new quantity '%f' for sample %s in %s, price item '%s', in Product sheet %s",
                                newQuantity, row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber()));
                }
                double delta = newQuantity - previouslyBilledQuantity;

                if ( delta != 0 ) {

                    Cell cell = row.getCell(BillingTrackerUtils.WORK_COMPLETE_DATE_COL_POS);
                    if (cell == null || cell.getDateCellValue() == null) {
                        throw new RuntimeException(String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product sheet %s",
                                SampleLedgerExporter.WORK_COMPLETE_DATE_HEADING, row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
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
        }
    }


    public File copyFromStreamToTempFile(InputStream is) throws IOException {

        Date now = new Date();
        File tempFile = File.createTempFile("BillingTrackerTempFile_" + now.getTime(), ".xls");

        OutputStream out = new FileOutputStream(tempFile);

        try {
            IOUtils.copy(is, out);

            logger.info("New file created!");
        } catch (IOException e) {
            logger.error(e);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return tempFile.getAbsoluteFile();
    }


}
