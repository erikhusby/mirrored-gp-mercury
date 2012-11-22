package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.*;

/**
 *
 */
public class SampleLedgerImporter {

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


    public SampleLedgerImporter() {

    }

    //TODO This was just for initial testing.
    public String readFromStream(FileInputStream fis) throws Exception {

        Workbook workbook;
        workbook = WorkbookFactory.create(fis);
        int numberOfProducts = workbook.getNumberOfSheets();
        Sheet sheet = workbook.getSheetAt(0);
        String productPartNumberStr = sheet.getSheetName();
        return productPartNumberStr;

    }


    public Map<String, Map<String, Map<String, OrderBillSummaryStat>>> parseTempFile(File tempFile) throws IOException {

        Map<String, Map<String, Map<String, OrderBillSummaryStat>>> trackerSummaryMap =
                new HashMap<String, Map<String, Map<String, OrderBillSummaryStat>>>();

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

                //Get a map (by PDOId) of a list of OrderBillSummaryStat object  for this sheet.
                Map<String, Map<String, OrderBillSummaryStat>> sheetSummaryMap = parseTrackerSheet(sheet, trackerHeaderList);
                trackerSummaryMap.put(productPartNumberStr, sheetSummaryMap);

            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fis);
        }

        return trackerSummaryMap;

    }

    Map<String, Map<String, OrderBillSummaryStat>> parseTrackerSheet(Sheet sheet, List<TrackerColumnInfo> trackerColumnInfos) {
        String primaryProductPartNumber = sheet.getSheetName();
        int maxNumberOfProductsInSheet = trackerColumnInfos.size();
        String currentPdoName = "";
        // A map (by PDO) of maps ( by PPN) of OrderBillSummaryStat objects
        Map<String, Map<String, OrderBillSummaryStat>> sheetSummaryMap = new HashMap<String, Map<String, OrderBillSummaryStat>>();

        for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
            Row row = rit.next();

            if ( row.getRowNum() == 0 ) {
                row = skipHeaderRows(rit, row);
            }

            String rowPdoIdStr = row.getCell(PDO_ID_COL_POS).getStringCellValue();

            Map<String, OrderBillSummaryStat> pdoSummaryStatsMap  = sheetSummaryMap.get(rowPdoIdStr);

            // For a newly found PdoId create a new map for it and add it to the sheet summary map
            if (! currentPdoName.equalsIgnoreCase( rowPdoIdStr ) && pdoSummaryStatsMap == null) {
                pdoSummaryStatsMap = new HashMap<String, OrderBillSummaryStat>(maxNumberOfProductsInSheet);
                sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);
                currentPdoName = rowPdoIdStr;
            }

            pdoSummaryStatsMap = processRow(row, pdoSummaryStatsMap, trackerColumnInfos );
            sheetSummaryMap.put(rowPdoIdStr, pdoSummaryStatsMap);

        }

        return sheetSummaryMap;
    }

    private Map<String, OrderBillSummaryStat> processRow(Row row, Map<String, OrderBillSummaryStat> pdoSummaryStatsMap,
                            List<TrackerColumnInfo> trackerColumnInfos ) {

        for (int productIndex=0; productIndex < trackerColumnInfos.size();productIndex++) {
            double newQuantity;
            String productPartNumber = trackerColumnInfos.get(productIndex).getProductPartNumber();

            //Get the AlreadyBilled cell
            Cell billedCell = row.getCell(fixedHeaders.length + productIndex);

            //Get the newQuantity cells value
            Cell newQuantityCell = row.getCell(fixedHeaders.length + productIndex + 1);
            if ( newQuantityCell != null ) {
                newQuantity = newQuantityCell.getNumericCellValue();

                //TODO Get the actual value from the DB for this POS to calculate the delta  !!!!!!!!!
                double valueFromDB = 0;
                String sampleName = row.getCell(SAMPLE_ID_COL_POS).getStringCellValue();
                //TODO Get the actual value from the DB for this POS to calculate the delta  !!!!!!!!!

                double delta =  newQuantity - valueFromDB;

                if ( delta != 0 ) {
                    OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(productPartNumber);
                    if ( orderBillSummaryStat == null ) {
                        // create a new stat obj and add it to the map
                        orderBillSummaryStat = new OrderBillSummaryStat(productPartNumber);
                        pdoSummaryStatsMap.put(productPartNumber, orderBillSummaryStat);
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
                   StringUtils.isBlank( cell.getStringCellValue()) ||
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
        if ( columnHeaders.size() < (numFixedHeaders + 2) ) {
            throw new RuntimeException( "Tracker Sheet Header mismatch.  Expected at least <" +
                    (numFixedHeaders + 2 ) + "> non-null header columns but found only " +  columnHeaders.size()
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
                String productPartNumber = extractPartNumberFromHeader ( cell.getStringCellValue() ) ;
                TrackerColumnInfo columnInfo  = new TrackerColumnInfo(productPartNumber, i+numFixedHeaders );
                result.add( columnInfo );
                mergedCellAddOn++;
            }
        }

        return result;
    }

    String extractPartNumberFromHeader(String cellValueStr) {
        boolean result = false;

        if ( StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0 ) || ( startPos < 0 ) || ! (startPos+1 < endPos)) {
            throw new RuntimeException( "Tracker Sheet Header Format Error.  Could not find product partNumber in column header. Column header contained: <" +
                    cellValueStr + ">");
        }
        return cellValueStr.substring(startPos+1, endPos);
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
        private final String productPartNumber;
        private final int columnIndex;

        private TrackerColumnInfo(final String productPartNumber, final int columnIndex) {
            this.productPartNumber = productPartNumber;
            this.columnIndex = columnIndex;
        }

        public String getProductPartNumber() {
            return productPartNumber;
        }

        public int getColumnIndex() {
            return columnIndex;
        }
    }
}
