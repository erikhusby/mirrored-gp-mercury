package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CommonUtils {

    private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\p{ASCII}]");

    /**
     * For use with java Stream.collect to collect a list of one element down to one single object
     * @param <T>
     * @return
     */
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list->{
                    return (CollectionUtils.isNotEmpty(list))?list.get(0):null;
                }
        );
    }

    public static XSSFWorkbook csvToXLSX(InputStream csvFileInput, String fileName) {
        XSSFWorkbook workBook = new XSSFWorkbook();
        try {
            String[] fileNameSplit = StringUtils.split(fileName, ".");
            String xlsxFileAddress = fileNameSplit[0] + ".xlsx";
            XSSFSheet sheet = workBook.createSheet("sheet1");
            XSSFDataFormat fmt = workBook.createDataFormat();
            CellStyle textStyle = workBook.createCellStyle();
            textStyle.setDataFormat(fmt.getFormat("@"));
            sheet.setDefaultColumnStyle(0, textStyle);

            int RowNum = 0;
            List<String[]> rows = CsvParser.parseToCellGrid(csvFileInput);
            for (String[] row : rows) {
                XSSFRow currentRow = sheet.createRow(RowNum);
                for (int i = 0; i < row.length; i++) {
                    // The start of the stream sometimes contains junk characters, perhaps added by the browser
                    // because it infers a MIME type of Excel
                    currentRow.createCell(i).setCellValue(NON_ASCII_PATTERN.matcher(row[i]).replaceAll(""));
                }
                RowNum++;
            }

            FileOutputStream fileOutputStream = new FileOutputStream(xlsxFileAddress);
            workBook.write(fileOutputStream);
            fileOutputStream.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return workBook;
    }
}
