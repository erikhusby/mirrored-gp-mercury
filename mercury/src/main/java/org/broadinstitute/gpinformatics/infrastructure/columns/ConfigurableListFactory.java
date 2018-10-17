package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.ThreadEntityManager;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDao;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.ConstrainedValueDao;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hibernate.Criteria;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates ConfigurableList instances.
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ConfigurableListFactory {

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ThreadEntityManager threadEntityManager;

    @Inject
    private UserBean userBean;

    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private ConstrainedValueDao constrainedValueDao;

    /**
     * Create a ConfigurableList instance.
     * TODO jms 07/2015 not used, replace with ConfigurableList constructor as in other ConfigurableListActionBean methods
     *
     * @param entityList  Entities for which to display data
     * @param downloadColumnSetName Name of the column set to display
     * @param columnEntity ID of the entity
     * @param configurableSearchDefinition for add rows listeners
     *
     * @return ConfigurableList instance
     */
    public ConfigurableList create(@Nonnull List<?> entityList,
            @Nonnull String downloadColumnSetName,
            @Nonnull ColumnEntity columnEntity,
            @Nonnull SearchContext evalContext,
            @Nullable ConfigurableSearchDefinition configurableSearchDefinition) {

        List<ColumnTabulation> columnTabulations = buildColumnSetTabulations(downloadColumnSetName, columnEntity);
        ConfigurableList configurableList;
        if( evalContext.getSearchInstance() == null ) {
            configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP,0, "ASC", columnEntity);
        } else {
            configurableList = new ConfigurableList(columnTabulations, evalContext.getSearchInstance().getViewColumnParamMap(),0, "ASC", columnEntity);
        }
        if (configurableSearchDefinition != null) {
            configurableList.addAddRowsListeners(configurableSearchDefinition);
        }
        configurableList.addRows(entityList, evalContext);
        return configurableList;
    }

    /**
     * Build Column definitions for a column set name
     *
     * @param downloadColumnSetName name of column set that the user wants to download
     * @param columnEntity            e.g. "Sample"
     * @return list of column definitions
     */
    public List<ColumnTabulation> buildColumnSetTabulations(@Nonnull String downloadColumnSetName,
            @Nonnull ColumnEntity columnEntity) {
        // Determine which set of columns to use
/*
        PreferenceDomain.domain columnSetDomain = null;
        for (PreferenceDomain.domain domain : PreferenceDomain.domain.values()) {
            if (downloadColumnSetName.startsWith(domain.toString())) {
                columnSetDomain = domain;
                break;
            }
        }
        if (columnSetDomain == null) {
            throw new RuntimeException("Failed to extract domain preference from " + downloadColumnSetName);
        }
*/
        String columnSetNameSuffix = downloadColumnSetName.substring(downloadColumnSetName.indexOf('|') + 1);

//        EntityPreferenceFactory entityPreferenceFactory = new EntityPreferenceFactory();
//        PreferenceDefinitionListNameListString columnSets = entityPreferenceFactory.getColumnSets(columnSetDomain,
//                domainUser, sampleCollection.getGroup(), sampleCollection, entityName);
        Preference globalPreference = preferenceDao.getGlobalPreference(columnEntity.getGlobalColumnSets());
        ColumnSetsPreference columnSetsPreference;
        try {
            columnSetsPreference =
                    (ColumnSetsPreference) globalPreference.getPreferenceDefinition().getDefinitionValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // How to store column defs?  If enum, might need multiple for different result entities.  Need enum for
        // columns that aren't in use yet, otherwise would have to do discovery?
        ColumnSetsPreference.ColumnSet columnSet = ConfigurableList.getColumnNameList(columnSetNameSuffix,
                ConfigurableList.ColumnSetType.DOWNLOAD, columnSetsPreference);
//        ListConfig listConfig = entityPreferenceFactory.loadListConfig(entityName);
        ConfigurableSearchDefinition configurableSearchDefinition = SearchDefinitionFactory.getForEntity(
                columnEntity.getEntityName());
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnSet.getColumnDefinitions()) {
            SearchTerm searchTerm = configurableSearchDefinition.getSearchTerm(columnName);
            if (searchTerm == null) {
                throw new RuntimeException("searchTerm not found " + columnName);
            }
            columnTabulations.add(searchTerm);
        }

        return columnTabulations;
    }

    public ColumnSetsPreference getColumnSets(PreferenceType.PreferenceScope columnSetDomain, String entityName) {
        try {
            // todo jmt other scopes and entities
            return (ColumnSetsPreference) preferenceDao.getGlobalPreference(
                    PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS).getPreferenceDefinition().getDefinitionValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // todo jmt merge with ColumnSetsPreference?
    public static class ColumnSet {
        private final PreferenceType.PreferenceScope level;

        private final String name;

        private final List<String> columns;

        ColumnSet(PreferenceType.PreferenceScope level, String name, List<String> columns) {
            this.level = level;
            this.name = name;
            this.columns = columns;
        }

        public PreferenceType.PreferenceScope getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }

        public List<String> getColumns() {
            return columns;
        }
    }

    /**
     * Gets a list of all column sets that apply to the current user, project and group,
     * including the global sets.
     *
     * @param columnSetType     type of column set
     * @param globalColumnSets  type of columns sets for global scope
     * @return list of all applicable column sets
     */
    public List<ColumnSet> getColumnSubsets(ConfigurableList.ColumnSetType columnSetType,
            PreferenceType globalColumnSets) {

        List<ColumnSet> columnSets = new ArrayList<>();
        Preference preference = preferenceDao.getGlobalPreference(globalColumnSets);
/*
        evalVisibilityExpression(columnSetType, columnSets, preference, PreferenceDomain.domain.GLOBAL,
                bspDomainUser, group);
*/

/*
            // A user can have no group selected for their initial login.
            if (group != null && groupColumnSets != null) {
                Long groupId = group.getGroupId();
                preference = preferenceManager.getGroupPreference(groupId, groupColumnSets);
                evalVisibilityExpression(columnSetType, columnSets, preference, PreferenceDomain.domain.GROUP,
                        bspDomainUser, group);
            }

            // BSP-2240 A user can have no collection selected.
            if (sampleCollection != null && projectColumnSets != null) {
                Long projectId = sampleCollection.getCollectionId();
                preference = preferenceManager.getProjectPreference(projectId, projectColumnSets);
                evalVisibilityExpression(columnSetType, columnSets, preference, PreferenceDomain.domain.PROJECT,
                        bspDomainUser, group);
            }
*/

/*
        if (userColumnSets != null) {
            preference = preferenceManager.getUserPreference(bspDomainUser, userColumnSets);
            evalVisibilityExpression(columnSetType, columnSets, preference, PreferenceDomain.domain.USER,
                    bspDomainUser, group);
        }
*/
        return columnSets;
    }

    /**
     * Pick the column sets that are visible to the user.
     *
     * @param columnSetType     view or download
     * @param visibleColumnSets this method adds visible column sets to this list
     * @param preference        all column sets
     * @param domain            preference level
     * @param bspDomainUser     used to evaluate visibility expression
     * @param group             used to evaluate visibility expression
     */
/*
    private static void evalVisibilityExpression(ColumnSetType columnSetType, List<ColumnSet> visibleColumnSets,
            Preference preference, PreferenceDomain.domain domain,
            BspDomainUser bspDomainUser, Group group) {
        PreferenceDefinitionListNameListString listNameListString;
        if (preference != null) {
            listNameListString = (PreferenceDefinitionListNameListString) preference.getPreferenceDefinition();
            if (listNameListString != null) {
                Map<String, Object> context = new HashMap<>();
                context.put(SearchDefinitionFactory.CONTEXT_KEY_COLUMN_SET_TYPE, columnSetType);
                context.put("bspDomainUser", bspDomainUser);
                context.put("group", group);
                for (PreferenceDefinitionNameListString nameListString : listNameListString.getListNamedList()) {
                    List<String> columns = nameListString.getList();
                    Boolean useSet = false;
                    try {
                        useSet = (Boolean) MVEL.eval(columns.get(0), context);
                    } catch (Exception e) {
                        log.error(String.format("Evaluating expression %s : %s", columns.get(0), e));
                    }
                    if (useSet) {
                        // remove visibility expression.
                        visibleColumnSets.add(new ColumnSet(domain, nameListString.getName(), columns.subList(1,
                                columns.size())));
                    }
                }
            }
        }
    }
*/

    /**
     * Allows multiple returns from method.
     */
    public static class FirstPageResults {
        private ConfigurableList.ResultList resultList;
        private PaginationUtil.Pagination pagination;

        public FirstPageResults(ConfigurableList.ResultList resultList,
                PaginationUtil.Pagination pagination) {
            this.resultList = resultList;
            this.pagination = pagination;
        }

        public ConfigurableList.ResultList getResultList() {
            return resultList;
        }

        public PaginationUtil.Pagination getPagination() {
            return pagination;
        }
    }

    /**
     * Gets the first page of results.
     *
     * @param searchInstance        search terms and values
     * @param configurableSearchDef definitions of search terms
     * @param columnSetName         which column set the user wants results displayed in
     * @param sortColumnIndex       which column to in-memory sort on (single page results)
     * @param dbSortPath            which Hibernate property to database sort on (multi-page results)
     * @param sortDirection         ASC or DSC
     * @param entityName            the name of the entity we're searching, e.g. Sample
     * @return list of results
     */
    public FirstPageResults getFirstResultsPage(
            SearchInstance searchInstance,
            ConfigurableSearchDefinition configurableSearchDef,
            String columnSetName,
            Integer sortColumnIndex,
            String dbSortPath,
            String sortDirection,
            String entityName) {

        PaginationUtil.Pagination pagination = getPagination(searchInstance, configurableSearchDef, dbSortPath,
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
                throw new RuntimeException("Failed  to extract domain preference from " + columnSetName);
            }
            // Generate the subset of columns the user is interested in
            String columnSetNameSuffix = columnSetName.substring(columnSetName.indexOf('|') + 1);
            ColumnSetsPreference columnSets = getColumnSets(columnSetDomain, entityName);
            ColumnSetsPreference.ColumnSet columnSet = ConfigurableList.getColumnNameList(columnSetNameSuffix,
                    ConfigurableList.ColumnSetType.VIEW, columnSets);
            columnNameList = columnSet.getColumnDefinitions();
            searchInstance.setColumnSetColumnNameList(columnNameList);
        }
        // To improve performance, add join fetches for search values that have the "display" checkbox set
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
        pagination.setJoinFetchPaths(joinFetchPaths);
        List<?> entityList = PaginationUtil.getPage(configurableSearchDao.getEntityManager(), pagination, 0);

        // Format the results into columns
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, searchInstance.getViewColumnParamMap(),
                pagination.getNumberPages() == 1 ? sortColumnIndex : null, sortDirection,
                ColumnEntity.getByName(entityName));

        // Add any row listeners
        configurableList.addAddRowsListeners(configurableSearchDef);

        searchInstance.getEvalContext().setPagination(pagination);
        configurableList.addRows( entityList, searchInstance.getEvalContext() );

        ConfigurableList.ResultList resultList = configurableList.getResultList();

        return new FirstPageResults(resultList, pagination);
    }

    @Nonnull
    public PaginationUtil.Pagination getPagination(SearchInstance searchInstance,
            ConfigurableSearchDefinition configurableSearchDef, String dbSortPath, String sortDirection) {
        Criteria criteria;

        // Swap out base search definition when an alternate (and exclusive) search term is available
        if( searchInstance.hasAlternateSearchDefinition() ) {
            criteria = configurableSearchDao.buildCriteria(
                    searchInstance.getAlternateSearchDefinition(),
                    searchInstance, null,
                    null);
        } else {
            criteria = configurableSearchDao.buildCriteria(
                    configurableSearchDef,
                    searchInstance, dbSortPath,
                    sortDirection);
        }

        PaginationUtil.Pagination pagination = new PaginationUtil.Pagination( searchInstance.getPageSize() );
        configurableSearchDao.startPagination(pagination, criteria, searchInstance, configurableSearchDef );
        return pagination;
    }

    /**
     * Gets subsequent page of search results.
     *
     * @param searchInstance search terms and values
     * @param pageNumber     which page of results to return
     * @param entityName     the name of the entity we're searching, e.g. Sample
     * @param pagination     from HTTP session
     * @return list of results
     */
    public ConfigurableList.ResultList getSubsequentResultsPage(
            SearchInstance searchInstance, int pageNumber, String entityName, PaginationUtil.Pagination pagination) {
        // Get requested page of results
        List<?> entityList = PaginationUtil.getPage(configurableSearchDao.getEntityManager(), pagination, pageNumber);

        // Format the results into columns
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entityName);
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
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, searchInstance.getViewColumnParamMap(), null,
                ColumnEntity.getByName(entityName));

        // Add any row listeners
        configurableList.addAddRowsListeners(configurableSearchDef);

        configurableList.addRows( entityList,searchInstance.getEvalContext() );

        return configurableList.getResultList();
    }

    public ConfigurableList.ResultList fetchAllPages(PaginationUtil.Pagination pagination,
            SearchInstance searchInstance, String downloadColumnSetName, String entityName) {
        // Determine which columns the user wants to download
        List<ColumnTabulation> columnTabulations;
        if (downloadColumnSetName.equals("Viewed Columns")) {
            columnTabulations = searchInstance.buildViewedColumnTabulations(entityName);
        } else {
            columnTabulations = buildColumnSetTabulations(downloadColumnSetName,
                    ColumnEntity.getByName(entityName));
        }
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, searchInstance.getViewColumnParamMap(), 0, "ASC",
                ColumnEntity.getByName(entityName));

        // Add any row listeners
        configurableList.addAddRowsListeners(SearchDefinitionFactory.getForEntity(entityName));

        // Get each page and add it to the configurable list
        SearchContext context = buildSearchContext(searchInstance, entityName);
        context.setPagination(pagination);
        context.setResultCellTargetPlatform(SearchContext.ResultCellTargetPlatform.TEXT);
        for (int i = 0; i < pagination.getNumberPages(); i++) {
            List<?> resultsPage = PaginationUtil.getPage(threadEntityManager.getEntityManager(), pagination, i);
            configurableList.addRows(resultsPage, context);
            threadEntityManager.getEntityManager().clear();
        }
        return configurableList.getResultList(false);
    }

    /**
     *  BSP user lookup required in column eval expression
     *  Use context to avoid need to test in container
     */
    private SearchContext buildSearchContext(SearchInstance searchInstance, String entityName){
        SearchContext evalContext = new SearchContext();
        evalContext.setBspUserList( bspUserList );
        evalContext.setSearchInstance(searchInstance);
        evalContext.setColumnEntityType(ColumnEntity.getByName(entityName));
        evalContext.setUserBean(userBean);
        evalContext.setJiraConfig(jiraConfig);
        evalContext.setPriceListCache(priceListCache);
        evalContext.setQuoteLink(quoteLink);
        evalContext.setOptionValueDao(constrainedValueDao);
        return evalContext;
    }
}
