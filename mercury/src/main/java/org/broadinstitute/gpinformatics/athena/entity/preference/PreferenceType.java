/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.athena.entity.preference;

/**
 * This enum holds all the needed information for converting preferences to their associated data.
 */
public enum PreferenceType {
    PDO_SEARCH("PDO Search Preference", PreferenceScope.USER, 1,
            new NameValueDefinitionValue.NameValuePreferenceDefinitionCreator()),

    GLOBAL_LAB_VESSEL_SEARCH_INSTANCES("Global Lab Vessel Search Instances", PreferenceScope.GLOBAL, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    GLOBAL_LAB_VESSEL_COLUMN_SETS("Global Lab Vessel Column Sets", PreferenceScope.GLOBAL, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),
    USER_LAB_VESSEL_SEARCH_INSTANCES("User Lab Vessel Search Instances", PreferenceScope.USER, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    USER_LAB_VESSEL_COLUMN_SETS("User Lab Vessel Column Sets", PreferenceScope.USER, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),

    GLOBAL_LAB_EVENT_SEARCH_INSTANCES("Global Lab Event Search Instances", PreferenceScope.GLOBAL, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    GLOBAL_LAB_EVENT_COLUMN_SETS("Global Lab Event Column Sets", PreferenceScope.GLOBAL, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),
    USER_LAB_EVENT_SEARCH_INSTANCES("User Lab Event Search Instances", PreferenceScope.USER, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    USER_LAB_EVENT_COLUMN_SETS("User Lab Event Column Sets", PreferenceScope.USER, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),

    GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES("Global Mercury Sample Search Instances", PreferenceScope.GLOBAL, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    GLOBAL_MERCURY_SAMPLE_COLUMN_SETS("Global Mercury Sample Column Sets", PreferenceScope.GLOBAL, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),
    USER_MERCURY_SAMPLE_SEARCH_INSTANCES("User Mercury Sample Search Instances", PreferenceScope.USER, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    USER_MERCURY_SAMPLE_COLUMN_SETS("User Mercury Sample Column Sets", PreferenceScope.USER, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),

    GLOBAL_REAGENT_SEARCH_INSTANCES("Global Reagent Search Instances", PreferenceScope.GLOBAL, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    GLOBAL_REAGENT_COLUMN_SETS("Global Reagent Column Sets", PreferenceScope.GLOBAL, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator()),
    USER_REAGENT_SEARCH_INSTANCES("User Reagent Search Instances", PreferenceScope.USER, 1,
            new SearchInstanceList.SearchInstanceListPreferenceDefinitionCreator()),
    USER_REAGENT_COLUMN_SETS("User Reagent Column Sets", PreferenceScope.USER, 1,
            new ColumnSetsPreference.ColumnSetsPreferenceDefinitionCreator());

    private final String preferenceTypeName;
    private final PreferenceScope preferenceScope;
    private final PreferenceDefinitionCreator creator;
    private final int saveLimit;

    private PreferenceType(
            String preferenceTypeName,
            PreferenceScope preferenceScope,
            int saveLimit,
            PreferenceDefinitionCreator creator) {

        this.preferenceTypeName = preferenceTypeName;
        this.preferenceScope = preferenceScope;
        this.saveLimit = saveLimit;
        this.creator = creator;
    }

    public PreferenceDefinitionCreator getCreator() {
        return creator;
    }

    public String getPreferenceTypeName() {
        return preferenceTypeName;
    }

    public PreferenceScope getPreferenceScope() {
        return preferenceScope;
    }

    public int getSaveLimit() {
        return saveLimit;
    }

    public PreferenceDefinitionValue create(String xml) throws Exception {
        return creator.create(xml);
    }

    /**
     * This enum specifies the intention for this type of preference. Currently they are just global or user, but later
     * could be tied to other classifications.
     */
    public enum PreferenceScope {
        GLOBAL,
        USER
    }
}
