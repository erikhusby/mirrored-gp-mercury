package org.broadinstitute.gpinformatics.infrastructure.common;

import javafx.scene.input.DataFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CommonUtils {

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
            final String[] fileNameSplit = StringUtils.split(fileName, ".");
            String xlsxFileAddress = fileNameSplit[0]+".xlsx"; //xlsx file address
            XSSFSheet sheet = workBook.createSheet("sheet1");
            XSSFDataFormat fmt = workBook.createDataFormat();
            CellStyle textStyle = workBook.createCellStyle();
            textStyle.setDataFormat(fmt.getFormat("@"));
            sheet.setDefaultColumnStyle(0, textStyle);

            String currentLine=null;
            int RowNum=0;
            BufferedReader br = new BufferedReader(new InputStreamReader(csvFileInput));
            while ((currentLine = br.readLine()) != null) {
                String str[] = currentLine.split(",");
                XSSFRow currentRow=sheet.createRow(RowNum);
                for(int i=0;i<str.length;i++){
                    currentRow.createCell(i).setCellValue(str[i]);
                }
                RowNum++;
            }

            FileOutputStream fileOutputStream =  new FileOutputStream(xlsxFileAddress);
            workBook.write(fileOutputStream);
            fileOutputStream.close();

        } catch (Exception ex) {
            System.out.println(ex.getMessage()+"Exception in try");
        }
        return workBook;
    }
}
