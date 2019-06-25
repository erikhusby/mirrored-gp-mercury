package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An Excel spreadsheet processor that parses into a list of rows, each row being a list of columns.
 */
public class GenericTableProcessor extends TableProcessor {
    private int headerRowIndex = 0;
    private final List<List<String>> headerAndDataRows = new ArrayList<>();
    private Supplier<List<String>> headerNamesSupplier;

    public GenericTableProcessor() {
        super("Sheet1");
        // By default the PoiSpreadsheetParser will use the first row as the header names.
        headerNamesSupplier = () -> headerAndDataRows.get(headerRowIndex);
    }

    @Override
    public List<String> getHeaderNames() {
        return headerNamesSupplier.get();
    }

    // To set a non-default Supplier of header names.
    public void setHeaderNamesSupplier(Supplier<List<String>> headerNamesSupplier) {
        this.headerNamesSupplier = headerNamesSupplier;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerAndDataRows.clear();
        headerAndDataRows.add(headers);
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int rowIndex, boolean requiredValuesPresent) {
        List<String> dataValues = new ArrayList<>();
        // Puts data values in a list ordered by either header name or column order.
        for (String headerName : getHeaderNames().isEmpty() ?
                dataRow.keySet().stream().sorted().collect(Collectors.toList()) : getHeaderNames()) {
            dataValues.add(dataRow.get(headerName));
        }
        headerAndDataRows.add(dataValues);
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        // Column headers are unknown, so returns an empty array to preclude error processing.
        return new ColumnHeader[0];
    }

    @Override
    public void close() {
    }

    public List<List<String>> getHeaderAndDataRows() {
        return headerAndDataRows;
    }

    public void removeBlankRows() {
        for (Iterator<List<String>> iterator = headerAndDataRows.iterator(); iterator.hasNext(); ) {
            List<String> columns = iterator.next();
            if (!columns.stream().filter(value -> StringUtils.isNotBlank(value)).findFirst().isPresent()) {
                iterator.remove();
            }
        }
    }
}