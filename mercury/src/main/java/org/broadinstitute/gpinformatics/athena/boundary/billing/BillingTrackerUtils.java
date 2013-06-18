package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import java.util.*;

public class BillingTrackerUtils {

    private static final Log logger = LogFactory.getLog(BillingTrackerUtils.class);

    public static final String[] FIXED_HEADERS = {
            BillingTrackerHeader.SAMPLE_ID_HEADING.getText(),
            BillingTrackerHeader.COLLABORATOR_SAMPLE_ID.getText(),
            BillingTrackerHeader.MATERIAL_TYPE.getText(),
            BillingTrackerHeader.ON_RISK.getText(),
            BillingTrackerHeader.STATUS.getText(),
            BillingTrackerHeader.PRODUCT_NAME.getText(),
            BillingTrackerHeader.ORDER_ID_HEADING.getText(),
            BillingTrackerHeader.PRODUCT_ORDER_NAME.getText(),
            BillingTrackerHeader.PROJECT_MANAGER.getText(),
            BillingTrackerHeader.AUTO_LEDGER_TIMESTAMP_HEADING.getText(),
            BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText(),
            BillingTrackerHeader.QUOTE_ID_HEADING.getText(),
            BillingTrackerHeader.SORT_COLUMN_HEADING.getText()
    };

    public static final int SAMPLE_ID_COL_POS = ArrayUtils.indexOf(FIXED_HEADERS,
            BillingTrackerHeader.SAMPLE_ID_HEADING.getText());
    public static final int PDO_ID_COL_POS = ArrayUtils.indexOf(FIXED_HEADERS,
            BillingTrackerHeader.ORDER_ID_HEADING.getText());
    public static final int SORT_COLUMN_COL_POS = ArrayUtils.indexOf(FIXED_HEADERS,
            BillingTrackerHeader.SORT_COLUMN_HEADING.getText());
    public static final int WORK_COMPLETE_DATE_COL_POS = ArrayUtils.indexOf(FIXED_HEADERS,
            BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING);
    public static final int QUOTE_ID_COL_POS = ArrayUtils.indexOf(FIXED_HEADERS,
            BillingTrackerHeader.QUOTE_ID_HEADING);
    public static final int NUMBER_OF_HEADER_ROWS = 2;

    public static boolean isNonNullNumericCell(Cell cell) {
        return (cell != null) && (Cell.CELL_TYPE_NUMERIC == cell.getCellType());
    }

    public static Row skipHeaderRows(Iterator<Row> rit, Row row) {
        Row newRow = row;
        // skip the 3 header rows
        for (int i = 0; i < NUMBER_OF_HEADER_ROWS; i++) {
            newRow = rit.next();
        }
        return newRow;
    }

    public static List<TrackerColumnInfo> parseTrackerSheetHeader(Row row0, String primaryProductPartNumber) {

        String[] fixedHeaders = FIXED_HEADERS;
        int numFixedHeaders = fixedHeaders.length;

        if (row0 == null) {
            throw getRuntimeException("No rows in tracker sheet: " + primaryProductPartNumber);
        }

        // Check row0 header names. Should match what we write.
        Iterator<Cell> cells = row0.cellIterator();
        for (String fixedHeader : fixedHeaders) {
            Cell cell = cells.next();
            if ((cell == null) ||  StringUtils.isBlank(cell.getStringCellValue()) ||
                !cell.getStringCellValue().equals(fixedHeader)) {

                String cellValFound = (cell == null) ? "" : cell.getStringCellValue();
                throw getRuntimeException("Tracker Sheet Header mismatch.  Expected : " +
                        fixedHeader + " but found " + cellValFound);
            }
        }

        List<String> columnHeaders = new ArrayList<String>();
        for (Iterator<Cell> cit = row0.cellIterator(); cit.hasNext(); ) {
            Cell cell = cit.next();
            if ((cell != null) && StringUtils.isNotBlank(cell.getStringCellValue())) {
                columnHeaders.add(cell.getStringCellValue());
            }
        }

        //Check for minimum columns.
        if (columnHeaders.size() < (numFixedHeaders + 1)) {
            throw getRuntimeException("Tracker Sheet Header mismatch.  Expected at least <" +
                    (numFixedHeaders + 1) + "> non-null header columns but found only " + columnHeaders.size()
                    + " in tab " + primaryProductPartNumber);
        }

        // Check for primary product part number in row0.
        Cell primaryProductHeaderCell = row0.getCell(numFixedHeaders);
        if (!primaryProductHeaderCell.getStringCellValue().contains(primaryProductPartNumber)) {
            throw getRuntimeException("Tracker Sheet Header mismatch.  Expected Primary Product PartNumber <" +
                    primaryProductPartNumber + "> in the first row and cell position " + primaryProductHeaderCell.getColumnIndex());
        }

        //Derive the list of TrackerColumnInfo objects skip the Comments and Billing Error columns
        int totalProductsHeaders = columnHeaders.size() - numFixedHeaders - 2;

        List<TrackerColumnInfo> result = new ArrayList<TrackerColumnInfo>();
        int mergedCellAddOn = 0;
        for (int i = 0; i < totalProductsHeaders; i++) {
            Cell cell = row0.getCell(i + numFixedHeaders + mergedCellAddOn);
            if (StringUtils.isNotBlank(cell.getStringCellValue())) {
                BillableRef billableRef = extractBillableRefFromHeader(cell.getStringCellValue());
                TrackerColumnInfo columnInfo = new TrackerColumnInfo(billableRef, i + numFixedHeaders);
                result.add(columnInfo);
                mergedCellAddOn++;
            }
        }

        return result;
    }

    private static RuntimeException getRuntimeException(String errMsg) {
        logger.error(errMsg);
        return new RuntimeException(errMsg);
    }

    static BillableRef extractBillableRefFromHeader(String cellValueStr) {

        if (StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0) || (startPos < 0) || !(startPos + 1 < endPos)) {
            throw getRuntimeException(
                "Tracker Sheet Header Format Error.  Could not find product partNumber in " +
                "column header. Column header contained: <" +  cellValueStr + ">");
        }

        String productPartNumber = cellValueStr.substring(startPos + 1, endPos);
        // Substring from char position 0 to position before separating space char.
        String priceItemName = cellValueStr.substring(0, startPos - 1);
        return new BillableRef(productPartNumber, priceItemName);
    }

    public static Map<TrackerColumnInfo, PriceItem> createPriceItemMapForSheet(
            List<TrackerColumnInfo> trackerColumnInfos, Product product, PriceItemDao priceItemDao,
            PriceListCache priceListCache) {
        Map<TrackerColumnInfo, PriceItem> resultMap = new HashMap<TrackerColumnInfo, PriceItem>();

        List<PriceItem> productPriceItems = SampleLedgerExporter.getPriceItems(product, priceItemDao, priceListCache);
        Set<Product> productAddOns = product.getAddOns();

        for (TrackerColumnInfo trackerColumnInfo : trackerColumnInfos) {

            PriceItem targetPriceItem = findPriceItemForTrackerColumnInfo(trackerColumnInfo, product,
                    productPriceItems, productAddOns, priceItemDao, priceListCache);

            resultMap.put(trackerColumnInfo, targetPriceItem);
        }

        return resultMap;
    }

    private static PriceItem findPriceItemForTrackerColumnInfo(
            TrackerColumnInfo trackerColumnInfo, Product product, List<PriceItem> productPriceItems,
            Set<Product> productAddOns, PriceItemDao priceItemDao, PriceListCache priceListCache) {

        String parsedProductPartNumber = trackerColumnInfo.getBillableRef().getProductPartNumber();
        String parsedPriceItemName = trackerColumnInfo.getBillableRef().getPriceItemName();

        if (product.getPartNumber().equals(parsedProductPartNumber)) {
            //This is the primary product.
            PriceItem targetPriceItem = findTargetPriceItem(productPriceItems, parsedPriceItemName);
            if (targetPriceItem == null) {
                throw getRuntimeException("Cannot find PriceItem for Product part number  : " +
                        parsedProductPartNumber + " and PriceItem name : " + parsedPriceItemName);
            }
            return targetPriceItem;
        }

        // The parsed Product Part Number must be for an Add-on
        for (Product productAddOn : productAddOns) {
            if (productAddOn.getPartNumber().equals(parsedProductPartNumber)) {
                PriceItem targetPriceItem =
                        findTargetPriceItem(SampleLedgerExporter.getPriceItems(productAddOn, priceItemDao, priceListCache), parsedPriceItemName);
                if (targetPriceItem == null) {
                    throw getRuntimeException("Cannot find PriceItem for Product Add-on part number  : " +
                            parsedProductPartNumber + " and PriceItem name : " + parsedPriceItemName);
                }
                return targetPriceItem;
            }
        }

        throw getRuntimeException("Cannot find a Product matching the spreadsheet product part number  : " +
                trackerColumnInfo.getBillableRef().getProductPartNumber() + " and PriceItem name : " +
                trackerColumnInfo.getBillableRef().getPriceItemName());
    }

    /**
     * Find the matching PriceItem that's ref-ed in the BillableRef
     *
     * @param productPriceItems The list of price items
     * @param parsedPriceItemName The parsed names
     *
     * @return Found item
     */
    private static PriceItem findTargetPriceItem(List<PriceItem> productPriceItems, String parsedPriceItemName) {
        for (PriceItem priceItem : productPriceItems) {
            if (priceItem.getName().equals(parsedPriceItemName)) {
                return priceItem;
            }
        }

        return null;
    }

}
