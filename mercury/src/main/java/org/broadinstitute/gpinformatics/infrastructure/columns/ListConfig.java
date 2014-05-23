package org.broadinstitute.gpinformatics.infrastructure.columns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// todo jmt delete
/**
 * Configurable lists create a table of information from a list of object graphs.
 */
@SuppressWarnings("unused")
public class ListConfig /*extends PreferenceDefinition*/ {

    private static final long serialVersionUID = 8430617986102249008L;

    private static final String BATCH_EFFECTS_HEADER = "GTEx Batch Effects";

    public static class ColumnConfig implements ColumnTabulation, Serializable {

        private static final long serialVersionUID = 3250089282386424403L;

        private static final String MVEL_PREFIX = "MVEL:";

        /**
         * Name of the column, appears in the column header.
         */
        private final String name;

        /**
         * Expression to retrieve plain text value of column, used in spreadsheets, and
         * for in-memory sorting.
         */
        private final String plainTextExpression;

        /**
         * Expression to retrieve value, including HTML e.g. for a link to a detail page.
         */
        private final String formattedExpression;

        /**
         * The first row header in a spreadsheet, null if same as name.
         */
        private final String spreadsheetHeader1;

        /**
         * The second row header in a spreadsheet.
         */
        private final String spreadsheetHeader2;

        /**
         * To improve performance, the path to join fetch when retrieving from database.
         */
        private final String joinFetchPath;

        /**
         * For database sorting, the path to order by.
         */
        private final String dbSortPath;

        /**
         * Fully qualified name of class that yields column instances
         */
        private final String pluginClass;

        /**
         * Avoid compiling the expression more than once
         */
        transient Object compiledPlainTextExpression;

        /**
         * Avoid compiling the expression more than once
         */
        transient Object compiledFormattedExpression;

        public ColumnConfig(String name, String plainTextExpression, String formattedExpression,
                String spreadsheetHeader1, String spreadsheetHeader2, String joinFetchPath,
                String dbSortPath, String pluginClass) {
            this.name = name;
            this.plainTextExpression = plainTextExpression;
            this.formattedExpression = formattedExpression;
            this.spreadsheetHeader1 = spreadsheetHeader1;
            this.spreadsheetHeader2 = spreadsheetHeader2;
            this.joinFetchPath = joinFetchPath;
            this.dbSortPath = dbSortPath;
            this.pluginClass = pluginClass;
        }

        public ColumnConfig(String headerColumnName, String columnName) {
            this(headerColumnName, null,  headerColumnName, null, null, null, columnName, null);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object evalPlainTextExpression(Object entity, Map<String, Object> context) {
/*
            // Skip the expression if it is null because these will be post processed (or ignored)
            if (plainTextExpression == null) {
                return "";
            }

            if (compiledPlainTextExpression == null) {
                try {
                    if (plainTextExpression.startsWith(MVEL_PREFIX)) {
                        OptimizerFactory.setDefaultOptimizer("reflective");
                        compiledPlainTextExpression = MVEL.compileExpression(plainTextExpression.substring(MVEL_PREFIX
                                .length()));
                    } else {
                        compiledPlainTextExpression = Ognl.parseExpression(plainTextExpression.replace("\n", ""));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Parsing plain text expression for " + name, e);
                }
            }
            try {
                if (plainTextExpression.startsWith(MVEL_PREFIX)) {
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("entity", entity);
                    return MVEL.executeExpression(compiledPlainTextExpression, vars);
                }

                return Ognl.getValue(compiledPlainTextExpression, context, entity);
            } catch (Exception e) {
                throw new RuntimeException("Getting plain text value " + name + " for entity " + entity.toString(),
                        e);
            }
*/
            return null;
        }

        @Override
        public Object evalFormattedExpression(Object entity, Map<String, Object> context) {
/*
            // Skip the expression if it is null because these will be post processed (or ignored)
            if (plainTextExpression == null) {
                return "";
            }

            if (isOnlyPlainText()) {
                return evalPlainTextExpression(entity, context);
            }

            if (compiledFormattedExpression == null) {
                try {
                    if (formattedExpression.startsWith(MVEL_PREFIX)) {
                        OptimizerFactory.setDefaultOptimizer("reflective");
                        compiledFormattedExpression = MVEL.compileExpression(formattedExpression.substring(MVEL_PREFIX
                                .length()));
                    } else {
                        compiledFormattedExpression = Ognl.parseExpression(formattedExpression.replace("\n", ""));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Parsing formatted expression for " + name, e);
                }
            }

            try {
                if (plainTextExpression.startsWith(MVEL_PREFIX)) {
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("entity", entity);
                    return MVEL.executeExpression(compiledFormattedExpression, vars);
                }

                return Ognl.getValue(compiledFormattedExpression, context, entity);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Getting formatted value " + name + " for entity " + entity.toString(), e);
            }
*/
            return null;
        }

        @Override
        public Object evalViewHeaderExpression(Object entity, Map<String, Object> context) {
            return name;
        }

        @Override
        public Object evalDownloadHeader1Expression(Object entity, Map<String, Object> context) {
            // TODO jmt revisit download headers
            return null;
        }

        @Override
        public Object evalDownloadHeader2Expression(Object entity, Map<String, Object> context) {
            return null;
        }

        public String getPlainTextExpression() {
            return plainTextExpression;
        }

        public String getFormattedExpression() {
            return formattedExpression;
        }

        public String getSpreadsheetHeader1() {
            return spreadsheetHeader1;
        }

        public String getSpreadsheetHeader2() {
            return spreadsheetHeader2;
        }

        public String getJoinFetchPath() {
            return joinFetchPath;
        }

        @Override
        public String getDbSortPath() {
            return dbSortPath;
        }

        @Override
        public List<ColumnTabulation> getChildColumnTabulations() {
            // Configurable columns don't have hiearchies.
            return Collections.emptyList();
        }

        @Override
        public boolean isOnlyPlainText() {
            return formattedExpression == null;
        }

        @Override
        public String getPluginClass() {
            return pluginClass;
        }
    }

    /**
     * Name of the id field in the entity we're listing, used in pagination.
     */
    private String id;

    /**
     * Map from group name (e.g. IDs, Phenotypes, Annotations) to list of columns.
     */
    private final Map<String, List<ColumnConfig>> mapGroupToColumns =
            new LinkedHashMap<>();

    /**
     * List of column definitions.
     */
    transient private List<ColumnConfig> columns;

    public List<ColumnConfig> getColumns() {
        if (columns == null) {
            columns = new ArrayList<>();
            for (List<ColumnConfig> columnConfigs : mapGroupToColumns.values()) {
                for (ColumnConfig columnConfig : columnConfigs) {
                    columns.add(columnConfig);
                }
            }
        }

        return columns;
    }

    public ColumnConfig getColumnConfig(String name) {
        for (ColumnConfig column : getColumns()) {
            if (column.getName().equals(name)) {
                return column;
            }
        }
        throw new RuntimeException("Failed to find column config for " + name);
    }

    public Map<String, List<ColumnConfig>> getMapGroupToColumns() {
        return mapGroupToColumns;
    }

    /**
     * This takes the map of group to columns and tacks on the Batch Effects section right from the enum. Ideally,
     * this would all be enums and we could eliminate the XML streaming from this process, but this can be done
     * as part of bigger refactoring.
     *
     * @return The mapping with the batch effects added.
     */
/*
    public Map<String, List<ColumnConfig>> getUpdatedMapGroupToColumns() {
        Map<String, List<ColumnConfig>> updatedMap = new HashMap<> ();
        updatedMap.putAll(mapGroupToColumns);

        List<ListConfig.ColumnConfig> configList = new ArrayList<>();

        for (GTExPostProcessColumn gtExPostProcessColumn : GTExPostProcessColumn.values()) {
            configList.add(gtExPostProcessColumn.getColumnConfig());
        }

        updatedMap.put(BATCH_EFFECTS_HEADER, configList);

        return updatedMap;
    }
*/

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
