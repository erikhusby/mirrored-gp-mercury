/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2008 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.mercury.presentation.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.SearchInstanceNameCache;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.ConstrainedValueDao;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchRequestBean;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchValueBean;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This Stripes action bean allows the user to: create searches based on configurable
 * search definitions; save these searches for future use; execute created and saved
 * searches.
 */
@SuppressWarnings("UnusedDeclaration")
@UrlBinding("/search/ConfigurableSearch.action")
public class ConfigurableSearchActionBean extends CoreActionBean {

    private static final Log log = LogFactory.getLog(ConfigurableSearchActionBean.class);

    /**
     * Prefix for search instance session key
     */
    public static final String SEARCH_INSTANCE_PREFIX = "searchInstance_";

    /**
     * Prefix for pagination session key
     */
    public static final String PAGINATION_PREFIX = "pagination_";

    public static final String RACK_SCAN_PAGE_TITLE = "Rack Scan Barcodes";
    public static final String DRILL_DOWN_EVENT = "drillDown";

    /**
     * The definition from which the user will create the search
     */
    private ConfigurableSearchDefinition configurableSearchDef;

    /**
     * The available search names for this entity type.
     * Key is search name prefixed by scope (GLOBAL/USER),
     *   value is pipe delimited scope|preference type|search name (e.g. GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|Custom Search)
     */
    private Map<String,String> searchInstanceNames;

    /**
     * All available saved searches for every entity type (populates selection list on search type selection page).
     * [LabVessel, LabEvent, ...]
     *                '-> [Global Type, User Type]
     *                                      '-> [Saved Search Names...]
     */
    private  Map<ColumnEntity, Map<PreferenceType,List<String>>> allSearchInstances;

    /**
     * The name of an existing search the user is using
     */
    private String selectedSearchName;

    /**
     * The name of a new search the user is creating
     */
    private String newSearchName;

    /**
     * The preference level of a new search
     */
    private String newSearchLevel;

    /**
     * The available levels in which the user can save new searches.
     * keys are GLOBAL and LOCAL,
     *    values are associated preference name, never more than 1 each for each search entity
     */
    private Map<String,String> newSearchLevels = new HashMap<>();

    /**
     * The search the user is creating or running
     */
    private SearchInstance searchInstance;

    /**
     * The name of the search term to add (chosen from the drop down)
     */
    private String searchTermName;

    /**
     * Some of the entries in the search term drop down are actually expansions of the
     * constrained values in the top-level term, e.g. Primary Disease is a value for
     * Phenotype Name. When the user adds one of these pseudo-terms, this field holds the
     * constrained value.
     */
    private String searchTermFirstValue;

    /**
     * The dependent search values to render in an HTML fragment
     */
    private List<SearchInstance.SearchValue> searchValueList;

    /**
     * Which set of columns to use for displaying results
     */
    private String columnSetName;

    /**
     * For single page results, which result set column to in-memory sort by, default to first
     */
    private int sortColumnIndex;

    /**
     * For multi-page results, the Hibernate property to order by
     */
    private String dbSortPath;

    /**
     * Direction to order by, ascending by default
     */
    private String sortDirection = "ASC";

    /**
     * Search results in column set chosen by user
     */
    private ConfigurableList.ResultList configurableResultList;

    /**
     * True if the user is not allowed to change the structure of the search
     */
    private boolean readOnly;

    /**
     * True for a minimal user interface, i.e. unchangeable operators
     */
    private boolean minimal;

    /**
     * HTTP session key for search parameters, used in re-sorting results columns
     */
    private String sessionKey;

    /**
     * Map from preference type name to fetched preference
     */
    private Map<PreferenceType, Preference> preferenceMap;

    /**
     * The number of the page to fetch in the results set
     */
    private int pageNumber;

    /**
     * Which entity we're searching on (LabEvent, LabVessel as of 07/22/2014)
     */
    private ColumnEntity entityType;

    /**
     * Unmarshalled drill down link data
     */
    private SearchRequestBean drillDownRequest;

    // Dependencies
    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    @Inject
    private SearchInstanceNameCache searchInstanceNameCache;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ConstrainedValueDao constrainedValueDao;

    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private SapQuoteLink sapQuoteLink;

    /**
     * Called from the search menu selection link.
     * User must select the base entity to begin a search or select an existing USER or GLOBAL saved search.
     *
     * Populates a set of all global and user defined searches to select from
     *
     * @return JSP to edit search
     */
    @HandlesEvent("entitySelection")
    @DefaultHandler
    public Resolution entitySelectionPage() {
        allSearchInstances = new LinkedHashMap<>();
        try {
            allSearchInstances = searchInstanceNameCache.fetchInstanceNames();
        } catch (Exception e) {
            addGlobalValidationError("Failed to retrieve search definitions");
        }
        return new ForwardResolution("/search/config_search_choose_entity.jsp");
    }

    /**
     * Called when the user selects the entity to search, this method fetches preferences.
     *
     * @return JSP to edit search
     */
    @HandlesEvent("queryPage")
    public Resolution queryPage() {
        getPreferences();
        searchInstance = new SearchInstance();
        searchInstance.addRequired(configurableSearchDef);
        return new ForwardResolution("/search/configurable_search.jsp");
    }


    @HandlesEvent(DRILL_DOWN_EVENT)
    public Resolution drillDown() {
        if( drillDownRequest == null ) {
            addGlobalValidationError("Search drill down request is incomplete.");
            return new ForwardResolution("/search/config_search_choose_entity.jsp");
        }

        entityType = ColumnEntity.getByName(drillDownRequest.getEntityName());
        selectedSearchName = drillDownRequest.getSearchName();

        Resolution ignoreIt = fetchSearch();

        boolean isMissingValue = false;
        for( SearchInstance.SearchValue searchValue : searchInstance.getSearchValues() ) {
            for( SearchValueBean term : drillDownRequest.getSearchValueBeanList()) {
                isMissingValue = true;
                if( term.getTermName().equals(searchValue.getTermName() )) {
                    searchValue.setValues(term.getValues());
                    isMissingValue = false;
                    break;
                }
            }
            if(isMissingValue) {
                addGlobalValidationError("A term value for term '" + searchValue.getTermName() + "' is missing." );
                return new ForwardResolution("/search/configurable_search.jsp");
            }
        }

        // Dependencies are built, hand off to standard search logic
        return search();

    }

    private void getPreferences() {
        preferenceMap = new HashMap<>();
        searchInstanceNames = new HashMap<>();
        newSearchLevels = new HashMap<>();
        try {
            configurableSearchDef = SearchDefinitionFactory.getForEntity(entityType.getEntityName());
            // TODO JMS Use SearchInstanceNameCache
            searchInstanceEjb.fetchInstances( entityType, preferenceMap,  searchInstanceNames, newSearchLevels );
        } catch (Exception e) {
            addGlobalValidationError("Failed to retrieve search definitions");
        }
    }

    /**
     * Called when the user chooses a saved search.
     *
     * @return JSP to display search
     */
    public Resolution fetchSearch() {
        getPreferences();

        String[] searchValues = selectedSearchName.split("\\|");
        PreferenceType.PreferenceScope scope = PreferenceType.PreferenceScope.valueOf(searchValues[0]);
        PreferenceType type = PreferenceType.valueOf(searchValues[1]);
        String searchName = searchValues[2];

        searchInstance = SearchInstance.findSearchInstance(type, searchName, configurableSearchDef, preferenceMap);
        buildSearchContext();
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     * Handles AJAX request to add a top level search term
     *
     * @return JSP fragment to render term
     */
    public Resolution addTopLevelTerm() {
        configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );
        searchInstance = new SearchInstance();
        buildSearchContext();
        searchInstance.addTopLevelTerm(searchTermName, configurableSearchDef);

        searchValueList = searchInstance.getSearchValues();
        return new ForwardResolution("/search/search_term_fragment.jsp");
    }

    /**
     * Handles AJAX request to add a top level search term, and populate the next level,
     * e.g. add a phenotype and set the phenotype name
     *
     * @return JSP fragment to render term
     */
    public Resolution addTopLevelTermWithValue() {
        configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );
        searchInstance = new SearchInstance();
        buildSearchContext();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm(searchTermName, configurableSearchDef);

        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        List<String> values = new ArrayList<>();
        values.add(searchTermFirstValue);
        searchValue.setValues(values);
        searchValue.addDependentSearchValues();

        searchValueList = searchInstance.getSearchValues();
        return new ForwardResolution("/search/search_term_fragment.jsp");
    }

    /**
     * Handles AJAX request to add a child term
     *
     * @return JSP fragment to render search term
     */
    public Resolution addChildTerm() {
        configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );
        buildSearchContext();
        searchInstance.establishRelationships(configurableSearchDef);
        searchValueList = recurseToLeaf(searchInstance.getSearchValues());
        return new ForwardResolution("/search/search_term_fragment.jsp");
    }

    /**
     * Recurse down the search instance from an AJAX request (only one node per level)
     * until reach the lowest node; at the end of the recursion, add dependent terms and
     * values.
     *
     * @param searchValues list of values (actually only one) at current level of
     *                     recursion
     * @return list of new values
     */
    List<SearchInstance.SearchValue> recurseToLeaf(List<SearchInstance.SearchValue> searchValues) {
        SearchInstance.SearchValue searchValue = searchValues.get(0);
        if (searchValue.getChildren() == null || searchValue.getChildren().isEmpty()) {
            return searchValue.addDependentSearchValues();
        }
        return recurseToLeaf(searchValue.getChildren());
    }

    /**
     * Called when the user clicks the Search button, this method calls the DAO to get the
     * search results, then creates a list in the requested column view set.
     *
     * @return JSP to display results
     */
    public Resolution search() {
        getPreferences();
        if (sessionKey == null) {
            // Save the searchInstance, in case the user re-sorts the results
            sessionKey = Integer.toString(new Random().nextInt(10000));
            getContext().getRequest().getSession().setAttribute(SEARCH_INSTANCE_PREFIX + sessionKey, searchInstance);
        } else {
            // We're resorting results we retrieved previously
            // Get saved form parameters, so the JSP re-renders correctly
            searchInstance = (SearchInstance) getContext().getRequest().getSession()
                    .getAttribute(SEARCH_INSTANCE_PREFIX + sessionKey);
        }

        if (searchInstance == null || searchInstance.getSearchValues() == null ) {
            // Handles search attempt without any SearchInstance (Cause is questionable)
            addGlobalValidationError("Search has no result columns or search terms");
        } else if ( searchInstance.getSearchValues().isEmpty()) {
            // Handles search attempt without any terms
            //    (see ConfigurableListFactory.getFirstResultsPage for more validations)
            searchInstance.establishRelationships(configurableSearchDef);
            addGlobalValidationError("You must add at least one search term");
        } else {
            searchInstance.establishRelationships(configurableSearchDef);

            buildSearchContext();

            // Set up proper sort direction display/functionality of entity headers
            if( dbSortPath == null || dbSortPath.isEmpty() ) {
                dbSortPath = configurableSearchDef.getResultEntity().getEntityIdProperty();
            }

            try {
                ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDef, columnSetName, sortColumnIndex, dbSortPath,
                        sortDirection,  getEntityName() );
                configurableResultList = firstPageResults.getResultList();
                getContext().getRequest().getSession().setAttribute(PAGINATION_PREFIX + sessionKey,
                        firstPageResults.getPagination());
            } catch (InformaticsServiceException ve) {
                log.error(ve.getMessage(), ve);
                addGlobalValidationError(ve.getMessage());
            } catch (BSPLookupException bspse) {
                handleRemoteServiceFailure(bspse);
            }
        }
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     * Gets a subsequent page from the search results
     *
     * @return JSP to display results subset
     */
    public Resolution searchPage() {
        // Get saved form parameters, so the JSP re-renders correctly
        getPreferences();
        searchInstance = (SearchInstance) getContext().getRequest().getSession().getAttribute(
                SEARCH_INSTANCE_PREFIX + sessionKey);

        // Search configuration and paging data have been lost due to session expiration.
        // The best recovery is to use the back button and click Search button, but relies on browser page caching.
        // Code gets here by user logging in from another browser tab then clicking on a page link in the expired page
        //  or clicking a page link and getting forwarded after a session timeout login.
        if( searchInstance == null ) {
            addGlobalValidationError("The paging results are no longer available.  Rerun the search.");
            return new ForwardResolution("/search/configurable_search.jsp");
        }

        buildSearchContext();

        try {
            configurableResultList =
                    configurableListFactory.getSubsequentResultsPage(searchInstance, pageNumber, getEntityName(),
                            (PaginationUtil.Pagination) getContext().getRequest().getSession().getAttribute(
                                    PAGINATION_PREFIX + sessionKey));
        } catch (BSPLookupException bspse) {
            handleRemoteServiceFailure( bspse );
        }
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     * If a search relies on data retrieved via web services (Lab Vessel BSP columns),
     * Gracefully allow the user to view the issue and avoid the hard failure page
     */
    private void handleRemoteServiceFailure( BSPLookupException bspse ) {
        log.error(bspse.getMessage(), bspse);
        addGlobalValidationError( "BSP access failure:  " + bspse.getMessage() );
        if( bspse.getCause() != null ){
            addGlobalValidationError( " Root cause:  " + bspse.getCause().getMessage() );
        }
        addGlobalValidationError( "Remove BSP columns from search or try again later" );
    }

    /**
     * Called when the user clicks the Update Search button, this method saves changes to
     * an existing search.
     *
     * @return JSP to display confirmation message
     */
    public Resolution updateSearch() {
        persistSearch(false);
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     * Called when the user clicks the Save New Search button, this method saves a search
     * for future user
     *
     * @return JSP to display confirmation message
     */
    public Resolution saveNewSearch() {
        persistSearch(true);
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     *  Add components needed for search functionality
     *    BSP user lookup required in user name column eval expression
     *  Use context to avoid need to test in container
     */
    private void buildSearchContext(){
        searchInstance.getEvalContext().setBspUserList(bspUserList);
        searchInstance.getEvalContext().setBspSampleSearchService(bspSampleSearchService);
        searchInstance.getEvalContext().setOptionValueDao(constrainedValueDao);
        searchInstance.getEvalContext().setSearchInstance(searchInstance);
        searchInstance.getEvalContext().setResultCellTargetPlatform(SearchContext.ResultCellTargetPlatform.WEB);
        searchInstance.getEvalContext().setBaseSearchURL(getContext().getRequest().getRequestURL());
        searchInstance.getEvalContext().setUserBean(userBean);
        searchInstance.getEvalContext().setJiraConfig(jiraConfig);
        searchInstance.getEvalContext().setPriceListCache(priceListCache);
        searchInstance.getEvalContext().setQuoteLink(quoteLink);
        searchInstance.getEvalContext().setSapQuoteLink(sapQuoteLink);
    }

    /**
     * Save a search into a preference.  An update needs to extract values from selectedSearchName pick list.
     *
     * @param newSearch true if inserting a new search, false if updating an existing
     *                  search
     */
    private void persistSearch(boolean newSearch) {
        String searchName = null;
        PreferenceType preferenceType = null;
        if( newSearch ) {
            searchName = newSearchName;
            preferenceType = PreferenceType.valueOf(newSearchLevel);
        } else {
            String[] searchValues = selectedSearchName.split("\\|");
            preferenceType = PreferenceType.valueOf(searchValues[1]);
            searchName = searchValues[2];
        }

        getPreferences();
        MessageCollection messageCollection = new MessageCollection();
        searchInstanceEjb.persistSearch(newSearch, searchInstance, messageCollection,
                preferenceType, searchName, preferenceMap);
        addMessages(messageCollection);
        getPreferences();
        searchInstance.establishRelationships(configurableSearchDef);

        // Tried to do the UI a favor and select the new search but fails to do so
        /*
        if( newSearch ) {
            setSelectedSearchName( preferenceType.getPreferenceScope().toString() + "|" + preferenceType.toString() + "|" + searchName );
        }
        */
    }

    /**
     * Called when the user clicks delete search, deletes the currently loaded search
     *
     * @return JSP to display confirmation
     */
    public Resolution deleteSearch() {

        if( selectedSearchName != null ) {
            String[] searchValues = selectedSearchName.split("\\|");
            PreferenceType preferenceType = PreferenceType.valueOf(searchValues[1]);
            String searchName = searchValues[2];
            getPreferences();
            MessageCollection messageCollection = new MessageCollection();
            searchInstanceEjb.deleteSearch( messageCollection, preferenceType, searchName, preferenceMap);
            addMessages(messageCollection);
        }
        return queryPage();
    }

    public List<ConfigurableListFactory.ColumnSet> getViewColumnSets() {
        // TODO handle other entities
        return configurableListFactory.getColumnSubsets(ConfigurableList.ColumnSetType.VIEW,
                PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS);
    }

    public List<ConfigurableListFactory.ColumnSet> getDownloadColumnSets() {
        // TODO handle other entities
        return configurableListFactory.getColumnSubsets(ConfigurableList.ColumnSetType.DOWNLOAD,
                PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS);
    }

    public Map<String, List<ColumnTabulation>> getAvailableMapGroupToColumnNames() {
        return configurableSearchDef.getMapGroupToColumnTabulations();
    }

    public Map<Integer,String> getAvailableMapGroupToHelpText() {
        return configurableSearchDef.getMapGroupHelpText();
    }

    public ConfigurableSearchDefinition getConfigurableSearchDef() {
        return configurableSearchDef;
    }

    public SearchInstance getSearchInstance() {
        return searchInstance;
    }

    public void setSearchInstance(SearchInstance searchInstance) {
        this.searchInstance = searchInstance;
    }

    public void setSearchTermName(String searchTermName) {
        this.searchTermName = searchTermName;
    }

    public List<SearchInstance.SearchValue> getSearchValueList() {
        return searchValueList;
    }

    public String getColumnSetName() {
        return columnSetName;
    }

    public void setColumnSetName(String columnSetName) {
        this.columnSetName = columnSetName;
    }

    public int getSortColumnIndex() {
        return sortColumnIndex;
    }

    public void setSortColumnIndex(int sortColumnIndex) {
        this.sortColumnIndex = sortColumnIndex;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public ConfigurableList.ResultList getConfigurableSampleList() {
        return configurableResultList;
    }

    public void setConfigurableSampleList(ConfigurableList.ResultList configurableResultList) {
        this.configurableResultList = configurableResultList;
    }

    public String getNewSearchName() {
        return newSearchName;
    }

    public void setNewSearchName(String newSearchName) {
        this.newSearchName = newSearchName;
    }

    public String getSelectedSearchName() {
        return selectedSearchName;
    }

    public void setSelectedSearchName(String selectedSearchName) {
        this.selectedSearchName = selectedSearchName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Map<String,String> getSearchInstanceNames() {
        return searchInstanceNames;
    }

    public Map<ColumnEntity, Map<PreferenceType,List<String>>> getAllSearchInstances(){
        return allSearchInstances;
    }

    public String getNewSearchLevel() {
        return newSearchLevel;
    }

    public void setNewSearchLevel(String newSearchLevel) {
        this.newSearchLevel = newSearchLevel;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getDbSortPath() {
        return dbSortPath;
    }

    public void setDbSortPath(String dbSortPath) {
        this.dbSortPath = dbSortPath;
    }

    public boolean isMinimal() {
        return minimal;
    }

    public void setMinimal(boolean minimal) {
        this.minimal = minimal;
    }

    public String getEntityName() {
        return ( entityType == null ? null : entityType.getEntityName() );
    }

    public void setEntityName( String entityName ) {
        entityType = null;
        for( ColumnEntity type : entityType.values() ) {
            if( type.getEntityName().equals(entityName) ) entityType = type;
        }
    }

    public ColumnEntity getEntityType(){
        return entityType;
    }

    public Map<String,String> getNewSearchLevels() {
        return newSearchLevels;
    }

    public void setSearchTermFirstValue(String searchTermFirstValue) {
        this.searchTermFirstValue = searchTermFirstValue;
    }

    public ColumnEntity[] getAvailableEntityTypes(){
        return ColumnEntity.values();
    }

    /**
     * Flags as an ajax call to simply append the results of a scan to the output element
     * @return true use-case is only ajax
     */
    public boolean isAppendScanResults(){
        return true;
    }

    /**
     * Unmarshall search drill down descriptor
     * @param drillDownRequestString JSON data from link URL
     */
    public void setDrillDownRequest(String drillDownRequestString) {
        try {
            this.drillDownRequest = new ObjectMapper().readValue(drillDownRequestString, SearchRequestBean.class);
        } catch (Exception ex ) {
            throw new RuntimeException("Failure to parse drill down request", ex );
        }
    }

}
