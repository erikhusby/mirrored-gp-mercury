/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2008 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the definition of a term in a user-defined search. Intended to be XStreamed.
 */
public class SearchTerm implements Serializable, ColumnTabulation {

    private static final long serialVersionUID = -7452519036319121392L;

    /**
     * Attached to a search term display expression
     * Generates the value to be displayed
     * @param <T>
     */
    public abstract static class Evaluator <T> {
        public abstract T evaluate(Object entity, Map<String, Object> context);
    }

    /**
     * Defines Hibernate path from search result entity to the property being searched.
     * Allows one term to search multiple properties, e.g. All Aliases
     */
    public static class CriteriaPath implements Serializable {

        private static final long serialVersionUID = -6543579712758279966L;

        /**
         * List of criteria that defines Hibernate path to property
         */
        private List<String> criteria;

        /**
         * True if Hibernate should join fetch the criteria; this improves performance,
         * but should be done only with associations mapped as Sets, not Lists
         */
        private Boolean joinFetch;

        /**
         * Property in class at end of criteria list
         */
        private String propertyName;

        private Evaluator<String> propertyNameExpression;

        public List<String> getCriteria() {
            return criteria;
        }

        public void setCriteria(List<String> criteria) {
            this.criteria = criteria;
        }

        public Boolean isJoinFetch() {
            return joinFetch;
        }

        public void setJoinFetch(Boolean joinFetch) {
            this.joinFetch = joinFetch;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public Evaluator<String> getPropertyNameExpression() {
            return propertyNameExpression;
        }

        public void setPropertyNameExpression(Evaluator<String> propertyNameExpression) {
            this.propertyNameExpression = propertyNameExpression;
        }
    }

    /**
     * Name displayed in the user interface
     */
    private String name;

    /**
     * True if this term is required in every SearchInstance
     */
    private Boolean required;

    /**
     * Expression to access the parent entity's collection of child entities
     *     to include in a nested table presentation
     */
    private List<ColumnTabulation> nestedEntityColumns;

    private Class pluginClass;

    /**
     * True if this term requires a new detached criteria (because it has the same
     * association path as another term in the same hierarchy, e.g. two individual traits)
     */
    private Boolean newDetachedCriteria;

    /**
     * Hibernate criteria path from entity to property; usually only one, but if there are
     * multiple (e.g. All Aliases) then they are ORed together
     */
    private List<CriteriaPath> criteriaPaths;

    /**
     * Expression that fetches list of constrained values
     */
    private Evaluator<List<ConstrainedValue>> valuesExpression;

    /**
     * Expression that determines data type
     */
    private Evaluator<String> typeExpression;

    /**
     * Value that isn't supplied by the user, referenced by dependent search terms, null
     * if user supplies value
     */
    private String constantValue;

    /**
     * Expression to navigate to the property, for displaying search results, null if not
     * displayed
     */
    private Evaluator<Object> displayExpression;

    /**
     * Header text (or expression to derive it) for displaying search results, null if
     * same as name.
     */
    private Evaluator<Object> viewHeader;

    /**
     * First header text (or expression to derive it) for downloading search results, null
     * if same as viewHeader
     */
    private String downloadFirstHeader;

    /**
     * Second header text (or expression to derive it) for downloading search results,
     * e.g. First header is trait name, second header is metadata name. Null if none.
     */
    private String downloadSecondHeader;

    /**
     * SearchTerms that depend on the value of this term
     */
    private List<SearchTerm> dependentSearchTerms;

    /**
     * Expression to convert HTML form value from String to property data type
     */
    private Evaluator<Object> valueConversionExpression;

    /**
     * Hibernate SQL restriction (currently constant, i.e. doesn't reference values)
     */
    private String sqlRestriction;

    // TODO jmt allow multiple metadata in user interface
    /**
     * Has meaning only for dependent search terms, true if there can be multiple
     * children, e.g. a trait can have multiple metadata
     */
    private Boolean multipleForParent;

    /**
     * True if the constrained values should be added to the list of search terms, e.g.
     * phenotype names
     */
    private Boolean addConstrainedValuesToSearchTermList;

    /**
     * The maximum number of values that will be returned by the constrained values
     * expression; if the number is less than this, display the list, otherwise display a
     * text box; e.g. the values expression for trait values makes a DAO call that limits
     * the size of the list to 101 distinct values, if the DAO returns 100, use the list,
     * otherwise assume that there are more than 101, so the user will have to type the
     * value, rather than choosing it from the list.
     */
    private Evaluator<Integer> constrainedValuesSizeLimitExpression;

    /**
     * The name of the AddRowsListener implementation.
     * Ideally, need an abstract way to include reference to BSPSampleSearchColumn, so the set of columns can be gathered in one request.
     * The Listener needs to be constructed and added to ConfigurableList, but construction probably can't be generic.
     * Could add an evaluator called by the listener
     */
    private Evaluator<Object> addRowsListenerHelper;

    /**
     * Flag this term as a parent to a nested table
     */
    private Boolean isNestedParent = Boolean.FALSE;

    /**
     * Evaluate the expression that returns constrained values, e.g. list of phenotypes
     *
     * @param context any additional entities referred to by the expression
     * @return list of constrained values
     */
    public List<ConstrainedValue> evalConstrainedValues(Map<String, Object> context) {
        if (this.getValuesExpression() == null) {
            return Collections.emptyList();
        }
        List<ConstrainedValue> constrainedValues = valuesExpression.evaluate(null, context);
        if (constrainedValues != null && constrainedValues.size() == 1 && constrainedValues.get(0).getCode() == null) {
            constrainedValues = null;
        }
        return constrainedValues;
    }

    public List<ConstrainedValue> getConstrainedValues() {
        return evalConstrainedValues(new HashMap<String, Object>());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean isNewDetachedCriteria() {
        return newDetachedCriteria;
    }

    public void setNewDetachedCriteria(Boolean newDetachedCriteria) {
        this.newDetachedCriteria = newDetachedCriteria;
    }

    public Evaluator<List<ConstrainedValue>> getValuesExpression() {
        return valuesExpression;
    }

    public void setValuesExpression(Evaluator<List<ConstrainedValue>> valuesExpression) {
        this.valuesExpression = valuesExpression;
    }

    public Evaluator<String> getTypeExpression() {
        return typeExpression;
    }

    public void setTypeExpression(Evaluator<String> typeExpression) {
        this.typeExpression = typeExpression;
    }

    public String getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(String constantValue) {
        this.constantValue = constantValue;
    }

    public Evaluator<Object> getDisplayExpression() {
        return displayExpression;
    }

    public void setDisplayExpression(Evaluator<Object> displayExpression) {
        this.displayExpression = displayExpression;
    }

    @Override
    public Boolean isNestedParent( ){
        return  isNestedParent;
    }

    @Override
    public void setIsNestedParent(Boolean isNestedParent) {
        this.isNestedParent = isNestedParent;
    }

    @Override
    public List<? extends ColumnTabulation> getNestedEntityColumns() {
        return nestedEntityColumns;
    }

    @Override
    public void addNestedEntityColumn(ColumnTabulation nestedEntityColumn) {
        if( nestedEntityColumns == null ) {
            nestedEntityColumns = new ArrayList<>();
        }
        nestedEntityColumns.add(nestedEntityColumn);
    }

    public Evaluator<Object> getViewHeader() {
        return viewHeader;
    }

    public void setViewHeader(Evaluator<Object> viewHeader) {
        this.viewHeader = viewHeader;
    }

    public String getDownloadFirstHeader() {
        return downloadFirstHeader;
    }

    public void setDownloadFirstHeader(String downloadFirstHeader) {
        this.downloadFirstHeader = downloadFirstHeader;
    }

    public String getDownloadSecondHeader() {
        return downloadSecondHeader;
    }

    public void setDownloadSecondHeader(String downloadSecondHeader) {
        this.downloadSecondHeader = downloadSecondHeader;
    }

    public List<SearchTerm> getDependentSearchTerms() {
        return dependentSearchTerms;
    }

    public void setDependentSearchTerms(List<SearchTerm> dependentSearchTerms) {
        this.dependentSearchTerms = dependentSearchTerms;
    }

    public Evaluator<Object> getValueConversionExpression() {
        return valueConversionExpression;
    }

    public void setValueConversionExpression(Evaluator<Object> valueConversionExpression) {
        this.valueConversionExpression = valueConversionExpression;
    }

    public String getSqlRestriction() {
        return sqlRestriction;
    }

    public void setSqlRestriction(String sqlRestriction) {
        this.sqlRestriction = sqlRestriction;
    }

    public Boolean isMultipleForParent() {
        return multipleForParent;
    }

    public void setMultipleForParent(Boolean multipleForParent) {
        this.multipleForParent = multipleForParent;
    }

    public Boolean getAddConstrainedValuesToSearchTermList() {
        return addConstrainedValuesToSearchTermList;
    }

    public void setAddConstrainedValuesToSearchTermList(Boolean addConstrainedValuesToSearchTermList) {
        this.addConstrainedValuesToSearchTermList = addConstrainedValuesToSearchTermList;
    }

    public List<CriteriaPath> getCriteriaPaths() {
        return criteriaPaths;
    }

    public void setCriteriaPaths(List<CriteriaPath> criteriaPaths) {
        this.criteriaPaths = criteriaPaths;
    }

    public void setConstrainedValuesSizeLimitExpression(
            Evaluator<Integer> constrainedValuesSizeLimitExpression) {
        this.constrainedValuesSizeLimitExpression = constrainedValuesSizeLimitExpression;
    }

    public Evaluator<Integer> getConstrainedValuesSizeLimitExpression() {
        return constrainedValuesSizeLimitExpression;
    }

    public Evaluator<Object> getAddRowsListenerHelper() {
        return addRowsListenerHelper;
    }

    public void setAddRowsListenerHelper(Evaluator<Object> addRowsListenerHelper) {
        this.addRowsListenerHelper = addRowsListenerHelper;
    }

    public List<String> getJoinFetchPaths() {
        List<String> joinFetchPaths = new ArrayList<>();
        if (getCriteriaPaths() != null) {
            for (CriteriaPath criteriaPath : getCriteriaPaths()) {
                if (criteriaPath.isJoinFetch() != null && criteriaPath.isJoinFetch()) {
                    if (criteriaPath.getCriteria() != null && !criteriaPath.getCriteria().isEmpty()) {
                        String associationPath = null;
                        for (String criteriaName : criteriaPath.getCriteria()) {
                            if (associationPath == null) {
                                associationPath = criteriaName;
                            } else {
                                associationPath += "." + criteriaName;
                            }
                        }
                        joinFetchPaths.add(associationPath);
                    }
                }
            }
        }
        return joinFetchPaths;
    }

    @Override
    public Object evalPlainTextExpression(Object entity, Map<String, Object> context) {
        return getDisplayExpression().evaluate(entity, context);
    }

    @Override
    public Object evalFormattedExpression(Object entity, Map<String, Object> context) {
        return getDisplayExpression().evaluate(entity, context);
    }

    @Override
    public Object evalViewHeaderExpression(Object entity, Map<String, Object> context) {
        if (getViewHeader() == null) {
            return getName();
        }
        return getViewHeader().evaluate(entity, context);
    }

    /**
     * Get the collection of entities associated with parent row
     * Convenience method to eliminate ambiguity of calling evalPlainTextExpression
     * @param entity  root of object graph that expression navigates.
     * @param context name / value pairs of other variables used in the expression.
     * @return Nested table entity collection
     */
    @Override
    public Collection<?> evalNestedTableExpression(Object entity, Map<String, Object> context) {
        return (Collection<?>) getDisplayExpression().evaluate(entity, context);
    }

    @Override
    public Object evalDownloadHeader1Expression(Object entity, Map<String, Object> context) {
        return null;
    }

    @Override
    public Object evalDownloadHeader2Expression(Object entity, Map<String, Object> context) {
        return null;
    }

    @Override
    public Class getPluginClass() {
        return pluginClass;
    }
    @Override
    public void setPluginClass(Class pluginClass) {
        this.pluginClass = pluginClass;
    }

    @Override
    public String getDbSortPath() {
        return null;
    }

    @Override
    public List<ColumnTabulation> getChildColumnTabulations() {
        return Collections.emptyList();
    }

    @Override
    public boolean isOnlyPlainText() {
        return true;
    }

}
