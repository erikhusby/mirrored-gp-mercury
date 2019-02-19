package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;

import java.util.Collection;
import java.util.List;

/**
 * This interface abstracts the behavior necessary to convert a list of entities to a
 * table of values, with variable columns. This interface allows SearchTerm and
 * SearchValue objects to be used interchangeably by ConfigurableList.
 */
public interface ColumnTabulation {
    /**
     * @return name of the column.
     */
    String getName();

    /**
     * Returns the results of evaluating the value expression.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     * @return The property value object, which could be a collection of values.
     */
    Object evalValueExpression(Object entity, SearchContext context);

    /**
     * Returns the plain text display value of a column value property
     *
     * @param value Property value or collection of values to be displayed
     * @param context Other objects which (may be) used in the expression.
     * @return Results converted to a String to display
     */
    String evalPlainTextOutputExpression(Object value, SearchContext context);

    /**
     * Returns the display value enhanced for display in UI <br />
     * If no override implemented in evaluator, defaults to evalPlainTextOutputExpression
     *
     * @param value Property value or collection of values to be displayed
     * @param context Other objects which (may be) used in the expression.
     * @return Results converted to an enhanced String (e.g. HTML CSS or Hyperlink) to display in UI
     */
    String evalUiDisplayOutputExpression(Object value, SearchContext context);

    /**
     * Whether to apply XSS escaping.
     * @return true if needs central escaping, false if already escaped by the output expression
     */
    boolean mustEscape();

    /**
     * returns the results of evaluating the expression for the view column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     */
    Object evalViewHeaderExpression(Object entity, SearchContext context);

    /**
     * Utility method to eliminate ambiguity of using evalPlainTextOutputExpression
     *   to access nested table collection.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     * @return results, which must be a collection
     */
    Collection<?> evalNestedTableExpression(Object entity, SearchContext context);

    /**
     * Access nested entity ColumnTabulation objects associated with this ColumnTabulation
     */
    List<? extends ColumnTabulation> getNestedEntityColumns();

    void addNestedEntityColumn(ColumnTabulation columnTabulation);

    void setIsNestedParent( Boolean isNestedParent );

    Boolean isNestedParent( );

    /**
     * returns the results of evaluating the expression for the first row of the
     * spreadsheet column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     * @return results, which could be a list.
     */
    Object evalDownloadHeader1Expression(Object entity, SearchContext context);

    /**
     * returns the results of evaluating the expression for the second row of the
     * spreadsheet column header.
     *
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     * @return results, which could be a list.
     */
    Object evalDownloadHeader2Expression(Object entity, SearchContext context);

    /**
     * @return the name of the plugin class that returns data for this pseudo-column
     */
    Class getPluginClass();

    void setPluginClass( Class pluginClass );

    /**
     * For re-sorting of results
     *
     * @return path to a Hibernate property.
     */
    String getDbSortPath();

    /**
     * Allows hierarchies of columns, e.g. a trait (parent) and its metadata (children).
     *
     * @return list of child columns.
     */
    List<ColumnTabulation> getChildColumnTabulations();

    /**
     * Returns the column value type from an expression evaluation
     *
     * @param value  The column object value
     * @param context Other objects which (may be) used in the expression.
     * @return
     */
    ColumnValueType evalValueTypeExpression( Object value, SearchContext context );

}
