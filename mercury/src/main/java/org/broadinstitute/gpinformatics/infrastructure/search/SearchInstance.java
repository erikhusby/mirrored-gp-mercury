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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.owasp.encoder.Encode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class represents an instance of a user-defined search.
 */
@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchInstance implements Serializable {

    private static final long serialVersionUID = -7253856859396073349L;

    public static final String CHOOSE_VALUE = "(Choose one)";

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SearchValue implements ColumnTabulation, Serializable {

        private static final long serialVersionUID = -9161359246882136450L;

        /**
         * The type of comparison between the term and its value.
         */
        private Operator operator;

        /**
         * The name of the search term (using name, rather than object reference, to ease serialization).
         */
        private String termName;

        /**
         * The value(s) of the search term.
         */
        private List<String> values;

        /**
         * The optional parent search value (e.g. metadata is dependent on a trait).
         */
        private SearchValue parent;

        /**
         * Dependent search values (e.g. metadata for a trait).
         */
        private List<SearchValue> children = new ArrayList<>();

        /**
         * When a rack scan is used as a source of barcodes, persist it so additional data is available in results
         * Stored as JSON text
         */
        private String rackScanData;

        /**
         * The list of valid values
         */
        @XmlTransient
        private List<ConstrainedValue> constrainedValues;

        /**
         * The user defined search data type of the value
         */
        @XmlTransient
        private ColumnValueType dataType;

        /**
         * True if this term should be displayed in the results.
         */
        private Boolean includeInResults = Boolean.FALSE;

        /**
         * True if comparisons should ignore case.
         */
        private Boolean caseInsensitive;

        /**
         * The search term definition on which this SearchValue is based.
         */
        @XmlTransient
        private SearchTerm searchTerm;

        /**
         * Map from value to value, allows testing of presence in JSP EL.
         */
        @XmlTransient
        private Map<String, String> mappedValues = new HashMap<>();

        /**
         * Whether the constrained values expression has been evaluated, avoid doing it multiple times.
         */
        @XmlTransient
        private boolean evaluatedConstrainedValues = false;

        /**
         * Unique ID for HTML divs.
         */
        @XmlTransient
        private static long uniqueId = System.currentTimeMillis();

        /**
         * True if the value was set when the instance was loaded from persistence, useful in read-only views.
         */
        @XmlTransient
        private Boolean valueSetWhenLoaded;

        /**
         * Holder of search values.
         */
        @XmlTransient
        private SearchInstance searchInstance;

        /**
         * False if the term has no value, for example.
         */
        @XmlTransient
        private Boolean addToCriteria;

        /**
         * Result of searchTerm.constrainedValuesSizeLimitExpression.
         */
        @XmlTransient
        private Integer constrainedValuesSizeLimit;

        /**
         * Default constructor for Stripes.
         */
        public SearchValue() {
        }

        public List<String> getJoinFetchPaths() {
            return searchTerm.getJoinFetchPaths();
        }

        /**
         * Constrained values are derived from an expression.
         *
         * @return results of expression.
         */
        public List<ConstrainedValue> getConstrainedValues() {
            if (searchTerm.getConstrainedValuesExpression() == null) {
                return null;
            }
            if (!evaluatedConstrainedValues) {
                SearchContext context = getSearchInstance().getEvalContext();
                context.setSearchValue(this);
                constrainedValues = searchTerm.getConstrainedValues(context);
                evaluatedConstrainedValues = true;
            }
            return constrainedValues;
        }

        /**
         * Evaluates the expression that limits the size of constrained values lists.
         *
         * @return maximum number of entries in the list.
         */
        public Integer getConstrainedValuesSizeLimit() {
            if (constrainedValuesSizeLimit == null) {
                if (searchTerm.getConstrainedValuesSizeLimitExpression() == null) {
                    constrainedValuesSizeLimit = 1000;
                } else {
                    SearchContext context = getSearchInstance().getEvalContext();
                    context.setSearchValue(this);
                    constrainedValuesSizeLimit = searchTerm.getConstrainedValuesSizeLimitExpression().evaluate(
                            null, context);
                }
            }
            return constrainedValuesSizeLimit;
        }

        /**
         * Determines whether the constrained values list should be displayed.
         *
         * @return true if drop-down should be shown for constrained values, false if a
         *         text box should be displayed.
         */
        public Boolean getConstrainedValuesListDisplayed() {
            return (getConstrainedValues() != null && !getConstrainedValues().isEmpty() &&
                    getConstrainedValues().size() < getConstrainedValuesSizeLimit());
        }

        /**
         * Property name may be determined an expression, e.g. for phenotypes and
         * annotations, the values are valueString, valueDouble, valueDate etc.
         *
         * @return name of the property used in a Hibernate restriction.
         */
        public String getPropertyName() {
            String propertyName = searchTerm.getCriteriaPaths().get(0).getPropertyName();
            SearchTerm.Evaluator<String> propertyNameExpression =
                    searchTerm.getCriteriaPaths().get(0).getPropertyNameExpression();
            if (propertyNameExpression != null) {
                SearchContext context = getSearchInstance().getEvalContext();
                context.setSearchValue(this);
                propertyName = propertyNameExpression.evaluate(null, context);
            }

            return propertyName;
        }

        /**
         * Data type may be determined by an expression, e.g. phenotypes.
         *
         * @return data type used in Hibernate restriction.
         */
        public ColumnValueType getDataType() {
            if (dataType == null) {
                SearchContext context = getSearchInstance().getEvalContext();
                context.setSearchValue(this);
                dataType = searchTerm.evalValueTypeExpression(null, context);
            }
            return dataType;
        }

        @SuppressWarnings("unused")
        public void setDataType(ColumnValueType dataType) {
            this.dataType = dataType;
        }

        /**
         * The depth of the hierarchy, used in building HTML structures.
         *
         * @return the number of ancestors.
         */
        public int getDepth() {
            int depth = 0;
            SearchValue ancestor = parent;
            while (ancestor != null) {
                ancestor = ancestor.getParent();
                depth++;
            }
            return depth;
        }

        public Map<String, String> getMappedValues() {
            if (mappedValues == null) {
                mappedValues = new HashMap<>();
            }
            if (mappedValues.isEmpty() && values != null && !values.isEmpty()) {
                for (String value : values) {
                    mappedValues.put(value, value);
                }
            }
            return mappedValues;
        }

        /**
         * Called when the user adds a sub-term.
         *
         * @return list of new search values, based on the dependent search terms.
         */
        public List<SearchValue> addDependentSearchValues() {
            // Add dependent values / terms.
            List<SearchValue> newSearchValues = new ArrayList<>();
            for (SearchTerm searchTerm : getSearchTerm().getDependentSearchTerms()) {
                SearchValue childSearchValue = new SearchValue();
                // If the value is a constant, set it and add dependent terms.
                if (searchTerm.getConstantValue() != null) {
                    List<String> values = new ArrayList<>();
                    values.add(searchTerm.getConstantValue());
                    childSearchValue.setValues(values);
                    childSearchValue.setOperator(Operator.EQUALS);
                    for (SearchTerm dependentSearchTerm : searchTerm.getDependentSearchTerms()) {
                        SearchValue dependentSearchValue = new SearchValue();
                        dependentSearchValue.setSearchInstance(getSearchInstance());
                        dependentSearchValue.setTermName(dependentSearchTerm.getName());
                        dependentSearchValue.setSearchTerm(dependentSearchTerm);
                        dependentSearchValue.setIncludeInResults(true);
                        List<SearchValue> children = new ArrayList<>();
                        children.add(dependentSearchValue);
                        dependentSearchValue.setParent(childSearchValue);
                        childSearchValue.setChildren(children);
                    }
                }

                childSearchValue.setTermName(searchTerm.getName());
                childSearchValue.setSearchTerm(searchTerm);
                childSearchValue.setParent(this);
                childSearchValue.setSearchInstance(getSearchInstance());
                childSearchValue.setIncludeInResults(true);
                getChildren().add(childSearchValue);
                newSearchValues.add(childSearchValue);
            }
            return newSearchValues;
        }

        /**
         * Convert the values to the appropriate types, splitting multiple values.
         *
         * @return list of objects of appropriate types
         */
        public List<Object> convertSearchValue() {
            List<Object> propertyValues = new ArrayList<>();
            // If the operator is IN and the values are free-form (rather than constrained)
            // then assume that the values are carriage return separated, and split them.
            if ((getOperator() == Operator.IN || getOperator() == Operator.NOT_IN) &&
                    (getSearchTerm().getConstrainedValuesExpression() == null || !getConstrainedValuesListDisplayed()) &&
                    getValues().size() == 1) {
                String[] lines = getValues().get(0).split("\\r?\\n");
                setValues(new ArrayList<String>());
                for (String value : lines) {
                    if (!StringUtils.isBlank(value)) {
                        getValues().add(value.trim());
                    }
                }
            }

            // Values could be null, e.g. a sqlRestriction.
            if (getValues() != null) {
                SimpleDateFormat mdyDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                for (String value : getValues()) {
                    if (searchTerm.getSearchValueConversionExpression() != null) {
                        SearchContext localContext = searchInstance.getEvalContext();
                        localContext.setSearchValue(this);
                        localContext.setSearchValueString(value);
                        propertyValues.add(searchTerm.getSearchValueConversionExpression().evaluate(null, localContext));
                    } else {
                        switch(getDataType()) {
                        case STRING:
                        case NOT_NULL:
                            propertyValues.add(value);
                            break;
                        case TWO_PLACE_DECIMAL:
                            propertyValues.add(new BigDecimal(value));
                            break;
                        case UNSIGNED:
                            propertyValues.add(new Long(value));
                            break;
                        case DATE:
                        case DATE_TIME:
                            try {
                                    propertyValues.add(mdyDateFormat.parse(value));
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            break;
                        case BOOLEAN:
                            propertyValues.add( Boolean.parseBoolean(value));
                            break;
                        default:
                            throw new RuntimeException("Unknown data type " + getDataType());
                        }
                    }
                }
            }
            return propertyValues;
        }

        public String evalTextOutputExpression(Object root, SearchContext context) {
            context = addValueToContext(context);
            try {
                return getSearchTerm().evalPlainTextOutputExpression(root, context);
            } catch (Exception e) {
                throw new RuntimeException("Exception evaluating text display expression for term "
                                              + getSearchTerm().getName() + ", for object " + root, e);
            }
        }

        public String evalUiOutputExpression(Object root, SearchContext context) {
            context = addValueToContext(context);
            try {
                return getSearchTerm().evalUiDisplayOutputExpression(root, context);
            } catch (Exception e) {
                throw new RuntimeException("Exception evaluating UI display expression for term "
                        + getSearchTerm().getName() + ", for object " + root, e);
            }
        }

        public Object evalHeaderExpression(Object root, SearchContext context) {
            if (getSearchTerm().getViewHeaderExpression() == null) {
                return getSearchTerm().evalViewHeaderExpression(root, context);
            } else {
                // The header is an expression, evaluate it.
                context = addValueToContext(context);
                return getSearchTerm().getViewHeaderExpression().evaluate(root, context);
            }
        }

        public List<SearchValue> getChildren() {
            return children;
        }

        public void setChildren(List<SearchValue> children) {
            this.children = children;
            for (SearchValue child : children) {
                child.setParent(this);
            }
        }

        public Operator getOperator() {
            return operator;
        }

        public void setOperator(Operator operator) {
            this.operator = operator;
        }

        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        public SearchValue getParent() {
            return parent;
        }

        public void setParent(SearchValue parent) {
            this.parent = parent;
        }

        public String getTermName() {
            return termName;
        }

        public void setTermName(String termName) {
            this.termName = termName;
        }

        public SearchTerm getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(SearchTerm searchTerm) {
            this.searchTerm = searchTerm;
        }

        public long getUniqueId() {
            return uniqueId++;
        }

        public Boolean getIncludeInResults() {
            return includeInResults;
        }

        public void setIncludeInResults(Boolean includeInResults) {
            this.includeInResults = includeInResults;
        }

        public Boolean getValueSetWhenLoaded() {
            return valueSetWhenLoaded;
        }

        public void setValueSetWhenLoaded(Boolean valueSetWhenLoaded) {
            this.valueSetWhenLoaded = valueSetWhenLoaded;
        }

        public SearchInstance getSearchInstance() {
            return searchInstance;
        }

        public void setSearchInstance(SearchInstance searchInstance) {
            this.searchInstance = searchInstance;
        }

        public Boolean getAddToCriteria() {
            return addToCriteria;
        }

        public void setAddToCriteria(Boolean addToCriteria) {
            this.addToCriteria = addToCriteria;
        }

        public void setRackScanData( String rackScanData ) {
            this.rackScanData = rackScanData;
        }

        public String getRackScanData() {
            return rackScanData;
        }

        @Override
        public String getName() {
            return searchTerm.getName();
        }

        @Override
        public Object evalValueExpression(Object entity, SearchContext context) {
            return searchTerm.evalValueExpression(entity, context);
        }

        @Override
        public String evalPlainTextOutputExpression(Object value, SearchContext context) {
            return evalTextOutputExpression(value, context);
        }

        @Override
        public String evalUiDisplayOutputExpression(Object value, SearchContext context) {
            return evalUiOutputExpression(value, context);
        }

        @Override
        public Object evalViewHeaderExpression(Object entity, SearchContext context) {
            return evalHeaderExpression(entity, context);
        }

        /* ***********************************************
         * None of the nested table logic applies to SearchValue
         */
        @Override
        public List<? extends ColumnTabulation> getNestedEntityColumns() {
            return getSearchTerm().getNestedEntityColumns();
        }

        @Override
        public void addNestedEntityColumn(ColumnTabulation x) {
            throw new UnsupportedOperationException(
                    "Method addNestedEntityColumn not implemented in SearchInstance.SearchValue");
        }

        @Override
        public Boolean isNestedParent() {
            return getSearchTerm().isNestedParent();
        }

        @Override
        public void setIsNestedParent(Boolean x) {
            throw new UnsupportedOperationException(
                    "Method setIsNestedParent not implemented in SearchInstance.SearchValue");
        }
        /* ***********************************************
         * End of the nested table logic
         */


        @Override
        public Object evalDownloadHeader1Expression(Object entity, SearchContext context) {
            return evalHeaderExpression(entity, context);
        }

        @Override
        public Object evalDownloadHeader2Expression(Object entity, SearchContext context) {
            return null;
        }

        public Collection<?> evalNestedTableExpression(Object entity, SearchContext context) {
            return getSearchTerm().evalNestedTableExpression(entity, context);
        }

        @Override
        public Class getPluginClass() {
            if( getSearchTerm() != null ) {
                return getSearchTerm().getPluginClass();
            } else {
                return null;
            }
        }
        @Override
        public void setPluginClass( Class pluginClass){
            if( getSearchTerm() != null ) {
                getSearchTerm().setPluginClass(pluginClass);
            }
        }

        @Override
        public String getDbSortPath() {
            if( getSearchTerm() != null ) {
                return getSearchTerm().getDbSortPath();
            } else {
                return null;
            }
        }

        /**
         * Get child columns, e.g. metadata for a trait
         *
         * @return list of child (and otherwise unreachable nephew) columns
         */
        @Override
        public List<ColumnTabulation> getChildColumnTabulations() {
            List<ColumnTabulation> columnTabulations = new ArrayList<>();
            for (SearchValue child : getChildren()) {
                if (child.getIncludeInResults() != null && child.getIncludeInResults()) {
                    columnTabulations.add(child);
                }
            }
            // In addition to children, we need to return included nephews whose parents are not included
            // in results, i.e. we need to return items from the next generation that would not be returned otherwise.
            // Look for excluded siblings, between the current node and the next included sibling, and add their
            // included children.
            if (getParent() != null) {
                boolean looking = false;
                for (SearchValue sibling : getParent().getChildren()) {
                    if (sibling.equals(this)) {
                        // We're interested in the siblings that follow the current one.
                        looking = true;
                        continue;
                    }
                    if (looking && (sibling.getIncludeInResults() != null && sibling.getIncludeInResults())) {
                        // We found an included sibling, so we're done, because it will look at its own children later.
                        break;
                    }
                    if (looking) {
                        for (SearchValue child : sibling.getChildren()) {
                            if (child.getIncludeInResults() != null && child.getIncludeInResults()) {
                                columnTabulations.add(child);
                            }
                        }
                    }
                }
            }
            return columnTabulations;
        }

        @Override
        public ColumnValueType evalValueTypeExpression(Object value, SearchContext context) {
            return getSearchTerm().evalValueTypeExpression(value,context);
        }

        public Boolean getCaseInsensitive() {
            return caseInsensitive;
        }

        public void setCaseInsensitive(Boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        /**
         * Determines whether the search value is displayed in the search editor.
         *
         * @return true if the value should be displayed.
         */
        public Boolean getDisplayed() {
            // Don't display the value if it has a constrained list expression, but the list is empty.
            // it confuses the user to see an empty text box which has no meaningful options.
            return searchTerm.getConstrainedValuesExpression() == null
                   || (getConstrainedValues() != null && !getConstrainedValues().isEmpty());
        }

        private SearchContext addValueToContext( SearchContext context ){
            if( context == null ) {
                context = getSearchInstance().getEvalContext();
                if( context == null ) {
                    context = new SearchContext();
                }
            }
            // May require this SearchTerm to extract metadata key from column name
            context.setSearchValue(this);
            return context;
        }
    }

    public enum Operator {
        EQUALS,
        IN,
        NOT_IN,
        NOT_EQUALS,
        LESS_THAN,
        LESS_THAN_EQUAL,
        GREATER_THAN,
        GREATER_THAN_EQUAL,
        BETWEEN,
        LIKE,
        NOT_NULL;

        public String getName() {
            return name();
        }
    }

    @SuppressWarnings("unused")
    public static class SearchOrderBy {

        private String dbSortPath;

        private String columnName;

        private String sortDirection;

        public SearchOrderBy() {
            // Default do nothing.
        }

        public SearchOrderBy(String columnName, String dbSortPath, String sortDirection) {

            this.columnName = columnName;
            this.dbSortPath = dbSortPath;
            this.sortDirection = sortDirection;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getDbSortPath() {
            return dbSortPath;
        }

        public void setDbSortPath(String dbSortPath) {
            this.dbSortPath = dbSortPath;
        }

        public String getSortDirection() {
            return sortDirection;
        }

        public void setSortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
        }
    }

    /**
     * Name of the search instance, so the user knows the contents
     */
    private String name;

    /**
     * List of top level search values, each may have children
     */
    private List<SearchValue> searchValues = new ArrayList<>();

    /**
     * List of columns names that the user wants included in results
     */
    private List<String> predefinedViewColumns = new ArrayList<>();

    /**
     * Map of view column indexes to any optional parameters associated
     */
    private Map<Integer,ResultParamValues> viewColumnParamMap = new HashMap<>();

    /**
     * List of columns names that the user wants to download
     */
    private List<String> predefinedDownloadColumns = new ArrayList<>();

    /**
     * List of order by values associating a db sort path with a sort direction.
     */
    private List<SearchOrderBy> orderByList;

    /**
     * List of columns in the column set that the user chose to view
     */
    @XmlTransient
    private List<String> columnSetColumnNameList;

    /**
     * Context for evaluation of expressions.
     */
    @XmlTransient
    private SearchContext evalContext;

    /**
     * Should searches include ancestors/descendants of directly located base entities?
     */
    private Map<String,Boolean> traversalEvaluatorValues = new HashMap<>();

    /**
     * Should a custom ancestor/descendant traversal be enlisted?
     */
    private String customTraversalOptionConfig;

    /**
     * customTraversalOptionConfig has been digested into name and params
     */
    @XmlTransient
    private String customTraversalOptionName;
    @XmlTransient
    private ResultParamValues customTraversalOptionParams;

    private boolean excludeInitialEntitiesFromResults = false;

    private boolean isDbSortable = true;

    private int pageSize;

    /**
     * Default constructor for Stripes.
     */
    public SearchInstance() {
    }

    /**
     * For JSP EL
     * @return value of constant
     */
    public String getChooseValue() {
        return CHOOSE_VALUE;
    }

    /**
     * Adds all required search terms.
     *
     * @param configurableSearchDef definitions of search terms.
     */
    public void addRequired(ConfigurableSearchDefinition configurableSearchDef) {
        for (SearchTerm searchTerm : configurableSearchDef.getRequiredSearchTerms()) {
            SearchValue searchValue = new SearchValue();
            searchValue.setTermName(searchTerm.getName());
            searchValue.setSearchTerm(searchTerm);
            searchValue.setSearchInstance(this);
            getSearchValues().add(searchValue);
        }
    }

    /**
     * Called when the user adds a term to a search.
     *
     * @param searchTermName        the name of the term.
     * @param configurableSearchDef the definitions of all terms.
     * @return new search value.
     */
    public SearchValue addTopLevelTerm(String searchTermName, ConfigurableSearchDefinition configurableSearchDef) {
        // Get the term and create an empty value for it.
        SearchTerm searchTerm = configurableSearchDef.getSearchTerm(searchTermName);
        if (searchTerm == null) {
            throw new RuntimeException("Failed to find search term " + searchTermName);
        }
        SearchValue searchValue = new SearchValue();
        searchValue.setTermName(searchTermName);
        searchValue.setSearchTerm(searchTerm);
        searchValue.setSearchInstance(this);
        searchValue.setIncludeInResults(true);

        // If the value is constant, set it, and add dependent terms.
        if (searchTerm.getConstantValue() != null) {
            List<String> values = new ArrayList<>();
            values.add(searchTerm.getConstantValue());
            searchValue.setValues(values);
            searchValue.setOperator(Operator.EQUALS);
            for (SearchTerm dependentSearchTerm : searchTerm.getDependentSearchTerms()) {
                SearchValue dependentSearchValue = new SearchValue();
                dependentSearchValue.setSearchInstance(this);
                dependentSearchValue.setTermName(dependentSearchTerm.getName());
                dependentSearchValue.setSearchTerm(dependentSearchTerm);
                dependentSearchValue.setIncludeInResults(true);
                List<SearchValue> children = new ArrayList<>();
                children.add(dependentSearchValue);
                dependentSearchValue.setParent(searchValue);
                searchValue.setChildren(children);
            }
        }
        searchValues.add(searchValue);
        return searchValue;
    }

    private Pair<String,ResultParamValues> splitTermAndParams(String nameAndParams ) {
        // Quick JSON test
        int index = nameAndParams.indexOf("{");
        if (index < 0) {
            return Pair.of(nameAndParams, null);
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(nameAndParams);
                ResultParamValues resultParams = new ResultParamValues(
                        root.get("paramType").getTextValue(),
                        root.get("entityName").getTextValue(),
                        root.get("elementName").getTextValue());
                for(Iterator<JsonNode> iter = root.get("paramValues").getElements(); iter.hasNext(); ) {
                    JsonNode input = iter.next();
                    resultParams.addParamValue( input.get("name").getTextValue(), input.get("value").getTextValue() );
                }
                return Pair.of(resultParams.getElementName(), resultParams);
            } catch( Exception je ) {
                throw new RuntimeException( "Fail parsing result column options", je);
            }
        }
    }

    /**
     * A SearchInstance built top-down by Stripes, or one fetched from a preference,
     * doesn't have parent relationships or SearchTerms, so this method sets them up.
     *
     * @param configurableSearchDefinition search terms.
     */
    public void establishRelationships(ConfigurableSearchDefinition configurableSearchDefinition) {
        recurseRelationships(configurableSearchDefinition, searchValues, null);
        buildTraversalOptions(configurableSearchDefinition);
        // Context as passed through processing needs a reference to the SearchInstance
        getEvalContext().setColumnEntityType(configurableSearchDefinition.getResultEntity());

        // Don't overwrite result params from any session SearchInstances (sorting)
        if( viewColumnParamMap.size() == 0 ) {
            for (int i = 0; i < predefinedViewColumns.size(); i++) {
                Pair<String, ResultParamValues> nameAndParams = splitTermAndParams(predefinedViewColumns.get(i));
                viewColumnParamMap.put(i, nameAndParams.getRight());
                if (nameAndParams.getRight() != null && nameAndParams.getRight().getUserColumnName() != null) {
                    predefinedViewColumns.set(i, nameAndParams.getLeft());
                }
            }
        }
    }

    /**
     * Builds out traversal options to include all available options,
     *    not just the ones set as true with user checkbox selections.
     * @param configurableSearchDefinition
     */
    private void buildTraversalOptions(ConfigurableSearchDefinition configurableSearchDefinition) {
        Map<String,TraversalEvaluator> evaluators = configurableSearchDefinition.getTraversalEvaluators();
        if( evaluators != null ) {
            for (String id : evaluators.keySet() ) {
                Boolean isSelected = traversalEvaluatorValues.get(id);
                if (isSelected == null) {
                    traversalEvaluatorValues.put(id, Boolean.FALSE);
                } else if( isSelected ) {
                    isDbSortable = false;
                }
            }
        }
    }

    /**
     * An order by clause can only be appended to a search criteria if there are no traversal evaluators
     * or alternate search definitions configured and/or selected
     * @return
     */
    public boolean getIsDbSortable(){
        return isDbSortable;
    }

    /**
     * Associate search terms and parents.
     *
     * @param configurableSearchDefinition definitions of all search terms.
     * @param valuesToSearch                 list of terms at current level of recursion.
     * @param parent                       ancestor at previous level of recursion.
     */
    private void recurseRelationships(
            ConfigurableSearchDefinition configurableSearchDefinition,
            List<SearchValue> valuesToSearch, SearchValue parent) {
        for (SearchValue searchValue : valuesToSearch) {
            searchValue.setSearchTerm(configurableSearchDefinition.getSearchTerm(searchValue.getTermName()));
            searchValue.setParent(parent);
            searchValue.setSearchInstance(this);
            if( searchValue.getSearchTerm().getAlternateSearchDefinition() != null ) {
                isDbSortable = false;
            }
            recurseRelationships(configurableSearchDefinition, searchValue.getChildren(), searchValue);
        }
    }

    /**
     * For the read-only view, we need to set some indicators immediately after loading a saved instance.
     */
    public void postLoad() {
        recursePostLoad(searchValues);
    }

    /**
     * Recurse, looking for searchValues with values set.
     *
     * @param valuesToSearch list of searchValues at current level of recursion.
     */
    private void recursePostLoad(List<SearchValue> valuesToSearch) {
        for (SearchValue searchValue : valuesToSearch) {
            if (searchValue.getSearchTerm().getConstantValue() != null
                || (searchValue.getValues() != null && !searchValue.getValues().isEmpty())) {
                searchValue.setValueSetWhenLoaded(true);
            }
            recursePostLoad(searchValue.getChildren());
        }
    }

    /**
     * Prior to calling the DAO, we must determine which terms have values specified.
     * All terms must have values to prevent Oracle table scans.
     * (Search UI ConfigurableSearchActionBean intercepts a search with no terms)
     * @return false if any terms have empty values
     */
    public boolean checkValues() {
        return recurseCheckValues(searchValues);
    }

    private boolean recurseCheckValues(List<SearchValue> valuesToSearch) {
        boolean atLeastOneAdd = false;
        for (SearchValue searchValue : valuesToSearch) {
            boolean atLeastOneChildAdd = recurseCheckValues(searchValue.getChildren());
            if (!searchValue.getChildren().isEmpty() && !atLeastOneChildAdd) {
                searchValue.setAddToCriteria(false);
            } else if (searchValue.getValues() == null || searchValue.getValues().isEmpty() ||
                    searchValue.getValues().get(0).isEmpty() ||
                    searchValue.getValues().get(0).equals(CHOOSE_VALUE)) {
                searchValue.setAddToCriteria(false);
            } else {
                searchValue.setAddToCriteria(true);
                atLeastOneAdd = true;
            }
        }
        return atLeastOneAdd;
    }

    /**
     * Prior to calling the DAO, determine if any other terms are present along with an exclusive term.
     * Applies to parent terms only.
     * @return false if any other terms are present along with an exclusive term.
     */
    public boolean hasExclusiveViolation(){
        boolean hasExclusiveTerm = false;
        int count = 0;
        for( SearchValue value : getSearchValues() ) {
            if( value.getSearchTerm().isExclusive() ) {
                hasExclusiveTerm = true;
            }
            count++;
        }
        return hasExclusiveTerm && count > 1;
    }

    /**
     * Recurse through the search instance, looking for search values that the user wants
     * to display in the results.
     *
     * @return accumulated display values.
     */
    public List<SearchValue> findDisplaySearchValues() {
        List<SearchValue> displaySearchValues = new ArrayList<>();
        recurseForDisplaySearchValues(searchValues, displaySearchValues);
        return displaySearchValues;
    }

    /**
     * Recurse through the search instance, looking for search values that the user wants
     * to display in the results.
     *
     * @param valuesToSearch        values for current level.
     * @param displaySearchValues accumulated display values.
     */
    private void recurseForDisplaySearchValues(List<SearchValue> valuesToSearch,
                                               List<SearchValue> displaySearchValues) {
        for (SearchValue searchValue : valuesToSearch) {
            if (searchValue.getIncludeInResults() != null && searchValue.getIncludeInResults()
                    && (searchValue.getSearchTerm().getDisplayValueExpression() != null ||
                    searchValue.getSearchTerm().getDisplayExpression() != null)) {
                displaySearchValues.add(searchValue);
            }
            recurseForDisplaySearchValues(searchValue.getChildren(), displaySearchValues);
        }
    }

    /**
     * In the hierarchy of search values, find the shallowest terms that the user wants to
     * display in the search results. We don't want to go any deeper, because each
     * columnTabulation will return the next level when asked.
     *
     * @return top-level column tabulations.
     */
    public List<ColumnTabulation> findTopLevelColumnTabulations() {
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        int depth = 1;
        recurseForColumnTabulations(searchValues, columnTabulations, depth);
        return columnTabulations;
    }

    /**
     * Recurse through the search instance, looking for the shallowest terms that the user
     * wants to display in search results; when found, return without going deeper.
     *
     * @param valuesToSearch      values for current level.
     * @param columnTabulations accumulated result columns.
     * @param depth             current depth of recursion.
     */
    private void recurseForColumnTabulations(
            List<SearchValue> valuesToSearch, List<ColumnTabulation> columnTabulations, int depth) {
        for (SearchValue searchValue : valuesToSearch) {
            if (searchValue.getIncludeInResults() != null && searchValue.getIncludeInResults()
                    && (searchValue.getSearchTerm().getDisplayValueExpression() != null ||
                    searchValue.getSearchTerm().getDisplayExpression() != null)) {
                columnTabulations.add(searchValue);
                if (depth > 1) {
                    return;
                }
            } else {
                recurseForColumnTabulations(searchValue.getChildren(), columnTabulations, depth + 1);
            }
        }
    }

    /**
     * For result columns, a configurable search can combine column sets, individual
     * columns, and search terms. This method builds a list of columns that match what the
     * user is viewing, to allow the same columns to be downloaded.
     *
     * @return list of columns
     */
    public List<ColumnTabulation> buildViewedColumnTabulations(String entityName) {
        List<String> columnNameList;
        if (getPredefinedViewColumns() != null && !getPredefinedViewColumns().isEmpty()) {
            columnNameList = getPredefinedViewColumns();
        } else {
            columnNameList = getColumnSetColumnNameList();
        }
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entityName);
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnNameList) {
            columnTabulations.add(configurableSearchDef.getSearchTerm(columnName));
        }
        columnTabulations.addAll(findTopLevelColumnTabulations());
        return columnTabulations;
    }

    public List<SearchValue> getSearchValues() {
        return searchValues;
    }

    /**
     * Determine whether or not the base search definition for this instance should be used
     *   or if the alternate search definition attached to an exclusive search term should be used.
     * Configuration validation should have been previously tested via hasExclusiveViolation()
     * @see #hasExclusiveViolation()
     * @return
     */
    public boolean hasAlternateSearchDefinition(){
        if( searchValues != null && searchValues.size() == 1
                && searchValues.get(0).getSearchTerm().getAlternateSearchDefinition() != null ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the alternate search definition attached to the exclusive search term.
     * @return
     */
    public ConfigurableSearchDefinition getAlternateSearchDefinition(){
        if( !hasExclusiveViolation() && hasAlternateSearchDefinition() ){
            return searchValues.get(0).getSearchTerm().getAlternateSearchDefinition();
        } else {
            return null;
        }
    }

    public static SearchInstance findSearchInstance(PreferenceType type, String searchName,
            ConfigurableSearchDefinition configurableSearchDef, Map<PreferenceType, Preference> preferenceMap) {
        SearchInstanceList searchInstanceList = null;
        if( preferenceMap.containsKey(type)){
            try {
                searchInstanceList =
                        (SearchInstanceList) preferenceMap.get(type).getPreferenceDefinition().getDefinitionValue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (searchInstanceList == null) {
            throw new RuntimeException("No saved search instances available for type " + type);
        }

        SearchInstance searchInstance = null;
        for (SearchInstance searchInstanceLocal : searchInstanceList.getSearchInstances()) {
            if (searchInstanceLocal.getName().equals(searchName)) {
                searchInstance = searchInstanceLocal;
                break;
            }
        }
        if (searchInstance == null) {
            throw new RuntimeException("No saved search instance named '" + Encode.forHtml(searchName) + "' is available");
        }
        searchInstance.establishRelationships(configurableSearchDef);
        searchInstance.postLoad();
        return searchInstance;
    }

    public void setSearchValues(List<SearchValue> searchValues) {
        this.searchValues = searchValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPredefinedViewColumns() {
        return predefinedViewColumns;
    }

    public void setPredefinedViewColumns(List<String> predefinedViewColumns) {
        this.predefinedViewColumns = predefinedViewColumns;
    }

    public Map<Integer,ResultParamValues> getViewColumnParamMap() {
        return viewColumnParamMap;
    }

    public ResultParamValues getTraverserParams() {
        return customTraversalOptionParams;
    }

    public List<String> getPredefinedDownloadColumns() {
        return predefinedDownloadColumns;
    }

    public void setPredefinedDownloadColumns(List<String> predefinedDownloadColumns) {
        this.predefinedDownloadColumns = predefinedDownloadColumns;
    }

    public SearchContext getEvalContext() {
        if( evalContext == null ) {
            evalContext = new SearchContext();
        }
        evalContext.setSearchInstance(this);
        return evalContext;
    }

    public void setEvalContext(SearchContext evalContext) {
        this.evalContext = evalContext;
    }

    public List<String> getColumnSetColumnNameList() {
        return columnSetColumnNameList;
    }

    public void setColumnSetColumnNameList(List<String> columnSetColumnNameList) {
        this.columnSetColumnNameList = columnSetColumnNameList;
    }

    public List<SearchOrderBy> getOrderByList() {
        return orderByList;
    }

    public void setOrderByList(List<SearchOrderBy> orderByList) {
        this.orderByList = orderByList;
    }

    /**
     * "ancestorOptionEnabled" --> TRUE/FALSE
     * "descendantOptionEnabled" --> TRUE/FALSE
     */
    public Map<String,Boolean> getTraversalEvaluatorValues(){
        return traversalEvaluatorValues;
    }

    public void setCustomTraversalOptionConfig( String customTraversalOptionConfig ) {
        this.customTraversalOptionConfig = customTraversalOptionConfig;
    }


    public String getCustomTraversalOptionName(){
        if( customTraversalOptionName == null ) {
            if( customTraversalOptionConfig == null || customTraversalOptionConfig.isEmpty() ) {
                customTraversalOptionName = "none";  // ConfigurableSearchDefinition returns null by name and quietly ignores
            } else {
                Pair<String,ResultParamValues> nameAndParams = splitTermAndParams(customTraversalOptionConfig);
                customTraversalOptionName = nameAndParams.getLeft();
                customTraversalOptionParams = nameAndParams.getRight();
            }
        }
        return customTraversalOptionName;
    }

    public ResultParamValues getCustomTraversalOptionParams(){
        if( customTraversalOptionName == null ) {
           // Force a split of name and params
            getCustomTraversalOptionName();
        }
        return customTraversalOptionParams;
    }

    public boolean getExcludeInitialEntitiesFromResults(){
        return excludeInitialEntitiesFromResults;
    }

    public void setExcludeInitialEntitiesFromResults( boolean excludeInitialEntitiesFromResults ) {
        this.excludeInitialEntitiesFromResults = excludeInitialEntitiesFromResults;
    }

    public void setPageSize( int pageSize ) {
        this.pageSize = pageSize;
    }

    public int getPageSize(){
        // Default to 100 if uninitialized
        if( pageSize == 0 ) {
            return 100;
        } else {
            return pageSize;
        }
    }

}
