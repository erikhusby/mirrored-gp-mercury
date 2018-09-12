package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNewTech;
import org.broadinstitute.gpinformatics.mercury.control.sample.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@UrlBinding(value = "/sample/ExternalLibraryUpload.action")
public class ExternalLibraryUploadActionBean extends CoreActionBean {
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private static final String DOWNLOAD_TEMPLATE = "downloadTemplate";
    public static final String ACCESSION = "accession";
    public static final String MAKE_KIT = "makeKit";
    private boolean overWriteFlag;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    /**
     * The types of spreadsheet that can be uploaded.
     */
    public enum SpreadsheetType {
        PooledTubes("Pooled Tubes", VesselPooledTubesProcessor.class),
        EzPassLibraries("EZ Pass Libraries", ExternalLibraryProcessorEzPass.class),
        PooledMultiOrganismLibraries("New Tech Libraries", ExternalLibraryProcessorNewTech.class),
        BarcodeUpdates("Tube Barcode Updates", ExternalLibraryBarcodeUpdate.class);

        private String displayName;
        private Class processor;

        SpreadsheetType(String displayName, Class processor) {
            this.displayName = displayName;
            this.processor = processor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Class getProcessor() {
            return processor;
        }
    }

    @Validate(required = true, on = {ACCESSION, MAKE_KIT, DOWNLOAD_TEMPLATE})
    private SpreadsheetType spreadsheetType;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Validate(required = true, on = {ACCESSION, MAKE_KIT})
    private FileBean samplesSpreadsheet;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(MAKE_KIT)
    public Resolution makeKit() {
        if (spreadsheetType != SpreadsheetType.PooledMultiOrganismLibraries) {
            addMessage("A kit is not required for this type of spreadsheet.");
            return view();
        } else {
            return uploadSpreadsheet(true);
        }
    }

    @HandlesEvent(ACCESSION)
    public Resolution accession() {
        return uploadSpreadsheet(false);
    }

    public Resolution uploadSpreadsheet(boolean kitOnly) {
        InputStream inputStream = null;
        try {
            inputStream = samplesSpreadsheet.getInputStream();
        } catch (IOException e) {
            addMessage("Cannot upload spreadsheet: " + e);
        }

        Class processorClass = spreadsheetType.getProcessor();
        ExternalLibraryProcessor processor;
        try {
            processor = (ExternalLibraryProcessor) processorClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + processorClass.getCanonicalName() + ": " + e);
        }

        MessageCollection messageCollection = new MessageCollection();
        sampleInstanceEjb.doExternalUpload(inputStream, overWriteFlag, processor, messageCollection, null, kitOnly);
        addMessages(messageCollection);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(DOWNLOAD_TEMPLATE)
    public Resolution template() {
        HeaderValueRow[] headerValueRows = null;
        ColumnHeader[] columnHeaders = null;
        if (spreadsheetType.getProcessor().equals(VesselPooledTubesProcessor.class)) {
            columnHeaders = VesselPooledTubesProcessor.Headers.values();
        } else if (spreadsheetType.getProcessor().equals(ExternalLibraryProcessorEzPass.class)) {
            headerValueRows = ExternalLibraryProcessorEzPass.HeaderValueRows.values();
            columnHeaders = ExternalLibraryProcessorEzPass.Headers.values();
        } else if (spreadsheetType.getProcessor().equals(ExternalLibraryProcessorNewTech.class)) {
            headerValueRows = ExternalLibraryProcessorNewTech.HeaderValueRows.values();
            columnHeaders = ExternalLibraryProcessorNewTech.Headers.values();
        } else if (spreadsheetType.getProcessor().equals(ExternalLibraryBarcodeUpdate.class)) {
            columnHeaders = ExternalLibraryBarcodeUpdate.Headers.values();
        } else {
            throw new RuntimeException("Unsupported processor type: " +
                    spreadsheetType.getProcessor().getCanonicalName());
        }
        try {
            ByteArrayOutputStream stream = templateSpreadsheet(headerValueRows, columnHeaders);
            final byte[] bytes = stream.toByteArray();
            IOUtils.closeQuietly(stream);

            Resolution resolution = new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response)
                        throws Exception {
                    response.setContentType("application/ms-excel");
                    response.setContentLength(bytes.length);
                    response.setHeader("Expires:", "0"); // eliminates browser caching
                    response.setHeader("Content-Disposition", "attachment; filename=testxls.xls");
                    OutputStream outStream = response.getOutputStream();
                    outStream.write(bytes);
                    outStream.flush();
                }
            };
            return resolution;

        } catch (IOException e) {
            addMessage("Cannot generate spreadsheet: " + e.toString());
            return view();
        }
    }

    private final String dataAnalysisTypeHeader = ExternalLibraryProcessor.fixupHeaderName(
            ExternalLibraryProcessorNewTech.Headers.DATA_ANALYSIS_TYPE.getText());
    private final String referenceSequenceHeader = ExternalLibraryProcessor.fixupHeaderName(
            ExternalLibraryProcessorNewTech.Headers.REFERENCE_SEQUENCE.getText());
    private final String sequencingTechnologyHeader = ExternalLibraryProcessor.fixupHeaderName(
            ExternalLibraryProcessorNewTech.Headers.SEQUENCING_TECHNOLOGY.getText());

    /**
     * Makes a template spreadsheet containing headers.
     */
    private ByteArrayOutputStream templateSpreadsheet(HeaderValueRow[] headerValues, ColumnHeader[] columnHeaders)
            throws IOException {
        // Makes lists of valid values to put in the templates.
        List<String> validAnalysisTypes = analysisTypeDao.findAll().
                stream().map(AnalysisType::getBusinessKey).sorted().collect(Collectors.toList());
        List<String> validReferenceSequence = referenceSequenceDao.findAllCurrent().
                stream().map(ReferenceSequence::getName).sorted().collect(Collectors.toList());
        List<String> validSequencingTechnology = Arrays.asList(IlluminaFlowcell.FlowcellType.values()).stream().
                filter(flowcellType -> flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.YES).
                map(flowcellType -> SampleInstanceEjb.makeSequencerValue(flowcellType)).
                sorted().collect(Collectors.toList());

        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        int rowIndex = 0;

        // The first row shows the cell color code.
        Row colorRow = sheet.createRow(rowIndex++);
        int colIndex = 0;
        colorRow.createCell(colIndex++).setCellValue("Cell color indicates data presence is: ");
        Cell colorCell = colorRow.createCell(colIndex++);
        colorCell.setCellValue(" Required ");
        setBackground(workbook, colorCell, true, false, false);

        colorCell = colorRow.createCell(colIndex++);
        colorCell.setCellValue(" Required Once per Tube, Sample, Library, etc. ");
        setBackground(workbook, colorCell, false, false, true);

        colorCell = colorRow.createCell(colIndex++);
        colorCell.setCellValue(" Optional ");
        setBackground(workbook, colorCell, false, false, false);

        colorCell = colorRow.createCell(colIndex++);
        colorCell.setCellValue(" Ignored ");
        setBackground(workbook, colorCell, false, true, false);

        // Adds a blank row.
        sheet.createRow(rowIndex++).createCell(0).setCellValue("");

        // Writes the headerValue rows, if any, with name in column 0 and value in column 1.
        if (headerValues != null) {
            for (HeaderValueRow headerValue : headerValues) {
                Row row = sheet.createRow(rowIndex++);
                Cell col0 = row.createCell(0);
                col0.setCellValue(headerValue.getText());
                setBackground(workbook, col0, headerValue.isRequiredValue(), false, false);
                Cell col1 = row.createCell(1);
                col1.setCellValue("(value" + (headerValue.isRequiredValue() ? ")" : " or blank)"));
                setBackground(workbook, col1, headerValue.isRequiredValue(), false, false);
            }
            sheet.createRow(rowIndex++).createCell(0).setCellValue(""); //a blank row
        }

        // Writes a row of header names.
        int column = 0;
        Row headerRow = sheet.createRow(rowIndex++);
        Row firstDataRow = sheet.createRow(rowIndex++);
        for (ColumnHeader columnHeader : columnHeaders) {
            String headerName = columnHeader.getText();
            if (!headerName.isEmpty()) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headerName);
                setBackground(workbook, cell, columnHeader);

                Cell dataCell = firstDataRow.createCell(column);
                dataCell.setCellValue("(value" + (columnHeader.isRequiredValue() ? ")" : " or blank)"));
                setBackground(workbook, dataCell, columnHeader);

                ++column;
            }
        }
        sheet.createRow(rowIndex++).createCell(0).setCellValue(""); //a blank row

        // Writes the possible selections for some columns having categorical data values.
        int numberDataRows = Math.max(validSequencingTechnology.size(),
                Math.max(validAnalysisTypes.size(), validReferenceSequence.size()));
        String firstItem = "-- must be one of the following values --";
        for (int index = -1; index < numberDataRows; ++index) {
            Row dataRow = sheet.createRow(rowIndex++);
            column = 0;
            for (ColumnHeader columnHeader : columnHeaders) {
                String headerName = columnHeader.getText();
                if (!headerName.isEmpty()) {
                    Cell cell = dataRow.createCell(column++);
                    String fixedUpHeader = ExternalLibraryProcessor.fixupHeaderName(columnHeader.getText());
                    if (fixedUpHeader.equals(dataAnalysisTypeHeader)) {
                        cell.setCellValue(index < 0 ? firstItem :
                                index < validAnalysisTypes.size() ? validAnalysisTypes.get(index) : "");
                    } else if (fixedUpHeader.equals(referenceSequenceHeader)) {
                        cell.setCellValue(index < 0 ? firstItem :
                                index < validReferenceSequence.size() ? validReferenceSequence.get(index) : "");
                    } else if (fixedUpHeader.equals(sequencingTechnologyHeader)) {
                        cell.setCellValue(index < 0 ? firstItem :
                                index < validSequencingTechnology.size() ? validSequencingTechnology.get(index) : "");
                    } else {
                        cell.setCellValue("");
                    }
                }
            }
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        return stream;
    }

    /** Sets the cell color depending on whether the value is required, optional, or ignored. */
    private void setBackground(HSSFWorkbook workbook, Cell cell, ColumnHeader columnHeader) {
        boolean ignored = (columnHeader instanceof ColumnHeader.Ignorable) ?
                ((ColumnHeader.Ignorable) columnHeader).isIgnoredValue() : false;
        boolean isOnlyOnce = (columnHeader instanceof ColumnHeader.Ignorable) ?
                ((ColumnHeader.Ignorable) columnHeader).isOnlyOncePerEntity() : false;
        setBackground(workbook, cell, columnHeader.isRequiredValue(), ignored, isOnlyOnce);
    }

    /** Sets the cell color depending on whether the value is required, optional, or ignored. */
    private void setBackground(HSSFWorkbook workbook, Cell cell, boolean isRequiredValue, boolean isIgnoredValue,
            boolean isOnlyOncePerEntity) {
        PoiSpreadsheetParser.setBackgroundColor(workbook, cell,
                isRequiredValue ? HSSFColor.RED.index :
                        isOnlyOncePerEntity ? HSSFColor.LAVENDER.index :
                                isIgnoredValue ? HSSFColor.GREY_25_PERCENT.index : HSSFColor.WHITE.index);
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

    public SpreadsheetType getSpreadsheetType() {
        return spreadsheetType;
    }

    public void setSpreadsheetType(SpreadsheetType spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }
}
