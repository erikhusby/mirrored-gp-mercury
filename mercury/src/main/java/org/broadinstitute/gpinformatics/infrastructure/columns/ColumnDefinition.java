package org.broadinstitute.gpinformatics.infrastructure.columns;

// todo jmt need different sets of columns for different starting entities.
/**
 * Created by thompson on 5/21/2014.
 */
public enum ColumnDefinition {
    LABEL(new ListConfig.ColumnConfig());

    private ColumnTabulation columnTabulation;

    ColumnDefinition(ColumnTabulation columnTabulation) {
        this.columnTabulation = columnTabulation;
    }
}
