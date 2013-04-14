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
            new NameValuePreferenceDefinition.NameValuePreferenceDefinitionCreator());

    public final static Integer UNLIMITED = null;

    private final String preferenceTypeName;
    private final PreferenceScope preferenceScope;
    private final NameValuePreferenceDefinition.PreferenceDefinitionCreator creator;
    private final int saveLimit;

    private PreferenceType(
            String preferenceTypeName,
            PreferenceScope preferenceScope,
            int saveLimit,
            PreferenceDefinition.PreferenceDefinitionCreator creator) {

        this.preferenceTypeName = preferenceTypeName;
        this.preferenceScope = preferenceScope;
        this.saveLimit = saveLimit;
        this.creator = creator;
    }

    public PreferenceDefinition.PreferenceDefinitionCreator getCreator() {
        return creator;
    }

    public static PreferenceType findByName(String name) {
        for (PreferenceType theEnum : PreferenceType.values()) {
            if (theEnum.name().equals(name)) {
                return theEnum;
            }
        }

        return null;
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

    public enum PreferenceScope {
        GLOBAL,
        USER
    }
}
