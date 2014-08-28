package org.broadinstitute.gpinformatics.infrastructure.columns;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This interface abstracts the behavior necessary to convert a list of entities to a
 * table of values, with variable columns. This interface allows SearchTerm and
 * ColumnConfig objects to be used interchangeably by ConfigurableList.
 */
public interface ColumnTabulation {
    /**
     * @return name of the column.
     */
    public String getName();

    /**
     * returns the results of evaluating the plain text, sortable expression.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which could be a list.
     */
    public Object evalPlainTextExpression(Object entity, Map<String, Object> context);

    /**
     * returns the results of evaluating the formatted expression.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which could be a list, and could contain HTML.
     */
    public Object evalFormattedExpression(Object entity, Map<String, Object> context);

    /**
     * returns the results of evaluating the expression for the view column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which could be a list.
     */
    Object evalViewHeaderExpression(Object entity, Map<String, Object> context);

    /**
     * Utility method to eliminate ambiguity of using evalPlainTextExpression
     *   to access nested table collection.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which must be a collection
     */
    public Collection<?> evalNestedTableExpression(Object entity, Map<String, Object> context);

    /**
     * Access nested entity ColumnTabulation objects associated with this ColumnTabulation
     */
    public List<? extends ColumnTabulation> getNestedEntityColumns();

    public void addNestedEntityColumn(ColumnTabulation columnTabulation);

    public void setIsNestedParent( Boolean isNestedParent );

    public Boolean isNestedParent( );

    /**
     * returns the results of evaluating the expression for the first row of the
     * spreadsheet column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which could be a list.
     */
    public Object evalDownloadHeader1Expression(Object entity, Map<String, Object> context);

    /**
     * returns the results of evaluating the expression for the second row of the
     * spreadsheet column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return results, which could be a list.
     */
    public Object evalDownloadHeader2Expression(Object entity, Map<String, Object> context);

    /**
     * @return the name of the plugin class that returns data for this pseudo-column
     */
    public Class getPluginClass();

    public void setPluginClass( Class pluginClass );

    /**
     * For re-sorting of results
     *
     * @return path to a Hibernate property.
     */
    public String getDbSortPath();

    /**
     * Allows hierarchies of columns, e.g. a trait (parent) and its metadata (children).
     *
     * @return list of child columns.
     */
    public List<ColumnTabulation> getChildColumnTabulations();

    /**
     * This is used to optimize formatted value operation. Without this, there were cases where text only evaluations
     * where happening twice.
     *
     * @return Is formatted evaluation going to call plain text anyway.
     */
    public boolean isOnlyPlainText();
}
