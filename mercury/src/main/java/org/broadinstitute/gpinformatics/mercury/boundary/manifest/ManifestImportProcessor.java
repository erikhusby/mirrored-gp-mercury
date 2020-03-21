/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.CommonUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalResource;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for parsing <b>Buick</b> manifest files which are used when importing sample metadata.
 * <p/>
 * <br/>See:<ul>
 * <li><a href="https://labopsconfluence.broadinstitute.org/display/GPI/Sample+Receipt+and+Accessioning">
 * Sample Receipt and Accessioning</a></li>
 * </ul>
 */
public class ManifestImportProcessor extends TableProcessor {
    private static final int ALLOWABLE_NUMBER_OF_SHEETS = 1;
    public static final String EMPTY_FILE_ERROR = "The file uploaded was empty.";
    public static final String NO_DATA_ERROR = "The uploaded Manifest has no data.";
    private ColumnHeader[] columnHeaders;
    private List<ManifestRecord> manifestRecords = new ArrayList<>();
    private ManifestSessionEjb.AccessioningProcessType accessioningProcess;
    private String importFileName;
    List<String> errors = new ArrayList<>();

    private FileType uploadFileType = FileType.EXCEL;

    public enum FileType {
        EXCEL, CSV
    }

    private ManifestImportProcessor() {
        this(ManifestSessionEjb.AccessioningProcessType.CRSP, "");
    }

    public ManifestImportProcessor(ManifestSessionEjb.AccessioningProcessType processType,
                                   String importFileName) {
        this(processType, importFileName, FileType.EXCEL);
    }

    public ManifestImportProcessor(ManifestSessionEjb.AccessioningProcessType processType,
                                   String importFileName, FileType parserFileType) {
        super(null, IgnoreTrailingBlankLines.YES);
        accessioningProcess = processType;
        this.importFileName = importFileName;
        this.uploadFileType = parserFileType;
    }
    @Override
    public List<String> getHeaderNames() {
        return ColumnHeader.headerNames(columnHeaders);
    }

    /**
     * Process headers of Manifest file.
     *
     * @param headers List header strings in this row
     * @param rowIndex the 0-based row index.
     */
    @Override
    public void processHeader(List<String> headers, int rowIndex) {
        Collection<? extends ColumnHeader> foundHeaders = null;
        if(accessioningProcess == ManifestSessionEjb.AccessioningProcessType.CRSP) {
            foundHeaders = ManifestHeader.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        } else if(accessioningProcess == ManifestSessionEjb.AccessioningProcessType.COVID) {
            foundHeaders = CovidHeader.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        }
        columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
    }

    /**
     * Iterate through the data and add it to the list of ManifestRecords.
     * @param rowIndex the 0-based row index.
     * @param requiredValuesPresent
     */
    @Override
    public void processRowDetails(Map<String, String> dataRow, int rowIndex, boolean requiredValuesPresent) {

        validateRow(dataRow, rowIndex);

        ManifestRecord manifestRecord = null;
        if(accessioningProcess == ManifestSessionEjb.AccessioningProcessType.CRSP) {
            manifestRecord = new ManifestRecord(ManifestHeader.toMetadata(dataRow));
        } else if (accessioningProcess == ManifestSessionEjb.AccessioningProcessType.COVID){
            manifestRecord = new ManifestRecord(CovidHeader.toMetadata(dataRow));
        }
        manifestRecord.setManifestRecordIndex(rowIndex);
        manifestRecords.add(manifestRecord);
    }

    /**
     * Adds error message for invalid material type.
     * @param rowIndex the 0-based row index.
     */
    private void validateRow(Map<String, String> dataRow, int rowIndex) {
        for(Map.Entry<String, String> rowEntry: dataRow.entrySet()) {
            Metadata.Key metadataKey = Metadata.Key.fromDisplayName(rowEntry.getKey());
            if (metadataKey == Metadata.Key.MATERIAL_TYPE && !MaterialType.isValid(rowEntry.getValue())) {
                addDataMessage(ClinicalResource.UNRECOGNIZED_MATERIAL_TYPE + ": " + rowEntry.getValue(), rowIndex);
            }
        }
    }

    @Override
    public void validateHeaderRow(List<String> headers) {
        getErrors().addAll(getWarnings());
        getWarnings().clear();
    }

    /**
     * Once the import is complete, the resulting data can be obtained here.
     *
     * @return All ManifestRecords from parsed file.
     *
     * @throws ValidationException if there were any errors.
     */
    public List<ManifestRecord> getManifestRecords() throws ValidationException {
        if (columnHeaders == null && manifestRecords.isEmpty()) {
            getMessages().add(EMPTY_FILE_ERROR);
        } else if (manifestRecords.isEmpty()) {
            getMessages().add(NO_DATA_ERROR);
        }
        if (!getMessages().isEmpty()) {
            throw new ValidationException("There was an error importing the Manifest.", getMessages());
        }
        return manifestRecords;
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        ColumnHeader[] values = null;
        if(accessioningProcess == ManifestSessionEjb.AccessioningProcessType.CRSP) {
            values = ManifestHeader.values();
        } else if (accessioningProcess == ManifestSessionEjb.AccessioningProcessType.COVID) {
            values = CovidHeader.values();
        }
        return values;
    }

    @Override
    public void close() {

    }

    /**
     * In this implementation of TableProcessor, all messages are errors. This method simply returns the messages.
     *
     * @return A list of validation messages created when the spreadsheet was parsed.
     */
    public List<String> getErrors() {
        return getMessages();
    }

    /**
     * In Buick, we only allow workbooks with one worksheet.
     *
     * @param actualNumberOfSheets number of sheets in workbook
     *
     * @throws ValidationException if the actualNumber of sheets differs from ALLOWABLE_NUMBER_OF_SHEETS.
     */
    @Override
    public void validateNumberOfWorksheets(int actualNumberOfSheets) throws ValidationException {
        if (actualNumberOfSheets != ALLOWABLE_NUMBER_OF_SHEETS) {
            String errorMessage =
                    String.format("Expected %d Worksheets, but Workbook has %d", ALLOWABLE_NUMBER_OF_SHEETS,
                            actualNumberOfSheets);
            throw new ValidationException(errorMessage);
        }
    }

    /**
     * Read a single worksheet from the specified InputStream using this ManifestImportProcessor.
     */
    public List<String> processSingleWorksheet(InputStream inputStream)
            throws InvalidFormatException, IOException, ValidationException {
        List<String> strings = new ArrayList<>();
        if(uploadFileType == FileType.CSV) {
            final XSSFWorkbook convertedWorkbook = CommonUtils.csvToXLSX(inputStream, this.importFileName);
            strings = PoiSpreadsheetParser.processSingleWorksheet(convertedWorkbook, this);
        } else {
            strings = PoiSpreadsheetParser.processSingleWorksheet(inputStream, this);
        }
        return strings;
    }
}
