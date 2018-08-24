package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
    private JSONObject rackScanData;
    private ResultCellTargetPlatform resultCellTargetPlatform = ResultCellTargetPlatform.TEXT;
    private String baseSearchURL;
    private PaginationUtil.Pagination pagination;
    private ResultParamValues columnParams;
    private JiraConfig jiraConfig;
    private PriceListCache priceListCache;
    private QuoteLink quoteLink;
    private UserBean userBean;

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

    /**
     * Rack scan data (if used as search term input for multiple vessel barcodes) is passed from browser as JSON.
     * This method avoids having to re-parse JSON for every required result column and row. <br/>
     * Note:  Only a single rack scan can be associated with a search instance
     */
    public JSONObject getScanData(){
        if( rackScanData != null ) {
            return rackScanData;
        } else {
            try {
                for (SearchInstance.SearchValue searchValue : searchInstance.getSearchValues()) {
                    if (searchValue.getRackScanData() != null) {
                        rackScanData = new JSONObject(new JSONTokener(searchValue.getRackScanData()));
                        break;
                    }
                }
            } catch (JSONException jse) {
                throw new RuntimeException("Unable to parse JSON rack scan data", jse);
            }
        }
        return rackScanData;
    }

    /**
     * What is put in result cells based upon target platform (e.g. rendering, drill-down hyperlinks)
     * Initial configuration typically done in action bean or REST endpoint
     * @return One of the three enum types, TEXT being the legacy default because
     * org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator handles text only
     */
    public ResultCellTargetPlatform getResultCellTargetPlatform() {
        return resultCellTargetPlatform;
    }

    /**
     * Configured at the initial action bean or REST endpoint to control the output options
     * at the render phase: ColumnTabulation#evalFormattedExpression(...)
     */
    public void setResultCellTargetPlatform(ResultCellTargetPlatform resultCellTargetPlatform) {
        this.resultCellTargetPlatform = resultCellTargetPlatform;
    }

    /**
     * Base URL for optional hyperlinks in result cells.
     * Initialized at action bean end point.
     */
    public String getBaseSearchURL() {
        return baseSearchURL;
    }

    /**
     * Set the base URL for constructing optional hyperlinks in result cells.
     * Initialized at action bean end point.
     */
    public void setBaseSearchURL(StringBuffer baseSearchURL) {
        int qsDelimiterIndex = baseSearchURL.indexOf("?");
        if( qsDelimiterIndex > 0 ) {
            this.baseSearchURL = baseSearchURL.substring(0, qsDelimiterIndex );
        } else {
            this.baseSearchURL = baseSearchURL.toString();
        }
    }

    public PaginationUtil.Pagination getPagination() {
        return pagination;
    }

    public void setPagination(PaginationUtil.Pagination pagination) {
        this.pagination = pagination;
    }

    /**
     * Gets a copy of result column parameter values for use in generating output header and value
     */
    public ResultParamValues getColumnParams(){
        return columnParams;
    }

    /**
     * Sets a copy of result column parameter values for use in generating output header and value
     */
    public void setColumnParams( ResultParamValues columnParams ) {
        this.columnParams = columnParams;
    }

    public UserBean getUserBean() {
        return userBean;
    }

    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    public JiraConfig getJiraConfig() {
        return jiraConfig;
    }

    public void setJiraConfig(JiraConfig jiraConfig) {
        this.jiraConfig = jiraConfig;
    }

    public PriceListCache getPriceListCache() {
        return priceListCache;
    }

    public void setPriceListCache(PriceListCache priceListCache) {
        this.priceListCache = priceListCache;
    }

    public QuoteLink getQuoteLink() {
        return quoteLink;
    }

    public void setQuoteLink(QuoteLink quoteLink) {
        this.quoteLink = quoteLink;
    }

    // Need to know what to put in cells at rendering stage at ColumnTabulation#evalFormattedExpression(...)
    public enum ResultCellTargetPlatform {
        WEB,  // Apply optional UI output formatting expression
        TEXT  // Raw text (default)
    }

}
