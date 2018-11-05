package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.ResultParamValues;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.owasp.encoder.Encode;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * A way to capture high latency bulk data for a page of results
     */
    public interface AddRowsListener {
        void addRows(List<?> entityList, SearchContext context, Map<Integer,ColumnTabulation> nonPluginTabulations);
        void reset();
    }

    /**
     * Allow multiple named AddRowsListener instances for a single search
     */
    private final Map<String,AddRowsListener> addRowsListeners = new HashMap<>();

    /**
     Correlates custom result params with their associated ColumnTabluation **/
    private final Map<Integer,ResultParamValues> resultParamsMap = new HashMap<>();

    private final Map<Integer,ColumnTabulation> pluginTabulations = new HashMap<>();

    private final  Map<Integer,ColumnTabulation> nonPluginTabulations = new HashMap<>();

    private final Integer sortColumnIndex;

    private final String sortDirection;

    private final String multiValueDelimiter;

    private List<SortColumn> sortColumnIndexes;

    /**
     * Cache row plugin instances
     */
    private Map<Class<?>, ListPlugin> pluginCache = new HashMap<>();

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

    public static final String DEFAULT_MULTI_VALUE_DELIMITER = " ";

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

        // If re-enabled, do not allow this context to stomp any existing evaluation context!
//        SearchContext context = new SearchContext();
//        context.put(SearchInstance.CONTEXT_KEY_COLUMN_SET_TYPE, columnSetType);
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

    public ConfigurableList(List<ColumnTabulation> columnTabulations, Map<Integer, ResultParamValues> resultParamsMap, Integer sortColumnIndex,
                            String sortDirection, /*Boolean admin, */ @Nonnull ColumnEntity columnEntity) {
        this( columnTabulations, resultParamsMap, sortColumnIndex, sortDirection, /*admin, */columnEntity,
                DEFAULT_MULTI_VALUE_DELIMITER);
    }

    /**
     * Constructor without sort direction.
     * @param columnTabulations The tabulations used.
     * @param resultParamsMap Any result column parameters (indexed by associated column's position in columnTabulations)
     * @param sortColumnIndexes The columns and sorting configurations
     * @param columnEntity The base entity of the search result
     */
    public ConfigurableList(List<ColumnTabulation> columnTabulations, Map<Integer, ResultParamValues> resultParamsMap, List<SortColumn> sortColumnIndexes,
                            @Nonnull ColumnEntity columnEntity) {

        this( columnTabulations, resultParamsMap, null, null, /*admin, */columnEntity);
        if (sortColumnIndexes != null && !sortColumnIndexes.isEmpty()) {
            this.sortColumnIndexes = new ArrayList<>(sortColumnIndexes);
        }
    }

    /**
     * Constructor.
     *
     * @param columnTabulations The tabulations used.
     * @param resultParamsMap Any result column parameters (indexed by associated column's position in columnTabulations)
     * @param sortColumnIndex The column to sort.
     * @param sortDirection Ascending or descending.
     * @param columnEntity The base entity of the search result
     * @param multiValueDelimiter The text to use as the delimiter between values in a multi-valued field.
     */
    public ConfigurableList(List<ColumnTabulation> columnTabulations, Map<Integer, ResultParamValues> resultParamsMap, Integer sortColumnIndex,
                            String sortDirection, /*Boolean admin, */ @Nonnull ColumnEntity columnEntity, String multiValueDelimiter) {
        this.columnEntity = columnEntity;
        this.multiValueDelimiter = multiValueDelimiter;
        this.resultParamsMap.putAll(resultParamsMap);

        for (int i = 0; i < columnTabulations.size(); i++ ) {
            ColumnTabulation columnTabulation = columnTabulations.get(i);
            // Ignore header logic on nested table ColumnTabulation
            if( !columnTabulation.isNestedParent() ) {
                ResultParamValues resultParams = resultParamsMap.get(i);
                if( resultParams == null ) {
                    headerGroupMap.put(columnTabulation.getName(), new HeaderGroup(columnTabulation.getName()));
                } else {
                    headerGroupMap.put(resultParams.getUserColumnName(), new HeaderGroup(columnTabulation.getName()));
                }
            }
        }
        this.sortColumnIndex = sortColumnIndex;
        this.sortDirection = sortDirection;
//        isAdmin = admin;

        // Get the plugin tabulations and the non plugin tabulations.
        for (int i = 0; i < columnTabulations.size();i++ ) {
            ColumnTabulation columnTabulation = columnTabulations.get(i);
            if (columnTabulation.getPluginClass() != null) {
                pluginTabulations.put(i, columnTabulation);
            } else {
                nonPluginTabulations.put(i, columnTabulation);
            }
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

        Header(String viewHeader, String downloadHeader1, String downloadHeader2) {
            this.viewHeader = viewHeader;
            this.downloadHeader1 = downloadHeader1;
            this.downloadHeader2 = downloadHeader2;
            this.sortPath = "";
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
     * expressions (e.g. phenotypes with intervals) yield multiple columns.
     * Headers within a group are sorted based upon the order they are added.
     */
    static class HeaderGroup {
        private final String name;

        private final Map<String, Header> headerMap = new LinkedHashMap<>();

        HeaderGroup(String name) {
            this.name = name;
        }

        void addHeader(Header header) {
            headerMap.put(header.getViewHeader(), header);
        }

        public String getName() {
            return name;
        }

        public Map<String, Header> getHeaderMap() {
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
        private Comparable<?> sortableValue;
        private String formattedValue;

        Cell(Header header, Comparable<?> sortableValue, String formattedValue) {
            this(header, sortableValue, formattedValue, true);
        }

        Cell(Header header, Comparable<?> sortableValue, String formattedValue, boolean xssEncode) {
            this.header = header;
            this.sortableValue = sortableValue;
            if (formattedValue != null) {
                this.formattedValue = xssEncode ? Encode.forHtml(formattedValue) : formattedValue;
            }
        }

        public Header getHeader() {
            return header;
        }

        public Comparable<?> getSortableValue() {
            return sortableValue;
        }

        public String getFormattedValue() {
            return formattedValue;
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
        private final Map<ColumnTabulation, ResultList> nestedTableEntities = new LinkedHashMap<>();

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
        Map<ColumnTabulation, ResultList> getNestedTableEntities() {
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
        addRows(entityList, new SearchContext() );
    }

    /**
     * Enhanced addRows to provide ability to supply context objects
     * @param entityList list of objects, where each object is likely to be the root of a
     *                   graph that is navigated by column definition expressions.
     * @param context Any required context objects (e.g. as required in eval expressions)
     */
    public void addRows(List<?> entityList, @Nonnull SearchContext context ) {

        context.setMultiValueDelimiter(multiValueDelimiter);

        for (Map.Entry<String,AddRowsListener> entry : addRowsListeners.entrySet()) {
            entry.getValue().addRows(entityList, context, nonPluginTabulations);
            context.addRowsListener(entry.getKey(), entry.getValue());
        }
        for (Object entity : entityList) {
            // evaluate expression to get ID
            Row row = new Row(columnEntity.getIdGetter().getId(entity));
            rows.add(row);
            for (Map.Entry<Integer,ColumnTabulation> intColumnEntry : nonPluginTabulations.entrySet()) {
                ColumnTabulation columnTabulation = intColumnEntry.getValue();
                Integer paramIndex = intColumnEntry.getKey();
                context.setColumnParams( resultParamsMap.get(paramIndex) );
                if( !columnTabulation.isNestedParent() ) {
                    if( resultParamsMap.get(paramIndex) == null ) {
                        recurseColumns(context, entity, row, columnTabulation, columnTabulation.getName());
                    } else {
                        recurseColumns(context, entity, row, columnTabulation, resultParamsMap.get(paramIndex).getUserColumnName());
                    }
                } else {
                    Collection<?> nestedEntities = columnTabulation.evalNestedTableExpression(entity, context);
                    // Build final nested ResultList here...
                    if( nestedEntities != null && !nestedEntities.isEmpty()) {
                        ResultList nestedResultList = buildNestedTable(columnTabulation, nestedEntities, context);
                        row.getNestedTableEntities().put(columnTabulation, nestedResultList);
                    }
                }
            }
            // Plugins for nested table processing handled on a row-by-row basis
            for (Map.Entry<Integer,ColumnTabulation> intColumnEntry : pluginTabulations.entrySet()) {
                ColumnTabulation columnTabulation = intColumnEntry.getValue();
                Integer paramIndex = intColumnEntry.getKey();
                context.setColumnParams( resultParamsMap.get(paramIndex) );
                context.setSearchTerm((SearchTerm) columnTabulation);
                if( columnTabulation.isNestedParent() ) {
                    ListPlugin listPlugin = getPlugin(columnTabulation.getPluginClass());
                    // Nested table will never be a SearchValue ... cast to SearchTerm
                    ResultList nestedResultList = listPlugin.getNestedTableData(entity, columnTabulation, context);
                    if( nestedResultList != null ) {
                        row.getNestedTableEntities().put(columnTabulation, nestedResultList);
                    }
                }
            }
        }

        // Call the plugins, and add their data to the accumulated rows.
        for (ColumnTabulation columnTabulation : pluginTabulations.values()) {
            context.setSearchTerm((SearchTerm) columnTabulation);
            ListPlugin listPlugin = getPlugin(columnTabulation.getPluginClass());
            // Legacy plugin process from BSP
            if( !columnTabulation.isNestedParent() ) {
                List<Row> pluginRows =
                        listPlugin.getData(entityList, headerGroupMap.get(columnTabulation.getName()), context);
                if (pluginRows.size() != entityList.size()) {
                    throw new RuntimeException("Plugin returned " + pluginRows.size() +
                            " rows, but entityList size is " + entityList.size());
                }
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
        }

        pageStartingRow = rows.size();
    }

    @SuppressWarnings("unchecked")
    private void recurseColumns(SearchContext context, Object entity, Row row, ColumnTabulation columnTabulation,
            String headerGroupName) {
        HeaderGroup headerGroup = headerGroupMap.get(headerGroupName);

        // For child terms, header may be value extracted from parent
        //   (e.g metadata value = "Male" for metadata name = "Gender" header name )
        if( columnTabulation instanceof SearchInstance.SearchValue ) {
            context.setSearchValue((SearchInstance.SearchValue)columnTabulation);
        }

        // Evaluate value expression (value and header count and order must be same, unless header count == 1).
        Object sortableValue = columnTabulation.evalValueExpression(entity, context);
        Object viewHeaderResult = columnTabulation.evalViewHeaderExpression(entity, context);

        // The headers could be a list, e.g. there could be multiple trait values for multiple intervals.
        List<String> viewHeaders;
        List<Object> sortableValues;
        if (viewHeaderResult instanceof List ) {
            viewHeaders = (List<String>) viewHeaderResult;
            sortableValues = (List<Object>) sortableValue;
            // If the headers are all the same (e.g. multiple values for a trait), prefix a
            //    sequence number, so the columns sort correctly.
            // TODO jmt Need a flag to indicate whether to comma-separate multiple values,
            // or put them in sequence number columns.
            if(viewHeaders.size() > 1 ) {
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
            sortableValues = new ArrayList<>();
            sortableValues.add(sortableValue);
        }

        // Determine which column to put each value in
        int valueIndex = 0;
        for (Object currentValue : sortableValues) {
            String currentViewHeader = viewHeaders.get(valueIndex);
            Header header = headerGroup.getHeaderMap().get(currentViewHeader);
            // TODO jmt two columns with same name currently overwrite (e.g. search term
            // TODO value and pre-defined column value) If this is the first time we've seen this header.
            if (header == null) {
                header = new Header(currentViewHeader, currentViewHeader, null, columnTabulation.getDbSortPath() );
                headerGroup.getHeaderMap().put(currentViewHeader, header);
            }

            String formattedString;
            if( context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB ) {
                formattedString = columnTabulation.evalUiDisplayOutputExpression(currentValue, context);
            } else {
                formattedString = columnTabulation.evalPlainTextOutputExpression(currentValue, context);
            }

            Comparable<?> comparableValue =
                    columnTabulation.evalValueTypeExpression(entity,context)
                        .getComparableValue(currentValue,multiValueDelimiter);
            Cell cell = new Cell(header, comparableValue, formattedString, columnTabulation.mustEscape());
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
    private ResultList buildNestedTable(ColumnTabulation columnTabulation, Collection<?> nestedEntityList, SearchContext context) {
        List<ResultRow> localRows = new ArrayList<>();
        List<Header> headers = new ArrayList<>();
        List<Comparable<?>> emptySortableCells = new ArrayList<>();

        for( ColumnTabulation nestedColumnTabulation : columnTabulation.getNestedEntityColumns() ) {
            String nestedName = nestedColumnTabulation.getName();
            headers.add(new Header(nestedName, nestedName, null));
        }

        for( Object entity : nestedEntityList ) {
            List<String> cells = new ArrayList<>();
            for( ColumnTabulation nestedColumnTabulation : columnTabulation.getNestedEntityColumns() ) {
                Object value = nestedColumnTabulation.evalValueExpression(entity, context);
                String output;
                if( context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB ) {
                    output = nestedColumnTabulation.evalUiDisplayOutputExpression(value, context);
                } else {
                    output = nestedColumnTabulation.evalPlainTextOutputExpression(value, context);
                }
                cells.add(nestedColumnTabulation.mustEscape() ? Encode.forHtml(output) : output);
            }
            ResultRow row = new ResultRow( emptySortableCells, cells, null );
            localRows.add(row);
        }

        return new ResultList(localRows, headers, 0, "ASC" );
    }

    /**
     * Default is to re-sort results
     */
    public ResultList getResultList() {
        return getResultList( true );
    }

    /**
     * Converts from Rows of compact Cells to a sparse matrix with empty cells to align
     * each cell with its associated header.
     *
     * @return data structure intended to be easy to render to HTML or spreadsheet.
     * Re-sorting not done for spreadsheet downloads
     */
    public ResultList getResultList(  boolean doSort ) {

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

            // Append nested tables to result row
            for( Map.Entry<ColumnTabulation, ResultList> entry : row.getNestedTableEntities().entrySet() ){
                resultRow.addNestedTable( entry.getKey().getName() , entry.getValue() );
            }

            resultRows.add(resultRow);
        }

        // In-memory sort if only one page.
        if (doSort && sortColumnIndexes != null) {
            Collections.sort(resultRows, new ResultRowComparator(sortColumnIndexes));
        } else if (doSort && sortColumnIndex != null) {
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
     * A list of results, including column names and rows of data.
     */
    public static class ResultList {
        private final List<ResultRow> resultRows;

        private List<Header> headers;

        /** The header for the conditional checkboxes, if any. */
        private String conditionalCheckboxHeader;

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
            Pair<Integer, Integer> rowColPair = fillArray(null, this, 0, 0, false);
            Object[][] rowObjects = new Object[rowColPair.getLeft()][rowColPair.getRight()];
            fillArray(rowObjects, this, 0, 0, true);
            return rowObjects;
        }

        /**
         * Recursively renders a resultList, and any nested resultLists, into a 2D array.  First call with
         * render = false, to get size information, then allocate array, then call with render = true.
         * @param rowObjects array of cells to render into
         * @param resultList source to render
         * @param startRow offsets for nested tables, start with 0
         * @param startColumn offsets for nested tables, start with 0
         * @param render true to fill rowObjects, false to only caculate size
         * @return pair of row count, column count
         */
        private Pair<Integer, Integer> fillArray(Object[][] rowObjects, ResultList resultList, int startRow,
                int startColumn, boolean render) {

            // Determine whether headers are one row or two.
            boolean headerRow2Present = false;
            for (Header header : resultList.getHeaders()) {
                String header2Name = header.getDownloadHeader2();
                if (header2Name != null && !header2Name.isEmpty()) {
                    headerRow2Present = true;
                    break;
                }
            }

            int currentRow = startRow + (headerRow2Present ? 2 : 1);
            List<Integer> headerWidths = new ArrayList<>();
            boolean firstRow = true;
            int currentColumn = startColumn;
            int returnRow = 0;
            int returnColumn = 0;
            for (ResultRow resultRow : resultList.getResultRows()) {

                // Render cells
                List<String> renderableCells = resultRow.getRenderableCells();
                returnRow++;
                int maxNestedRow = 0;
                boolean atLeastOneNested = false;
                for (int i = 0; i < renderableCells.size(); i++) {
                    if (render) {
                        rowObjects[currentRow][currentColumn] = renderableCells.get(i);
                    }

                    // Render nested tables that are inside each cell
                    if (resultRow.getCellNestedTables().isEmpty() || resultRow.getCellNestedTables().get(i) == null ||
                            resultRow.getCellNestedTables().get(i).getHeaders().isEmpty()) {
                        if (firstRow) {
                            headerWidths.add(1);
                            returnColumn++;
                        }
                        currentColumn++;
                        continue;
                    }
                    atLeastOneNested = true;
                    Pair<Integer, Integer> cellNestedRowCol = fillArray(rowObjects,
                            resultRow.getCellNestedTables().get(i), currentRow + 1, currentColumn, render);
                    maxNestedRow = Math.max(maxNestedRow, cellNestedRowCol.getLeft());

                    currentColumn += cellNestedRowCol.getRight();
                    if (firstRow) {
                        headerWidths.add(cellNestedRowCol.getRight());
                        returnColumn += cellNestedRowCol.getRight();
                    }
                }
                if (resultRow.getCellNestedTables().isEmpty() || !atLeastOneNested) {
                    currentRow++;
                    returnRow += maxNestedRow;
                } else {
                    currentRow += maxNestedRow + 1;
                    returnRow += maxNestedRow;
                }
                currentColumn = startColumn;

                // Render nested tables that are the width of the table
                for (Map.Entry<String, ResultList> stringResultListEntry : resultRow.getNestedTables().entrySet()) {
                    if (render) {
                        rowObjects[currentRow][currentColumn] = stringResultListEntry.getKey();
                    }
                    Pair<Integer, Integer> nestedRowCol = fillArray(rowObjects, stringResultListEntry.getValue(),
                            currentRow + 1, currentColumn, render);
                    returnRow += nestedRowCol.getLeft() + 1;
                    currentRow += nestedRowCol.getLeft() + 1;
                    returnColumn = Math.max(nestedRowCol.getRight(), returnColumn);
                }

                firstRow = false;
            }

            // Render headers (after we know how wide the nested tables are)
            currentColumn = startColumn;
            if (render) {
                List<Header> headers1 = resultList.getHeaders();
                for (int i = 0; i < headers1.size(); i++) {
                    Header header = headers1.get(i);
                    rowObjects[startRow][currentColumn] = new SpreadsheetCreator.ExcelHeader(
                            header.getDownloadHeader1());
                    String header2Name = header.getDownloadHeader2();
                    if (header2Name != null && !header2Name.isEmpty()) {
                        rowObjects[startRow + 1][currentColumn] = new SpreadsheetCreator.ExcelHeader(header2Name);
                    }
                    currentColumn += (header.getDownloadHeader1() == null ? 1 :
                            headerWidths.isEmpty() ? 1 : headerWidths.get(i));
                }
            }
            return new ImmutablePair<>(returnRow + (headerRow2Present ? 2 : 1),
                    returnColumn == 0 ? resultList.getHeaders().size() : returnColumn);
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

        public String getConditionalCheckboxHeader() {
            return conditionalCheckboxHeader;
        }

        public void setConditionalCheckboxHeader(String conditionalCheckboxHeader) {
            this.conditionalCheckboxHeader = conditionalCheckboxHeader;
        }
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

        private String cssStyles;

        private Boolean conditionalCheckbox;

        /**
         * Each row may have a nested table
         */
        private Map<String, ResultList> nestedTables = new LinkedHashMap<>();

        /**
         * Each cell may have a nested table.
         */
        private List<ResultList> cellNestedTables = new ArrayList<>();

        /**
         *
         * @param sortableCells see field
         * @param renderableCells must be escaped for XSS
         * @param resultId see field
         */
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

        public String getCssStyles() {
            return cssStyles;
        }

        public void setCssStyles(String cssStyles) {
            this.cssStyles = cssStyles;
        }

        public Boolean hasConditionalCheckbox() {
            return conditionalCheckbox;
        }

        public void setConditionalCheckbox(Boolean conditionalCheckbox) {
            this.conditionalCheckbox = conditionalCheckbox;
        }

        /**
         * This finds the position of the column name in the headers and then updates the value for that position.
         *
         * @param headers All the headers
         * @param columnName The name of the column to update
         * @param theValue The value to update
         */
        // todo jmt delete?
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

        public void setCellNestedTables(List<ResultList> cellNestedTables) {
            this.cellNestedTables = cellNestedTables;
        }

        public List<ResultList> getCellNestedTables() {
            return cellNestedTables;
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
     * This takes a formatted value and escapes the substrings pushed into the formatted value in all places it was
     * pushed into.
     *
     * @param substring         String pushed into the fully formatted string.
     * @param formattedValue    Formatted string which contains at least one copy of the sortable value.
     * @param escapedSubstring  Escaped sortable value we'd like to replace the sortableValue with.
     * @return fully formatted value where all instances of the substring are escaped.
     */
    // todo jmt delete or use?
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

    public void addAddRowsListeners(ConfigurableSearchDefinition configurableSearchDef) {
        ConfigurableSearchDefinition.AddRowsListenerFactory addRowsListenerFactory =
                configurableSearchDef.getAddRowsListenerFactory();
        if (addRowsListenerFactory != null ) {
            for (Map.Entry<String, AddRowsListener> entry : addRowsListenerFactory.getAddRowsListeners().entrySet()) {
                addRowsListeners.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Cache row plugins.
     */
    private ListPlugin getPlugin( Class<?> pluginClass ) {
        ListPlugin plugin = null;
        if( pluginCache == null ) {
            pluginCache = new HashMap<>();
        } else {
            plugin = pluginCache.get(pluginClass);
        }
        if( plugin == null ) {
            try {
                plugin =(ListPlugin)pluginClass.newInstance();
                pluginCache.put(pluginClass, plugin);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Cannot instantiate plugin class " + pluginClass.getName(), e );
            }
        }
        return plugin;
    }
}
