package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable search definitions for various entities.
 */
public class SearchDefinitionFactory {

    // State of ConfigurableSearchDefinition does not change once created.
    private static Map<String, ConfigurableSearchDefinition> MAP_NAME_TO_DEF = new HashMap<>();

    /**
     * Convenience to allow users to just enter the number of the LCSET.
     */
    private static SearchTerm.Evaluator<Object> lcsetConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, SearchContext context) {
            String value = context.getSearchValueString();
            if( value.matches("[0-9]*")){
                value = "LCSET-" + value;
            }
            return value;
        }
    };

    /**
     * Convenience to allow users to just enter the number of the PDO.
     */
    private static SearchTerm.Evaluator<Object> pdoConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, SearchContext context) {
            String value = context.getSearchValueString();
            if( value.matches("[0-9]*")){
                value = "PDO-" + value;
            }
            return value;
        }
    };

    private SearchDefinitionFactory(){}

    static {
        SearchDefinitionFactory fact = new SearchDefinitionFactory();
        fact.buildLabEventSearchDef();
        fact.buildLabVesselSearchDef();
        fact.buildMercurySampleSearchDef();
        fact.buildReagentSearchDef();
        fact.buildLabMetricSearchDef();
    }

    public static ConfigurableSearchDefinition getForEntity(String entity) {
        /* **** Change condition to true during development to rebuild for JVM hot-swap changes **** */
        //noinspection ConstantIfStatement
        if( false ) {
            SearchDefinitionFactory fact = new SearchDefinitionFactory();
            fact.buildLabEventSearchDef();
            fact.buildLabVesselSearchDef();
            fact.buildMercurySampleSearchDef();
            fact.buildReagentSearchDef();
            fact.buildLabMetricSearchDef();
        }

        return MAP_NAME_TO_DEF.get(entity);
    }

    private void buildLabVesselSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabVesselSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_VESSEL.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabEventSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabEventSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_EVENT.getEntityName(), configurableSearchDefinition);
    }

    private void buildMercurySampleSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new MercurySampleSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.MERCURY_SAMPLE.getEntityName(), configurableSearchDefinition);
    }

    private void buildReagentSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new ReagentSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.REAGENT.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabMetricSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabMetricSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_METRIC.getEntityName(), configurableSearchDefinition);
    }

    static SearchTerm.Evaluator<Object> getLcsetInputConverter(){
        return lcsetConverter;
    }

    static SearchTerm.Evaluator<Object> getPdoInputConverter(){
        return pdoConverter;
    }

    /**
     * Shared logic to extract the type of any lab vessel
     */
    static String findVesselType( LabVessel vessel ) {
        String vesselTypeName;
        switch( vessel.getType() ) {
        case STATIC_PLATE:
            StaticPlate p = OrmUtil.proxySafeCast(vessel, StaticPlate.class);
            vesselTypeName = p.getPlateType()==null?"":p.getPlateType().getAutomationName();
            break;
        case TUBE_FORMATION:
            TubeFormation tf = OrmUtil.proxySafeCast(vessel, TubeFormation.class );
            vesselTypeName = tf.getRackType()==null?"":tf.getRackType().getDisplayName();
            break;
        case RACK_OF_TUBES:
            RackOfTubes rot = OrmUtil.proxySafeCast(vessel, RackOfTubes.class );
            vesselTypeName = rot.getRackType()==null?"":rot.getRackType().getDisplayName();
            break;
        default:
            // Not sure of others for in-place vessels
            vesselTypeName = vessel.getType().getName();
        }
        return vesselTypeName;
    }


    /**
     * Shared value list of all lab event types.
     */
    static class EventTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {
        @Override
        public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
            List<ConstrainedValue> constrainedValues = new ArrayList<>();
            for (LabEventType labEventType : LabEventType.values()) {
                constrainedValues.add(new ConstrainedValue(labEventType.toString(), labEventType.getName()));
            }
            Collections.sort(constrainedValues);
            return constrainedValues;
        }
    }

    /**
     * Shared conversion of input String to LabEventType enumeration value
     */
    static class EventTypeValueConversionExpression extends SearchTerm.Evaluator<Object> {
        @Override
        public LabEventType evaluate(Object entity, SearchContext context) {
            return Enum.valueOf(LabEventType.class, context.getSearchValueString());
        }
    }

    /**
     * Shared display expression for sample metadata (supports LabVessel and MercurySample)
     * Methods must remain thread-safe
     */
    static class SampleMetadataDisplayExpression extends SearchTerm.Evaluator<Object> {

        // Build a quick way to lookup metadata key by display name
        private static Map<String,Metadata.Key> KEY_MAP = new HashMap<>();

        static{
            for(Metadata.Key key : Metadata.Key.values() ){
                if( key.getCategory() == Metadata.Category.SAMPLE ) {
                    KEY_MAP.put(key.getDisplayName(), key);
                }
            }
        }

        /**
         * Locates sample metadata values by navigating back in vessel/event hierarchy using SampleInstanceV2 logic
         * Shared by LabEvent, LabVessel, and MercurySample display code
         * @param entity  Can be an instance of LabVessel, LabEvent, or MercurySample depending on which search type
         *                this shared SampleMetadataDisplayExpression is used with
         * @param context Any named objects supplied by call stack
         */
        @Override
        public Set<String> evaluate(Object entity, SearchContext context) {
            SearchTerm searchTerm = context.getSearchTerm();
            String metaName = searchTerm.getName();

            if( entity instanceof LabVessel) {
                // Samples from LabVessel search
                LabVessel labVessel = (LabVessel) entity;
                return getMetadataFromVessel(labVessel, metaName);

            } else if(entity instanceof LabEvent) {
                // Samples from LabEvent search
                LabEvent labEvent = (LabEvent) entity;
                LabVessel labVessel = labEvent.getInPlaceLabVessel();
                if (labVessel != null) {
                    return getMetadataFromVessel(labVessel, metaName);
                } else {
                    Set<String> results = new HashSet<>();
                    for( LabVessel srcVessel : labEvent.getSourceLabVessels() ) {
                        results.addAll(getMetadataFromVessel(srcVessel, metaName));
                    }
                    return results;
                }
            } else {
                // Sample from MercurySample search
                MercurySample sample = (MercurySample) entity;

                // Material type comes from event
                if( metaName.equals(Metadata.Key.MATERIAL_TYPE.getDisplayName())
                        && sample.getLabVessel().iterator().hasNext()) {
                    return getMetadataFromVessel( sample.getLabVessel().iterator().next(), metaName );
                }

                Set<String> results = new HashSet<>();
                String value = getSampleMetadataForDisplay(sample, metaName);
                if( value != null && !value.isEmpty() ) {
                    results.add(value);
                }
                return results;
            }
        }

        private String getSampleMetadataForDisplay( MercurySample sample, String metaName ){
            String value = null;
            Set<Metadata> metadata = sample.getMetadata();
            if( metadata != null && !metadata.isEmpty() ) {
                Metadata.Key key = KEY_MAP.get(metaName);
                for( Metadata meta : metadata){
                    if( meta.getKey() == key ) {
                        value = meta.getValue();
                        // Assume only one metadata type (e.g. Gender, Sample ID) per sample.
                        break;
                    }
                }
            }

            return value;
        }

        private Set<String> getMetadataFromVessel( LabVessel labVessel, String metaName ) {
            String metaValue;
            Set<String> results = new HashSet<>();

            // Material type should come from event, not sample
            if( metaName.equals(Metadata.Key.MATERIAL_TYPE.getDisplayName())) {
                MaterialType materialType = labVessel.getLatestMaterialTypeFromEventHistory();
                if( materialType != null && materialType != MaterialType.NONE ) {
                    results.add(materialType.getDisplayName());
                    return results;
                }
            }

            // A vessel can end up with more than 1 sample in it
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
                if (sample != null) {
                    metaValue = getSampleMetadataForDisplay(sample, metaName);
                    if( metaValue != null ) {
                        results.add(metaValue);
                    }
                }
            }
            return results;
        }
    }

}
