package org.broadinstitute.gpinformatics.athena.boundary.billing;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BillingTrackerImporter {
    private static final Log logger = LogFactory.getLog(BillingTrackerImporter.class);

    private ProductOrderDao productOrderDao;
    private BillingLedgerDao billingLedgerDao;

    private ValidationErrors validationErrors;

    public BillingTrackerImporter(ProductOrderDao productOrderDao, BillingLedgerDao billingLedgerDao, ValidationErrors validationErrors) {
        this(productOrderDao);
        this.validationErrors = validationErrors;
        this.billingLedgerDao = billingLedgerDao;
    }

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
            if (!validationErrors.isEmpty()) {
                return null;
            }

            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {

                Sheet sheet = workbook.getSheetAt(i);
                String productPartNumberStr = sheet.getSheetName();

                List<TrackerColumnInfo> trackerHeaderList = BillingTrackerUtils.parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

                // Get a map (by PDOId) of a map of OrderBillSummaryStat objects (by BillableRef) for this sheet.
                Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap = parseSheetForSummaryMap(sheet, trackerHeaderList);
                if (!validationErrors.isEmpty()) {
                    return null;
                }

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
                    String error =
                            "Row " + (row.getRowNum() + 1) + " of spreadsheet tab " + productPartNumberStr +
                            " has a non-numeric value is the " + SampleLedgerExporter.SORT_COLUMN_HEADING +
                            " cell. Please correct and ensure the spreadsheet is ordered by the " +
                            SampleLedgerExporter.SORT_COLUMN_HEADING + " column heading.";
                    addError(error);
                    return;
                }

                double sortCellVal = sortCell.getNumericCellValue();

                String currentSampleName = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS).getStringCellValue();
                if (!(String.valueOf(sortCellVal)).equals(String.valueOf(expectedSortColValue))) {
                    String error = "Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                   " of spreadsheet tab " + productPartNumberStr +
                                   " is not in the expected position. Please re-order the spreadsheet by the " +
                                   SampleLedgerExporter.SORT_COLUMN_HEADING + " column heading.";
                    addError(error);
                    return;
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
                    String error = "Product Order " + currentPdoId + " on row " + (row.getRowNum() + 1) +
                               " of sheet " + primaryProductPartNumber + " is not found in the database.";
                    addError(error);
                    return null;
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
                String error = "Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples.";
                addError(error);
                return null;
            }

            ProductOrderSample productOrderSample = samples.get(sampleIndexInOrder);
            if (!productOrderSample.getSampleName().equals(currentSampleName)) {
                String error = "Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                           " of spreadsheet " + primaryProductPartNumber +
                                           " is in different position than expected. Expected value from Order is "
                                           + productOrderSample.getSampleName();
                addError(error);
                return null;
            }

            String error = parseRowForSummaryMap(row, pdoSummaryStatsMap, trackerColumnInfos, product, productOrderSample, priceItemMap);
            if (!StringUtils.isBlank(error)) {
                addError(error);
                return null;
            }

            sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);

            sampleIndexInOrder++;
        }

        return sheetSummaryMap;
    }

    private void addError(String error) {
        if (validationErrors == null) {
            throw new RuntimeException(error);
        } else {
            validationErrors.addGlobalError(new SimpleError(error));
        }
    }

    private static String parseRowForSummaryMap(
            Row row, Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap,
            List<TrackerColumnInfo> trackerColumnInfos, Product product, ProductOrderSample productOrderSample,
            Map<TrackerColumnInfo, PriceItem> priceItemMap) {

        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = productOrderSample.getLedgerQuantities();

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
                if ((quantities != null) && (quantities.getBilled() != previouslyBilledQuantity)) {
                    return String.format("Found a different billed quantity '%f' in the database for sample in %s in %s, price item '%s', in Product sheet %s. " +
                                    "The billed quantity in the spreadsheet is '%f', please download a recent copy of the BillingTracker spreadsheet.",
                                    quantities.getBilled(), row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber(), previouslyBilledQuantity );
                }
            }

            // Get the newQuantity cell value
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if ( BillingTrackerUtils.isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();
                if (newQuantity < 0) {
                    return String.format("Found negative new quantity '%f' for sample %s in %s, price item '%s', in Product sheet %s",
                                newQuantity, row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber());
                }

                double delta = newQuantity - previouslyBilledQuantity;

                if ( delta != 0 ) {

                    Cell cell = row.getCell(BillingTrackerUtils.WORK_COMPLETE_DATE_COL_POS);
                    if (cell == null || cell.getDateCellValue() == null) {
                        return String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product sheet %s",
                                SampleLedgerExporter.WORK_COMPLETE_DATE_HEADING, row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber());
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

        return "";
    }


    public Map<String, List<ProductOrder>> parseFileForBilling(InputStream fis) throws Exception {

        Map<String, List<ProductOrder>> trackerBillingMap = new HashMap<String, List<ProductOrder>>();

        Workbook workbook = WorkbookFactory.create(fis);

        Map<String, List<String>> productOrderIdMap = getUnlockedProductOrderIdsFromWorkbook(workbook);
        if (!validationErrors.isEmpty()) {
            return null;
        }

        for (String ppnSheetName : productOrderIdMap.keySet()) {
            List<String> productOrderIds = productOrderIdMap.get(ppnSheetName);
            for (String productOrderId : productOrderIds) {
                logger.info("BILLING: About to update ledger for productOrder " + productOrderId + " for ProductPartNumber " + ppnSheetName);
            }
        }

        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {

            Sheet sheet = workbook.getSheetAt(i);
            String productPartNumberStr = sheet.getSheetName();

            List<TrackerColumnInfo> trackerHeaderList = BillingTrackerUtils.parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

            //Get a list of Product Orders for this sheet.
            List<ProductOrder> sheetBillingMap = parseSheetForBilling(sheet, trackerHeaderList);
            if (!validationErrors.isEmpty()) {
                return null;
            }

            trackerBillingMap.put(productPartNumberStr, sheetBillingMap);
        }

        return trackerBillingMap;
    }

    private Map<String, List<String>> getUnlockedProductOrderIdsFromWorkbook(Workbook workbook) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();

        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {

            Sheet sheet = workbook.getSheetAt(i);
            String productPartNumberStr = sheet.getSheetName();

            // Just reparsing for fun and checking that we are dealing with the correct file.
            List<TrackerColumnInfo> trackerHeaderList = BillingTrackerUtils.parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

            //Get a list of Product OrderIds for this sheet.
            List<String> sheetOrderIdMap = extractOrderIdsFromSheet(sheet);

            for (String pdoIdStr : sheetOrderIdMap) {
                // Check if this PDO has any locked Ledger rows
                Set<BillingLedger> lockedBillingLedgerSet = billingLedgerDao.findLockedOutByOrderList(
                        Collections.singletonList(pdoIdStr));
                if (lockedBillingLedgerSet.size() > 0) {
                    addError("Product Order " + pdoIdStr + " of sheet " + productPartNumberStr +
                            " is locked out and has " + lockedBillingLedgerSet.size() + " rows that are locked in the DB.");
                }
            }
            result.put(productPartNumberStr, sheetOrderIdMap);

        }

        return result;
    }

    private List<String> extractOrderIdsFromSheet(Sheet sheet) {
        List<String> result = new ArrayList<String>();

        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();
            if (row.getRowNum() == 0) {
                row = BillingTrackerUtils.skipHeaderRows(rit, row);
            }

            Cell pdoCell = row.getCell(BillingTrackerUtils.PDO_ID_COL_POS);
            if (pdoCell == null) {
                //Break out of this loop since there is no PDO for this row. Assuming at the end of the valued rows.
                break;
            }
            String rowPdoIdStr = pdoCell.getStringCellValue();
            if (!result.contains(rowPdoIdStr)) {
                result.add(rowPdoIdStr);
            }
        }
        return result;
    }

    private List<ProductOrder> parseSheetForBilling(Sheet sheet, List<TrackerColumnInfo> trackerColumnInfos) {
        ProductOrder productOrder = null;
        Product product = null;
        ProductOrderSample productOrderSample = null;
        List<ProductOrderSample> samples = null;
        Map<TrackerColumnInfo, PriceItem> priceItemMap = null;

        String primaryProductPartNumber = sheet.getSheetName();
        String currentPdoId = "";

        List<ProductOrder> sheetBillingMap = new ArrayList<ProductOrder>();
        int sampleIndexInOrder = 0;

        // Iterate over each row in this tab of the spreadsheet.
        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();

            if (row.getRowNum() == 0) {
                row = BillingTrackerUtils.skipHeaderRows(rit, row);
            }

            Cell pdoCell = row.getCell(BillingTrackerUtils.PDO_ID_COL_POS);
            if (pdoCell == null) {
                //Break out of this loop since there is no PDO for this row. Assuming at the end of the valued rows.
                break;
            }

            String rowPdoIdStr = row.getCell(BillingTrackerUtils.PDO_ID_COL_POS).getStringCellValue();
            String currentSampleName = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS).getStringCellValue();

            // For a newly found PdoId create a new map for it and add it to the sheet summary map
            if (!currentPdoId.equalsIgnoreCase(rowPdoIdStr)) {

                //Persist any BillingLedger Items captured for the previous productOrder (if not blank) before changing to the next PDO
                if (StringUtils.isNotBlank(currentPdoId)) {
                    persistProductOrder(productOrder);
                    sheetBillingMap.add(productOrder);
                }

                //Switch to the next orderId
                currentPdoId = rowPdoIdStr;

                sampleIndexInOrder = 0;

                // Find the order in the DB
                productOrder = productOrderDao.findByBusinessKey(currentPdoId);
                if (productOrder == null) {
                    addError("Product Order " + currentPdoId + " on row " + (row.getRowNum() + 1) +
                            " of sheet " + primaryProductPartNumber + " is not found in the database.");
                    return null;
                }

                product = productOrder.getProduct();
                samples = productOrder.getSamples();

                // Find the target priceItems for the data that was parsed from the header, happens one per sheet parse.
                // create a map (by trackerColumnInfo) of PriceItems
                if (priceItemMap == null) {
                    priceItemMap = BillingTrackerUtils.createPriceItemMapForSheet(trackerColumnInfos, product);
                }

                //Remove any pending billable Items from the ledger for all samples in this PDO
                ProductOrder[] productOrderArray = new ProductOrder[]{productOrder};
                billingLedgerDao.removeLedgerItemsWithoutBillingSession(productOrderArray);
                billingLedgerDao.flush();

            }

            if (samples == null) {
                throw new RuntimeException("There should be no way to have no sample or product order when reading tracker");
            }

            //TODO hmc We are assuming ( for now ) that the order is the same in the spreadsheet as returned in the productOrder !
            if (sampleIndexInOrder >= samples.size()) {
                addError("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                        " of spreadsheet " + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples.");
                return null;
            }

            productOrderSample = samples.get(sampleIndexInOrder);
            if (!productOrderSample.getSampleName().equals(currentSampleName)) {
                addError("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                        " of spreadsheet " + primaryProductPartNumber +
                        " is in different position than expected. Expected value from Order is " + productOrderSample.getSampleName());
                return null;
            }

            // Create a list of BillingLedger objs for this ProductOrderSample that is part of the current PDO.
            parseSampleRowForBilling(row, productOrderSample, product, trackerColumnInfos, priceItemMap);
            if (!validationErrors.isEmpty()) {
                return null;
            }

            sampleIndexInOrder++;

        }

        //Persist any BillingLedger Items captured for the last productOrder (if not blank) before finishing the sheet
        if (StringUtils.isNotBlank(currentPdoId)) {
            persistProductOrder(productOrder);
            sheetBillingMap.add(productOrder);
        }

        return sheetBillingMap;
    }

    private void parseSampleRowForBilling(Row row, ProductOrderSample productOrderSample, Product product,
                                          List<TrackerColumnInfo> trackerColumnInfos,
                                          Map<TrackerColumnInfo, PriceItem> priceItemMap) {

        // Get the date complete cell for changes
        Cell workCompleteDateCell = row.getCell(BillingTrackerUtils.WORK_COMPLETE_DATE_COL_POS);
        Date workCompleteDate = null;
        if (isNonNullDateCell(workCompleteDateCell)) {
            workCompleteDate = workCompleteDateCell.getDateCellValue();
        }

        for (int billingRefIndex = 0; billingRefIndex < trackerColumnInfos.size(); billingRefIndex++) {
            TrackerColumnInfo trackerColumnInfo = trackerColumnInfos.get(billingRefIndex);
            BillableRef billableRef = trackerColumnInfos.get(billingRefIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = BillingTrackerUtils.fixedHeaders.length + (billingRefIndex * 2);

            //Get the AlreadyBilled cell and amount
            Cell billedCell = row.getCell(currentBilledPosition);
            Double billedQuantity = getCellValueAsNonNullDouble(row, productOrderSample, product, billedCell);

            //Get the newQuantity cell and amount
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            Double newQuantity = getCellValueAsNonNullDouble(row, productOrderSample, product, newQuantityCell);

            if (!validationErrors.isEmpty()) {
                return;
            }

            if ( (billedQuantity != null ) && (newQuantity != null) ) {

                //Calculate the delta, quantities are non-null here
                double delta = newQuantity - billedQuantity;

                if (delta != 0) {
                    PriceItem priceItem = priceItemMap.get(trackerColumnInfo);

                    // Only need to check date existence when newQuantity is different than Billed Quantity
                    if (workCompleteDate == null) {
                        addError("Sample " + productOrderSample.getSampleName() + " on row " + (row.getRowNum() + 1) +
                                " of spreadsheet " + product.getPartNumber() +
                                " has an invalid Date Completed value. Please correct and try again.");
                        return;
                    } else {
                        productOrderSample.addLedgerItem(workCompleteDate, priceItem, delta);
                    }
                }
            }
        }
    }

    private Double getCellValueAsNonNullDouble(
            Row row, ProductOrderSample productOrderSample, Product product, Cell cell) {
        Double quantity = null;
        if (BillingTrackerUtils.isNonNullNumericCell(cell)) {
            quantity = cell.getNumericCellValue();
            if (quantity == null) {
                addError("Sample " + productOrderSample.getSampleName() + " on row " + (row.getRowNum() + 1) +
                        " of spreadsheet " + product.getPartNumber() +
                        " has a blank value. Please re-download the tracker to populate this.");
            }
        }

        return quantity;
    }

    private void persistProductOrder(ProductOrder productOrder) {

        Set<BillingLedger> billableLedgerItems = new HashSet<BillingLedger>();

        try {
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                billableLedgerItems.addAll(productOrderSample.getBillableLedgerItems());
            }

            // No need to persist if there are no ledger Items for this order.
            if (!billableLedgerItems.isEmpty()) {
                productOrderDao.persist(productOrder);
                productOrderDao.flush();

                logger.info("Persisted " + billableLedgerItems.size() + " BillingLedger records for Product Order <" +
                        productOrder.getTitle() + "> with PDO Id: " + productOrder.getBusinessKey());
            }
        } catch (RuntimeException e) {
            logger.error("Exception when persisting " + billableLedgerItems.size() +
                    " BillingLedger items for Product Order <" + productOrder.getTitle() +
                    "> with PDO Id: " + productOrder.getBusinessKey(), e);
            throw e;
        }
    }

    private boolean isNonNullDateCell(Cell cell) {
        return ((cell != null) && (HSSFDateUtil.isCellDateFormatted(cell)));
    }

}
