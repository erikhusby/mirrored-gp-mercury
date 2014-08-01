package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * Enumeration of the entities for which configurable columns have been created.
 */
public enum ColumnEntity {
    LAB_VESSEL("LabVessel", "Lab Vessel", new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabVessel) entity).getLabel();
        }
    }),
    LAB_EVENT("LabEvent", "Lab Event", new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabEvent) entity).getLabEventId().toString();
        }
    })
//    ,
//    REAGENT("Reagent", "Reagent", new IdGetter() {
//        @Override
//        public String getId(Object entity) {
//            return ((Reagent) entity).getReagentId().toString();
//        }
//    })
    ;

    private IdGetter idGetter;
    private String entityName, displayName;

    ColumnEntity(String entityName, String displayName, IdGetter idGetter) {
        this.entityName = entityName;
        this.idGetter = idGetter;
        this.displayName = displayName;
    }

    public interface IdGetter {
        String getId(Object entity);
    }

    public String getDisplayName(){
        return displayName;
    }

    public String getEntityName(){
        return entityName;
    }

    public IdGetter getIdGetter() {
        return idGetter;
    }

    public static ColumnEntity getByName(String entityName) {
        for (ColumnEntity columnEntity : ColumnEntity.values()) {
            if (columnEntity.entityName.equals(entityName)) {
                return columnEntity;
            }
        }
        throw new RuntimeException("ColumnEntity not found for " + entityName);
    }
}
