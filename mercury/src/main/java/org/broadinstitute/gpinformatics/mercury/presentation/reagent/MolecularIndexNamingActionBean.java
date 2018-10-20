package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * For uploading a spreadsheet, tsv, or csv text file of sequences and
 * generating molecular index names for them.
 *
 * Regardless of the type of input file it must have a header row whose columns are
 * molecular index positions or a delimited combination of index positions (e.g. P5+P7).
 *
 * The file's data rows are columns of one sequence or delimited multiple sequences,
 * each sequence being a string consisting of A,C,T,G,U characters.
 *
 * For each row a molecular index name (e.g. Illumina_P5-Wodel_P7-Focok) is looked up
 * or newly created for the index position combinations.
 */
@UrlBinding("/reagent/MolecularIndexNaming.action")
public class MolecularIndexNamingActionBean extends CoreActionBean {
    public static final String JSP_PAGE = "/reagent/mol_ind_scheme_naming.jsp";
    public static final String UPLOAD = "upload";
    public static final String DOWNLOAD = "download";
    // Regex of delimiters to split the header and data values.
    static final Pattern DELIMITER_REGEX = Pattern.compile("[|\\+\\- ]");
    // Used in the returned spreadsheet as header for the added name column.
    static final String NAME_HEADER = "Index Name";
    // Used in the returned spreadsheet for the index name value when name is unknown.
    static final String UNKNOWN_NAME = "(no name found)";
    // Error messages sent to the UI.
    static final String DUPLICATE_HEADER = "Header in column %d contains duplicate position \"%s\"";
    static final String BLANK_HEADER = "Header in column %d is blank. Please supply an index position for it or remove the column.";
    static final String BLANK_ROW = "Row %d is blank. Please remove that row.";
    static final String WRONG_COLUMN_COUNT = "Row %d has %d values but should have %d.";
    static final String UNKNOWN_POSITION = "Header column %d has unknown position \"%s\" (valid positions are %s).";

    static final String CONCATENATOR = ",";

    private List<String> technologies = new ArrayList<>();

    // The header names from the input file, possibly having embedded delimiters.
    private List<String> sequenceFileHeaderNames = new ArrayList();

    // The index positions found in the header names. These are 1:1 with the sequences in dataRows
    // i.e. no embedded delimiters here.
    private List<MolecularIndexingScheme.IndexPosition> indexPositions = new ArrayList();

    // The collection of uploaded sequences, organized as a list of rows, with each row consisting of
    // a string concatenation of the individual sequences. Note the concatenator used here is internal
    // and unrelated to any upload index combination delimiters.
    private List<String> dataRows = new ArrayList<>();

    // The list of existing or new molecular index names, one for each dataRow.
    private List<String> molecularIndexNames = new ArrayList<>();

    @Validate(required = true, on = {UPLOAD})
    private FileBean fileBean;

    @Validate(required = true, on = {UPLOAD})
    private String technology;

    private boolean createMissingNames;
    private InputStream inputStream;
    private String filename;

    @Inject
    private MolecularIndexingSchemeFactory molecularIndexingSchemeFactory;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(JSP_PAGE);
    }

    @HandlesEvent(UPLOAD)
    public Resolution upload() {
        int nullCount = 0;
        indexPositions.clear();
        dataRows.clear();
        molecularIndexNames.clear();

        if (inputStream != null && StringUtils.isNotBlank(filename)) {
            // Determines the file type from the file name extension.
            boolean isExcel = !filename.endsWith(".csv") && !filename.endsWith(".tsv");

            // Regardless of the input file type, parses the file into these collections:
            // - The header on each column is put into sequenceFileHeaderNames.
            // - Each position parsed from the headers is put into indexPositions.
            // - Each sequence in a data row is put into dataRows.
            try {
                if (isExcel) {
                    PoiSpreadsheetParser.processSingleWorksheet(inputStream, new MolecularIndexNamingProcessor());
                } else {
                    parseTsvOrCsv(inputStream);
                }
            } catch (Exception e) {
                addGlobalValidationError((isExcel ? "Error while reading Excel file " : "Error while reading ") +
                        filename + ": ", e);
            }

            // Using the sequence combinations in each dataRow, looks up an existing molecular indexing scheme.
            // If the checkbox for creating missing names is on, then creates the missing indexes and names.
            if (!hasErrors()) {
                int rowNum = 0;
                for (String dataRow : dataRows) {
                    ++rowNum;
                    String[] sequences = dataRow.split(CONCATENATOR);
                    if (sequences.length != indexPositions.size()) {
                        addGlobalValidationError("Expected " + indexPositions.size() + " sequences on row " +
                                rowNum + " but found " + sequences.length);
                    }
                    List<MolecularIndexingSchemeFactory.IndexPositionPair> indexPositionPairs = new ArrayList<>();
                    for (int i = 0; i < indexPositions.size(); i++) {
                        indexPositionPairs.add(new MolecularIndexingSchemeFactory.IndexPositionPair(
                                indexPositions.get(i), sequences[i]));
                    }
                    MolecularIndexingScheme mis = null;
                    try {
                        mis = createMissingNames ?
                                molecularIndexingSchemeFactory.findOrCreateIndexingScheme(indexPositionPairs, true) :
                                molecularIndexingSchemeFactory.findIndexingScheme(indexPositionPairs);
                    } catch (IllegalArgumentException e) {
                        // Ignores the missing index exception and counts it as a null.
                    }
                    nullCount += (mis == null) ? 1 : 0;
                    molecularIndexNames.add(mis != null ? mis.getName() : UNKNOWN_NAME);
                }
                addMessage("Found " + molecularIndexNames.size() + " indexes in the upload.");
                if (nullCount > 0) {
                    addMessage(nullCount + " indexes are unknown. Re-upload and check \"Create\" to add them to Mercury.");
                }
            }
        }
        return new ForwardResolution(JSP_PAGE);
    }

    /**
     * Passes a spreadsheet with headers, sequences, and names back to the browser.
     */
    @HandlesEvent(DOWNLOAD)
    public Resolution download() {
        Resolution resolution = new ForwardResolution(JSP_PAGE);
        try {
            if (!indexPositions.isEmpty() && !dataRows.isEmpty() && !molecularIndexNames.isEmpty()) {
                final byte[] returnedSpreadsheet = makeSpreadsheet(indexPositions, dataRows, molecularIndexNames);
                indexPositions.clear();
                dataRows.clear();
                molecularIndexNames.clear();
                // The spreadsheet must be the only data returned to the page. No UI messages are given.
                resolution = new Resolution() {
                    @Override
                    public void execute(HttpServletRequest in, HttpServletResponse response) throws Exception {
                        response.setContentType("application/ms-excel");
                        response.setContentLength(returnedSpreadsheet.length);
                        response.setHeader("Expires:", "0"); // eliminates browser caching
                        response.setHeader("Content-Disposition", "attachment; filename=IndexNames.xls");
                        OutputStream outStream = response.getOutputStream();
                        outStream.write(returnedSpreadsheet);
                        outStream.flush();
                    }
                };
            } else {
                addGlobalValidationError("No data available. Please re-upload.");
            }
        } catch (Exception e) {
            addGlobalValidationError("Cannot create spreadsheet to download " + e);
        }
        return resolution;
    }

    /**
     * TableProcessor implementation for parsing the spreadsheet. Expects one header row and
     * one or more data rows having one or more columns. The column count and header names
     * are not predefined.
     */
    private class MolecularIndexNamingProcessor extends TableProcessor {
        private boolean hasHeaderError = false;

        public MolecularIndexNamingProcessor() {
            super(null);
        }

        @Override
        public List<String> getHeaderNames() {
            // Returns the actual header names found in the spreadsheet.
            return sequenceFileHeaderNames;
        }

        @Override
        public void processHeader(List<String> headers, int row) {
            processSpreadsheetHeader(headers);
            hasHeaderError = hasErrors();
        }

        @Override
        public void processRowDetails(Map<String, String> dataRow, int dataRowIndex, boolean requiredValuesPresent) {
            // Skips parsing the data if there are header errors.
            if (!hasHeaderError) {
                validateAndCollectRowData(dataRow, dataRowIndex);
            }
        }

        @Override
        protected ColumnHeader[] getColumnHeaders() {
            // There are no pre-defined header names so an empty array is returned here.
            return new ColumnHeader[0];
        }

        @Override
        public void close() {
            // Does nothing.
        }
    }

    private void parseTsvOrCsv(InputStream inputStream) throws IOException {
        String separator = filename.endsWith(".tsv") ? "\\t" : ",";
        List<String> lines = IOUtils.readLines(inputStream);
        for (int i = 0; i < lines.size(); ++i) {
            int rowNumber = i + 1;
            String line = lines.get(i);
            if (StringUtils.isBlank(line)) {
                addGlobalValidationError(String.format(BLANK_ROW, rowNumber));
            } else {
                // The header row.
                if (i == 0) {
                    processSpreadsheetHeader(Arrays.asList(line.split(separator)));
                    // Skips parsing the data if there are header errors.
                    if (hasErrors()) {
                        break;
                    }
                } else {
                    // The data rows.
                    String[] columns = line.split(separator);
                    if (columns.length != sequenceFileHeaderNames.size()) {
                        addGlobalValidationError(String.format(WRONG_COLUMN_COUNT, rowNumber, columns.length,
                                sequenceFileHeaderNames.size()));
                    } else {
                        validateAndCollectRowData(columns, rowNumber);
                    }
                }
            }
        }
    }

    /**
     * Validates and collects the headers that are present in the spreadsheet.
     */
    private void processSpreadsheetHeader(List<String> headers) {
        sequenceFileHeaderNames.clear();
        for (int i = 0; i < headers.size(); ++i) {
            String header = StringUtils.strip(headers.get(i).trim(), "\\\"").toUpperCase();
            int columnNumber = i + 1;
            if (StringUtils.isBlank(header)) {
                addGlobalValidationError(String.format(BLANK_HEADER, columnNumber));
            }
            sequenceFileHeaderNames.add(header);
            for (String position : DELIMITER_REGEX.split(header)) {
                if (StringUtils.isNotBlank(position)) {
                    boolean found = false;
                    for (MolecularIndexingScheme.IndexPosition indexPosition :
                            MolecularIndexingScheme.IndexPosition.values()) {
                        if (indexPosition.getTechnology().equals(technology) &&
                                indexPosition.getPosition().equals(position)) {
                            found = true;
                            if (indexPositions.contains(indexPosition)) {
                                addGlobalValidationError(String.format(DUPLICATE_HEADER, columnNumber, position));
                            } else {
                                indexPositions.add(indexPosition);
                            }
                        }
                    }
                    if (!found) {
                        List<String> validPositions = new ArrayList<>();
                        for (MolecularIndexingScheme.IndexPosition indexPosition :
                                MolecularIndexingScheme.IndexPosition.values()) {
                            if (indexPosition.getTechnology().equals(technology)) {
                                validPositions.add(indexPosition.getPosition());
                            }
                        }
                        Collections.sort(validPositions);
                        addGlobalValidationError(String.format(UNKNOWN_POSITION, columnNumber, position,
                                StringUtils.join(validPositions, ", ")));
                    }
                }
            }
        }
    }

    /** Validates and collects the data from one input file data row. */
    private void validateAndCollectRowData(Map<String, String> dataMap, int dataRowIndex) {
        List<String> dataRow = new ArrayList<>();
        for (String header : sequenceFileHeaderNames) {
            dataRow.add(dataMap.get(header));
        }
        validateAndCollectRowData(dataRow.toArray(new String[0]), dataRowIndex);
    }

    /** Validates and collects the data from one input file data row. */
    private void validateAndCollectRowData(String[] columns, int dataRowIndex) {
        List<String> dataRow = new ArrayList<>();
        boolean allValid = true;
        for (String column : columns) {
            String value = StringUtils.strip(column.trim(), "\\\"").toUpperCase();
            for (String sequence : DELIMITER_REGEX.split(value)) {
                Pair<Boolean, String> pair = MolecularIndex.validatedUpperCaseSequence(sequence);
                boolean isValid = pair.getLeft();
                String sequenceOrMessage = pair.getRight();
                if (isValid) {
                    dataRow.add(sequenceOrMessage);
                } else {
                    allValid = false;
                    addGlobalValidationError("At row " + dataRowIndex + " column " + column + ": " +
                            sequenceOrMessage);
                }
            }
        }
        if (allValid) {
            if (dataRow.size() != indexPositions.size()) {
                addGlobalValidationError(String.format(WRONG_COLUMN_COUNT, dataRowIndex , dataRow.size(),
                        indexPositions.size()));
            } else {
                dataRows.add(StringUtils.join(dataRow, CONCATENATOR));
            }
        }
    }

    public static byte[] makeSpreadsheet(List<MolecularIndexingScheme.IndexPosition> indexPositions,
            List<String> dataRows, List<String> molecularIndexNames) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        int rowIndex = 0;

        // Writes a row of header names.
        int column = 0;
        Row row = sheet.createRow(rowIndex++);
        for (MolecularIndexingScheme.IndexPosition indexPosition : indexPositions) {
            String headerName = indexPosition.getPosition();
            row.createCell(column).setCellValue(headerName);
            ++column;
        }
        if (molecularIndexNames != null) {
            // Adds the index name column.
            row.createCell(column).setCellValue(NAME_HEADER);
        }

        // Writes the dataRow followed by the looked up or created mis name.
        for (int i = 0; i < dataRows.size(); ++i) {
            row = sheet.createRow(rowIndex++);
            column = 0;
            for (String sequence : dataRows.get(i).split(CONCATENATOR)) {
                row.createCell(column).setCellValue(sequence);
                ++column;
            }
            if (molecularIndexNames != null) {
                // Writes the index name value.
                Cell cell = row.createCell(column);
                cell.setCellValue(molecularIndexNames.get(i));
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        byte[] bytes = outputStream.toByteArray();
        IOUtils.closeQuietly(outputStream);
        return bytes;
    }

    public String getTechnology() {
        return technology;
    }

    public boolean isCreateMissingNames() {
        return createMissingNames;
    }

    public List<String> getTechnologies() {
        if (technologies.isEmpty()) {
            Set<String> names = new HashSet<>();
            for (MolecularIndexingScheme.IndexPosition indexPosition : MolecularIndexingScheme.IndexPosition.values()) {
                names.add(indexPosition.getTechnology());
            }
            technologies.addAll(names);
            Collections.sort(technologies);
        }
        return technologies;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSequenceFile(FileBean fileBean) {
        try {
            inputStream = fileBean.getInputStream();
            filename = fileBean.getFileName();
        } catch (IOException e) {
            addGlobalValidationError("Cannot read file " + filename);
        }
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public void setCreateMissingNames(boolean createMissingNames) {
        this.createMissingNames = createMissingNames;
    }

    /** Setter for testing. */
    public void setMolecularIndexingSchemeFactory(MolecularIndexingSchemeFactory molecularIndexingSchemeFactory) {
        this.molecularIndexingSchemeFactory = molecularIndexingSchemeFactory;
    }

    public List<MolecularIndexingScheme.IndexPosition> getIndexPositions() {
        return indexPositions;
    }

    public void setIndexPositions(List<MolecularIndexingScheme.IndexPosition> indexPositions) {
        this.indexPositions = indexPositions;
    }

    public List<String> getDataRows() {
        return dataRows;
    }

    public void setDataRows(List<String> dataRows) {
        this.dataRows = dataRows;
    }

    public List<String> getMolecularIndexNames() {
        return molecularIndexNames;
    }

    public void setMolecularIndexNames(List<String> molecularIndexNames) {
        this.molecularIndexNames = molecularIndexNames;
    }
}
