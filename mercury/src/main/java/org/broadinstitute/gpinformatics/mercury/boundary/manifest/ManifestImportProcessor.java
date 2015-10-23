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
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalResource;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.jvnet.inflector.Noun;

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
    static final String UNKNOWN_HEADER_FORMAT = "Unknown header(s) '%s'.";
    static final String DUPLICATE_HEADER_FORMAT = "Duplicate header found: %s";

    protected ManifestImportProcessor() {
        super(null, IgnoreTrailingBlankLines.YES);
    }

    @Override
    public List<String> getHeaderNames() {
        return ManifestHeader.headerNames(columnHeaders);
    }

    @Override
    public void validateHeaderRow(List<String> headers) {
        List<String> errors = new ArrayList<>();
        ManifestHeader.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        if (!errors.isEmpty()){
            getMessages()
                    .add(String.format("Unknown %s '%s' present.", Noun.pluralOf("header", errors.size()), errors));
        }
    }

    /**
     * Process headers of Manifest file.
     * Validations protect against duplicate headers and unknown headers
     *
     * @param headers List header strings in this row
     * @param row     The row number in the file.
     */
    @Override
    public void processHeader(List<String> headers, int row) {
        List<String> errors = new ArrayList<>();
        List<String> seenHeaders = new ArrayList<>();
        for (String header : headers) {
            if (seenHeaders.contains(header)) {
                addDataMessage(String.format(DUPLICATE_HEADER_FORMAT, header), row);
            }
            seenHeaders.add(header);
        }

        Collection<? extends ColumnHeader> foundHeaders =
                ManifestHeader.fromColumnName(errors, headers.toArray(new String[headers.size()]));
        columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
        if (!errors.isEmpty()) {
            addDataMessage(String.format(UNKNOWN_HEADER_FORMAT, errors), row);
        }
    }

    /**
     * Iterate through the data and add it to the list of ManifestRecords.
     */
    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {

        validateRow(dataRow, dataRowIndex);

        ManifestRecord manifestRecord = new ManifestRecord(ManifestHeader.toMetadata(dataRow));
        // The dataRowIndex is 1-based, but the manifest index is 0-based.
        manifestRecord.setManifestRecordIndex(dataRowIndex - 1);
        manifestRecords.add(manifestRecord);
    }

    private void validateRow(Map<String, String> dataRow, int dataRowIndex) {
        for(Map.Entry<String, String> rowEntry: dataRow.entrySet()) {
            Metadata.Key metadataKey = Metadata.Key.fromDisplayName(rowEntry.getKey());
            MaterialType materialType = MaterialType.fromDisplayName(rowEntry.getValue());
            if (metadataKey == Metadata.Key.MATERIAL_TYPE && (materialType == MaterialType.NONE || materialType == null)) {
                addDataMessage(ClinicalResource.UNRECOGNIZED_MATERIAL_TYPE + ": " + rowEntry.getValue(), dataRowIndex);
            }
        }
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
        return ManifestHeader.values();
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
        return PoiSpreadsheetParser.processSingleWorksheet(inputStream, this);
    }
}
