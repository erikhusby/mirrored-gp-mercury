package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An Excel spreadsheet processor that parses into a list of rows.
 * The first is the header row, followed by the data rows.
 */
public class GenericTableProcessor extends TableProcessor {
    private int headerRowIndex = 0;
    private List<List<String>> headerAndDataRows = new ArrayList<>();

    public GenericTableProcessor() {
        super("Sheet1");
    }

    @Override
    public List<String> getHeaderNames() {
        return headerAndDataRows.get(headerRowIndex);
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerAndDataRows.clear();
        headerAndDataRows.add(headers);
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int rowIndex, boolean requiredValuesPresent) {
        List<String> dataValues = new ArrayList<>();
        // Puts data values in a list ordered by header name.
        for (String header : getHeaderNames()) {
            dataValues.add(dataRow.get(header));
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
}