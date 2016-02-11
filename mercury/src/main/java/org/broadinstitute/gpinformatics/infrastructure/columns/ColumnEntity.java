package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * Enumeration of the entities for which configurable columns have been created.
 */
public enum ColumnEntity {
    LAB_VESSEL("LabVessel", "Lab Vessel", "label", LabVessel.class,
            new IdGetter() {
                @Override
                public String getId(Object entity) {
                    return ((LabVessel) entity).getLabel();
                }
            },
            PreferenceType.GLOBAL_LAB_VESSEL_COLUMN_SETS, PreferenceType.USER_LAB_VESSEL_COLUMN_SETS,
            new PreferenceType[]{PreferenceType.GLOBAL_LAB_VESSEL_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_VESSEL_SEARCH_INSTANCES}),
    LAB_EVENT("LabEvent", "Lab Event", "labEventId", LabEvent.class,
            new IdGetter() {
                @Override
                public String getId(Object entity) {
                    return ((LabEvent) entity).getLabEventId().toString();
                }
            },
            PreferenceType.GLOBAL_LAB_EVENT_COLUMN_SETS, PreferenceType.USER_LAB_EVENT_COLUMN_SETS,
            new PreferenceType[]{PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_EVENT_SEARCH_INSTANCES}),
    MERCURY_SAMPLE("MercurySample", "Mercury Sample", "mercurySampleId", MercurySample.class,
            new IdGetter() {
                @Override
                public String getId(Object entity) {
                    return ((MercurySample) entity).getMercurySampleId().toString();
                }
            },
            PreferenceType.GLOBAL_MERCURY_SAMPLE_COLUMN_SETS, PreferenceType.USER_MERCURY_SAMPLE_COLUMN_SETS,
            new PreferenceType[]{PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES,
                    PreferenceType.USER_MERCURY_SAMPLE_SEARCH_INSTANCES}),
    REAGENT("Reagent", "Reagent", "reagentId", Reagent.class,
            new IdGetter() {
                @Override
                public String getId(Object entity) {
                    return ((Reagent) entity).getReagentId().toString();
                }
            },
            PreferenceType.GLOBAL_REAGENT_COLUMN_SETS, PreferenceType.USER_REAGENT_COLUMN_SETS,
            new PreferenceType[]{PreferenceType.GLOBAL_REAGENT_SEARCH_INSTANCES,
                    PreferenceType.USER_REAGENT_SEARCH_INSTANCES}),
    LAB_METRIC("LabMetric", "Lab Metric", "labMetricId", LabMetric.class,
            new IdGetter() {
                @Override
                public String getId(Object entity) {
                    return ((LabMetric) entity).getLabMetricId().toString();
                }
            },
            PreferenceType.GLOBAL_LAB_METRIC_COLUMN_SETS, PreferenceType.USER_LAB_METRIC_COLUMN_SETS,
            new PreferenceType[]{PreferenceType.GLOBAL_LAB_METRIC_SEARCH_INSTANCES,
                    PreferenceType.USER_LAB_METRIC_SEARCH_INSTANCES});

    private IdGetter idGetter;
    private String entityName;
    private String displayName;
    private String entityIdProperty;
    private Class entityClass;
    private PreferenceType globalColumnSets;
    private PreferenceType userColumnSets;
    private PreferenceType[] searchInstancePrefs;

    ColumnEntity(String entityName, String displayName, String entityIdProperty, Class clazz, IdGetter idGetter,
            PreferenceType globalColumnSets, PreferenceType userColumnSets, PreferenceType[] searchInstancePrefs) {
        this.entityName = entityName;
        this.entityIdProperty = entityIdProperty;
        this.entityClass = clazz;
        this.idGetter = idGetter;
        this.displayName = displayName;
        this.globalColumnSets = globalColumnSets;
        this.userColumnSets = userColumnSets;
        this.searchInstancePrefs = searchInstancePrefs;
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

    public PreferenceType getGlobalColumnSets() {
        return globalColumnSets;
    }

    public PreferenceType getUserColumnSets() {
        return userColumnSets;
    }

    public PreferenceType[] getSearchInstancePrefs() {
        return searchInstancePrefs;
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
