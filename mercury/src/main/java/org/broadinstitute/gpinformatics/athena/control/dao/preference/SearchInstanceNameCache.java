package org.broadinstitute.gpinformatics.athena.control.dao.preference;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Path("/searchUtils")
public class SearchInstanceNameCache implements Serializable {

    private static final Log log = LogFactory.getLog(SearchInstanceNameCache.class);

    /**
     * Avoid constantly re-parsing enum structure by keeping
     * a list of GLOBAL-USER search instance PreferenceType Pairs ordered same as ColumnEntity enum
     */
    private Map<ColumnEntity, Pair<PreferenceType,PreferenceType>> preferenceTypes = null;

    /**
     * A Map keyed by the type of entity returned in the search results.  <br/>
     * Value is a list of all potential global drill down searches <br />
     * e.g. If the key is LabVessel, value is all searches with only one search term
     */
    private Map<String,List<DrillDownOption>> globalDrillDowns = null;

    /**
     * A Map keyed by user ID of entity returned in the search results.  <br/>
     * Value is a map of all potential user drill down searches keyed by the entity type
     */
    private Map<Long,Map<String,List<DrillDownOption>>> userDrillDowns = null;

    /**
     * A Map keyed by the type of entity returned in the search results.  <br/>
     * Value is a list of all global searches matched with a PreferenceType <br />
     */
    private Map<ColumnEntity, Pair<PreferenceType,List<String>>> globalSearches = null;

    /**
     * A Map keyed by user ID of entity returned in the search results.  <br/>
     * The value is a Map keyed by the type of entity returned in the search results.  <br/>
     * Value is a list of all user searches matched with a PreferenceType <br />
     */
    private Map<Long,Map<ColumnEntity, Pair<PreferenceType,List<String>>>> userSearches = null;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private UserBean userBean;

    /**
     * Build out all lists from existing data. <br/>
     * Refreshes/updates triggered from SearchInstanceEjb CRUD methods
     */
    @PostConstruct
    public void refreshCache() {

        preferenceTypes = new LinkedHashMap<>();
        PreferenceType globalPreferenceType;
        PreferenceType userPreferenceType;
        for( ColumnEntity columnEntity : ColumnEntity.values() ) {
            globalPreferenceType = null;
            userPreferenceType = null;
            for( PreferenceType preferenceType : columnEntity.getSearchInstancePrefs() ) {
                if( preferenceType.getPreferenceScope() == PreferenceType.PreferenceScope.GLOBAL ) {
                    globalPreferenceType = preferenceType;
                } else {
                    userPreferenceType = preferenceType;
                }
            }
            preferenceTypes.put( columnEntity, Pair.of( globalPreferenceType, userPreferenceType ) );
        }

        refreshGlobalCache();
        refreshUserCache();
    }

    @GET
    @Path("/drillDown")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DrillDownOption> getDrillDowns( @QueryParam("columnEntity") String columnEntityName ) {

        List<DrillDownOption> options = new ArrayList<>(globalDrillDowns.get(columnEntityName));
        Long userID = userBean.getBspUser().getUserId();

        if( userDrillDowns.containsKey(userID ) && userDrillDowns.get(userID).get(columnEntityName) != null ) {
            options.addAll( userDrillDowns.get(userID).get(columnEntityName) );
        }

        return options;
    }

    /**
     * To support list of saved searches on UI choose search entity type page:  <br />
     * Builds a list of all available saved search instances at USER and GLOBAL scopes
     *       Global searches application scope, User searches session scope?
     *       Refreshes triggered by deleteSearch and persistSearch methods
     * @return searchInstanceNames the target search entity type
     *                        [LabVessel, LabEvent]
     *                                       '-> [Global Type, User Type]
     *                                                             '-> [Saved Search Names]
     */
    public Map<ColumnEntity, Map<PreferenceType, List<String>>> fetchInstanceNames(Long userID) {

        Map<ColumnEntity, Map<PreferenceType,List<String>>> instanceNames = new LinkedHashMap<>();

        // Build initial from globals (Map is fully populated with all entity type keys)
        for ( Map.Entry<ColumnEntity, Pair<PreferenceType,List<String>> > entry : globalSearches.entrySet()) {
            Map<PreferenceType,List<String>> typeMap = new LinkedHashMap<>();
            typeMap.put(entry.getValue().getLeft(), entry.getValue().getRight());
            instanceNames.put(entry.getKey(), typeMap);
        }

        // UI needs, at minimum, empty lists for all user preference types
        for( Map.Entry<ColumnEntity, Pair<PreferenceType,PreferenceType>> prefTypeEntry : preferenceTypes.entrySet()  ) {
            List<String> searchNames = new ArrayList<>();
            instanceNames.get(prefTypeEntry.getKey()).put(prefTypeEntry.getValue().getRight(), searchNames);
        }

        // Now, fill user search lists with any existing
        if( userSearches.containsKey(userID ) ) {
            Map<ColumnEntity, Pair<PreferenceType,List<String>>> searchesforUser = userSearches.get(userID );
            for ( Map.Entry<ColumnEntity, Pair<PreferenceType,List<String>> > entry : searchesforUser.entrySet()) {
                Map<PreferenceType,List<String>> typeMap = instanceNames.get(entry.getKey());
                typeMap.put(entry.getValue().getLeft(), entry.getValue().getRight());
            }
        }

        return instanceNames;
    }

    /**
     * Rebuild global search caches, one for each entity type regardless of whether or not any saved searches exist
     */
    public synchronized void refreshGlobalCache() {

        // Build new structures from scratch
        Map<String,List<DrillDownOption>> globalDrillDownOptions = new HashMap<>();
        Map<ColumnEntity, Pair<PreferenceType,List<String>>> globalSearchNames = new LinkedHashMap<>();

        // Fill out both with empty lists
        fillEntityDrillDownMap( globalDrillDownOptions );
        for( Map.Entry<ColumnEntity, Pair<PreferenceType,PreferenceType>> prefTypeEntry : preferenceTypes.entrySet()  ) {
            List<String> searchNames = new ArrayList<>();
            globalSearchNames.put(prefTypeEntry.getKey(), Pair.of(prefTypeEntry.getValue().getLeft(), searchNames));
        }

        for( Map.Entry<ColumnEntity, Pair<PreferenceType,PreferenceType>> prefTypeEntry : preferenceTypes.entrySet()  ) {
            ColumnEntity columnEntity = prefTypeEntry.getKey();
            PreferenceType globalPreferenceType = prefTypeEntry.getValue().getLeft();

            List<String> allSearchNames = globalSearchNames.get(columnEntity).getRight();
            List<DrillDownOption> drillDownSearches = globalDrillDownOptions.get( columnEntity.getEntityName() );

            try {
                // Global has only one for each
                Preference preference = preferenceDao.getGlobalPreference(globalPreferenceType);
                if( preference == null ) {
                    continue;
                }
                SearchInstanceList searchInstanceList =
                        (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                for( SearchInstance searchInstance : searchInstanceList.getSearchInstances() ) {
                    searchInstance.establishRelationships(SearchDefinitionFactory.getForEntity(columnEntity.getEntityName()));
                    // Drill down logic requires a single search value
                    if( searchInstance.getSearchValues().size() == 1 ) {
                        SearchInstance.SearchValue searchValue = searchInstance.getSearchValues().get(0);
                        drillDownSearches.add( new DrillDownOption( globalPreferenceType.getPreferenceScope(), globalPreferenceType.name(), searchInstance.getName(), searchValue.getTermName(), columnEntity ) );
                    }
                    allSearchNames.add(searchInstance.getName());
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

        // Replace instance cached structures
        globalDrillDowns = globalDrillDownOptions;
        globalSearches = globalSearchNames;
    }

    /**
     * Rebuild user search caches, only for each entity type with existing saved user search preferences
     */
    private void refreshUserCache() {

        Map<Long,Map<String,List<DrillDownOption>>> userDrillDownOptions = new HashMap<>();
        Map<Long, Map<ColumnEntity, Pair<PreferenceType, List<String>>>> allUsersSearchTypeNames = new HashMap<>();

        for( Map.Entry<ColumnEntity, Pair<PreferenceType,PreferenceType>> prefTypeEntry : preferenceTypes.entrySet()  ) {
            ColumnEntity columnEntity = prefTypeEntry.getKey();
            PreferenceType userPreferenceType = prefTypeEntry.getValue().getRight();

            try {
                List<Preference> preferences = preferenceDao.getPreferences(userPreferenceType);

                for (Preference preference : preferences) {
                    Long userId = preference.getAssociatedUser();

                    // Create a map entry for the user if none yet
                    Map<ColumnEntity, Pair<PreferenceType, List<String>>> searchTypeNamesForUser = allUsersSearchTypeNames.get(userId);
                    if (searchTypeNamesForUser == null) {
                        searchTypeNamesForUser = new LinkedHashMap<>();
                        allUsersSearchTypeNames.put(userId, searchTypeNamesForUser);
                    }

                    // Add user search preference to user map entry - only one allowed per type per user so overwrite
                    List<String> allSearchNames = new ArrayList<>();
                    Pair<PreferenceType, List<String>> searchListPair = Pair.of(userPreferenceType, allSearchNames);
                    searchTypeNamesForUser.put(columnEntity, searchListPair);

                    SearchInstanceList searchInstanceList =
                            (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();

                    for( SearchInstance searchInstance : searchInstanceList.getSearchInstances() ) {
                        searchInstance.establishRelationships(SearchDefinitionFactory.getForEntity(columnEntity.getEntityName()));

                        allSearchNames.add(searchInstance.getName());

                        if( searchInstance.getSearchValues().size() == 1 ) {
                            SearchInstance.SearchValue searchValue = searchInstance.getSearchValues().get(0);

                            // Create a map entry for the user if none yet
                            Map<String, List<DrillDownOption>> userDrillDowns = userDrillDownOptions.get(userId);
                            if (userDrillDownOptions == null) {
                                userDrillDowns = new HashMap<>();
                                fillEntityDrillDownMap(userDrillDowns);
                                userDrillDownOptions.put(userId, userDrillDowns);
                            }

                            userDrillDowns.get(columnEntity.getEntityName()).add(new DrillDownOption(userPreferenceType.getPreferenceScope(), userPreferenceType.name(), searchInstance.getName(), searchValue.getTermName(), columnEntity));
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

        userDrillDowns = userDrillDownOptions;
        userSearches = allUsersSearchTypeNames;
    }

    /**
     * Rebuild user search cache for a single user, only for each entity type with existing saved user search preferences
     */
    public void refreshCacheForUser( Long userId ) {

        Map<String,List<DrillDownOption>> userDrillDownOptions = new HashMap<>();
        Map<ColumnEntity, Pair<PreferenceType,List<String>>> userSearchNames = new LinkedHashMap<>();

        // Fill with empty lists for each column entity name
        fillEntityDrillDownMap(userDrillDownOptions);

        for( Map.Entry<ColumnEntity, Pair<PreferenceType,PreferenceType>> prefTypeEntry : preferenceTypes.entrySet()  ) {
            ColumnEntity columnEntity = prefTypeEntry.getKey();
            PreferenceType userPreferenceType = prefTypeEntry.getValue().getRight();
            try {
                // User has (should have) only one for each user
                List<Preference> preferences = preferenceDao.getPreferences(userId, userPreferenceType);
                for( Preference preference : preferences ) {

                    List<String> allSearchNames = new ArrayList<>();
                    Pair<PreferenceType,List<String>> typeList = Pair.of(userPreferenceType, allSearchNames);
                    userSearchNames.put(columnEntity, typeList);

                    SearchInstanceList searchInstanceList =
                            (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                    for( SearchInstance searchInstance : searchInstanceList.getSearchInstances() ) {
                        searchInstance.establishRelationships(SearchDefinitionFactory.getForEntity(columnEntity.getEntityName()));
                        if( searchInstance.getSearchValues().size() == 1 ) {
                            SearchInstance.SearchValue searchValue = searchInstance.getSearchValues().get(0);
                            userDrillDownOptions.get( columnEntity.getEntityName() ).add( new DrillDownOption( userPreferenceType.getPreferenceScope(), userPreferenceType.name(), searchInstance.getName(), searchValue.getTermName(), columnEntity ) );
                        }
                        allSearchNames.add(searchInstance.getName());
                    }
                }

            } catch (Exception e) {
                log.error(e);
            }
        }

        userDrillDowns.put(userId, userDrillDownOptions);
        userSearches.put(userId, userSearchNames);
    }

    /**
     * Utility method to populate a map of all column entities to an empty list of drill down options
     */
    private void fillEntityDrillDownMap(Map<String,List<DrillDownOption>> drillDowns){
        for(ColumnEntity columnEntity : ColumnEntity.values()) {
            drillDowns.put(columnEntity.getEntityName(), new ArrayList<>());
        }
    }

    /**
     * Data structure required to hold what's necessary to build a drill down search link
     */
    public static class DrillDownOption {
        private PreferenceType.PreferenceScope preferenceScope;
        private String preferenceName;
        private String searchName;
        private String searchTermName;
        private ColumnEntity targetEntity;

        DrillDownOption(PreferenceType.PreferenceScope preferenceScope,
                        String preferenceName, String searchName,
                        String searchTermName, ColumnEntity targetEntity) {
            this.preferenceScope = preferenceScope;
            this.preferenceName = preferenceName;
            this.searchName = searchName;
            this.searchTermName = searchTermName;
            this.targetEntity = targetEntity;
        }

        public PreferenceType.PreferenceScope getPreferenceScope() {
            return preferenceScope;
        }

        public String getPreferenceName() {
            return preferenceName;
        }

        public String getSearchName() {
            return searchName;
        }

        public String getSearchTermName() {
            return searchTermName;
        }

        public ColumnEntity getTargetEntity() {
            return targetEntity;
        }

        /**
         * This is the value required to be passed around in the UI logic <br/>
         * e.g.
         */
        @Override
        public String toString(){
            return preferenceScope.toString() + "|" + preferenceName + "|" + searchName + "|" + searchTermName + "|" + targetEntity.getEntityName();
        }

        /**
         * Disassembles the UI value into a DrillDownOption
         */
        public static DrillDownOption buildFromString( String uiValue ) {
            String[] tokens = uiValue.split("\\|", 5);
            return new DrillDownOption( PreferenceType.PreferenceScope.valueOf( tokens[0]),
                    tokens[1],  tokens[2],  tokens[3], ColumnEntity.getByName(tokens[4]));
        }
    }
}
