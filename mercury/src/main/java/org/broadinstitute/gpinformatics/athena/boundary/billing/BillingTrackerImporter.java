package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
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

    private ProductOrderDao productOrderDao;
    private ProductOrderSampleDao productOrderSampleDao;

    private final static String[] fixedHeaders = SampleLedgerExporter.FIXED_HEADERS;
    private final static Map<String,Integer> headerColumnIndices = new HashMap<String, Integer>();

    static {
        for (int i=0; i< fixedHeaders.length; i++) {
            headerColumnIndices.put(fixedHeaders[i], i);
        }
    }
    private final static int SAMPLE_ID_COL_POS = headerColumnIndices.get("Sample ID");
    private final static int PDO_ID_COL_POS = headerColumnIndices.get("Product Order ID");
    private final static int numberOfHeaderRows = 3;


    public BillingTrackerImporter(ProductOrderDao productOrderDao, ProductOrderSampleDao productOrderSampleDao) {
        this.productOrderDao = productOrderDao;
        this.productOrderSampleDao = productOrderSampleDao;
    }

    //TODO This was just for initial prototyping.
    public String readFromStream(FileInputStream fis) throws Exception {

        Workbook workbook;
        workbook = WorkbookFactory.create(fis);
        int numberOfProducts = workbook.getNumberOfSheets();
        Sheet sheet = workbook.getSheetAt(0);
        String productPartNumberStr = sheet.getSheetName();
        return productPartNumberStr;

    }

    public Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> parseFileForSummaryMap(File tempFile) throws IOException {

        Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> trackerSummaryMap =
                new HashMap<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>>();

        Workbook workbook;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(tempFile);
            workbook = WorkbookFactory.create(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i=0; i< numberOfSheets;i++) {

                Sheet sheet = workbook.getSheetAt(i);
                String productPartNumberStr = sheet.getSheetName();

                List<TrackerColumnInfo> trackerHeaderList = parseTrackerSheetHeader(sheet.getRow(0), productPartNumberStr);

                //Get a map (by PDOId) of a map of OrderBillSummaryStat objects (by BillablRef) for this sheet.
                Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap = parseSheetForSummaryMap(sheet, trackerHeaderList);
                trackerSummaryMap.put(productPartNumberStr, sheetSummaryMap);

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return trackerSummaryMap;

    }

    Map<String, Map<BillableRef, OrderBillSummaryStat>> parseSheetForSummaryMap(Sheet sheet, List<TrackerColumnInfo> trackerColumnInfos) {
        ProductOrder productOrder = null;
        Product product = null;
        ProductOrderSample productOrderSample = null;
        List<ProductOrderSample> samples = null;

        String primaryProductPartNumber = sheet.getSheetName();
        int maxNumberOfProductsInSheet = trackerColumnInfos.size();
        String currentPdoId = "";


        // A map (by PDO) of maps ( by PPN) of OrderBillSummaryStat objects
        Map<String, Map<BillableRef, OrderBillSummaryStat>> sheetSummaryMap = new HashMap<String, Map<BillableRef, OrderBillSummaryStat>>();

        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();

            if ( row.getRowNum() == 0 ) {
                row = skipHeaderRows(rit, row);
            }

            String rowPdoIdStr = row.getCell(PDO_ID_COL_POS).getStringCellValue();
            String currentSampleName = row.getCell(SAMPLE_ID_COL_POS).getStringCellValue();
            Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap  = sheetSummaryMap.get(rowPdoIdStr);

            // For a newly found PdoId create a new map for it and add it to the sheet summary map
            if (! currentPdoId.equalsIgnoreCase( rowPdoIdStr ) && pdoSummaryStatsMap == null) {
                pdoSummaryStatsMap = new HashMap<BillableRef, OrderBillSummaryStat>(maxNumberOfProductsInSheet);
                sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);
                currentPdoId = rowPdoIdStr;

                // Find the order in the DB
                productOrder = productOrderDao.findByBusinessKey(currentPdoId);
                if ( productOrder == null ) {
                    throw new RuntimeException("Product Order " + currentPdoId + " on row " +  (row.getRowNum() + 1 ) +
                            " of sheet "  + primaryProductPartNumber + " is not found in the database.");
                }

                product = productOrder.getProduct();
                samples  = productOrder.getSamples();

                // Find the target priceItems for the data that was parsed from the header.
                Map<TrackerColumnInfo, PriceItem> priceItemMap = createPriceItemMapForSheet(trackerColumnInfos, product);
            }

            //TODO hmc We are assuming ( for now ) that the order is the same in the spreadsheet as returned in the productOrder !
            int sampleNumber =  row.getRowNum() - numberOfHeaderRows;
            if ( sampleNumber >= samples.size() ) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is not in the expected position. The Order <" + productOrder.getTitle() + " (Id: " + currentPdoId +
                        ")> has only " + samples.size() + " samples." );
            }

            productOrderSample = samples.get(row.getRowNum() - numberOfHeaderRows );
            if (! productOrderSample.getSampleName().equals( currentSampleName ) ) {
                throw new RuntimeException("Sample " + currentSampleName + " on row " +  (row.getRowNum() + 1 ) +
                        " of spreadsheet "  + primaryProductPartNumber +
                        " is in different position than expected. Expected value from Order is " + productOrderSample.getSampleName());
            }

            pdoSummaryStatsMap = parseRowForSummaryMap(row, productOrderSample, product, pdoSummaryStatsMap, trackerColumnInfos);
            sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);

        }

        return sheetSummaryMap;
    }

    private Map<BillableRef, OrderBillSummaryStat> parseRowForSummaryMap(
            Row row, ProductOrderSample productOrderSample, Product product, Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap,
            List<TrackerColumnInfo> trackerColumnInfos) {


        for (int productIndex=0; productIndex < trackerColumnInfos.size();productIndex++) {
            double newQuantity;
            double billedQuantity;
            BillableRef billableRef = trackerColumnInfos.get(productIndex).getBillableRef();

            // There are two cells per product header cell, so we need to account for this.
            int currentBilledPosition = fixedHeaders.length + (productIndex*2);

            //Get the AlreadyBilled cell
            Cell billedCell = row.getCell(currentBilledPosition);
            if (isNonNullNumericCell(billedCell)) {
                billedQuantity = billedCell.getNumericCellValue();
                //TODO hmc need the AlreadyBilled validation check here.
                //Check billedQuantity parsed against that which is already billed for this POS and PriceItem - should match
                //TODO Sum the already billed amount for this sample
            }

            //Get the newQuantity cell value
            Cell newQuantityCell = row.getCell(currentBilledPosition + 1);
            if ( isNonNullNumericCell(newQuantityCell)) {
                newQuantity = newQuantityCell.getNumericCellValue();

                //TODO Sum the newQuantity amount for this sample
                double valueFromDB = 0;
                //TODO Get the actual value from the DB for this POS to calculate the delta  !!!!!!!!!

                double delta =  newQuantity - valueFromDB;

                if ( delta != 0 ) {
                    OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(billableRef);
                    if ( orderBillSummaryStat == null ) {
                        // create a new stat obj and add it to the map
                        orderBillSummaryStat = new OrderBillSummaryStat();
                        pdoSummaryStatsMap.put(billableRef, orderBillSummaryStat);
                    }

                    if ( delta < 0 ) {
                        orderBillSummaryStat.addCredit( delta );
                    } else {
                        orderBillSummaryStat.addCharge( delta );
                    }
                }
            }
        } // end of for each product loop
        return pdoSummaryStatsMap;
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
                throw new RuntimeException( "Cannot find PriceItem for Product part number  : " +
                        parsedProductPartnumber + " and PriceItem name : " + parsedPriceItemName );
            }
            return targetPriceItem;
        }

        // The parsedProductPartnumber must be for an Addon
        for ( Product productAddon : productAddons ) {
            if ( productAddon.getPartNumber().equals( parsedProductPartnumber ) ) {
                targetPriceItem = findTargetPriceItem( SampleLedgerExporter.getPriceItems( productAddon ), parsedPriceItemName);
                if ( targetPriceItem == null) {
                    throw new RuntimeException( "Cannot find PriceItem for Product Addon part number  : " +
                            parsedProductPartnumber + " and PriceItem name : " + parsedPriceItemName );
                }
                return targetPriceItem;
            }
        }
        if ( targetPriceItem == null ) {
            throw new RuntimeException( "Cannot find a Product matching the spreadsheet product part number  : " +
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

    private Row skipHeaderRows(Iterator<Row> rit,  Row row) {
        Row newRow=row;
        // skip the 3 header rows
        for (int i=0; i< numberOfHeaderRows; i++) {
            newRow = rit.next();
        }
        return newRow;
    }

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
                throw new RuntimeException( "Tracker Sheet Header mismatch.  Expected : " +
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
            throw new RuntimeException( "Tracker Sheet Header mismatch.  Expected at least <" +
                    (numFixedHeaders + 1 ) + "> non-null header columns but found only " +  columnHeaders.size()
                    + " in tab " +  primaryProductPartNumber ) ;
        }

        // Check for primary product part number in row0.
        Cell primaryProductHeaderCell = row0.getCell( numFixedHeaders );
        if (! primaryProductHeaderCell.getStringCellValue().contains( primaryProductPartNumber) ) {
            throw new RuntimeException( "Tracker Sheet Header mismatch.  Expected Primary Product PartNumber <" +
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
            throw new RuntimeException( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header. Column header contained: <" +
                    cellValueStr + ">");
        }
        productPartNumber = cellValueStr.substring(startPos+1, endPos);
        // Substring from char position 0 to position before separating space char.
        priceItemName = cellValueStr.substring(0, startPos-1);
        BillableRef billableRef = new BillableRef(productPartNumber, priceItemName);

        return billableRef;
    }


    public File copyFromStreamToTempFile(InputStream is) throws IOException {

        Date now = new Date();
        File tempFile = File.createTempFile("BillingTrackerTempFile_" + now.getTime(), ".xls");

        OutputStream out = new FileOutputStream( tempFile );

        try {
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = is.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            System.out.println("New file created!");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            out.flush();
            out.close();
        }

        return tempFile.getAbsoluteFile();
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
