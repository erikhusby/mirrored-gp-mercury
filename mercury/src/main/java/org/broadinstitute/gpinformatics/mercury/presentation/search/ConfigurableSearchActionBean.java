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

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDao;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationDao;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * The definition from which the user will create the search
     */
    private ConfigurableSearchDefinition configurableSearchDef;

    /**
     * List of all search instances, prefixed with level
     */
    private List<String> searchInstanceNames;

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
     * The levels in which the user can save new searches
     */
    private List<String> newSearchLevels = new ArrayList<>();

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
     * For single page results, which result set column to in-memory sort by, default to
     * first
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
     * Map from preference level name to fetched preference
     */
    private Map<PreferenceType.PreferenceScope, Preference> preferenceMap;

    /**
     * The number of the page to fetch in the results set
     */
    private int pageNumber;

    /**
     * Which entity we're searching on, e.g. Sample
     */
    private String entityName;

    // Dependencies
    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private PreferenceEjb preferenceEjb;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

    @Inject
    private PaginationDao paginationDao;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    /**
     * Called when the user first visits the page, this method fetches preferences.
     *
     * @return JSP to edit search
     */
    @HandlesEvent("queryPage")
    @DefaultHandler
    public Resolution queryPage() {
        getPreferences();
        searchInstance = new SearchInstance();
        initEvalContext(searchInstance, getContext());
        searchInstance.addRequired(configurableSearchDef);
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    private void getPreferences() {
        preferenceMap = new HashMap<>();
        searchInstanceNames = new ArrayList<>();
        newSearchLevels = new ArrayList<>();
        try {
            configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);

            searchInstanceEjb.fetchInstances(preferenceMap,  searchInstanceNames, newSearchLevels);
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
        String searchName = null;
        SearchInstanceList searchInstanceList = null;
        for (Map.Entry<PreferenceType.PreferenceScope, Preference> stringPreferenceEntry : preferenceMap.entrySet()) {
            if (selectedSearchName.startsWith(stringPreferenceEntry.getKey().toString())) {
                searchName = selectedSearchName.substring(stringPreferenceEntry.getKey().toString().length() +
                        SearchInstanceEjb.PREFERENCE_SEPARATOR.length());
                try {
                    searchInstanceList =
                            (SearchInstanceList) stringPreferenceEntry.getValue().getPreferenceDefinition().getDefinitionValue();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        assert searchInstanceList != null;

        for (SearchInstance searchInstanceLocal : searchInstanceList.getSearchInstances()) {
            if (searchInstanceLocal.getName().equals(searchName)) {
                searchInstance = searchInstanceLocal;
                break;
            }
        }
        initEvalContext(searchInstance, getContext());
        searchInstance.establishRelationships(configurableSearchDef);
        searchInstance.postLoad();
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    /**
     * Handles AJAX request to add a top level search term
     *
     * @return JSP fragment to render term
     */
    public Resolution addTopLevelTerm() {
        configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);
        searchInstance = new SearchInstance();
        initEvalContext(searchInstance, getContext());
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
        configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);
        searchInstance = new SearchInstance();
        initEvalContext(searchInstance, getContext());
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
        configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);
        initEvalContext(searchInstance, getContext());
        searchInstance.establishRelationships(configurableSearchDef);
        searchValueList = recurseToLeaf(searchInstance.getSearchValues());
        return new ForwardResolution("/search/search_term_fragment.jsp");
    }

    /**
     * Initialize the evaluation context with objects associated with the current user
     *
     * @param searchInstance       where to set the evaluation context
     * @param bspActionBeanContext web application context
     */
    private static void initEvalContext(SearchInstance searchInstance, CoreActionBeanContext bspActionBeanContext) {
        Map<String, Object> evalContext = new HashMap<>();
//        evalContext.put("contextGroup", bspActionBeanContext.getGroup());
//        evalContext.put("contextSampleCollection", bspActionBeanContext.getSampleCollection());
//        evalContext.put("contextBspDomainUser", bspActionBeanContext.getUserProfile());
        searchInstance.setEvalContext(evalContext);
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
        if (searchInstance == null || searchInstance.getSearchValues() == null
            || searchInstance.getSearchValues().isEmpty()) {
            addGlobalValidationError("You must add at least one search term");
        } else {
            searchInstance.establishRelationships(configurableSearchDef);

            try {
                ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDef, columnSetName, sortColumnIndex, dbSortPath,
                        sortDirection, entityName);
                configurableResultList = firstPageResults.getResultList();
                getContext().getRequest().getSession().setAttribute(PAGINATION_PREFIX + sessionKey,
                        firstPageResults.getPagination());
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage(), e);
                addGlobalValidationError(e.getMessage());
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
        configurableResultList = configurableListFactory.getSubsequentResultsPage(searchInstance, pageNumber, entityName,
                (PaginationDao.Pagination) getContext().getRequest().getSession().getAttribute(
                        PAGINATION_PREFIX + sessionKey));
        return new ForwardResolution("/search/configurable_search.jsp");
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
     * Save a search into a preference.
     *
     * @param newSearch true if inserting a new search, false if updating an existing
     *                  search
     */
    private void persistSearch(boolean newSearch) {
        getPreferences();
        MessageCollection messageCollection = new MessageCollection();
        searchInstanceEjb.persistSearch(newSearch, searchInstance, messageCollection,
                PreferenceType.PreferenceScope.valueOf(newSearchLevel), newSearchName, selectedSearchName, preferenceMap);
        addMessages(messageCollection);
        getPreferences();
        initEvalContext(searchInstance, getContext());
        searchInstance.establishRelationships(configurableSearchDef);
    }

    /**
     * Called when the user clicks delete search, deletes the currently loaded search
     *
     * @return JSP to display confirmation
     */
    public Resolution deleteSearch() {
        getPreferences();
        MessageCollection messageCollection = new MessageCollection();
        searchInstanceEjb.deleteSearch(messageCollection, selectedSearchName, preferenceMap);
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

    public List<String> getSearchInstanceNames() {
        return searchInstanceNames;
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
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<String> getNewSearchLevels() {
        return newSearchLevels;
    }

    public void setSearchTermFirstValue(String searchTermFirstValue) {
        this.searchTermFirstValue = searchTermFirstValue;
    }

}
