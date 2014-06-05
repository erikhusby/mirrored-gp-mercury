package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * Enumeration of the entities for which configurable columns have been created.
 */
public enum ColumnEntity {
    LAB_VESSEL("LabVessel", new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabVessel) entity).getLabel();
        }
    });

    private IdGetter idGetter;
    private String displayName;

    ColumnEntity(String displayName, IdGetter idGetter) {
        this.displayName = displayName;
        this.idGetter = idGetter;
    }

    public interface IdGetter {
        String getId(Object entity);
    }

    public IdGetter getIdGetter() {
        return idGetter;
    }

    public static ColumnEntity getByName(String displayName) {
        for (ColumnEntity columnEntity : ColumnEntity.values()) {
            if (columnEntity.displayName.equals(displayName)) {
                return columnEntity;
            }
        }
        throw new RuntimeException("ColumnEntity not found for " + displayName);
    }
}
