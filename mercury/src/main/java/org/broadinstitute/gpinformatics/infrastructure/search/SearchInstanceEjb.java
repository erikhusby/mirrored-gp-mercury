package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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

    @Inject
    private UserBean userBean;

    @Inject
    private PreferenceEjb preferenceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    /**
     * Map from preference level name to a method to get the preference
     */
    private static final Map<String, PreferenceAccess> preferenceAccessMap = new LinkedHashMap<>();

    /**
     * Method to retrieve preference from the database, has a different implementation for
     * each level
     */
    private interface PreferenceAccess {
        Preference getPreference(/*CoreActionBeanContext bspActionBeanContext, */PreferenceDao preferenceDao);

        Preference createNewPreference(/*CoreActionBeanContext bspActionBeanContext*/);

        boolean canModifyPreference(/*CoreActionBeanContext bspActionBeanContext*/);
    }

    static {
        // TODO jmt do we need search instances for entities other than Sample?
        preferenceAccessMap.put("GLOBAL", new PreferenceAccess() {
            @Override
            public Preference getPreference(/*CoreActionBeanContext bspActionBeanContext, */PreferenceDao preferenceDao) {
                return preferenceDao.getGlobalPreference(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES);
            }

            @Override
            public Preference createNewPreference(/*CoreActionBeanContext bspActionBeanContext*/) {
                return new Preference(PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES, "");
            }

            @Override
            public boolean canModifyPreference(/*CoreActionBeanContext bspActionBeanContext*/) {
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

    public void fetchInstances(Map<String, Preference> preferenceMap, List<String> searchInstanceNames,
            List<String> newSearchLevels) {
        for (Map.Entry<String, PreferenceAccess> stringPreferenceAccessEntry : preferenceAccessMap.entrySet()) {
            PreferenceAccess preferenceAccess = stringPreferenceAccessEntry.getValue();
            Preference preference = preferenceAccess.getPreference(preferenceDao);
            if (preference != null) {
                SearchInstanceList searchInstanceList;
                try {
                    searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                    searchInstanceNames.add(stringPreferenceAccessEntry.getKey() + PREFERENCE_SEPARATOR + instance.getName());
                }
            }
            preferenceMap.put(stringPreferenceAccessEntry.getKey(), preference);
            if (preferenceAccess.canModifyPreference()) {
                newSearchLevels.add(stringPreferenceAccessEntry.getKey());
            }
        }
    }
    /**
     * Save a search into a preference.
     *
     * @param newSearch true if inserting a new search, false if updating an existing
     *                  search
     */
    public void persistSearch(boolean newSearch, SearchInstance searchInstance, MessageCollection messageCollection,
            String newSearchLevel, String newSearchName, String selectedSearchName,
            Map<String, Preference> preferenceMap) {
        try {
            boolean save = true;
            // For new searches, check the user's authorization
            if (newSearch) {
                if (!preferenceAccessMap.get(newSearchLevel).canModifyPreference()) {
                    messageCollection.addError("You are not authorized to save searches at level {2}", newSearchLevel);
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
                                    .canModifyPreference()) {
                                searchName = selectedSearchName.substring(level.length()
                                                                          + PREFERENCE_SEPARATOR.length());
                                preference = preferenceMap.get(level);
                            } else {
                                messageCollection.addError("You are not authorized to update searches at level {2}",
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
                            .createNewPreference();
                    searchInstanceList = new SearchInstanceList();
                } else {
                    searchInstanceList = (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
                }
                if (newSearch) {
                    // Check for uniqueness
                    for (SearchInstance searchInstanceLocal : searchInstanceList.getSearchInstances()) {
                        if (searchInstanceLocal.getName().equals(newSearchName)) {
                            messageCollection.addError("There is already a search called " + newSearchName + " in the "
                                                       + newSearchLevel + " level");
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
                    preferenceEjb.add(userBean.getBspUser().getUserId(),
                            PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES, searchInstanceList);
                    messageCollection.addInfo("The search was saved");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            messageCollection.addError("Failed to save the search");
        }
    }

    public void deleteSearch(MessageCollection messageCollection, String selectedSearchName,
            Map<String, Preference> preferenceMap) {
        String searchName = null;
        Preference preference = null;
        for (Map.Entry<String, Preference> stringPreferenceEntry : preferenceMap.entrySet()) {
            if (selectedSearchName.startsWith(stringPreferenceEntry.getKey())) {
                if (preferenceAccessMap.get(selectedSearchName.substring(0, stringPreferenceEntry.getKey().length())).
                        canModifyPreference()) {
                    searchName = selectedSearchName.substring(stringPreferenceEntry.getKey().length() +
                                                              PREFERENCE_SEPARATOR.length());
                    preference = stringPreferenceEntry.getValue();
                } else {
                    messageCollection.addError("You are not authorized to delete this search");
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
                messageCollection.addError("Failed to delete the search");
            }
            messageCollection.addInfo("The search was deleted");
        }
    }
}
