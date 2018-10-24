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
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the definition of both a search criteria term (input) and a search result column (output) <br />
 * To be displayed as user selectable input criteria, (basically) will have criteria path(s) assigned  <br />
 * Output value as a result column will be generated via display expressions <br />
 * Intended to be XStreamed.
 */
public class SearchTerm implements Serializable, ColumnTabulation {

    private static final long serialVersionUID = -7452519036319121392L;

     /**
     * Attached to various search term expressions to dynamically generate required properties.
     * @param <T>
     */
     public abstract static class Evaluator <T> {

         public abstract T evaluate(Object entity, SearchContext context);


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

        /**
         * Optional nested subquery criteria definition
         * Note:  Nested criteria path and child search terms are mutually exclusive
         */
        private CriteriaPath nestedCriteriaPath;

        /**
         * Optional non user-editable criteria to be used as a global filter for user entered search term criteria
         */
        private List<ImmutableTermFilter> immutableTermFilters;

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

        public CriteriaPath getNestedCriteriaPath(){
            return nestedCriteriaPath;
        }

        public void setNestedCriteriaPath( CriteriaPath nestedCriteriaPath ) {
            this.nestedCriteriaPath = nestedCriteriaPath;
        }

        @Nullable
        public List<ImmutableTermFilter> getImmutableTermFilters(){
            return immutableTermFilters;
        }

        public void addImmutableTermFilter(ImmutableTermFilter immutableTermFilter){
            if( immutableTermFilters == null ) {
                immutableTermFilters = new ArrayList<>();
            }
            immutableTermFilters.add(immutableTermFilter);
        }
    }

    /**
     * Allow the case where one or more additional filters can be attached to a search term.
     * Filters are not user editable so values must be type safe with entity values
     */
    public static class ImmutableTermFilter {
        private String propertyName;
        private SearchInstance.Operator operator;
        private Object[] values;

        /**
         * Constructor for when one value is required to support operator
         * @param propertyName A property of the search term entity to base the filter on
         * @param operator Filter restriction operator
         * @param values Filter values, 2 required for between operator, 1 or more for list operators
         */
        public ImmutableTermFilter(
                String propertyName, SearchInstance.Operator operator, Object ... values ){
            this.propertyName = propertyName;
            this.operator     = operator;
            this.values = values;
        }

        public String getPropertyName(){
            return propertyName;
        }

        public SearchInstance.Operator getOperator(){
            return operator;
        }

        public Object[] getValues() {
            return values;
        }

    }

    /**
     * Name displayed in the user interface
     */
    private String name;

    /**
     * HTML to display in UI to assist user (optional)
     * (If none, no UI help is configured)
     */
    private String helpText;

    /**
     * True if this term is required in every SearchInstance
     */
    private Boolean required;

    /**
     * Provides sorting in criteria API if value is not null
     * (Works only on simple entity properties on multi page results)
     */
    private String dbSortPath;

    /**
     * Provides access the parent entity's collection of child entities
     *     to include in a nested table presentation
     */
    private List<ColumnTabulation> nestedEntityColumns;

    /**
     * Plugins programmatically expand the list of search columns returned for a single search term
     */
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
    private Evaluator<List<ConstrainedValue>> constrainedValuesExpression;

    /**
     * Expression that provides list of constrained result column values
     * TODO JMS This might be better off as part of a list plugin as parameter configurations get more complicated
     */
    private Evaluator<ResultParamConfiguration> resultParamConfigurationExpression;

    /**
     * Dynamic value type expression.
     * Required to handle String, Data, Number metadata
     */
    private Evaluator<ColumnValueType> valueTypeExpression;

    /**
     * Constant value type, superceded by valueTypeExpression if exists
    */
    private ColumnValueType valueType = ColumnValueType.STRING;

    /**
     * Value that isn't supplied by the user, referenced by dependent search terms.
     * Null if user supplies value
     */
    private String constantValue;

    /**
     * Expression to navigate to the property to generate value for display and sorting
     */
    private Evaluator<Object> displayValueExpression;

    /**
     * Expression from enum (can be shared across entities)
     */
    private DisplayExpression displayExpression;

    /**
     * Optional expression to enhance UI presentation of result column value
     */
    private Evaluator<String> uiDisplayOutputExpression;

    private boolean mustEscape = true;

    /**
     * Header text (or expression to derive it) for displaying search results.
     * Null if same as name.
     */
    private Evaluator<Object> viewHeaderExpression;

    /**
     * First header text (or expression to derive it) for downloading search results.
     * Null if same as viewHeaderExpression
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
     * Expression applied to results of traversal, to determine whether they should be included.  Allows a dependent
     * search term (e.g lab event type) to be combined with a primary term (e.g. LCSET) and traversal results
     * (e.g. descendants).
     */
    private Evaluator<Boolean> traversalFilterExpression;

    /**
     * Expression to convert HTML form input value from String to property data type
     */
    private Evaluator<Object> searchValueConversionExpression;

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
     * True if dependent search terms should be expanded and added to the list of criteria and result columns.
     */
    private Boolean addDependentTermsToSearchTermList = Boolean.FALSE;

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
     * Flag this for search criteria only, do not display as column option.
     * (Don't want parent term to show up in columns list)
     */
    private Boolean isExcludedFromResultColumns = Boolean.FALSE;

    /**
     * Flag this as an exclusive search term because it cannot logically be combined with others.
     * If any other terms are included with an exclusive term,
     *   the search should be rejected and a warning presented to the user.
     */
    private Boolean isExclusive = Boolean.FALSE;

    /**
     * Should term be added to result column as a default if user forgets to select any results
     * (isExcludedFromResultColumns value conflict takes precedence)
     */
    private Boolean isDefaultResultColumn = Boolean.FALSE;


    private ConfigurableSearchDefinition alternateSearchDefinition;

    private Boolean rackScanSupported = Boolean.FALSE;

    /**
     * Evaluate the expression that returns constrained values, e.g. list of phenotypes
     *
     * @param context any additional entities referred to by the expression
     * @return list of constrained values
     */
    public List<ConstrainedValue> getConstrainedValues(SearchContext context) {
        if (this.getConstrainedValuesExpression() == null) {
            return Collections.emptyList();
        }
        List<ConstrainedValue> constrainedValues = constrainedValuesExpression.evaluate(null, context);
        if (constrainedValues != null && constrainedValues.size() == 1 && constrainedValues.get(0).getCode() == null) {
            constrainedValues = null;
        }
        return constrainedValues;
    }

    /**
     * Display using JSP expression
     */
    public List<ConstrainedValue> getConstrainedValues() {
        return getConstrainedValues(new SearchContext());
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * A pseudo unique identifier to allow UI functionality to be tied to the display of this term (e.g. help text)
     */
    public String getUiId(){
        return "srchTrm_" + String.valueOf(this.hashCode());
    }

    /**
     * Display this text in UI if not null or empty string.
     */
    public void setHelpText( String helpText ) {
        this.helpText = helpText;
    }

    /**
     * Optional help text for this search term.
     * If not null or empty string, set up for display in UI.
     */
    public String getHelpText() {
        return helpText;
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

    public Evaluator<List<ConstrainedValue>> getConstrainedValuesExpression() {
        return constrainedValuesExpression;
    }

    public void setConstrainedValuesExpression(Evaluator<List<ConstrainedValue>> constrainedValuesExpression) {
        this.constrainedValuesExpression = constrainedValuesExpression;
    }

    /**
     * Allow user selection of one or more logically related result columns
     */
    public Evaluator<ResultParamConfiguration> getResultParamConfigurationExpression() {
        return resultParamConfigurationExpression;
    }

    public void setResultParamConfigurationExpression(Evaluator<ResultParamConfiguration> resultParamConfigurationExpression) {
        this.resultParamConfigurationExpression = resultParamConfigurationExpression;
    }

    /**
     * Set a constant value type for this term
     */
    public void setValueType( ColumnValueType valueType ) {
        this.valueType = valueType;
    }

    public void setValueTypeExpression( Evaluator<ColumnValueType> valueTypeExpression) {
        this.valueTypeExpression = valueTypeExpression;
    }

    public Evaluator<Object> getSearchValueConversionExpression() {
        return searchValueConversionExpression;
    }

    public void setSearchValueConversionExpression(Evaluator<Object> searchValueConversionExpression) {
        this.searchValueConversionExpression = searchValueConversionExpression;
    }

    public String getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(String constantValue) {
        this.constantValue = constantValue;
    }

    public Evaluator<Object> getDisplayValueExpression() {
        return displayValueExpression;
    }

    public DisplayExpression getDisplayExpression() {
        return displayExpression;
    }

    /**
     * Using an expression implementation pre-defined in the enum DisplayExpression, this method sets the
     * implementation to extract value(s) from the base entity object to be used as the source of the result column
     * value presented in UI or download.
     *
     * @param displayExpression
     */
    public void setDisplayExpression(DisplayExpression displayExpression) {
        this.displayExpression = displayExpression;
    }

    /**
     * Sets the expression implementation to extract value(s) from base entity object
     * to use as the source of the result column value presented in UI or download.
     *
     * @param displayValueExpression The expression implementation to extract the value(s) from base entity
     */
    public void setDisplayValueExpression(Evaluator<Object> displayValueExpression) {
        this.displayValueExpression = displayValueExpression;
    }

    /**
     * Sets the expression implementation used to convert result column value(s) returned from displayValueExpression
     * into a format for custom formatted display in UI (e.g. HTML CSS, Hyperlink)<br />
     * Display output defaults to plain text if expression not set.
     *
     * @param uiDisplayOutputExpression Custom expression implementation to enhance UI format of display value
     */
    public void setUiDisplayOutputExpression(Evaluator<String> uiDisplayOutputExpression) {
        this.uiDisplayOutputExpression = uiDisplayOutputExpression;
    }

    @Override
    public Boolean isNestedParent( ){
        return  isNestedParent;
    }

    @Override
    public void setIsNestedParent(Boolean isNestedParent) {
        this.isNestedParent = isNestedParent;
    }

    /**
     * Handles cases where a search term cannot be combined with any others.
     */
    public Boolean isExclusive(){
        if( isExclusive ) {
            return isExclusive;
        } else if( alternateSearchDefinition != null ) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public ConfigurableSearchDefinition getAlternateSearchDefinition(){
        return alternateSearchDefinition;
    }

    /**
     * Allow the criteria API to access a different set of root entity types than expected in the display.
     * A traversal evaluator must be attached to the search with logic which will replace the returned entity list
     * with a new list of the proper entity type.
     * (Term should also be flagged as exclusive)
     */
    public void setAlternateSearchDefinition(ConfigurableSearchDefinition alternateSearchDefinition){
        this.alternateSearchDefinition = alternateSearchDefinition;
    }

    /**
     * In certain cases, terms usable as search criteria need to be excluded from result column list.
     */
    public Boolean isExcludedFromResultColumns(){
        return  isExcludedFromResultColumns;
    }

    public void setIsExcludedFromResultColumns(Boolean isExcludedFromResultColumns) {
        this.isExcludedFromResultColumns = isExcludedFromResultColumns;
    }

    /**
     * If user does not select any result columns, add any flagged as default to results
     */
    public Boolean isDefaultResultColumn(){
        return  isDefaultResultColumn && !isExcludedFromResultColumns;
    }

    public void setIsDefaultResultColumn(Boolean isDefaultResultColumn) {
        this.isDefaultResultColumn = isDefaultResultColumn;
    }

    public Boolean isRackScanSupported() {
        return rackScanSupported;
    }

    public void setRackScanSupported(Boolean rackScanSupported) {
        this.rackScanSupported = rackScanSupported;
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

    public Evaluator<Object> getViewHeaderExpression() {
        return viewHeaderExpression;
    }

    public void setViewHeaderExpression(Evaluator<Object> viewHeaderExpression) {
        this.viewHeaderExpression = viewHeaderExpression;
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

    public void setTraversalFilterExpression(Evaluator<Boolean> traversalFilterExpression) {
        this.traversalFilterExpression = traversalFilterExpression;
    }

    public Evaluator<Boolean> getTraversalFilterExpression() {
        return traversalFilterExpression;
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

    public Boolean getAddDependentTermsToSearchTermList() {
        return addDependentTermsToSearchTermList;
    }

    public void setAddDependentTermsToSearchTermList(Boolean addDependentTermsToSearchTermList) {
        this.addDependentTermsToSearchTermList = addDependentTermsToSearchTermList;
    }

    public List<CriteriaPath> getCriteriaPaths() {
        if( getAlternateSearchDefinition() == null ) {
            return criteriaPaths;
        } else {
            return getAlternateSearchDefinition().getSearchTerm(this.name).getCriteriaPaths();
        }
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
    public Object evalValueExpression(Object entity, SearchContext context) {
        context = addTermToContext(context);
        if (getDisplayValueExpression() == null) {
            // todo jmt revisit this
/*
            // Don't show results for this term if they will also be displayed in a layout.
            ConfigurableSearchDefinition configurableSearchDefinition = SearchDefinitionFactory.getForEntity(
                    context.getColumnEntityType().getEntityName());
            for (String columnName : context.getSearchInstance().getPredefinedViewColumns()) {
                SearchTerm searchTerm = configurableSearchDefinition.getSearchTerm(columnName);
                if (searchTerm.getPluginClass() != null &&
                        EventVesselPositionPlugin.class.isAssignableFrom(searchTerm.getPluginClass())) {
                    return null;
                }
            }
*/

            Set resultObjects = new HashSet<>();
            Collection<?> expressionObjects = DisplayExpression.rowObjectToExpressionObject(
                    entity,
                    getDisplayExpression().getExpressionClass(),
                    context);
            for (Object object : expressionObjects) {
                Object resultObject = getDisplayExpression().getEvaluator().evaluate(object, context);
                if (resultObject instanceof Collection) {
                    resultObjects.addAll((Collection) resultObject);
                } else {
                    resultObjects.add(resultObject);
                }
            }
            return resultObjects;
        }
        return getDisplayValueExpression().evaluate(entity, context);
    }

    /**
     * If a dynamic type expression has been explicitly set, use it, otherwise, use the default for the type
     */
    @Override
    public ColumnValueType evalValueTypeExpression( Object value, SearchContext context ) {
        context = addTermToContext(context);
        if( valueTypeExpression == null ) {
            return valueType.getDefaultEvaluator().evaluate( value, context);
        } else {
            return valueTypeExpression.evaluate( value, context);
        }
    }

    @Override
    public String evalPlainTextOutputExpression(Object value, SearchContext context) {
        context = addTermToContext(context);
        String multiValueDelimiter = context.getMultiValueDelimiter();
        return evalValueTypeExpression(value, context).format(value, multiValueDelimiter);
    }

    @Override
    public String evalUiDisplayOutputExpression(Object value, SearchContext context) {
        context = addTermToContext(context);
        if( uiDisplayOutputExpression == null ) {
            return evalPlainTextOutputExpression(value, context);
        }
        return uiDisplayOutputExpression.evaluate(value, context);
    }

    @Override
    public boolean mustEscape() {
        return mustEscape;
    }

    public void setMustEscape(boolean mustEscape) {
        this.mustEscape = mustEscape;
    }

    @Override
    public Object evalViewHeaderExpression(Object entity, SearchContext context) {
        if (getViewHeaderExpression() == null) {
            if( context.getColumnParams() == null ) {
                return getName();
            } else {
                return context.getColumnParams().getUserColumnName();
            }
        } else {
            context = addTermToContext(context);
            return getViewHeaderExpression().evaluate(entity, context);
        }
    }

    /**
     * Get the collection of entities associated with parent row
     * Convenience method to eliminate ambiguity of calling evalPlainTextOutputExpression
     * @param entity  root of object graph that expression navigates.
     * @param context Other objects which (may be) used in the expression.
     */
    @Override
    public Collection<?> evalNestedTableExpression(Object entity,SearchContext context) {
        context = addTermToContext(context);
        return (Collection<?>) getDisplayValueExpression().evaluate(entity, context);
    }

    @Override
    public Object evalDownloadHeader1Expression(Object entity, SearchContext context) {
        return null;
    }

    @Override
    public Object evalDownloadHeader2Expression(Object entity, SearchContext context) {
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
        return this.dbSortPath;
    }

    public void setDbSortPath( String dbSortPath ) {
        this.dbSortPath = dbSortPath;
    }

    @Override
    public List<ColumnTabulation> getChildColumnTabulations() {
        return Collections.emptyList();
    }

    private SearchContext addTermToContext( SearchContext context ){
        if( context == null ) {
            context = new SearchContext();
        }
        // May require this SearchTerm to extract metadata key from column name
        context.setSearchTerm(this);
        return context;
    }
}
