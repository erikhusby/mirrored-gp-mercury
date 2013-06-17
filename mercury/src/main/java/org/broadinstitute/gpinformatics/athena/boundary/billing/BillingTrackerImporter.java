package org.broadinstitute.gpinformatics.athena.boundary.billing;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BillingTrackerImporter {

    private final ProductOrderDao productOrderDao;
    private final PriceItemDao priceItemDao;
    private final PriceListCache priceListCache;

    private ValidationErrors validationErrors;

    public BillingTrackerImporter(
        ProductOrderDao productOrderDao, PriceItemDao priceItemDao, PriceListCache priceListCache, ValidationErrors validationErrors) {
        this(productOrderDao, priceItemDao, priceListCache);
        this.validationErrors = validationErrors;
    }

    public BillingTrackerImporter(ProductOrderDao productOrderDao, PriceItemDao priceItemDao, PriceListCache priceListCache) {
        this.productOrderDao = productOrderDao;
        this.priceItemDao = priceItemDao;
        this.priceListCache = priceListCache;
    }

    public Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> parseFileForSummaryMap(
            InputStream inputStream) throws IOException {

        Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> trackerSummaryMap = new HashMap<>();

        try {
            Workbook workbook = WorkbookFactory.create(inputStream);

            checkSampleOrdering(workbook);
            if (!validationErrors.isEmpty()) {
                return null;
            }

            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {

                Sheet sheet = workbook.getSheetAt(i);
                String productPartNumberStr = sheet.getSheetName();

                List<TrackerColumnInfo> trackerHeaderList =
                        BillingTrackerUtils.parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

                // Get a map (by PDOId) of a map of OrderBillSummaryStat objects (by BillableRef) for this sheet.
                Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap =
                        parseSheetForSummaryMap(sheet, trackerHeaderList);
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

    private void checkSampleOrdering(Workbook workbook) {
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
                            " has a non-numeric value is the " + BillingTrackerHeader.SORT_COLUMN_HEADING.getText() +
                            " cell. Please correct and ensure the spreadsheet is ordered by the " +
                            BillingTrackerHeader.SORT_COLUMN_HEADING.getText() + " column heading.";
                    addError(error);
                    return;
                }

                double sortCellVal = sortCell.getNumericCellValue();

                String currentSampleName = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS).getStringCellValue();
                if (!(String.valueOf(sortCellVal)).equals(String.valueOf(expectedSortColValue))) {
                    String error = "Sample " + currentSampleName + " on row " + (row.getRowNum() + 1) +
                                   " of spreadsheet tab " + productPartNumberStr +
                                   " is not in the expected position. Please re-order the spreadsheet by the " +
                                   BillingTrackerHeader.SORT_COLUMN_HEADING.getText() + " column heading.";
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

    private Map<String, Map<BillableRef, OrderBillSummaryStat>> parseSheetForSummaryMap(
            Sheet sheet, List<TrackerColumnInfo> trackerColumnInfos) {

        ProductOrder productOrder = null;
        Product product = null;
        List<ProductOrderSample> samples = null;
        Map<TrackerColumnInfo, PriceItem> priceItemMap = null;
        String primaryProductPartNumber = sheet.getSheetName();
        int maxNumberOfProductsInSheet = trackerColumnInfos.size();
        String currentPdoId = "";

        // A map (by PDO) of maps ( by PPN) of OrderBillSummaryStat objects
        Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap = new HashMap<>();

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

            // For a newly found PDO Id, create a new map for it and add it to the sheet summary map
            if (!currentPdoId.equalsIgnoreCase(rowPdoIdStr) && pdoSummaryStatsMap == null) {
                pdoSummaryStatsMap = new HashMap<>(maxNumberOfProductsInSheet);
                sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);
                currentPdoId = rowPdoIdStr;
                sampleIndexInOrder = 0;

                // Find the order in the DB.
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
                    priceItemMap =
                        BillingTrackerUtils.createPriceItemMapForSheet(
                            trackerColumnInfos, product, priceItemDao, priceListCache);
                }
            }

            // There must always be a sample and a product order, so go to the next line, if this does not get one.
            if ((CollectionUtils.isEmpty(samples)) || (productOrder == null)) {
                break;
            }

            // The order in the spreadsheet is the same as returned in the productOrder.
            if (sampleIndexInOrder >= samples.size()) {
                String error = "Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() +
                        " (Id: " + currentPdoId + ")> has only " + samples.size() + " samples.";
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

            String error =
                parseRowForSummaryMap(
                    row, pdoSummaryStatsMap, trackerColumnInfos, product, productOrderSample, priceItemMap);
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
            int currentBilledPosition = BillingTrackerUtils.FIXED_HEADERS.length + (priceItemIndex * 2);

            // Get the AlreadyBilled cell
            Cell billedCell = row.getCell(currentBilledPosition);
            Cell sampleId = row.getCell(BillingTrackerUtils.SAMPLE_ID_COL_POS);
            Cell pdoId = row.getCell(BillingTrackerUtils.PDO_ID_COL_POS);
            String partNumber = product.getPartNumber();
            String priceItemName = billableRef.getPriceItemName();
            if (BillingTrackerUtils.isNonNullNumericCell(billedCell)) {
                // Check the already billed amount against the DB.
                previouslyBilledQuantity = billedCell.getNumericCellValue();
                PriceItem priceItem = priceItemMap.get( trackerColumnInfos.get(priceItemIndex) );
                // Check billedQuantity parsed against that which is already billed for this sample and PriceItem, they
                // should match.
                ProductOrderSample.LedgerQuantities quantities = billCounts.get(priceItem);
                if ((quantities != null) && !MathUtils.isSame(quantities.getBilled(), previouslyBilledQuantity)) {
                    return String.format(
                            "Found a different billed quantity '%f' in the database for sample in %s in %s, " +
                            "price item '%s', in Product %s. The billed quantity in the spreadsheet is " +
                            "'%f', please download a recent copy of the BillingTracker spreadsheet.",
                            quantities.getBilled(), sampleId, pdoId,
                            priceItemName, partNumber, previouslyBilledQuantity);
                }
                if (quantities == null && previouslyBilledQuantity != 0) {
                    return String.format("No billed quantity found in the database for sample %s in %s, price item " +
                                         "'%s', in Product %s. However the billed quantity in the spreadsheet " +
                                         "is '%f', indicating the Billed column of this spreadsheet has accidentally " +
                                         "been edited.",
                            sampleId, pdoId, priceItemName, partNumber, previouslyBilledQuantity );
                }
            }

            // Get the newQuantity cell value
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if (BillingTrackerUtils.isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();
                if (newQuantity < 0) {
                    return String.format("Found negative new quantity '%f' for sample %s in %s, price item '%s', in Product %s",
                            newQuantity, sampleId, pdoId, priceItemName, partNumber);
                }

                double delta = newQuantity - previouslyBilledQuantity;

                if (delta != 0) {

                    Cell cell = row.getCell(BillingTrackerUtils.QUOTE_ID_COL_POS);
                    if (cell == null || cell.getStringCellValue() == null) {
                        return String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product %s",
                                BillingTrackerHeader.QUOTE_ID_HEADING.getText(), sampleId, pdoId, priceItemName, partNumber);
                    }

                    String uploadedQuoteId = cell.getStringCellValue().trim();
                    if (!productOrderSample.getProductOrder().getQuoteId().equals(uploadedQuoteId)) {
                        return MessageFormat
                                .format("Found quote ID ''{0}'' for updated sample ''{1}'' in ''{2}'' in Product" +
                                        " ''{3}'', this differs from quote ''{4}'' currently associated with ''{2}''.",
                                        uploadedQuoteId, sampleId, pdoId, partNumber,
                                        productOrderSample.getProductOrder().getQuoteId());
                    }


                    cell = row.getCell(BillingTrackerUtils.WORK_COMPLETE_DATE_COL_POS);
                    if (cell == null || cell.getDateCellValue() == null) {
                        return String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product %s",
                                BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText(), sampleId, pdoId,
                                priceItemName, partNumber);
                    }

                    OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(billableRef);
                    if (orderBillSummaryStat == null) {
                        // Create a new stat obj and add it to the map.
                        orderBillSummaryStat = new OrderBillSummaryStat();
                        pdoSummaryStatsMap.put(billableRef, orderBillSummaryStat);
                    }

                    orderBillSummaryStat.applyDelta(delta);
                }
            }
        }

        return "";
    }
}
