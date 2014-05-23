package org.broadinstitute.gpinformatics.infrastructure.columns;

// todo jmt need different sets of columns for different starting entities.

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by thompson on 5/21/2014.
 */
public enum ColumnDefinition implements ColumnTabulation {
    LABEL(ColumnEntity.LAB_VESSEL);

    interface Evaluator {
        public Object evaluate(Object entity, Map<String, Object> context);
    }

    private String name;
    private Evaluator plainText;
    private Evaluator formatted;
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

    private ColumnEntity columnEntity;

    ColumnDefinition(ColumnEntity columnEntity) {
        this.columnEntity = columnEntity;
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

    @Override
    public String getPluginClass() {
        return null;
    }

    @Override
    public String getDbSortPath() {
        return null;
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
