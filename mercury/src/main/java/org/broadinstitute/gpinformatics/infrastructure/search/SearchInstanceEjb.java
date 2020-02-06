package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.SearchInstanceNameCache;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists SearchInstance.
 */
@Stateful
@RequestScoped
public class SearchInstanceEjb {

    private static final Log log = LogFactory.getLog(SearchInstanceEjb.class);

    /**
     * Separator between preference level name and search instance name
     */
    public static final String PREFERENCE_SEPARATOR = " - ";

    /**
     * Search lookup uses delimited value
     */
    public static final String PREFERENCE_DELIM = "|";

    @Inject
    private UserBean userBean;

    @Inject
    private PreferenceEjb preferenceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private SearchInstanceNameCache searchInstanceNameCache;

    /**
     * Map from preference type name to a method to get the preference
     */
    private static final Map<PreferenceType, PreferenceAccess> mapTypeToPreferenceAccess =
            new LinkedHashMap<>();

    /**
     * Method to retrieve preference from the database, has a different implementation for each type
     */
    private interface PreferenceAccess {

        List<Preference> getPreferences( Long userID, PreferenceDao preferenceDao) throws Exception;

        Preference createNewPreference( Long userID );

        boolean canModifyPreference( Long userID );

        PreferenceType.PreferenceScope getScope();
    }

    static {
        for (PreferenceType preferenceType : new PreferenceType[]{
                PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_LAB_METRIC_RUN_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_LAB_METRIC_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_PRODUCT_ORDER_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_REAGENT_SEARCH_INSTANCES,
                PreferenceType.GLOBAL_EXT_LIBRARY_SEARCH_INSTANCES,
                PreferenceType.USER_LAB_EVENT_SEARCH_INSTANCES,
                PreferenceType.USER_LAB_METRIC_RUN_SEARCH_INSTANCES,
                PreferenceType.USER_LAB_METRIC_SEARCH_INSTANCES,
                PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES,
                PreferenceType.USER_MERCURY_SAMPLE_SEARCH_INSTANCES,
                PreferenceType.USER_PRODUCT_ORDER_SEARCH_INSTANCES,
                PreferenceType.USER_REAGENT_SEARCH_INSTANCES,
                PreferenceType.USER_EXT_LIBRARY_SEARCH_INSTANCES,
        }) {
            mapTypeToPreferenceAccess.put(preferenceType, new PreferenceAccess() {
                @Override
                public List<Preference> getPreferences(Long userId, PreferenceDao preferenceDao) throws Exception {
                    if (preferenceType.getPreferenceScope() == PreferenceType.PreferenceScope.USER) {
                        return preferenceDao.getPreferences(userId, preferenceType);
                    } else if (preferenceType.getPreferenceScope() == PreferenceType.PreferenceScope.GLOBAL) {
                        Preference preference = preferenceDao.getGlobalPreference(preferenceType);
                        if (preference != null) {
                            return Collections.singletonList(preference);
                        }
                    }
                    return null;
                }

                @Override
                public Preference createNewPreference(Long userID) {
                    return new Preference(userID, preferenceType, "");
                }

                @Override
                public boolean canModifyPreference(Long userID) {
                    return true; // actionBeanContext.isAdmin();  (BSP carryover not implemented as of 7/22/2014)
                }

                @Override
                public PreferenceType.PreferenceScope getScope() {
                    return preferenceType.getPreferenceScope();
                }
            });
        }
    }

    /**
     * @param entityType the target search entity type
     * @param mapTypeToPreference output, maps preference type to preference
     * @param searchInstanceNames output, maps name of search instances (with scope prefi)
     *                            to pipe delimited values
     * @param newSearchLevels output, map of scopes (GLOBAL/USER) to the preference name for  each
     */
    public void fetchInstances( ColumnEntity entityType,
            Map<PreferenceType, Preference> mapTypeToPreference,
            Map<String,String> searchInstanceNames, Map<String,String> newSearchLevels) throws Exception {

        // Required for user defined searches
        Long userID = userBean.getBspUser().getUserId();

        // Entity types available for GLOBAL and USER scopes
        PreferenceType[] types = entityType.getSearchInstancePrefs();
        if( types == null || types.length == 0 ) {
            String msg = "No search preference types declared for entity type " + entityType;
            log.error( msg );
            throw new RuntimeException( msg );
        }

        for ( PreferenceType type : types ) {
            PreferenceAccess preferenceAccess  = mapTypeToPreferenceAccess.get(type);
            // An entity can only have USER and GLOBAL scope
            if (preferenceAccess.canModifyPreference(userID )) {
                newSearchLevels.put(preferenceAccess.getScope().toString(), type.toString());
            }
            List<Preference> preferences = preferenceAccess.getPreferences( userID, preferenceDao);
            if ( preferences == null ) {
                continue;
            }
            for( Preference preference : preferences ) {
                SearchInstanceList searchInstanceList;
                try {
                    searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                    searchInstanceNames.put( preferenceAccess.getScope().toString() + PREFERENCE_SEPARATOR + instance.getName(),
                            preferenceAccess.getScope().toString() + PREFERENCE_DELIM + type + PREFERENCE_DELIM + instance.getName() );
                }

                mapTypeToPreference.put(type, preference);

            }
        }
    }


    /**
     * Save a search into a preference.
     *
     * @param newSearch true if inserting a new search, false if updating an existing
     *                  search
     */
    public void persistSearch( boolean newSearch, SearchInstance searchInstance, MessageCollection messageCollection,
            PreferenceType newSearchType, String searchName,
            Map<PreferenceType, Preference> mapTypeToPreference) {

        try {
            boolean save = true;

            // Required for user defined searches
            Long userID = userBean.getBspUser().getUserId();

            // Check the user's authorization
            if (!mapTypeToPreferenceAccess.get(newSearchType).canModifyPreference(userID)) {
                messageCollection.addError("You are not authorized to save searches at level {2}", newSearchType);
                save = false;
            }

            if (save) {
                // Find the existing preference (if any) for the type
                //   (each preference holds many searches)
                Preference preference = mapTypeToPreference.get(newSearchType);

                SearchInstanceList searchInstanceList;

                // Flag to create a brand new global or user preference
                //    (vs. adding/updating a search instance in an existing preference)
                boolean preferenceExists = true;
                if (preference == null) {
                    // Didn't find an existing preference, so create a new one
                    preference = mapTypeToPreferenceAccess.get(newSearchType)
                            .createNewPreference(userID);
                    searchInstanceList = new SearchInstanceList();
                    preferenceExists = false;
                } else {
                    searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                }

                if (newSearch) {
                    // Check for uniqueness
                    for (SearchInstance searchInstanceLocal : searchInstanceList.getSearchInstances()) {
                        if (searchInstanceLocal.getName().equals(searchName)) {
                            messageCollection.addError("There is already a search called " + searchName + " in the "
                                                       + newSearchType + " level");
                            save = false;
                            break;
                        }
                    }
                    searchInstance.setName(searchName);
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
                    if(!preferenceExists) {
                        preferenceEjb.add(userID,
                                newSearchType, searchInstanceList);
                    } else {
                        preference.markModified(searchInstanceList.marshal());
                    }

                    messageCollection.addInfo("The search was saved");
                }

                preferenceDao.flush();
                // Refresh cached instance names
                if( newSearchType.getPreferenceScope() == PreferenceType.PreferenceScope.GLOBAL ) {
                    searchInstanceNameCache.refreshGlobalCache();
                } else {
                    searchInstanceNameCache.refreshCacheForUser(userID);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            messageCollection.addError("Failed to save the search");
        }
    }

    public void deleteSearch(MessageCollection messageCollection, PreferenceType preferenceType, String searchName,
            Map<PreferenceType, Preference> mapTypeToPreference) {
        Preference preference = mapTypeToPreference.get(preferenceType);

        // Required for user defined searches
        Long userID = userBean.getBspUser().getUserId();

        if (!mapTypeToPreferenceAccess.get(preferenceType).canModifyPreference( userID )) {
            messageCollection.addError("You are not authorized to delete this search");
        } else {
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
            preference.markModified(searchInstanceList.marshal());
            preference.setModifiedDate(new Date());
            try {
                preferenceDao.persist(preference);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                messageCollection.addError("Failed to delete the search");
            }
            messageCollection.addInfo("The search was deleted");

            preferenceDao.flush();
            // Refresh cached instance names
            if( preferenceType.getPreferenceScope() == PreferenceType.PreferenceScope.GLOBAL ) {
                searchInstanceNameCache.refreshGlobalCache();
            } else {
                searchInstanceNameCache.refreshCacheForUser(userID);
            }
        }
    }
}
