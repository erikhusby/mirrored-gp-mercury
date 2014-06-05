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
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDao;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationDao;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.hibernate.Criteria;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
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

    /**
     * Separator between preference level name and search instance name
     */
    private static final String PREFERENCE_SEPARATOR = " - ";

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
     * Map from preference level name to a method to get the preference
     */
    private static Map<String, PreferenceAccess> preferenceAccessMap = new LinkedHashMap<>();

    /**
     * Map from preference level name to fetched preference
     */
    private Map<String, Preference> preferenceMap;

    /**
     * The number of the page to fetch in the results set
     */
    private int pageNumber;

    /**
     * Which entity we're searching on, e.g. Sample
     */
    private String entityName;

    /*
     * Dependencies:
     */
    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

    @Inject
    private PaginationDao paginationDao;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

//    private WorkRequestItem<?> workRequestItem;

    public List<String> getNewSearchLevels() {
        return newSearchLevels;
    }

    public void setSearchTermFirstValue(String searchTermFirstValue) {
        this.searchTermFirstValue = searchTermFirstValue;
    }

    /**
     * Method to retrieve preference from the database, has a different implementation for
     * each level
     */
    interface PreferenceAccess {
        Preference getPreference(CoreActionBeanContext bspActionBeanContext, PreferenceDao preferenceDao);

        Preference createNewPreference(CoreActionBeanContext bspActionBeanContext);

        boolean canModifyPreference(CoreActionBeanContext bspActionBeanContext);
    }

    static {
        // TODO jmt do we need search instances for entities other than Sample?
        preferenceAccessMap.put("GLOBAL", new PreferenceAccess() {
            @Override
            public Preference getPreference(CoreActionBeanContext bspActionBeanContext, PreferenceDao preferenceDao) {
                return preferenceDao.getGlobalPreference(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES);
            }

            @Override
            public Preference createNewPreference(CoreActionBeanContext bspActionBeanContext) {
                return new Preference(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES, "");
            }

            @Override
            public boolean canModifyPreference(CoreActionBeanContext bspActionBeanContext) {
                return true; // bspActionBeanContext.isAdmin();
            }
        });
/*
        preferenceAccessMap.put("GROUP", new PreferenceAccess() {
            @Override
            public Preference getPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                return bspActionBeanContext.getGroup() == null ? null : (new PreferenceManager()).getGroupPreference(
                        bspActionBeanContext.getGroup().getGroupId(),
                        PreferenceType.Type.GROUP_SEARCH_INSTANCES);
            }

            @Override
            public Preference createNewPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                Preference preference = new Preference();
                PreferenceType preferenceType = new PreferenceType();
                preferenceType.setPreferenceTypeId(PreferenceType.Type.GROUP_SEARCH_INSTANCES.getId());
                preference.setPreferenceType(preferenceType);
                preference.setGroupId(bspActionBeanContext.getGroup().getGroupId());
                return preference;
            }

            @Override
            public boolean canModifyPreference(BspActionBeanContext bspActionBeanContext) {
                return bspActionBeanContext.isAdmin();
            }
        });
        preferenceAccessMap.put("COLLECTION", new PreferenceAccess() {
            @Override
            public Preference getPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                return bspActionBeanContext.getSampleCollection() == null ? null : (new PreferenceManager())
                        .getProjectPreference(bspActionBeanContext
                                .getSampleCollection().getCollectionId(), PreferenceType.Type.PROJECT_SEARCH_INSTANCES);
            }

            @Override
            public Preference createNewPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                Preference preference = new Preference();
                PreferenceType preferenceType = new PreferenceType();
                preferenceType.setPreferenceTypeId(PreferenceType.Type.PROJECT_SEARCH_INSTANCES.getId());
                preference.setPreferenceType(preferenceType);
                preference.setProjectId(bspActionBeanContext.getSampleCollection().getCollectionId());
                return preference;
            }

            @Override
            public boolean canModifyPreference(BspActionBeanContext bspActionBeanContext) {
                return bspActionBeanContext.isAdmin();
            }
        });
        preferenceAccessMap.put("USER", new PreferenceAccess() {
            @Override
            public Preference getPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                return (new PreferenceManager()).getUserPreference(bspActionBeanContext.getUserProfile(),
                        PreferenceType.Type.USER_SEARCH_INSTANCES);
            }

            @Override
            public Preference createNewPreference(BspActionBeanContext bspActionBeanContext) throws MPGException {
                Preference preference = new Preference();
                PreferenceType preferenceType = new PreferenceType();
                preferenceType.setPreferenceTypeId(PreferenceType.Type.USER_SEARCH_INSTANCES.getId());
                preference.setPreferenceType(preferenceType);
                preference.setUserId(bspActionBeanContext.getUserProfile().getId());
                return preference;
            }

            @Override
            public boolean canModifyPreference(BspActionBeanContext bspActionBeanContext) {
                return true;
            }
        });
*/
    }

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

            for (Map.Entry<String, PreferenceAccess> stringPreferenceAccessEntry : preferenceAccessMap.entrySet()) {
                PreferenceAccess preferenceAccess = stringPreferenceAccessEntry.getValue();
                Preference preference = preferenceAccess.getPreference(getContext(), preferenceDao);
                if (preference != null) {
                    SearchInstanceList searchInstanceList =
                            (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                    for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                        searchInstanceNames.add(stringPreferenceAccessEntry.getKey() + PREFERENCE_SEPARATOR + instance.getName());
                    }
                }
                preferenceMap.put(stringPreferenceAccessEntry.getKey(), preference);
                if (preferenceAccess.canModifyPreference(getContext())) {
                    newSearchLevels.add(stringPreferenceAccessEntry.getKey());
                }
            }
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
        for (Map.Entry<String, Preference> stringPreferenceEntry : preferenceMap.entrySet()) {
            if (selectedSearchName.startsWith(stringPreferenceEntry.getKey())) {
                searchName = selectedSearchName.substring(stringPreferenceEntry.getKey().length() + PREFERENCE_SEPARATOR.length());
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

    // TODO Clear instance

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
                configurableResultList = getFirstResultsPage(searchInstance, configurableSearchDef, columnSetName,
                        getContext(), sortColumnIndex, dbSortPath, sortDirection, sessionKey, entityName);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage(), e);
                addGlobalValidationError(e.getMessage());
            }
        }
        return new ForwardResolution("/search/configurable_search.jsp");
    }


    // todo jmt move out of ActionBean
    /**
     * Gets the first page of results, shared with "Search Classic" action bean
     *
     * @param searchInstance        search terms and values
     * @param configurableSearchDef definitions of search terms
     * @param columnSetName         which column set the user wants results displayed in
     * @param context               for authorization and errors
     * @param sortColumnIndex       which column to in-memory sort on (single page results)
     * @param dbSortPath            which Hibernate property to database sort on (multi-page results)
     * @param sortDirection         ASC or DSC
     * @param sessionKey            suffix for key for pagination object in session
     * @param entityName            the name of the entity we're searching, e.g. Sample
     * @return list of results
     */
    public ConfigurableList.ResultList getFirstResultsPage(
            SearchInstance searchInstance,
            ConfigurableSearchDefinition configurableSearchDef,
            String columnSetName,
            CoreActionBeanContext context,
            Integer sortColumnIndex, String dbSortPath,
            String sortDirection, String sessionKey,
            String entityName) throws IllegalArgumentException {
        initEvalContext(searchInstance, context);

        Criteria criteria = configurableSearchDao.buildCriteria(configurableSearchDef, searchInstance, dbSortPath,
                sortDirection);

        // TODO move join-fetch stuff into ListConfig and SearchInstance
        // If the user chose columns to view, use those, else use a pre-defined column set
        List<String> columnNameList;
        if (searchInstance.getPredefinedViewColumns() != null && !searchInstance.getPredefinedViewColumns().isEmpty()) {
            columnNameList = searchInstance.getPredefinedViewColumns();
        } else {
            // Determine which set of columns to use
            PreferenceType.PreferenceScope columnSetDomain = null;
            for (PreferenceType.PreferenceScope domain : PreferenceType.PreferenceScope.values()) {
                if (columnSetName.startsWith(domain.toString())) {
                    columnSetDomain = domain;
                    break;
                }
            }
            if (columnSetDomain == null) {
                context.getValidationErrors().addGlobalError(
                        new SimpleError("Failed  to extract domain preference from " + columnSetName));
                return null;
            }
            // Generate the subset of columns the user is interested in
            String columnSetNameSuffix = columnSetName.substring(columnSetName.indexOf('|') + 1);
            ColumnSetsPreference columnSets = configurableListFactory.getColumnSets(
                    columnSetDomain, /*context.getUserProfile(), context.getGroup(), context.getSampleCollection(),*/
                    entityName);
            ColumnSetsPreference.ColumnSet columnSet = ConfigurableList.getColumnNameList(columnSetNameSuffix,
                    ConfigurableList.ColumnSetType.VIEW, /*context.getUserProfile(), context.getGroup(), */columnSets);
            columnNameList = columnSet.getColumnDefinitions();
            searchInstance.setColumnSetColumnNameList(columnNameList);
        }

        // To improve performance, add join fetches for search values that have the
        // "display" checkbox set
        List<SearchInstance.SearchValue> displaySearchValues = searchInstance.findDisplaySearchValues();
        List<String> joinFetchPaths = new ArrayList<>();
        for (SearchInstance.SearchValue displaySearchValue : displaySearchValues) {
            joinFetchPaths.addAll(displaySearchValue.getJoinFetchPaths());
        }
        // Add join fetches for the column set
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnNameList) {
            SearchTerm searchTerm = configurableSearchDef.getSearchTerm(columnName);
            columnTabulations.add(searchTerm);
            if (searchTerm.getJoinFetchPaths() != null) {
                joinFetchPaths.addAll(searchTerm.getJoinFetchPaths());
            }
        }
        columnTabulations.addAll(searchInstance.findTopLevelColumnTabulations());

        PaginationDao.Pagination pagination = new PaginationDao.Pagination(configurableSearchDef.getPageSize());
        configurableSearchDao.startPagination(pagination, criteria);
        pagination.setJoinFetchPaths(joinFetchPaths);
        List<?> entityList = paginationDao.getPage(pagination, 0);

        // Format the results into columns
        ConfigurableList configurableList = new ConfigurableList(columnTabulations,
                pagination.getNumberPages() == 1 ? sortColumnIndex : null, sortDirection/*, context.isAdmin(),
                listConfig.getId()*/, ColumnEntity.getByName(entityName));
        configurableList.addListener(new BspSampleSearchAddRowsListener(bspSampleSearchService));
        configurableList.addRows(entityList);
        ConfigurableList.ResultList resultList = configurableList.getResultList();

        context.getRequest().getSession().setAttribute(PAGINATION_PREFIX + sessionKey, pagination);
        return resultList;
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
        configurableResultList = getSubsequentResultsPage(searchInstance, getContext(), pageNumber, sessionKey,
                entityName);
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    // todo jmt move out of ActionBean
    /**
     * Gets subsequent page of search results, shared with "Search Classic" action bean
     *
     * @param searchInstance search terms and values
     * @param context        authorization and errors
     * @param pageNumber     which page of results to return
     * @param sessionKey     HTTP session key suffix, for pagination object
     * @param entityName     the name of the entity we're searching, e.g. Sample
     * @return list of results
     */
    public ConfigurableList.ResultList getSubsequentResultsPage(
            SearchInstance searchInstance,
            CoreActionBeanContext context,
            int pageNumber, String sessionKey,
            String entityName) {
        // Get requested page of results
        PaginationDao.Pagination pagination = (PaginationDao.Pagination) context.getRequest().getSession()
                .getAttribute(PAGINATION_PREFIX + sessionKey);
        List<?> entityList = paginationDao.getPage(pagination, pageNumber);

        // Format the results into columns
        ConfigurableSearchDefinition configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        List<SearchInstance.SearchValue> displaySearchValues = searchInstance.findDisplaySearchValues();
        List<String> columnNameList;
        if (searchInstance.getPredefinedViewColumns() != null && !searchInstance.getPredefinedViewColumns().isEmpty()) {
            columnNameList = searchInstance.getPredefinedViewColumns();
        } else {
            columnNameList = searchInstance.getColumnSetColumnNameList();
        }
        for (String columnName : columnNameList) {
            columnTabulations.add(configurableSearchDef.getSearchTerm(columnName));
        }
        for (SearchInstance.SearchValue displaySearchValue : displaySearchValues) {
            columnTabulations.add(displaySearchValue);
        }
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, null,
                ColumnEntity.getByName(entityName));
        configurableList.addListener(new BspSampleSearchAddRowsListener(bspSampleSearchService));
        configurableList.addRows(entityList);

        return configurableList.getResultList();
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
        try {
            boolean save = true;
            // For new searches, check the user's authorization
            if (newSearch) {
                if (!preferenceAccessMap.get(newSearchLevel).canModifyPreference(getContext())) {
                    addGlobalValidationError("You are not authorized to save searches at level {2}", newSearchLevel);
                    save = false;
                }
            }
            String existingSearchLevel = null;
            String searchName = null;
            Preference preference = null;
            if (save) {
                // Find the existing preference (if any) at the chosen level (each
                // preference holds many searches)
                for (String level : preferenceMap.keySet()) {
                    if (newSearch) {
                        if (newSearchLevel.equals(level)) {
                            preference = preferenceMap.get(level);
                            break;
                        }
                    } else {
                        if (selectedSearchName.startsWith(level)) {
                            existingSearchLevel = level;
                            // For existing searches, check the user's authorization
                            if (preferenceAccessMap.get(selectedSearchName.substring(0, level.length()))
                                    .canModifyPreference(getContext())) {
                                searchName = selectedSearchName.substring(level.length()
                                                                          + PREFERENCE_SEPARATOR.length());
                                preference = preferenceMap.get(level);
                            } else {
                                addGlobalValidationError("You are not authorized to update searches at level {2}",
                                        level);
                                save = false;
                            }
                            break;
                        }
                    }
                }
            }
            if (save) {
                SearchInstanceList searchInstanceList;
                if (preference == null) {
                    // Didn't find an existing preference, so create a new one
                    preference = preferenceAccessMap.get(newSearch ? newSearchLevel : existingSearchLevel)
                            .createNewPreference(getContext());
                    searchInstanceList = new SearchInstanceList();
                } else {
                    searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                }
                if (newSearch) {
                    // Check for uniqueness
                    for (SearchInstance searchInstanceLocal : searchInstanceList.getSearchInstances()) {
                        if (searchInstanceLocal.getName().equals(newSearchName)) {
                            getContext().getMessages().add(
                                    new SimpleMessage("There is already a search called " + newSearchName + " in the "
                                                      + newSearchLevel + " level"));
                            save = false;
                            break;
                        }
                    }
                    searchInstance.setName(newSearchName);
                    searchInstanceList.getSearchInstances().add(searchInstance);
                } else {
                    // Find the search we're updating
                    boolean found = false;
                    for (int i = 0; i < searchInstanceList.getSearchInstances().size(); i++) {
                        if (searchInstanceList.getSearchInstances().get(i).getName().equals(searchName)) {
                            searchInstance.setName(searchName);
                            searchInstanceList.getSearchInstances().set(i, searchInstance);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // We didn't find it, so add it. Arguably this should be an error.
                        searchInstance.setName(searchName);
                        searchInstanceList.getSearchInstances().add(searchInstance);
                    }
                }
                if (save) {
                    preference.markModified(searchInstanceList.marshal());
                    // Changing the preference definition doesn't seem to make the
                    // Hibernate
                    // object "dirty", so change something else too
                    preference.setModifiedDate(new Date());
                    preferenceDao.persist(preference);
                    getContext().getMessages().add(new SimpleMessage("The search was saved"));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            addGlobalValidationError("Failed to save the search");
        }
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
        String searchName = null;
        Preference preference = null;
        for (Map.Entry<String, Preference> stringPreferenceEntry : preferenceMap.entrySet()) {
            if (selectedSearchName.startsWith(stringPreferenceEntry.getKey())) {
                if (preferenceAccessMap.get(selectedSearchName.substring(0, stringPreferenceEntry.getKey().length())).canModifyPreference(
                        getContext())) {
                    searchName = selectedSearchName.substring(stringPreferenceEntry.getKey().length() + PREFERENCE_SEPARATOR.length());
                    preference = stringPreferenceEntry.getValue();
                } else {
                    addGlobalValidationError("You are not authorized to delete this search");
                }
                break;
            }
        }
        if (preference != null) {
            SearchInstanceList searchInstanceList;
            try {
                searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                if (instance.getName().equals(searchName)) {
                    searchInstanceList.getSearchInstances().remove(instance);
                    break;
                }
            }
            preference.setModifiedDate(new Date());
            try {
                preferenceDao.persist(preference);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                addGlobalValidationError("Failed to delete the search");
            }
            getContext().getMessages().add(new SimpleMessage("The search was deleted"));
            return queryPage();
        }
        initEvalContext(searchInstance, getContext());
        searchInstance.establishRelationships(configurableSearchDef);
        getPreferences();
        return new ForwardResolution("/search/configurable_search.jsp");
    }

    public List<ConfigurableListFactory.ColumnSet> getViewColumnSets() {
        // TODO handle other entities
        return configurableListFactory.getColumnSubsets(ConfigurableList.ColumnSetType.VIEW, /*getContext()
                .getUserProfile(), getContext().getGroup(), getContext().getSampleCollection(),*/
                PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS/*, PreferenceType.Type.GROUP_SAMPLE_LIST_COLUMN_SETS,
                PreferenceType.Type.PROJECT_SAMPLE_LIST_COLUMN_SETS, PreferenceType.Type.USER_SAMPLE_LIST_COLUMN_SETS*/);
    }

    public List<ConfigurableListFactory.ColumnSet> getDownloadColumnSets() {
        // TODO handle other entities
        return configurableListFactory.getColumnSubsets(ConfigurableList.ColumnSetType.DOWNLOAD, /*getContext()
                .getUserProfile(), getContext().getGroup(), getContext().getSampleCollection(),*/
                PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS/*, PreferenceType.Type.GROUP_SAMPLE_LIST_COLUMN_SETS,
                PreferenceType.Type.PROJECT_SAMPLE_LIST_COLUMN_SETS, PreferenceType.Type.USER_SAMPLE_LIST_COLUMN_SETS*/);
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
}
