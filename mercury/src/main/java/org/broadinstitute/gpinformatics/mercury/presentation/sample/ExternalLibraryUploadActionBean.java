package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@UrlBinding(value = "/sample/ExternalLibraryUpload.action")
public class ExternalLibraryUploadActionBean extends CoreActionBean {
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private static final String DOWNLOAD_TEMPLATE = "downloadTemplate";
    private static final String UPLOAD = "upload";
    private boolean overWriteFlag;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Validate(required = true, on = {UPLOAD})
    private FileBean samplesSpreadsheet;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(UPLOAD)
    public Resolution accession() {
        InputStream inputStream = null;
        try {
            inputStream = samplesSpreadsheet.getInputStream();
        } catch (IOException e) {
            addMessage("Cannot upload spreadsheet: " + e);
        }

        ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
        MessageCollection messageCollection = new MessageCollection();
        sampleInstanceEjb.doExternalUpload(inputStream, overWriteFlag, processor, messageCollection, null);
        addMessages(messageCollection);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(DOWNLOAD_TEMPLATE)
    public Resolution template() {
        HeaderValueRow[] headerValueRows = null;
        ColumnHeader[] columnHeaders = ExternalLibraryProcessor.Headers.values();
        try {
            ByteArrayOutputStream stream = templateSpreadsheet(headerValueRows, columnHeaders);
            final byte[] bytes = stream.toByteArray();
            IOUtils.closeQuietly(stream);

            Resolution resolution = (request, response) -> {
                response.setContentType("application/ms-excel");
                response.setContentLength(bytes.length);
                response.setHeader("Expires:", "0"); // eliminates browser caching
                response.setHeader("Content-Disposition", "attachment; filename=testxls.xls");
                OutputStream outStream = response.getOutputStream();
                outStream.write(bytes);
                outStream.flush();
            };
            return resolution;

        } catch (IOException e) {
            addMessage("Cannot generate spreadsheet: " + e.toString());
            return view();
        }
    }

    /**
     * Makes a template spreadsheet containing headers.
     */
    private ByteArrayOutputStream templateSpreadsheet(HeaderValueRow[] headerValues, ColumnHeader[] columnHeaders)
            throws IOException {

        // Makes data for the dropdown lists.
        String[] validAnalysisTypes = analysisTypeDao.findAll().stream().
                map(AnalysisType::getBusinessKey).
                sorted().
                collect(Collectors.toList()).toArray(new String[0]);
        String[] validReferenceSequence = referenceSequenceDao.findAllCurrent().stream().
                map(ReferenceSequence::getName).
                sorted().
                collect(Collectors.toList()).toArray(new String[0]);
        String[] validSequencingTechnology = Arrays.asList(IlluminaFlowcell.FlowcellType.values()).stream().
                filter(flowcellType -> flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.YES).
                map(IlluminaFlowcell.FlowcellType::getTechnology).
                sorted().
                collect(Collectors.toList()).toArray(new String[0]);
        String[] validAggregationDataTypes = (
                new ArrayList<String>() {{
                    add("");
                    addAll(productDao.findAggregationDataTypes());
                }}).toArray(new String[0]);

        // Makes the header names for the drowdown columns.
        String dataAnalysisTypeHeader = ExternalLibraryProcessor.Headers.DATA_ANALYSIS_TYPE.getText();
        String referenceSequenceHeader = ExternalLibraryProcessor.Headers.REFERENCE_SEQUENCE.getText();
        String sequencingTechnologyHeader = ExternalLibraryProcessor.Headers.SEQUENCING_TECHNOLOGY.getText();
        String aggregationDataTypeHeader = ExternalLibraryProcessor.Headers.AGGREGATION_DATA_TYPE.getText();

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet1 = workbook.createSheet("samples");
        final String sheet2Name = "listInfo";
        HSSFSheet sheet2 = workbook.createSheet(sheet2Name);

        // Makes a mapping from type of data presence to the cell's background color.
        Map<ExternalLibraryProcessor.DataPresence, Short> colorMap = new HashMap<>();
        colorMap.put(ExternalLibraryProcessor.DataPresence.REQUIRED, HSSFColor.RED.index);
        colorMap.put(ExternalLibraryProcessor.DataPresence.ONCE_PER_TUBE, HSSFColor.PINK.index);
        colorMap.put(ExternalLibraryProcessor.DataPresence.OPTIONAL, HSSFColor.TAN.index);
        colorMap.put(ExternalLibraryProcessor.DataPresence.IGNORED, HSSFColor.GREY_25_PERCENT.index);
        // Tweaks the colors for better appearance in Excel.
        HSSFPalette palette = workbook.getCustomPalette();
        palette.setColorAtIndex(HSSFColor.RED.index, (byte)255, (byte)64, (byte)64);
        palette.setColorAtIndex(HSSFColor.PINK.index, (byte)255, (byte)160, (byte)148);
        palette.setColorAtIndex(HSSFColor.TAN.index, (byte)255, (byte)220, (byte)240);
        palette.setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte)200, (byte)195, (byte)190);

        // Puts the dropdown list content in sheet2. This sheet only holds the lists of
        // possible values for dropdowns in sheet1, so its appearance is not important.
        final int REF_SEQ_LIST_COLUMN = 0;
        final int ANALYSIS_LIST_COLUMN = 1;
        final int SEQ_TECH_LIST_COLUMN = 2;
        final int AGG_DATATYPE_LIST_COLUMN = 3;
        final int numberValidationRows = 1 + Collections.max(Arrays.asList(validSequencingTechnology.length,
                validAnalysisTypes.length, validReferenceSequence.length, validAggregationDataTypes.length));
        for (int index = 0; index < numberValidationRows; ++index) {
            sheet2.createRow(index);
        }
        // Puts a warning in the first row.
        sheet2.getRow(0).createCell(REF_SEQ_LIST_COLUMN).
                setCellValue("THESE AUTO-GENERATED LISTS MUST NOT BE EDITED.");
        // Writes the reference sequence values available in Mercury starting in the second row.
        Iterator<Row> rowIterator = sheet2.rowIterator();
        rowIterator.next();
        for (String value : validReferenceSequence) {
            rowIterator.next().createCell(REF_SEQ_LIST_COLUMN).setCellValue(value);
        }
        // Writes the analysis type values available in Mercury starting in the second row.
        rowIterator = sheet2.rowIterator();
        rowIterator.next();
        for (String value : validAnalysisTypes) {
            rowIterator.next().createCell(ANALYSIS_LIST_COLUMN).setCellValue(value);
        }
        // Writes the sequencing technology values available in Mercury starting in the second row.
        rowIterator = sheet2.rowIterator();
        rowIterator.next();
        for (String value : validSequencingTechnology) {
            rowIterator.next().createCell(SEQ_TECH_LIST_COLUMN).setCellValue(value);
        }
        // Writes the aggregation data type values available in Mercury starting in the second row.
        rowIterator = sheet2.rowIterator();
        rowIterator.next();
        for (String value : validAggregationDataTypes) {
            rowIterator.next().createCell(AGG_DATATYPE_LIST_COLUMN).setCellValue(value);
        }
        sheet2.autoSizeColumn(REF_SEQ_LIST_COLUMN);
        sheet2.autoSizeColumn(ANALYSIS_LIST_COLUMN);
        sheet2.autoSizeColumn(SEQ_TECH_LIST_COLUMN);
        sheet2.autoSizeColumn(AGG_DATATYPE_LIST_COLUMN);

        // Writes the color coded headers in sheet1.
        int rowIndex = 0;
        // A blank row.
        sheet1.createRow(rowIndex++).createCell(0).setCellValue("");

        // Writes rows of headerValue pairs, if any.
        if (headerValues != null) {
            for (HeaderValueRow headerValue : headerValues) {
                Row row = sheet1.createRow(rowIndex++);
                Cell cell = row.createCell(0);
                cell.setCellValue(headerValue.getText());
                // All the data in the header is currently ignored.
                HSSFCellStyle style = workbook.createCellStyle();
                style.setFillForegroundColor(colorMap.get(ExternalLibraryProcessor.DataPresence.IGNORED));
                style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                cell.setCellStyle(style);
            }
            sheet1.createRow(rowIndex++).createCell(0).setCellValue(""); //a blank row
        }

        // Writes the color coded headers in successive rows in a sheet1 column.
        final int colorColumnIdx = 3;
        final int maxColorRow = 5;
        // Either uses the existing header-value rows, or creates some blank ones.
        while(rowIndex < maxColorRow + 2) {
            sheet1.createRow(rowIndex++);
        }
        int colorRowIdx = 1;
        for (Pair<ExternalLibraryProcessor.DataPresence, String> pair : Arrays.asList(
                Pair.of((ExternalLibraryProcessor.DataPresence)null, "Cell color indicates:"),
                Pair.of(ExternalLibraryProcessor.DataPresence.REQUIRED, " Required "),
                Pair.of(ExternalLibraryProcessor.DataPresence.ONCE_PER_TUBE, " Required Once per Tube "),
                Pair.of(ExternalLibraryProcessor.DataPresence.OPTIONAL, " Optional "))) {

            HSSFCellStyle style = workbook.createCellStyle();
            style.setBorderTop(colorRowIdx == 1 ? CellStyle.BORDER_THIN : CellStyle.BORDER_NONE);
            style.setBorderTop(CellStyle.BORDER_NONE);
            style.setBorderLeft(CellStyle.BORDER_THIN);
            style.setBorderRight(CellStyle.BORDER_THIN);
            style.setBorderBottom(CellStyle.BORDER_NONE);
            if (pair.getLeft() != null) {
                style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                style.setFillForegroundColor(colorMap.get(pair.getLeft()));
            }
            Row row = sheet1.getRow(colorRowIdx++);
            Cell cell = row.createCell(colorColumnIdx);
            cell.setCellValue(pair.getRight());
            cell.setCellStyle(style);
        }
        // Puts a border on the top and bottom color cells.
        sheet1.getRow(1).getCell(colorColumnIdx).getCellStyle().setBorderTop(CellStyle.BORDER_THIN);
        sheet1.getRow(colorRowIdx - 1).getCell(colorColumnIdx).getCellStyle().setBorderBottom(CellStyle.BORDER_THIN);

        // A blank row.
        sheet1.createRow(rowIndex++).createCell(0).setCellValue("");

        // Makes the style elements to be used with the header row cells.
        Map<ExternalLibraryProcessor.DataPresence, HSSFCellStyle> headerStyles = new HashMap<>();
        for (ExternalLibraryProcessor.DataPresence dataPresence : ExternalLibraryProcessor.DataPresence.values()) {
            HSSFCellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(colorMap.get(dataPresence));
            style.setFillPattern(CellStyle.SOLID_FOREGROUND);
            headerStyles.put(dataPresence, style);
        }

        // Writes a row of header names in cells colored according to the column header properties.
        Row headerRow = sheet1.createRow(rowIndex++);
        Row dropdownRow = sheet1.createRow(rowIndex);
        int column = 0;
        for (ColumnHeader columnHeader : columnHeaders) {
            String headerName = columnHeader.getText();
            if (!headerName.isEmpty()) {
                Cell cell = headerRow.createCell(column);
                dropdownRow.createCell(column);
                cell.setCellValue(headerName);

                ExternalLibraryProcessor.DataPresence dataPresence =
                        ((ColumnHeader.Ignorable)columnHeader).getDataPresenceIndicator();
                if (headerStyles.containsKey(dataPresence)) {
                    cell.setCellStyle(headerStyles.get(dataPresence));
                }
                // If this cell requires a categorical value, puts a dropdown lists of valid values
                // in the corresponding cell of the next row.
                Character referenceColumn = null;
                int length = 0;
                if (columnHeader.getText().equalsIgnoreCase(referenceSequenceHeader)) {
                    dropdownRow.getCell(column).setCellValue(validReferenceSequence[0]);
                    referenceColumn = (char)('A' + REF_SEQ_LIST_COLUMN);
                    length = validReferenceSequence.length + 1;
                } else if (columnHeader.getText().equalsIgnoreCase(dataAnalysisTypeHeader)) {
                    dropdownRow.getCell(column).setCellValue(validAnalysisTypes[0]);
                    referenceColumn = (char)('A' + ANALYSIS_LIST_COLUMN);
                    length = validAnalysisTypes.length + 1;
                } else if (columnHeader.getText().equalsIgnoreCase(sequencingTechnologyHeader)) {
                    dropdownRow.getCell(column).setCellValue(validSequencingTechnology[0]);
                    referenceColumn = (char)('A' + SEQ_TECH_LIST_COLUMN);
                    length = validSequencingTechnology.length + 1;
                } else if (columnHeader.getText().equalsIgnoreCase(aggregationDataTypeHeader)) {
                    dropdownRow.getCell(column).setCellValue(validAggregationDataTypes[0]);
                    referenceColumn = (char)('A' + AGG_DATATYPE_LIST_COLUMN);
                    length = validAggregationDataTypes.length + 1;
                }
                if (referenceColumn != null) {
                    // Uses a cell range that is a column on sheet2 that contains the dropdown list values.
                    DVConstraint dropdownContent = DVConstraint.createFormulaListConstraint(
                            String.format("%s!$%s$%d:$%s$%d",
                                    sheet2Name, referenceColumn, 2, referenceColumn, length));
                    CellRangeAddressList dropdownCell = new CellRangeAddressList(dropdownRow.getRowNum(),
                            dropdownRow.getRowNum(), column, column);
                    DataValidation dropdownList = sheet1.getDataValidationHelper().
                            createValidation(dropdownContent, dropdownCell);
                    dropdownList.setEmptyCellAllowed(false);
                    dropdownList.setShowErrorBox(true);
                    sheet1.addValidationData(dropdownList);
                }
                sheet1.autoSizeColumn(column);
                ++column;
            }
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        return stream;
    }

    public void setSamplesSpreadsheet(FileBean spreadsheet) {
        this.samplesSpreadsheet = spreadsheet;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }

    public boolean isOverWriteFlag() {
        return overWriteFlag;
    }
}
