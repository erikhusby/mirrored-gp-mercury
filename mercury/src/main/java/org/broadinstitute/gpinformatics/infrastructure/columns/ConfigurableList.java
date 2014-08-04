package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//import org.apache.poi.ss.usermodel.Workbook;

/**
 * <p>
 * This class supports lists with configurable column sets. This feature is used for
 * search results. The column sets are configured using preferences. A global column
 * definition preference holds a list of column definitions; each column has a sortable
 * expression (plain text) and a renderable expression (an HTML link to a detail page, for
 * example).
 * </p>
 * <p>
 * There are column set Preferences at global, group, project and user level, that hold
 * named sets of columns, with each set holding a visibility expression (determines
 * whether the user can see the set) and a list of column names from the global definition
 * preference. The lists are sortable on any column, in ascending or descending order. <br/>
 * </p>
 * <p>
 * To support downloads of paged search results, this class allows you to accumulate list
 * rows, as follows:
 * </p>
 *
 * configurableListUtils = ConfigurableList(<br/>
 * <i>get first page</i><br/>
 * configurableListUtils.addRows(<br/>
 * <i>get second page</i><br/>
 * configurableListUtils.addRows(<br/>
 * resultList = configurableListUtils.getResultList(
 */
public class ConfigurableList {

    public interface AddRowsListener {
        void addRows(List<?> entityList, Map<String, Object> context, List<ColumnTabulation> nonPluginTabulations);
    }

    private final List<AddRowsListener> addRowsListeners = new ArrayList<>();

    private final List<ColumnTabulation> pluginTabulations = new ArrayList<>();

    private final List<ColumnTabulation> nonPluginTabulations = new ArrayList<>();

    private final Integer sortColumnIndex;

    private final String sortDirection;

    private final String multiValueDelimiter;

    private final boolean multiValueAddTrailingDelimiter;

    private List<SortColumn> sortColumnIndexes;

//    private final Boolean isAdmin;

    /**
     * For multiple-page lists, we need to know on which row the current page starts.
     */
    private int pageStartingRow = 0;

    /**
     * Map from column definition name to Header Group.
     */
    private final Map<String, HeaderGroup> headerGroupMap = new LinkedHashMap<>();

    /**
     * List of Rows of Cells, each Cell has a reference to a Header.
     */
    private final List<Row> rows = new ArrayList<>();

    /**
     * A means to access the ID field in the entity we're listing.
     */
    private final ColumnEntity columnEntity;

    private static final Format mdyFormat = FastDateFormat.getInstance("MM/dd/yyyy");

    public static final String DEFAULT_MULTI_VALUE_DELIMITER = " ";

    public static final boolean DEFAULT_MULTI_VALUE_ADD_TRAILING_DELIMITER = true;

    /**
     * Get the column names for a given column set name, and information required to find the preference.
     *
     * @param columnSetName which preference to use
     * @param columnSetType type of column set
     * @param columnSets    holds column sets
     * @return list of column names
     */
    public static ColumnSetsPreference.ColumnSet getColumnNameList(String columnSetName, ColumnSetType columnSetType,
            /*BspDomainUser bspDomainUser, Group group,*/
            ColumnSetsPreference columnSets) {

        Map<String, Object> context = new HashMap<>();
        context.put("columnSetType", columnSetType);
/*
        context.put("bspDomainUser", bspDomainUser);
        context.put("group", group);
*/

        // Find the set the user wanted.
        ColumnSetsPreference.ColumnSet columnSet1 = null;
        for (ColumnSetsPreference.ColumnSet columnSet : columnSets.getColumnSets()) {
            if (columnSet.getName().equals(columnSetName)) {
                Boolean useSet = true;
/*
                try {
                    useSet = (Boolean) MVEL.eval(columnSet.getList().get(0), context);
                } catch (Exception e) {
                    throw new RuntimeException("Evaluating expression for " + columnSet.getName(), e);
                }
*/
                if (useSet) {
                    // Remove the visibility expression entry
//                    columnSet1 = columnSet.getList().subList(1, columnSet.getList().size());
                    columnSet1 = columnSet;
                    break;
                }
            }
        }

        if (columnSet1 == null) {
            throw new RuntimeException("Failed to find column list preference for " + columnSetName);
        }
        return columnSet1;
    }

    public enum ColumnSetType {
        /**
         * A column set for viewing in a table on a web page.
         */
        VIEW,
        /**
         * A column set for downloading to Excel
         */
        DOWNLOAD
    }

    public ConfigurableList(List<ColumnTabulation> columnTabulations, Integer sortColumnIndex,
            String sortDirection, /*Boolean admin, */ @Nonnull ColumnEntity columnEntity) {
        this(columnTabulations, sortColumnIndex, sortDirection, /*admin, */columnEntity,
                DEFAULT_MULTI_VALUE_DELIMITER, DEFAULT_MULTI_VALUE_ADD_TRAILING_DELIMITER);
    }

    /**
     * Constructor.
     *
     * @param columnTabulations The tabulations used.
     * @param sortColumnIndex The column to sort.
     * @param sortDirection Ascending or descending.
     * @param columnEntity The field id.
     * @param multiValueDelimiter The text to use as the delimiter between values in a multi-valued field.
     * @param multiValueAddTrailingDelimiter Whether to include a trailing delimiter in multi-valued fields.
     */
    public ConfigurableList(List<ColumnTabulation> columnTabulations, Integer sortColumnIndex,
            String sortDirection, /*Boolean admin, */ @Nonnull ColumnEntity columnEntity, String multiValueDelimiter,
            boolean multiValueAddTrailingDelimiter) {
        this.columnEntity = columnEntity;
        this.multiValueDelimiter = multiValueDelimiter;
        this.multiValueAddTrailingDelimiter = multiValueAddTrailingDelimiter;
        for (ColumnTabulation columnTabulation : columnTabulations) {
            // Ignore header logic on nested table ColumnTabulation
            if( !columnTabulation.isNestedParent() ) {
                headerGroupMap.put(columnTabulation.getName(), new HeaderGroup(columnTabulation.getName()));
            }
        }
        this.sortColumnIndex = sortColumnIndex;
        this.sortDirection = sortDirection;
//        isAdmin = admin;

        // Get the plugin tabulations and the non plugin tabulations.
        for (ColumnTabulation columnTabulation : columnTabulations) {
            if (columnTabulation.getPluginClass() != null) {
                pluginTabulations.add(columnTabulation);
            } else {
                nonPluginTabulations.add(columnTabulation);
            }
        }

    }

    /**
     * Constructor without sort direction.
     *
     * @param columnTabulations The tabulations used.
     * @param sortColumnIndexes The columns to sort by.
     * @param columnEntity The field id.
     */
    public ConfigurableList(List<ColumnTabulation> columnTabulations, List<SortColumn> sortColumnIndexes,
            @Nonnull ColumnEntity columnEntity) {

        this(columnTabulations, null, null, /*admin, */columnEntity);
        if (sortColumnIndexes != null && !sortColumnIndexes.isEmpty()) {
            this.sortColumnIndexes = new ArrayList<>(sortColumnIndexes);
        }
    }

    /**
     * Holds information about column headers.
     */
    public static class Header {
        /**
         * Set only after headers have been sorted within groups, determines order of cells in rows.
         */
        private int order;

        /**
         * Single header for viewing results.
         */
        private final String viewHeader;

        /**
         * First row of double header for downloading results.
         */
        private final String downloadHeader1;

        /**
         * second row of double header for downloading results.
         */
        private final String downloadHeader2;

        /**
         * The HTML display table includes links to re-sort the results, so we need the sortPath.
         */
        private final String sortPath;

        Header(String viewHeader, String downloadHeader1, String downloadHeader2, String sortPath) {
            this.viewHeader = viewHeader;
            this.downloadHeader1 = downloadHeader1;
            this.downloadHeader2 = downloadHeader2;
            this.sortPath = sortPath;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getViewHeader() {
            return viewHeader;
        }

        public String getDownloadHeader1() {
            return downloadHeader1;
        }

        public String getDownloadHeader2() {
            return downloadHeader2;
        }

        public String getSortPath() {
            return sortPath;
        }

        /**
         * Helper method to create a single combined header.
         *
         * @return The combined header.
         */
        public String getDownloadHeaderCombined() {
            if (getDownloadHeader1() != null && getDownloadHeader1().trim().length() > 0
                && getDownloadHeader2() != null && getDownloadHeader2().trim().length() > 0) {
                return String.format("%s (%s)", getDownloadHeader1().trim(), getDownloadHeader2().trim());
            } else if (getDownloadHeader1() != null) {
                return getDownloadHeader1().trim();
            } else {
                return "";
            }
        }
    }

    /**
     * Each column definition has its own HeaderGroup, because plugins and some
     * expressions (e.g. phenotypes with intervals) yield multiple columns. Headers within
     * a group are sorted alphabetically.
     */
    static class HeaderGroup {
        private final String name;

        private final TreeMap<String, Header> headerMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        HeaderGroup(String name) {
            this.name = name;
        }

        void addHeader(Header header) {
            headerMap.put(header.getViewHeader(), header);
        }

        public String getName() {
            return name;
        }

        public TreeMap<String, Header> getHeaderMap() {
            return headerMap;
        }
    }

    /**
     * Denormalizes HeaderGroups into a flat list of Headers
     *
     * @return list of all Headers, extracted from HeaderGroups
     */
    List<Header> getHeaders() {
        List<Header> headers = new ArrayList<>();
        int order = 0;
        for (HeaderGroup headerGroup : headerGroupMap.values()) {
            for (Header header : headerGroup.getHeaderMap().values()) {
                header.setOrder(order);
                headers.add(header);
                order++;
            }
        }
        return headers;
    }

    /**
     * Represents a Cell in a Row, with a reference to a Header. The Cells are added to
     * rows in the order they are encountered, and may be re-ordered as they are converted
     * to a ResultList, depending on the order field in their Header.
     */
    static class Cell {
        private final Header header;

        private String sortableValue;

        private String formattedValue;

        Cell(Header header, String sortableValue, String formattedValue) {
            this.header = header;

            // If CRSP, protect against JS Injection.
            if (ApplicationInstance.CRSP.isCurrent()) {
                // Escapes the sortable string to avoid JS Injection.
                if (sortableValue != null) {
                    this.sortableValue = StringEscapeUtils.escapeXml(sortableValue);
                }
                // Utilizes the escaped sortable value to also escape the same thing within the formatted value.
                if (formattedValue != null && sortableValue != null) {

                    // Make sure to replace all instances of sortable values within the formatted values if the strings
                    // aren't the same.  If they are the same we don't need to re-run the escape code.
                    if (!formattedValue.equals(sortableValue)) {
                        this.formattedValue =
                                escapeSortableValueWithinFormatted(sortableValue, formattedValue, this.sortableValue);
                    } else {
                        this.formattedValue = this.sortableValue;
                    }

                } else if (formattedValue != null) {
                    this.formattedValue = StringEscapeUtils.escapeXml(formattedValue);
                }
            } else {
                this.sortableValue = sortableValue;
                this.formattedValue = formattedValue;
            }
        }

        public Header getHeader() {
            return header;
        }

        public String getSortableValue() {
            return sortableValue;
        }

        public String getFormattedValue() {
            return formattedValue;
        }

        /**
         * ADds additional values to the current cell.
         *
         * @param sortableValueToAdd  Sortable value to add...
         * @param formattedValueToAdd Formatted value to add.
         */
        public void addToValues(String sortableValueToAdd, String formattedValueToAdd) {
            sortableValue += " " + sortableValueToAdd;
            formattedValue += " " + formattedValueToAdd;
        }
    }

    /**
     * Represents a row of values for a particular entity. The values are not aligned into
     * columns until they are converted to a ResultList
     */
    static class Row {
        private final String id;

        private final List<Cell> cells = new ArrayList<>();

        /**
         * Hold the entity list for a nested table at the row level (required when building ResultList)
         */
        private final Map<ColumnTabulation, Collection<?>> nestedTableEntities = new LinkedHashMap<>();

        Row(String id) {
            this.id = id;
        }

        void addCell(Cell cell) {
            cells.add(cell);
        }

        String getId() {
            return id;
        }

        List<Cell> getCells() {
            return cells;
        }

        /**
         * Access nested tables when building ResultList
         * @return All nested tables for this row
         */
        Map<ColumnTabulation, Collection<?>> getNestedTableEntities() {
            return nestedTableEntities;
        }
    }

    /**
     * Adds rows to a list, by evaluating column definition expressions.
     * Supplies an empty context but some eval expressions require context objects (e.g. BSP user list)
     *
     * @param entityList list of objects, where each object is likely to be the root of a
     *                   graph that is navigated by column definition expressions.
     */
    @Deprecated
    public void addRows(List<?> entityList) {
        Map<String, Object> context = new HashMap<>();
        addRows(entityList, context);
    }

    /**
     * Enhanced addRows to provide ability to supply context objects
     * @param entityList list of objects, where each object is likely to be the root of a
     *                   graph that is navigated by column definition expressions.
     * @param context Any required context objects (e.g.as required in eval expressions
     */
    public void addRows(List<?> entityList, @Nonnull Map<String, Object> context ) {

//        context.put("isAdmin", isAdmin);

        for (AddRowsListener addRowsListener : addRowsListeners) {
            addRowsListener.addRows(entityList, context, nonPluginTabulations);
        }
        for (Object entity : entityList) {
            // evaluate expression to get ID
            Row row = new Row(columnEntity.getIdGetter().getId(entity));
            rows.add(row);
            for (ColumnTabulation columnTabulation : nonPluginTabulations) {
                if( !columnTabulation.isNestedParent() ) {
                    recurseColumns(context, entity, row, columnTabulation, columnTabulation.getName());
                } else {
                    Collection<?> nestedEntities = columnTabulation.evalNestedTableExpression(entity, context);
                    if( nestedEntities != null && nestedEntities.size() > 0 ) {
                        row.getNestedTableEntities().put(columnTabulation, nestedEntities);
                    }
                }
            }
        }

        try {
            // Call the plugins, and add their data to the accumulated rows.
            for (ColumnTabulation columnTabulation : pluginTabulations) {
                // Plugins not supported for nested tables
                if( columnTabulation.isNestedParent() ) continue;

                ListPlugin listPlugin = (ListPlugin) Class.forName(columnTabulation.getPluginClass()).getConstructor().
                        newInstance();
                List<Row> pluginRows = listPlugin.getData(entityList, headerGroupMap.get(columnTabulation.getName()));
                int rowIndex = pageStartingRow;
                for (Row row : pluginRows) {
                    // TODO jmt rows might be empty, if columns are all plugins
                    Row existingRow = rows.get(rowIndex);
                    for (Cell cell : row.getCells()) {
                        existingRow.addCell(cell);
                    }

                    rowIndex++;
                }
            }

            pageStartingRow = rows.size();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void recurseColumns(Map<String, Object> context, Object entity, Row row, ColumnTabulation columnTabulation,
            String headerGroupName) {
        HeaderGroup headerGroup = headerGroupMap.get(headerGroupName);

        // Evaluate value expression (value and header count and order must be same, unless header count == 1).
        Object formattedValue;
        Object plainTextValue = columnTabulation.evalPlainTextExpression(entity, context);
        if (columnTabulation.isOnlyPlainText()) {
            formattedValue = plainTextValue;
        } else {
            formattedValue = columnTabulation.evalFormattedExpression(entity, context);
        }

        Object viewHeaderResult = columnTabulation.evalViewHeaderExpression(entity, context);

        // The result could be a list, e.g. there could be multiple trait values for multiple intervals.
        List<Object> plainTextValues;
        if (plainTextValue instanceof List) {
            plainTextValues = (List<Object>) plainTextValue;
        } else {
            plainTextValues = new ArrayList<>();
            plainTextValues.add(plainTextValue);
        }
        List<Object> formattedValues;
        if (formattedValue instanceof List) {
            formattedValues = (List<Object>) formattedValue;
        } else {
            formattedValues = new ArrayList<>();
            formattedValues.add(formattedValue);
        }
        List<String> viewHeaders;
        if (viewHeaderResult instanceof List) {
            viewHeaders = (List<String>) viewHeaderResult;
            // If the headers are all the same (e.g. multiple values for a trait), prefix a
            // sequence number, so the columns sort correctly.
            // TODO jmt Need a flag to indicate whether to comma-separate multiple values,
            // or put them in sequence number columns.
            if (viewHeaders.size() > 1) {
                String previous = viewHeaders.get(0);
                boolean same = true;
                for (String viewHeader : viewHeaders) {
                    if (!viewHeader.equals(previous)) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    for (int i = 0; i < viewHeaders.size(); i++) {
                        @SuppressWarnings("StringBufferReplaceableByString")
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append('#');
                        stringBuilder.append(i + 1);
                        stringBuilder.append(' ');
                        stringBuilder.append(viewHeaders.get(i));
                        viewHeaders.set(i, stringBuilder.toString());
                    }
                }
            }
        } else {
            viewHeaders = new ArrayList<>();
            viewHeaders.add((String) viewHeaderResult);
        }

        // If we received multiple values for a single header (e.g. Participant aliases for
        // a pooled sample) convert the multiple values to a single value.
        if (viewHeaders.size() == 1) {
            StringBuilder stringBuilder = new StringBuilder();
            convertResultToString(plainTextValues, stringBuilder, multiValueDelimiter, multiValueAddTrailingDelimiter);
            plainTextValues.clear();
            plainTextValues.add(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            convertResultToString(formattedValues, stringBuilder, multiValueDelimiter, multiValueAddTrailingDelimiter);
            formattedValues.clear();
            formattedValues.add(stringBuilder.toString());
        }

        // Determine which column to put each value in
        int valueIndex = 0;
        for (Object currentValue : plainTextValues) {
            String currentViewHeader = viewHeaders.get(valueIndex);
            Header header = headerGroup.getHeaderMap().get(currentViewHeader);
            // TODO jmt two columns with same name currently overwrite (e.g. search term
            // TODO value and pre-defined column value) If this is the first time we've seen this header.
            if (header == null) {
                header = new Header(currentViewHeader, currentViewHeader, null, columnTabulation.getDbSortPath());
                headerGroup.getHeaderMap().put(currentViewHeader, header);
            }
            String plainString = currentValue == null ? null : currentValue.toString();
            String formattedString = formattedValues.get(valueIndex) == null ? null : formattedValues.get(valueIndex)
                    .toString();
            Cell cell = new Cell(header, plainString, formattedString);
            row.addCell(cell);
            valueIndex++;
        }

        for (ColumnTabulation childTabulation : columnTabulation.getChildColumnTabulations()) {
            recurseColumns(context, entity, row, childTabulation, headerGroupName);
        }
    }

    /**
     * Build a simple non-sortable ResultList to use as a nested table
     * @param columnTabulation Descriptor for nested table and all child columns
     * @param nestedEntityList Collection of entities in nested table
     * @param context Objects which may be required in evaluation
     * @return Simple nested table data set for use in UI
     */
    private ResultList buildNestedTable(ColumnTabulation columnTabulation, Collection<?> nestedEntityList, Map<String, Object> context) {
        List<ResultRow> rows = new ArrayList<>();
        List<Header> headers = new ArrayList<>();
        List<Comparable<?>> emptySortableCells = new ArrayList<>();

        for( ColumnTabulation nestedColumnTabulation : columnTabulation.getNestedEntityColumns() ) {
            String nestedName = nestedColumnTabulation.getName();
            headers.add(new Header(nestedName, nestedName, null, null));
        }

        for( Object entity : nestedEntityList ) {
            List<String> cells = new ArrayList<>();
            for( ColumnTabulation nestedColumnTabulation : columnTabulation.getNestedEntityColumns() ) {
                cells.add(nestedColumnTabulation.evalPlainTextExpression( entity, context ).toString());
            }
            ResultRow row = new ResultRow( emptySortableCells, cells, null );
            rows.add(row);
        }

        return new ResultList(rows, headers, 0, "ASC" );
    }

    /**
     * Converts from Rows of compact Cells to a sparse matrix with empty cells to align
     * each cell with its associated header.
     *
     * @return data structure intended to be easy to render to HTML or spreadsheet.
     */
    public ResultList getResultList() {

        List<Header> headers = getHeaders();
        List<ResultRow> resultRows = new ArrayList<>();

        for (Row row : rows) {

            List<Comparable<?>> sortableCells = new ArrayList<>();
            List<String> renderableCells = new ArrayList<>();
            for (Cell cell : row.getCells()) {

                int order = cell.getHeader().getOrder();
                int size = sortableCells.size();
                for (int i = size; i <= order; i++) {
                    sortableCells.add(null);
                    renderableCells.add(null);
                }

                // TODO jmt the cell could have been set already, e.g. multiple values for
                // TODO a trait, and no sequence numbers in headers.

                renderableCells.set(order, cell.getFormattedValue());
                sortableCells.set(order, cell.getSortableValue());

                /*
                 * TODO jdeffen somehow deal with the problem of having one header
                 * TODO reference for multiple columns.
                 */
                // renderableCells.set(order, getCellValue(renderableCells.get(order),
                // cell.getFormattedValue()));
                // sortableCells.set(order, getCellValue((sortableCells.get(order) == null
                // ? null : sortableCells.get(order).toString()),
                // cell.getSortableValue()));
            }
            // Add cells if the row is too short.
            for (int i = sortableCells.size(); i < headers.size(); i++) {

                sortableCells.add(null);
                renderableCells.add(null);
            }
            ResultRow resultRow = new ResultRow(sortableCells, renderableCells, row.getId());

            // Build nested tables
            for( Map.Entry<ColumnTabulation, Collection<?>> entry : row.getNestedTableEntities().entrySet() ){
                //TODO jms Deal with context (store as member variable?)
                ResultList nestedTable = buildNestedTable(entry.getKey(), entry.getValue(), new HashMap<String, Object>());
                resultRow.addNestedTable( entry.getKey().getName() , nestedTable );
            }

            resultRows.add(resultRow);
        }

        // In-memory sort if only one page.
        if (sortColumnIndexes != null) {
            Collections.sort(resultRows, new ResultRowComparator(sortColumnIndexes));
        } else if (sortColumnIndex != null) {
            Collections.sort(resultRows, new ResultRowComparator(sortColumnIndex, sortDirection));
        }
        return new ResultList(resultRows, headers, sortColumnIndex, sortDirection);
    }

    // See the above TODO with respect to multiple values.
    // private String getCellValue(String previousValue, String nextValue) {
    //
    // String result = null;
    //
    // if (previousValue == null) {
    // result = nextValue;
    // }
    // else {
    // result = previousValue + ", " + nextValue;
    // }
    // return result;
    // }

    /**
     * For testing, writes a result list to an HTML table.
     *
     * @param printWriter Where to write the HTML.
     * @param resultList  The list to write.
     */
    public void writeHtmlTable(PrintWriter printWriter, ResultList resultList) {
        printWriter.println("<html><body><table border='1'>");
        int rowNum = 0;
        for (ResultRow row : resultList.getResultRows()) {
            if (rowNum % 500 == 0) {
                for (Header header : resultList.getHeaders()) {
                    printWriter.println("<th>" + header.getViewHeader() + "</th>");
                }
            }
            printWriter.println("<tr>");
            for (String cell : row.getRenderableCells()) {
                printWriter.println("<td>" + (cell == null ? "&nbsp;" : cell) + "</td>");
            }
            printWriter.println("</tr>");
            if ((rowNum + 1) % 500 == 0) {
                printWriter.println("</table><table border='1'>");
            }
            rowNum++;
        }
        printWriter.println("</table></body></html>");
        printWriter.flush();

    }

    /**
     * Converts to String the results of an expression evaluation.
     *
     * @param expressionResult results of expression
     * @param stringBuilder    accumulated string
     * @param separator        string to use to separate list entries
     */
    private static void convertResultToString(Object expressionResult, StringBuilder stringBuilder, String separator,
            boolean multiValueAddTrailingDelimiter) {
        // Do nothing if the expression is null.
        if (expressionResult != null) {
            if (expressionResult instanceof List) {
                List<?> resultList = (List<?>) expressionResult;
                for (Object result : resultList) {
                    if (result != null) {
                        convertResultToString(result, stringBuilder, separator, multiValueAddTrailingDelimiter);
                        int size = resultList.size();
                        if (size > 1) {
                            // Add a separator if this is not the last element or a trailing separator is requested.
                            if (result != resultList.get(size - 1) || multiValueAddTrailingDelimiter) {
                                stringBuilder.append(separator);
                            }
                        }
                    }
                }
            } else if (expressionResult instanceof String) {
                stringBuilder.append(expressionResult);
            } else if (expressionResult instanceof Number || expressionResult instanceof Boolean) {
                stringBuilder.append(expressionResult.toString());
            } else if (expressionResult instanceof Date) {
                stringBuilder.append(mdyFormat.format(expressionResult));
            } else {
                throw new RuntimeException("Unexpected return from expression " + expressionResult);
            }
        }
    }

    /**
     * A list of results, including column names and rows of data.
     */
    public static class ResultList {
        private final List<ResultRow> resultRows;

        List<Header> headers;

        /**
         * For in-memory sorts, which column the user is sorting on.
         */
        private final Integer resultSortColumnIndex;

        /**
         * The direction the user is sorting in, ASC or DSC.
         */
        private final String resultSortDirection;

        public ResultList(List<ResultRow> resultRows, List<Header> headers, Integer sortColumnIndex,
                String sortDirection) {
            this.resultRows = resultRows;
            this.headers = headers;
            resultSortColumnIndex = sortColumnIndex;
            resultSortDirection = sortDirection;
        }

        /**
         * Gets results as an array, for use with SpreadsheetCreator.
         *
         * @return 2d array of cells
         */
        public Object[][] getAsArray() {

            // Calculate how many rows and columns required using nested tables
            int rows, cols;
            // Default for no nested tables
            rows = getResultRows().size() + 2;
            cols = getHeaders().size();

            // Some rows have 1 or more nested tables, some don't.
            // Adjust array size as required
            for (ConfigurableList.ResultRow resultRow : getResultRows()) {
                int nestCols;
                for( ResultList nestedTable: resultRow.getNestedTables().values() ){
                    rows += nestedTable.getResultRows().size() + 1;
                    nestCols = nestedTable.getHeaders().size() + 1;
                    if( nestCols > cols ) cols = nestCols;
                }
            }

            Object rowObjects[][] = new Object[rows][cols];

            // Set the first (name) and second (units, metadata) headers.
            int columnNumber;
            boolean headerRow2Present = false;
            for (columnNumber = 0; columnNumber < getHeaders().size(); columnNumber++) {
                ConfigurableList.Header header = getHeaders().get(columnNumber);
                rowObjects[0][columnNumber] = header.getDownloadHeader1();
                String header2Name = header.getDownloadHeader2();
                if (header2Name != null && header2Name.length() > 0) {
                    rowObjects[1][columnNumber] = header2Name;
                    headerRow2Present = true;
                }
            }
            // Set the data
            int rowNumber = headerRow2Present ? 2 : 1;
            for (ConfigurableList.ResultRow resultRow : getResultRows()) {
                columnNumber = 0;
                for (Comparable<?> comparable : resultRow.getSortableCells()) {
                    rowObjects[rowNumber][columnNumber] = comparable;
                    columnNumber++;
                }
                rowNumber++;

                // Nested tables
                columnNumber = 0;
                for( Map.Entry<String,ResultList> nestedTable : resultRow.getNestedTables().entrySet() ) {
                    rowObjects[rowNumber][columnNumber] = nestedTable.getKey();
                    columnNumber++;
                    rowNumber++;
                    rowNumber = appendNestedRows(rowNumber, 1, nestedTable.getValue(), rowObjects);
                }

            }
            return rowObjects;
        }

        /**
         * Append nested table rows to 2 dimensional Excel output data array
         * (Assume no deeper than 1 layer)
         * @param rowIndex
         * @param startColumn
         * @param resultList
         * @param rowObjects
         * @return
         */
        private int appendNestedRows( int rowIndex, int startColumn, ResultList resultList, Object rowObjects[][] ){

            int col = startColumn;

            for( Header header : resultList.getHeaders() ) {
                rowObjects[rowIndex][col] = header.getViewHeader();
                col++;
            }
            rowIndex++;

            for ( ConfigurableList.ResultRow resultRow : resultList.resultRows ) {
                col = startColumn;
                for (String val : resultRow.getRenderableCells()) {
                    rowObjects[rowIndex][col] = val;
                    col++;
                }
                rowIndex++;
            }

            return rowIndex;
        }

        public List<ResultRow> getResultRows() {
            return resultRows;
        }

        public List<Header> getHeaders() {
            return headers;
        }

        public Integer getResultSortColumnIndex() {
            return resultSortColumnIndex;
        }

        public String getResultSortDirection() {
            return resultSortDirection;
        }

/*
        public List<String> getSampleIds(int index) {
            List<String> resultIds = new ArrayList<> ();
            for (ConfigurableList.ResultRow resultRow : getResultRows()) {
                try {
                    resultIds.add(Sample.extractId(resultRow.getSortableCells().get(index).toString()));
                } catch (LSIDIdConversionException | MPGException e) {
                    // Should be able to extract the ids from the sample ids in the results, otherwise, get what we get
                }
            }
            return resultIds;
        }
*/
    }

    /**
     * This class is a row in a result list. The columns are configurable, so the list is dynamic.
     */
    public static class ResultRow {

        /**
         * List of plain data, that can be sorted.
         */
        private final List<Comparable<?>> sortableCells;

        /**
         * List of cells, where each cell is renderable, e.g. it may include HTML links.
         */
        private final List<String> renderableCells;

        /**
         * result ID, useful for add to basket etc.
         */
        private final String resultId;

        /**
         * String that when equal to {@code resultId} indicates a selected value.
         */
        private String checked;

        /**
         * Each row may have a nested table
         */
        private Map<String, ResultList> nestedTables = new HashMap<>();

        ResultRow(List<Comparable<?>> sortableCells, List<String> renderableCells, String resultId) {
            this.sortableCells = sortableCells;
            this.renderableCells = renderableCells;
            this.resultId = resultId;
        }

        public List<Comparable<?>> getSortableCells() {
            return sortableCells;
        }

        public List<String> getRenderableCells() {
            return renderableCells;
        }

        public String getResultId() {
            return resultId;
        }

        public String getChecked() {
            return checked;
        }

        public void setChecked(String checked) {
            this.checked = checked;
        }

        /**
         * This finds the position of the column name in the headers and then updates the value for that position.
         *
         * @param headers All the headers
         * @param columnName The name of the column to update
         * @param theValue The value to update
         */
        public void addValue(List<Header> headers, String columnName, Comparable<?> theValue) {

            // This is just using toString to get the rendered cell. Since this is for the specific case of
            // post processing, we can do something more complex when we need to.
            for (int index = 0; index < headers.size(); index++) {
                if (headers.get(index).getViewHeader().equals(columnName)) {
                    renderableCells.set(index, theValue.toString());
                    sortableCells.set(index, theValue);
                }
            }
        }

        /**
         * Any nested tables associated with this row, key is name of parent search term
         * @return Nested table collection for UI
         */
        public Map<String, ResultList> getNestedTables(){
            return nestedTables;
        }

        /**
         * Add a nested table to collection for this row
         * @param name Title of nested table
         * @param nestedTable Nested table ResultList data
         */
        public void addNestedTable(String name, ResultList nestedTable) {
            nestedTables.put(name, nestedTable);
        }
    }

    /**
     * Associate a sort column index with a direction.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class SortColumn {

        private Integer sortColumnIndex;

        private String sortDirection;

        public SortColumn(Integer sortColumnIndex, String sortDirection) {

            this.sortColumnIndex = sortColumnIndex;
            this.sortDirection = sortDirection;
        }

        public Integer getSortColumnIndex() {
            return sortColumnIndex;
        }

        public void setSortColumnIndex(Integer sortColumnIndex) {
            this.sortColumnIndex = sortColumnIndex;
        }

        public String getSortDirection() {
            return sortDirection;
        }

        public void setSortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
        }
    }

    /**
     * Used to sort rows, in result lists with configurable columns
     */
    public static class ResultRowComparator implements Comparator<ResultRow> {

        private int compareIndex;

        private String sortDirection;

        private List<SortColumn> sortColumnIndexes;

        public ResultRowComparator(int compareIndex, String sortDirection) {
            this.compareIndex = compareIndex;
            this.sortDirection = sortDirection;
        }

        public ResultRowComparator(List<SortColumn> sortColumnIndexes) {
            this.sortColumnIndexes = sortColumnIndexes;
        }

        @Override
        public int compare(ResultRow resultRow1, ResultRow resultRow2) {

            int result = 0;
            if (sortColumnIndexes == null) {
                result = compare(resultRow1, resultRow2, compareIndex, sortDirection);
            } else {

                for (SortColumn sort : sortColumnIndexes) {

                    result = compare(resultRow1, resultRow2, sort.getSortColumnIndex(), sort.getSortDirection());
                    /*
                     * If result == 0 then go on to the next sort column to evaluate
                     * subsequent sort by. If result != 0 then that is the answer.
                     */
                    if (result != 0) {
                        break;
                    }
                }
            }
            return result;
        }

        private static int compare(ResultRow resultRow1, ResultRow resultRow2, int index, String direction) {
            @SuppressWarnings("unchecked")
            Comparable<Comparable<?>> comparable1 =
                    (Comparable<Comparable<?>>) resultRow1.getSortableCells().get(index);
            Comparable<?> comparable2 = resultRow2.getSortableCells().get(index);
            if (comparable1 == null && comparable2 == null) {
                return 0;
            }
            if (comparable1 == null) {
                return -1;
            }
            if (comparable2 == null) {
                return 1;
            }

            int compareResult = comparable1.compareTo(comparable2);
            if (compareResult == 0) {
                return compareResult;
            }
            if (direction.equals("DSC")) {
                return -compareResult;
            }
            return compareResult;
        }
    }


    /**
     * Add the data from this instance as a sheet with the specified name to the workbook.
     */
/*
    public void createSampleInfoSheet(@Nonnull String sheetName, @Nonnull Workbook workbook) {
        SpreadsheetCreator.createSheet(sheetName, getResultList().getAsArray(), workbook);
    }
*/

    /**
     * This takes a formatted value and escapes the substrings pushed into the formatted value in all places it was
     * pushed into.
     *
     * @param substring         String pushed into the fully formatted string.
     * @param formattedValue    Formatted string which contains at least one copy of the sortable value.
     * @param escapedSubstring  Escaped sortable value we'd like to replace the sortableValue with.
     * @return fully formatted value where all instances of the substring are escaped.
     */
    public static String escapeSortableValueWithinFormatted(String substring, String formattedValue,
            String escapedSubstring) {
        StringBuilder finalString = new StringBuilder();
        String currentString = formattedValue;
        do {
            if (currentString.contains(substring)) {

                finalString.append(currentString.substring(0, currentString.indexOf(substring)))
                        .append(escapedSubstring);

                currentString = currentString.substring(
                        currentString.indexOf(substring) + substring.length());
            } else {
                finalString.append(currentString);
                break;
            }
        } while (!currentString.isEmpty());
        return finalString.toString();
    }

    public void addListener(AddRowsListener addRowsListener) {
        addRowsListeners.add(addRowsListener);
    }
}
