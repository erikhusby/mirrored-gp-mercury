package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
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
        public Object evaluate(Object entity, Map<String, Object> context) {
            String value = (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING);
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
        public Object evaluate(Object entity, Map<String, Object> context) {
            String value = (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING);
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
    }

    public static ConfigurableSearchDefinition getForEntity(String entity) {
        /* **** Change condition to true during development to rebuild for JVM hot-swap changes **** */
        if( false ) {
            SearchDefinitionFactory fact = new SearchDefinitionFactory();
            fact.buildLabEventSearchDef();
            fact.buildLabVesselSearchDef();
            fact.buildMercurySampleSearchDef();
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

    static SearchTerm.Evaluator<Object> getLcsetInputConverter(){
        return lcsetConverter;
    }

    static SearchTerm.Evaluator<Object> getPdoInputConverter(){
        return pdoConverter;
    }

    /**
     * Shared logic to extract the type of any lab vessel
     * @param vessel
     * @return
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
            vesselTypeName = vessel.getType()==null?"":vessel.getType().getName();
        }
        return vesselTypeName;
    }


    /**
     * Shared value list of all lab event types.
     */
    static class EventTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {
        @Override
        public List<ConstrainedValue> evaluate(Object entity, Map<String, Object> context) {
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
        public LabEventType evaluate(Object entity, Map<String, Object> context) {
            return Enum.valueOf(LabEventType.class, (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
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
         * @param entity
         * @param context
         * @return
         */
        @Override
        public Set<String> evaluate(Object entity, Map<String, Object> context) {
            SearchTerm searchTerm = (SearchTerm) context.get(SearchInstance.CONTEXT_KEY_SEARCH_TERM);
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
                Set<String> results = new HashSet<>();
                MercurySample sample = (MercurySample) entity;
                results.add(getSampleMetadataForDisplay(sample, metaName));
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
