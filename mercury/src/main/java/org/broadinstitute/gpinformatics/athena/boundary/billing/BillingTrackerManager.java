package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import java.io.FileInputStream;
import java.util.*;

/**
 *
 */
public class BillingTrackerManager {

    private Log logger = LogFactory.getLog(BillingTrackerManager.class);


    private BillingLedgerDao billingLedgerDao;
    private ProductOrderDao productOrderDao;


    private final static String[] fixedHeaders = SampleLedgerExporter.FIXED_HEADERS;
    private final static Map<String,Integer> headerColumnIndices = new HashMap<String, Integer>();

    static {
        for (int i=0; i< fixedHeaders.length; i++) {
            headerColumnIndices.put(fixedHeaders[i], i);
        }
    }

    private final static int SAMPLE_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.SAMPLE_ID_HEADING);
    private final static int PDO_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.ORDER_ID_HEADING);
    private final static int DATE_COMPLETE_COL_POS = headerColumnIndices.get(SampleLedgerExporter.DATE_COMPLETE_HEADING);
    private final static int numberOfHeaderRows = 3;


    public BillingTrackerManager(ProductOrderDao productOrderDao, BillingLedgerDao billingLedgerDao) {
        this.productOrderDao = productOrderDao;
        this.billingLedgerDao = billingLedgerDao;
    }

    public Map<String, List<ProductOrder>> parseFileForBilling(FileInputStream fis) throws Exception {

        Map<String, List<ProductOrder>> trackerBillingMap = new HashMap<String, List<ProductOrder>>();

        Workbook workbook = WorkbookFactory.create(fis);

        //TODO could use this map later on during parsing.
        Map<String, List<String>> productOrderIdMap = getUnlockedProductOrderIdsFromWorkbook(workbook);
        for ( String ppnSheetName : productOrderIdMap.keySet() ) {
            List<String> productOrderIds = productOrderIdMap.get( ppnSheetName );
            for ( String productOrderId : productOrderIds ) {
                logger.info( "BILLING: About to update ledger for productOrder " + productOrderId + " for ProductPartNumber " + ppnSheetName );
            }
        }

        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i=0; i< numberOfSheets;i++) {

            Sheet sheet = workbook.getSheetAt(i);
            String productPartNumberStr = sheet.getSheetName();

            List<TrackerColumnInfo> trackerHeaderList = parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

            //Get a list of Product Orders for this sheet.
            List<ProductOrder> sheetBillingMap = parseSheetForBilling(sheet, trackerHeaderList);

            trackerBillingMap.put(productPartNumberStr, sheetBillingMap);

        }

        return trackerBillingMap;
    }

    private Map<String, List<String>> getUnlockedProductOrderIdsFromWorkbook(Workbook workbook) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();

        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i=0; i< numberOfSheets;i++) {

            Sheet sheet = workbook.getSheetAt(i);
            String productPartNumberStr = sheet.getSheetName();

            // Just reparsing for fun and checking that we are dealing with the correct file.
            List<TrackerColumnInfo> trackerHeaderList = parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

            //Get a list of Product OrderIds for this sheet.
            List<String> sheetOrderIdMap = extractOrderIdsFromSheet( sheet );

            for ( String pdoIdStr : sheetOrderIdMap ) {
                // Check if this PDO has any locked Ledger rows
                Set<BillingLedger> lockedBillingLedgerSet = billingLedgerDao.findLockedOutByOrderList(
                        Collections.singletonList(pdoIdStr) );
                if ( lockedBillingLedgerSet.size() > 0 ) {
                    throwRuntimeException("Product Order " + pdoIdStr + " of sheet " + productPartNumberStr +
                            " is locked out and has " + lockedBillingLedgerSet.size() + " rows that are locked in the DB.");
                }
            }
            result.put(productPartNumberStr, sheetOrderIdMap );

        }

        return result;
    }

    private List<String> extractOrderIdsFromSheet(Sheet sheet) {
        List<String> result = new ArrayList<String>();

        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();
            if ( row.getRowNum() == 0 ) {
                row = skipHeaderRows(rit, row);
            }

            String rowPdoIdStr = row.getCell(PDO_ID_COL_POS).getStringCellValue();
            if (! result.contains(rowPdoIdStr)) {
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

        // Iterate over each row in this tab of the spreadsheet.
        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();

            if ( row.getRowNum() == 0 ) {
                row = skipHeaderRows(rit, row);
            }

            String rowPdoIdStr = row.getCell(PDO_ID_COL_POS).getStringCellValue();
            String currentSampleName = row.getCell(SAMPLE_ID_COL_POS).getStringCellValue();

            // For a newly found PdoId create a new map for it and add it to the sheet summary map
            if (! currentPdoId.equalsIgnoreCase( rowPdoIdStr ) ) {

                //Persist any BillingLedger Items captured for the previous productOrder (if not blank) before changing to the next PDO
                if ( StringUtils.isNotBlank( currentPdoId ) ) {
                    persistProductOrder(productOrder);
                    sheetBillingMap.add(productOrder);
                }

                //Switch to the next orderId
                currentPdoId = rowPdoIdStr;

                // Find the order in the DB
                productOrder = productOrderDao.findByBusinessKey(currentPdoId);
                if ( productOrder == null ) {
                    throwRuntimeException("Product Order " + currentPdoId + " on row " +  (row.getRowNum() + 1 ) +
                            " of sheet "  + primaryProductPartNumber + " is not found in the database.");
                }

                product = productOrder.getProduct();
                samples  = productOrder.getSamples();

                // Find the target priceItems for the data that was parsed from the header, happens one per sheet parse.
                // create a map (by trackerColumnInfo) of PriceItems
                if ( priceItemMap == null ) {
                    priceItemMap = createPriceItemMapForSheet(trackerColumnInfos, product);
                }

                //Remove any pending billable Items from the ledger for all samples in this PDO
                ProductOrder[] productOrderArray = new ProductOrder[]{productOrder};
                billingLedgerDao.removeLedgerItemsWithoutBillingSession( productOrderArray );
                billingLedgerDao.flush();

            }

            //TODO hmc We are assuming ( for now ) that the order is the same in the spreadsheet as returned in the productOrder !
            int sampleNumber =  row.getRowNum() - numberOfHeaderRows;
            if ( sampleNumber >= samples.size() ) {
                throwRuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples." );
            }

            productOrderSample = samples.get(row.getRowNum() - numberOfHeaderRows );
            if (! productOrderSample.getSampleName().equals( currentSampleName ) ) {
                throwRuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is in different position than expected. Expected value from Order is " + productOrderSample.getSampleName());
            }

            // Create a list of BillingLedger objs for this ProductOrderSample that is part of the current PDO.
            parseSampleRowForBilling(row, productOrderSample, product, trackerColumnInfos, priceItemMap);

        }

        //Persist any BillingLedger Items captured for the last productOrder (if not blank) before finishing the sheet
        if ( StringUtils.isNotBlank( currentPdoId ) ) {
            persistProductOrder(productOrder);
            sheetBillingMap.add(productOrder);
        }

        return sheetBillingMap;
    }

    private void parseSampleRowForBilling( Row row, ProductOrderSample productOrderSample, Product product,
                                           List<TrackerColumnInfo> trackerColumnInfos,
                                           Map<TrackerColumnInfo, PriceItem> priceItemMap ) {

        Date workCompleteDate = null;
        Cell workCompleteDateCell = row.getCell(DATE_COMPLETE_COL_POS);

        if ( isNonNullDateCell( workCompleteDateCell ) ) {
            workCompleteDate = workCompleteDateCell.getDateCellValue() ;
        } else {
            throwRuntimeException("Sample " + productOrderSample.getSampleName() + " on row " +  (row.getRowNum() + 1 ) +
                    " of spreadsheet "  + product.getPartNumber() +
                    " has an invalid Date Completed value. Please correct and try again.");
        }

        for (int billingRefIndex=0; billingRefIndex < trackerColumnInfos.size();billingRefIndex++) {
            double newQuantity;
            double billedQuantity;
            TrackerColumnInfo trackerColumnInfo = trackerColumnInfos.get(billingRefIndex);
            BillableRef billableRef = trackerColumnInfos.get(billingRefIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = fixedHeaders.length + (billingRefIndex*2);

            //Get the AlreadyBilled cell
            Cell billedCell = row.getCell(currentBilledPosition);
            if (isNonNullNumericCell(billedCell)) {
                billedQuantity = billedCell.getNumericCellValue();
                //TODO Do the validation check here ( same code as goes into BillingTrackerImporter )
            }

            //Get the newQuantity cell value and add it onto the productOrderSample
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if ( isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();
                if ( newQuantity > 0  ) {
                    PriceItem priceItem = priceItemMap.get( trackerColumnInfo );
                    BillingLedger billingLedger = new BillingLedger(productOrderSample, priceItem,
                            workCompleteDate, newQuantity);
                    productOrderSample.getBillableItems().add(billingLedger);
                    productOrderSample.setBillingStatus(BillingStatus.EligibleForBilling);
                    logger.debug("Added BillingLedger item for sample " + productOrderSample.getSampleName() +
                            " to PDO " + productOrderSample.getProductOrder().getBusinessKey() +
                               " for PriceItemName[PPN]: " + billableRef.getPriceItemName() + "[" +
                               billableRef.getProductPartNumber() +"] - Quantity:" + newQuantity );
                } else {
                    logger.debug("Skipping BillingLedger item for sample " + productOrderSample.getSampleName() +
                            " to PDO " + productOrderSample.getProductOrder().getBusinessKey() +
                               " for PriceItemName[PPN]: " + billableRef.getPriceItemName() + "[" +
                               billableRef.getProductPartNumber() +"] - quantity:" + newQuantity);
                }
            }
        } // end of for each productIndex loop
    }

    private void persistProductOrder(ProductOrder productOrder) {
        int numberOfLedgerItems = 0;
        try {
            for (ProductOrderSample productOrderSample : productOrder.getSamples() ) {
                if ( productOrderSample.getBillableItems() != null ) {
                    for ( BillingLedger billingLedger : productOrderSample.getBillableItems() ) {
                        // Only count the null-Billing Session ledgerItems.
                        if ( billingLedger.getBillingSession() == null) {
                            numberOfLedgerItems ++;
                        }
                    }
                }
            }
            // No need to persist if there are no ledger Items for this order.
            if ( numberOfLedgerItems > 0 ) {
                productOrderDao.persist(productOrder);
                productOrderDao.flush();
                logger.info("Persisted " + numberOfLedgerItems + " BillingLedger records for Product Order <" +
                        productOrder.getTitle() + "> with PDO Id: " + productOrder.getBusinessKey());
            }
        } catch (RuntimeException e) {
            logger.error("Exception when persisting " + numberOfLedgerItems +" BillingLedger items for Product Oder <" +
                    productOrder.getTitle() + "> with PDO Id: " + productOrder.getBusinessKey() , e);
            throw e;
        }
    }


    private Map<TrackerColumnInfo,PriceItem> createPriceItemMapForSheet(
            List<TrackerColumnInfo> trackerColumnInfos, Product product) {
        Map<TrackerColumnInfo,PriceItem> resultMap = new HashMap<TrackerColumnInfo, PriceItem>();

        List<PriceItem> productPriceItems = SampleLedgerExporter.getPriceItems( product );
        Set<Product> productAddons = product.getAddOns();

        for ( TrackerColumnInfo trackerColumnInfo : trackerColumnInfos ) {

            PriceItem targetPriceItem  = findPriceItemForTrackerColumnInfo ( trackerColumnInfo, product,
                    productPriceItems, productAddons );

            resultMap.put(trackerColumnInfo, targetPriceItem);
        }

        return resultMap;
    }

    private PriceItem findPriceItemForTrackerColumnInfo (
            TrackerColumnInfo trackerColumnInfo, Product product, List<PriceItem> productPriceItems,
            Set<Product> productAddons ) {

        PriceItem targetPriceItem = null;
        String parsedProductPartnumber = trackerColumnInfo.getBillableRef().getProductPartNumber();
        String parsedPriceItemName = trackerColumnInfo.getBillableRef().getPriceItemName();

        if ( product.getPartNumber().equals( parsedProductPartnumber ) ) {
            //This is the primary product.
            targetPriceItem = findTargetPriceItem(productPriceItems, parsedPriceItemName);
            if ( targetPriceItem == null) {
                throwRuntimeException( "Cannot find PriceItem for Product part number  : " +
                        parsedProductPartnumber + " and PriceItem name : " + parsedPriceItemName );
            }
            return targetPriceItem;
        }

        // The parsedProductPartnumber must be for an Addon
        for ( Product productAddon : productAddons ) {
            if ( productAddon.getPartNumber().equals( parsedProductPartnumber ) ) {
                targetPriceItem = findTargetPriceItem( SampleLedgerExporter.getPriceItems( productAddon ), parsedPriceItemName);
                if ( targetPriceItem == null) {
                    throwRuntimeException( "Cannot find PriceItem for Product Addon part number  : " +
                            parsedProductPartnumber + " and PriceItem name : " + parsedPriceItemName );
                }
                return targetPriceItem;
            }
        }
        if ( targetPriceItem == null ) {
            throwRuntimeException( "Cannot find a Product matching the spreadsheet product part number  : " +
                    trackerColumnInfo.getBillableRef().getProductPartNumber() + " and PriceItem name : " +
                    trackerColumnInfo.getBillableRef().getPriceItemName() );
        }
        return targetPriceItem;
    }

    private PriceItem findTargetPriceItem(List<PriceItem> productPriceItems, String parsedPriceItemName) {
        PriceItem targetPriceItem = null;
        //Find the matching PriceItem that's ref-ed in the BillableRef
        for ( PriceItem priceItem : productPriceItems ) {
            if ( priceItem.getName().equals( parsedPriceItemName )  ) {
                targetPriceItem = priceItem;
                break;
            }
        }
        return targetPriceItem;
    }

    private boolean isNonNullNumericCell(Cell cell) {
        return (cell != null ) && ( Cell.CELL_TYPE_NUMERIC == cell.getCellType());
    }

    private boolean isNonNullDateCell(Cell cell) {
        return  ( (cell != null ) && ( HSSFDateUtil.isCellDateFormatted(cell)) );
    }

    private Row skipHeaderRows(Iterator<Row> rit,  Row row) {
        Row newRow=row;
        // skip the 3 header rows
        for (int i=0; i< numberOfHeaderRows; i++) {
            newRow = rit.next();
        }
        return newRow;
    }

    //TODO merge with method in BillingTrackerImporter into base class
    List<TrackerColumnInfo> parseTrackerSheetHeader(Row row0, String primaryProductPartNumber) {

        List<TrackerColumnInfo> result = null;

        String[] fixedHeaders = SampleLedgerExporter.FIXED_HEADERS;
        int numFixedHeaders = fixedHeaders.length;

        // Check row0 header names. Should match what we write.
        Iterator<Cell> citer = row0.cellIterator();
        for (int i=0; i< numFixedHeaders; i++) {
            Cell cell = citer.next();
            if ( ( cell == null) ||
                    StringUtils.isBlank(cell.getStringCellValue()) ||
                    ! cell.getStringCellValue().equals( fixedHeaders[i] ) ) {
                String cellValFound = (cell == null) ? "" : cell.getStringCellValue();
                throwRuntimeException( "Tracker Sheet Header mismatch.  Expected : " +
                        fixedHeaders[i]  + " but found " + cellValFound );
            }
        }

        List<String> columnHeaders = new ArrayList<String>();
        for (Iterator<Cell> cit = row0.cellIterator(); cit.hasNext(); ) {
            Cell cell = cit.next();
            if ( (cell != null) &&  StringUtils.isNotBlank( cell.getStringCellValue()))  {
                columnHeaders.add(cell.getStringCellValue());
            }
        }

        //Check for minimum columns.
        if ( columnHeaders.size() < (numFixedHeaders + 1) ) {
            throwRuntimeException( "Tracker Sheet Header mismatch.  Expected at least <" +
                    (numFixedHeaders + 1 ) + "> non-null header columns but found only " +  columnHeaders.size()
                    + " in tab " +  primaryProductPartNumber ) ;
        }

        // Check for primary product part number in row0.
        Cell primaryProductHeaderCell = row0.getCell( numFixedHeaders );
        if (! primaryProductHeaderCell.getStringCellValue().contains( primaryProductPartNumber) ) {
            throwRuntimeException( "Tracker Sheet Header mismatch.  Expected Primary Product PartNumber <" +
                    primaryProductPartNumber + "> in the first row and cell position " +  primaryProductHeaderCell.getColumnIndex() );
        }

        //Derive the list of TrackerColumnInfo objects
        int totalProductsHeaders = columnHeaders.size() - numFixedHeaders;

        result = new ArrayList<TrackerColumnInfo>();
        int mergedCellAddOn = 0;
        for (int i=0; i< totalProductsHeaders; i++) {
            Cell cell = row0.getCell( i+numFixedHeaders+mergedCellAddOn );
            if (StringUtils.isNotBlank( cell.getStringCellValue())) {
                BillableRef billableRef = extractBillableRefFromHeader(cell.getStringCellValue()) ;
                TrackerColumnInfo columnInfo  = new TrackerColumnInfo(billableRef, i+numFixedHeaders );
                result.add( columnInfo );
                mergedCellAddOn++;
            }
        }

        return result;
    }

    BillableRef extractBillableRefFromHeader(String cellValueStr) {
        String productPartNumber = "";
        String priceItemName = "";

        if ( StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0 ) || ( startPos < 0 ) || ! (startPos+1 < endPos)) {
            throwRuntimeException( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header. Column header contained: <" +
                    cellValueStr + ">");
        }
        productPartNumber = cellValueStr.substring(startPos+1, endPos);
        // Substring from char position 0 to position before separating space char.
        priceItemName = cellValueStr.substring(0, startPos-1);
        BillableRef billableRef = new BillableRef(productPartNumber, priceItemName);

        return billableRef;
    }

    private void throwRuntimeException ( String errMsg ) {
        logger.error(errMsg);
        throw new RuntimeException( errMsg );
    }

    class TrackerColumnInfo {
        private final BillableRef billableRef;
        private final int columnIndex;

        private TrackerColumnInfo(final BillableRef billableRef, final int columnIndex) {
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
            if (this == o) return true;
            if (!(o instanceof TrackerColumnInfo)) return false;

            final TrackerColumnInfo that = (TrackerColumnInfo) o;

            if (columnIndex != that.columnIndex) return false;
            if (!billableRef.equals(that.billableRef)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = billableRef.hashCode();
            result = 31 * result + columnIndex;
            return result;
        }
    }
}
