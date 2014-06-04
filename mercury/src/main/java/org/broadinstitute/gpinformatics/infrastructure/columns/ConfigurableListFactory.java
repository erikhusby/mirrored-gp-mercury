package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates ConfigurableList instances.
 */
public class ConfigurableListFactory {

    @Inject
    private PreferenceDao preferenceDao;

    /**
     * Create a ConfigurableList instance.
     *
     * @param entityList  Entities for which to display data
     * @param downloadColumnSetName Name of the column set to display
     * @param entityName Name of the entity
     * @param entityId ID of the entity
     * @param domainUser BSPDomainUser object for the current user
     * @param sampleCollection Sample Collection for this data
     *
     * @return ConfigurableList instance
     */
    public ConfigurableList create(@Nonnull List<?> entityList,
            @Nonnull String downloadColumnSetName,
            @Nonnull String entityName,
            @Nonnull ColumnEntity entityId/*,
            @Nonnull BspDomainUser domainUser,
            @Nonnull SampleCollection sampleCollection*/) {

        List<ColumnTabulation> columnTabulations = buildColumnSetTabulations(downloadColumnSetName, entityName/*, domainUser, sampleCollection*/);
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 0, "ASC",
                /*domainUser.isAdmin(), */entityId);
        configurableList.addRows(entityList);
        return configurableList;
    }

    /**
     * Build Column definitions for a column set name
     *
     * @param downloadColumnSetName name of column set that the user wants to download
     * @param entityName            e.g. "Sample"
     * @param domainUser            BSP Domain User
     * @param sampleCollection      Sample Collection for which to build the column definitions
     * @return list of column definitions
     */
    public List<ColumnTabulation> buildColumnSetTabulations(@Nonnull String downloadColumnSetName,
            @Nonnull String entityName/*,
            @Nonnull BspDomainUser domainUser,
            @Nonnull SampleCollection sampleCollection*/) {
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
        // todo jmt other entities
        Preference globalPreference = preferenceDao.getGlobalPreference(PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS);
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
                ConfigurableList.ColumnSetType.DOWNLOAD, /*domainUser, sampleCollection.getGroup(), */columnSetsPreference);
//        ListConfig listConfig = entityPreferenceFactory.loadListConfig(entityName);
        ConfigurableSearchDefinition configurableSearchDefinition = new SearchDefinitionFactory().getForEntity(
                entityName);
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
     * @param bspDomainUser     to retrieve preferences and evaluate visibility expression
     * @param group             to retrieve preferences, and evaluate visibility expression
     * @param sampleCollection  to retrieve preferences, and evaluate visibility expression
     * @param globalColumnSets  type of columns sets for global scope
     * @param groupColumnSets   type of columns sets for group scope
     * @param projectColumnSets type of column sets for project scope
     * @param userColumnSets    type of column sets for user scope
     * @return list of all applicable column sets
     */
    public List<ColumnSet> getColumnSubsets(ConfigurableList.ColumnSetType columnSetType, /*BspDomainUser bspDomainUser,
            Group group, SampleCollection sampleCollection,*/
            PreferenceType globalColumnSets/*,
            PreferenceType.Type groupColumnSets,
            PreferenceType.Type projectColumnSets,
            PreferenceType.Type userColumnSets*/) {

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
                context.put("columnSetType", columnSetType);
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
}
