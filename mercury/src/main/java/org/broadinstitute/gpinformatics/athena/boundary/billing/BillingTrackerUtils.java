package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/5/12
 * Time: 11:12 AM
 */
public class BillingTrackerUtils {

    private static Log logger = LogFactory.getLog(BillingTrackerUtils.class);

    final static String[] fixedHeaders = SampleLedgerExporter.FIXED_HEADERS;
    final static Map<String,Integer> headerColumnIndices = new HashMap<String, Integer>();

    static {
        for (int i=0; i< fixedHeaders.length; i++) {
            headerColumnIndices.put(fixedHeaders[i], i);
        }
    }

    final static int NUM_HEADER_ROWS = 2;

    final static int SAMPLE_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.SAMPLE_ID_HEADING);
    final static int PDO_ID_COL_POS = headerColumnIndices.get(SampleLedgerExporter.ORDER_ID_HEADING);
    final static int SORT_COLUMN_COL_POS = headerColumnIndices.get( SampleLedgerExporter.SORT_COLUMN_HEADING);
    final static int WORK_COMPLETE_DATE_COL_POS = headerColumnIndices.get( SampleLedgerExporter.WORK_COMPLETE_DATE_HEADING);
    final static int numberOfHeaderRows = 2;


    static boolean isNonNullNumericCell(Cell cell) {
        return (cell != null ) && ( Cell.CELL_TYPE_NUMERIC == cell.getCellType());
    }

    static Row skipHeaderRows(Iterator<Row> rit,  Row row) {
        Row newRow=row;
        // skip the 3 header rows
        for (int i=0; i< numberOfHeaderRows; i++) {
            newRow = rit.next();
        }
        return newRow;
    }

    static List<TrackerColumnInfo> parseTrackerSheetHeader(Row row0, String primaryProductPartNumber) {

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

        //Derive the list of TrackerColumnInfo objects skip the Comments and Billing Error columns
        int totalProductsHeaders = columnHeaders.size() - numFixedHeaders - 2;

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

    static void throwRuntimeException ( String errMsg ) {
        logger.error(errMsg);
        throw new RuntimeException( errMsg );
    }

    static BillableRef extractBillableRefFromHeader(String cellValueStr) {
        String productPartNumber = "";
        String priceItemName = "";

        if ( StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0 ) || ( startPos < 0 ) || ! (startPos+1 < endPos)) {
            throw new RuntimeException( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header. Column header contained: <" +
                    cellValueStr + ">");
        }
        productPartNumber = cellValueStr.substring(startPos+1, endPos);
        // Substring from char position 0 to position before separating space char.
        priceItemName = cellValueStr.substring(0, startPos-1);
        BillableRef billableRef = new BillableRef(productPartNumber, priceItemName);

        return billableRef;
    }

    static Map<TrackerColumnInfo,PriceItem> createPriceItemMapForSheet(
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

    private static PriceItem findPriceItemForTrackerColumnInfo (
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

    private static PriceItem findTargetPriceItem(List<PriceItem> productPriceItems, String parsedPriceItemName) {
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

}
