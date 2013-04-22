package org.broadinstitute.gpinformatics.athena.entity.preference;

/**
 * This is the value object that is used by preferences. It can create itself and stream itself out for storage and
 * recreation.
 */
public interface PreferenceDefinitionValue {
    public String marshal();
    public PreferenceDefinitionValue unmarshal(String xml);
}
