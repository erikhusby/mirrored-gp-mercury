package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnDefinition;

import java.util.List;

/**
 * A list of column sets, where each set is a name and a list of column definition names.
 */
public class ColumnSetsPreference implements PreferenceDefinitionValue {

    @Override
    public String marshal() {
        return null;
    }

    @Override
    public PreferenceDefinitionValue unmarshal(String xml) {
        return null;
    }

    public static class ColumnSet {
        private String name;
//        private ? visibility;
        private List<ColumnDefinition> columnDefinitions;
    }

    private List<ColumnSet> columnSets;

    public List<ColumnSet> getColumnSets() {
        return columnSets;
    }
}
