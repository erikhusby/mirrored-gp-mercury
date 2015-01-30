package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * Enumeration of the entities for which configurable columns have been created.
 */
public enum ColumnEntity {
    LAB_VESSEL("LabVessel", "Lab Vessel", "label", LabVessel.class, new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabVessel) entity).getLabel();
        }
    }),
    LAB_EVENT("LabEvent", "Lab Event", "labEventId", LabEvent.class, new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((LabEvent) entity).getLabEventId().toString();
        }
    }),
    MERCURY_SAMPLE("MercurySample", "Mercury Sample", "mercurySampleId", MercurySample.class, new IdGetter() {
        @Override
        public String getId(Object entity) {
            return ((MercurySample) entity).getMercurySampleId().toString();
        }
    });

    private IdGetter idGetter;
    private String entityName, displayName, entityIdProperty;
    private Class entityClass;

    ColumnEntity(String entityName, String displayName, String entityIdProperty, Class clazz, IdGetter idGetter) {
        this.entityName = entityName;
        this.entityIdProperty = entityIdProperty;
        this.entityClass = clazz;
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

    public String getEntityIdProperty(){
        return entityIdProperty;
    }

    public Class getEntityClass(){
        return entityClass;
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
