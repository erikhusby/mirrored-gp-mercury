package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

/**
 *
 */
@Stateful
@RequestScoped
public class BillingTrackerManager {

    private static final Log logger = LogFactory.getLog(BillingTrackerManager.class);

    @Inject
    BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductOrderDao productOrderDao;

    public Map<String, List<ProductOrder>> parseFileForBilling(InputStream fis) throws Exception {

        Map<String, List<ProductOrder>> trackerBillingMap = new HashMap<String, List<ProductOrder>>();

        Workbook workbook = WorkbookFactory.create(fis);

        // Could use this map later on during parsing.
        Map<String, List<String>> productOrderIdMap = getUnlockedProductOrderIdsFromWorkbook(workbook);
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
                if (!lockedBillingLedgerSet.isEmpty()) {
                    throw BillingTrackerUtils.getRuntimeException("Product Order " + pdoIdStr + " of sheet " + productPartNumberStr +
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
        ProductOrderSample productOrderSample;
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
                    throw BillingTrackerUtils.getRuntimeException("Product Order " + currentPdoId + " on row " + (row.getRowNum() + 1) +
                            " of sheet " + primaryProductPartNumber + " is not found in the database.");
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

            // The samples in the order must be the same as the spreadsheet samples, so the size must be the same
            assert samples != null;
            if (sampleIndexInOrder >= samples.size()) {
                throw BillingTrackerUtils.getRuntimeException("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                        " of spreadsheet " + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples.");
            }

            productOrderSample = samples.get(sampleIndexInOrder);
            if (!productOrderSample.getSampleName().equals(currentSampleName)) {
                throw BillingTrackerUtils.getRuntimeException("Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                        " of spreadsheet " + primaryProductPartNumber +
                        " is in different position than expected. Expected value from Order is " + productOrderSample.getSampleName());
            }

            // Create a list of BillingLedger objs for this ProductOrderSample that is part of the current PDO.
            parseSampleRowForBilling(row, productOrderSample, product, trackerColumnInfos, priceItemMap);

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

        // Get the date complete cell for changes.
        Cell workCompleteDateCell = row.getCell(BillingTrackerUtils.WORK_COMPLETE_DATE_COL_POS);
        Date workCompleteDate = null;
        if (isNonNullDateCell(workCompleteDateCell)) {
            workCompleteDate = workCompleteDateCell.getDateCellValue();
        }

        for (int billingRefIndex = 0; billingRefIndex < trackerColumnInfos.size(); billingRefIndex++) {
            TrackerColumnInfo trackerColumnInfo = trackerColumnInfos.get(billingRefIndex);
            BillableRef billableRef = trackerColumnInfos.get(billingRefIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = BillingTrackerUtils.FIXED_HEADERS.length + (billingRefIndex * 2);

            // Get the AlreadyBilled cell and amount.
            Cell billedCell = row.getCell(currentBilledPosition);
            Double billedQuantity = getCellValueAsNonNullDouble(row, productOrderSample, product, billedCell);

            // Get the newQuantity cell and amount.
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            Double newQuantity = getCellValueAsNonNullDouble(row, productOrderSample, product, newQuantityCell);

            if ((billedQuantity != null) && (newQuantity != null)) {

                // Calculate the delta, quantities are non-null here.
                double delta = newQuantity - billedQuantity;

                if (delta != 0) {
                    PriceItem priceItem = priceItemMap.get(trackerColumnInfo);

                    // This is exactly the same validation as in the preview, a refactoring is in order.
                    Cell cell = row.getCell(BillingTrackerUtils.QUOTE_ID_COL_POS);
                    if (cell == null || cell.getStringCellValue() == null) {
                        throw BillingTrackerUtils.getRuntimeException(String.format(
                                "Found empty %s value for updated sample %s in %s, price item '%s', in Product sheet %s",
                                BillingTrackerUtils.QUOTE_ID_HEADING, row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS), row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                billableRef.getPriceItemName(), product.getPartNumber()));
                    }

                    String uploadedQuoteId = cell.getStringCellValue().trim();
                    if (!productOrderSample.getProductOrder().getQuoteId().equals(uploadedQuoteId)) {
                        throw BillingTrackerUtils.getRuntimeException(MessageFormat
                                .format("Found quote ID ''{0}'' for updated sample ''{1}'' in ''{2}'' in Product sheet ''{3}'', this differs from quote ''{4}'' currently associated with ''{2}''.",
                                        uploadedQuoteId,
                                        row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS),
                                        row.getCell(BillingTrackerUtils.PDO_ID_COL_POS),
                                        product.getPartNumber(),
                                        productOrderSample.getProductOrder().getQuoteId()));
                    }

                    // Only need to check date existence when newQuantity is different than Billed Quantity.
                    if (workCompleteDate == null) {
                        throw BillingTrackerUtils.getRuntimeException("Sample " + productOrderSample.getSampleName() + " on row " + (row.getRowNum() + 1) +
                                " of spreadsheet " + product.getPartNumber() +
                                " has an invalid Date Completed value. Please correct and try again.");
                    } else {
                        productOrderSample.addLedgerItem(workCompleteDate, priceItem, delta);
                    }
                } else {
                    logger.debug("Skipping BillingLedger item for sample " + productOrderSample.getSampleName() +
                            " to PDO " + productOrderSample.getProductOrder().getBusinessKey() +
                            " for PriceItemName[PPN]: " + billableRef.getPriceItemName() + "[" +
                            billableRef.getProductPartNumber() + "] - quantity:" + newQuantity + " same as Billed amount.");
                }
            }
        }
    }

    private Double getCellValueAsNonNullDouble(Row row, ProductOrderSample productOrderSample, Product product,
                                                      Cell cell) {
        Double quantity = null;
        if (BillingTrackerUtils.isNonNullNumericCell(cell)) {
            quantity = cell.getNumericCellValue();
            if (quantity == null) {
                throw BillingTrackerUtils.getRuntimeException(
                        "Sample " + productOrderSample.getSampleName() + " on row " + (row.getRowNum() + 1) +
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
