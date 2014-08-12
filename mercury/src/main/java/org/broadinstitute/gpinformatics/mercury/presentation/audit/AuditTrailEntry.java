package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import java.util.List;

/**
 * Represents one entity modification, addition, or deletion.  In the case of modifications, only the differences
 * are contained in previousEntity and entity, and the column names will have only those columns that were modified.
 * For additions, previousEntity will be null.  For deletions, previousEntity is non-null and entity is null.
 */
public class AuditTrailEntry {
    private List<String> columnNames;
    private AuditEntity previousEntity;
    private AuditEntity entity;

    public AuditTrailEntry(List<String> columnNames,
                           AuditEntity previousEntity,
                           AuditEntity entity) {
        this.columnNames = columnNames;
        this.previousEntity = previousEntity;
        this.entity = entity;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public AuditEntity getPreviousEntity() {
        return previousEntity;
    }

    public AuditEntity getEntity() {
        return entity;
    }
}
