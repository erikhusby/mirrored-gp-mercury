package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds various type-safe values required for search functionality.
 * Replaces a non type-safe Map<String,Object> implementation for which the list of names grew to be unwieldy.
 */
public class SearchContext {

    private BSPUserList bspUserList;
    // Not used:  columnSetType
    private SearchInstance.SearchValue searchValue;
    private SearchTerm searchTerm;
    private SearchInstance searchInstance;
    private ConstrainedValueDao optionValueDao;
    private BSPSampleSearchService bspSampleSearchService;
    private ColumnEntity columnEntityType;
    private String searchValueString;
    private String multiValueDelimiter = " ";
    private Map<String,ConfigurableList.AddRowsListener> addRowsListeners;

    /**
     * Avoid having to access EJB or web application context to get user data for display
     */
    public BSPUserList getBspUserList() {
        return bspUserList;
    }

    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    /**
     * Search related object instances
     */
    public SearchInstance.SearchValue getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(
            SearchInstance.SearchValue searchValue) {
        this.searchValue = searchValue;
    }

    public SearchTerm getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(SearchTerm searchTerm) {
        this.searchTerm = searchTerm;
    }

    public SearchInstance getSearchInstance() {
        return searchInstance;
    }

    public void setSearchInstance(SearchInstance searchInstance) {
        this.searchInstance = searchInstance;
    }

    /**
     * Some select options are derived from the database via sub-classes of ConstrainedValueDao
     */
    public ConstrainedValueDao getOptionValueDao() {
        return optionValueDao;
    }

    public void setOptionValueDao(
            ConstrainedValueDao optionValueDao) {
        this.optionValueDao = optionValueDao;
    }

    /**
     * A batch of BSP sample data from a web method request then passed throughout the page for display
     */
    public BSPSampleSearchService getBspSampleSearchService() {
        return bspSampleSearchService;
    }

    public void setBspSampleSearchService(
            BSPSampleSearchService bspSampleSearchService) {
        this.bspSampleSearchService = bspSampleSearchService;
    }

    /**
     * A plugin may require the entity type of the search in order to obtain the ConfigurableSearchDefinition
     */
    public ColumnEntity getColumnEntityType() {
        return columnEntityType;
    }

    public void setColumnEntityType(ColumnEntity columnEntityType) {
        this.columnEntityType = columnEntityType;
    }

    /**
     * Typical use case is for searchTerm.setSearchValueConversionExpression
     * User input is by default an array of strings, each of which must be separately converted.
     */
    public String getSearchValueString() {
        return searchValueString;
    }

    public void setSearchValueString(String searchValueString) {
        this.searchValueString = searchValueString;
    }

    /**
     * A loosely coupled object (plugin) may require a custom list delimiter other than the default single space
     */
    public String getMultiValueDelimiter() {
        return multiValueDelimiter;
    }

    public void setMultiValueDelimiter(String multiValueDelimiter) {
        this.multiValueDelimiter = multiValueDelimiter;
    }

    public void addRowsListener( String name, ConfigurableList.AddRowsListener rowsListener ) {
        if( addRowsListeners == null ) {
            addRowsListeners = new HashMap<>();
        }
        addRowsListeners.put(name, rowsListener);
    }

    public ConfigurableList.AddRowsListener getRowsListener( String name ) {
        if( addRowsListeners == null ) {
            return null;
        }
        return addRowsListeners.get(name);
    }


}
