/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The
 * Broad Institute cannot be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.athena.control.dao.preference;

import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;

/*
 * Manage the preference system with global, group, project and user-level preferences.
 *
 * Inspired by preferences written by Mike Dinsmore in GAP. This incorporates some recently used ideas developed in GAP.
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PreferenceEjb {

    @Inject
    private PreferenceDao preferenceDao;

    public PreferenceEjb() {
    }

    /**
     * Add the object preference.
     *
     * @param associatedUser  The user to associate with this preference.
     * @param preferenceType  The preference type for easy searching.
     * @param definitionValue The preference definition
     *
     * @throws PreferenceException Any problems processing the preference.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void add(
            @Nonnull Long associatedUser,
            @Nonnull PreferenceType preferenceType,
            @Nonnull PreferenceDefinitionValue definitionValue) throws Exception {

        String data = definitionValue.marshal();
        List<Preference> userPreferences = preferenceDao.getPreferences(associatedUser, preferenceType);

        // If the preference is the same as any of the existing preferences, then just update it and return.
        for (Preference preference : userPreferences) {
            if (preference.isSame(data)) {
                preference.markModified(data);
                return;
            }
        }

        // If we have preferences left, just save this one.
        if (userPreferences.size() < preferenceType.getSaveLimit()) {
            preferenceDao.persist(new Preference(associatedUser, preferenceType, data));
            return;
        }

        // These preferences are fetched in descending modified date order, so replace the oldest one with this one.
        Preference preference = userPreferences.get(userPreferences.size() - 1);
        preference.markModified(data);
    }

    /**
     * Add the object preference.
     *
     * @param object1Id       An id for an object that is used for look up.
     * @param object2Id       An id for a second object to be used for look up.
     * @param preferenceType  The preference type.
     * @param definitionValue The preference definition value
     *
     * @throws PreferenceException Any problems processing the preference.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void add(
            long createdBy,
            long object1Id,
            @Nullable Long object2Id,
            @Nonnull PreferenceType preferenceType,
            @Nonnull PreferenceDefinitionValue definitionValue) throws Exception {

        String data = definitionValue.marshal();
        List<Preference> userPreferences = preferenceDao.getPreferences(object1Id, object2Id, preferenceType);

        // If the preference is the same as any of the existing preferences, then just update it and return.
        for (Preference preference : userPreferences) {
            if (preference.isSame(object1Id, object2Id, data)) {
                preference.update(object1Id, object2Id, data);
                return;
            }
        }

        // If we have preferences left, just save this one.
        if (userPreferences.size() < preferenceType.getSaveLimit()) {
            preferenceDao.persist(new Preference(createdBy, object1Id, object2Id, preferenceType, data));
            return;
        }

        // These preferences are fetched in descending modified date order, so replace the oldest one with this one.
        Preference preference = userPreferences.get(userPreferences.size() - 1);
        preference.markModified(data);
    }

    /**
     * Remove every preference for this user.
     *
     * @param associatedUser The user to associate with this preference.
     *
     * @throws Exception Any problems processing the preference.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clear(@Nonnull Long associatedUser) throws Exception {

        List<Preference> userPreferences = preferenceDao.getPreferences(associatedUser);
        for (Preference preference : userPreferences) {
            preferenceDao.remove(preference);
        }
    }

    /**
     * Take all the preferences for this user and type and remove it.
     *
     * @param associatedUser The user to associate with this preference.
     * @param preferenceType The preference type for easy searching.
     *
     * @throws Exception Any problems processing the preference.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clear(@Nonnull Long associatedUser, @Nonnull PreferenceType preferenceType) throws Exception {
        List<Preference> userPreferences = preferenceDao.getPreferences(associatedUser, preferenceType);
        for (Preference preference : userPreferences) {
            preferenceDao.remove(preference);
        }
    }

    /**
     * Remove the preferences for objects and type. Object preferences can only be removed by type because we
     * just don't know whether there could be overlapping object types across preference types.
     *
     * @param object1Id      The primary object.
     * @param object2Id      The optional secondary object.
     * @param preferenceType The preference type.
     *
     * @throws PreferenceException Any errors.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clear(
            @Nonnull Long object1Id,
            @Nullable Long object2Id,
            @Nonnull PreferenceType preferenceType) throws Exception {
        List<Preference> userPreferences = preferenceDao.getPreferences(object1Id, object2Id, preferenceType);
        for (Preference preference : userPreferences) {
            preferenceDao.remove(preference);
        }
    }

}
