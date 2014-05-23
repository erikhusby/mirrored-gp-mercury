package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Enumeration of column definitions
 */
public enum ColumnDefinition implements ColumnTabulation {

    LABEL(ColumnEntity.LAB_VESSEL, "Label",
            new Evaluator() {
                @Override
                public Object evaluate(Object entity, Map<String, Object> context) {
                    return ((LabVessel) entity).getLabel();
                }
            }, null, null, null, null
    );

    public interface Evaluator {
        Object evaluate(Object entity, Map<String, Object> context);
    }

    private final ColumnEntity columnEntity;
    private final String name;
    private final Evaluator plainText;
    private final Evaluator formatted;
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

    // Many properties can be Strings, expressions can be replaced with two implementations of an interface


    ColumnDefinition(ColumnEntity columnEntity, String name, Evaluator plainText,
            Evaluator formatted, String joinFetchPath, String dbSortPath, String pluginClass) {
        this.name = name;
        this.columnEntity = columnEntity;
        this.plainText = plainText;
        this.formatted = formatted;
        this.joinFetchPath = joinFetchPath;
        this.dbSortPath = dbSortPath;
        this.pluginClass = pluginClass;
    }

    public ColumnEntity getColumnEntity() {
        return columnEntity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object evalPlainTextExpression(Object entity, Map<String, Object> context) {
        return plainText.evaluate(entity, context);
    }

    @Override
    public Object evalFormattedExpression(Object entity, Map<String, Object> context) {
        return formatted.evaluate(entity, context);
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

    @Override
    public String getPluginClass() {
        return pluginClass;
    }

    @Override
    public String getDbSortPath() {
        return dbSortPath;
    }

    @Override
    public List<ColumnTabulation> getChildColumnTabulations() {
        // Configurable columns don't have hierarchies.
        return Collections.emptyList();
    }

    @Override
    public boolean isOnlyPlainText() {
        return formatted == null;
    }
}
