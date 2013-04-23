/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.athena.entity.preference;

/**
 * Preference definitions are responsible for converting data from the stored preference data into a real object or from
 * the object to the stored data.
 */
public class PreferenceDefinition {
    private final PreferenceDefinitionValue definitionValue;

    public PreferenceDefinition(PreferenceType type, String xml) throws Exception {
        definitionValue = type.create(xml);
    }

    public PreferenceDefinitionValue getDefinitionValue() {
        return definitionValue;
    }
}

