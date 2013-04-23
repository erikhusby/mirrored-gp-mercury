package org.broadinstitute.gpinformatics.athena.entity.preference;

/**
 *
 * This interface allows different preference types to create themselves for use with the the preference definition
 * class.
 */
public interface PreferenceDefinitionCreator {
    public PreferenceDefinitionValue create(String xml) throws Exception;
}
