package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Shared functionality for mercury search definitions
 */
public abstract class EntitySearchDefinition {

    /**
     * Convenience to allow users to just enter the number of the LCSET.
     */
    private SearchTerm.Evaluator<Object> lcsetConverter = new SearchTerm.Evaluator<Object>() {
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
    private SearchTerm.Evaluator<Object> pdoConverter = new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, Map<String, Object> context) {
            String value = (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING);
            if( value.matches("[0-9]*")){
                value = "PDO-" + value;
            }
            return value;
        }
    };

    protected SearchTerm.Evaluator<Object> getLcsetInputConverter(){
        return lcsetConverter;
    }

    protected SearchTerm.Evaluator<Object> getPdoInputConverter(){
        return pdoConverter;
    }

    public abstract ConfigurableSearchDefinition buildSearchDefinition();


    /**
     * Shared logic to extract the type of any lab vessel
     * @param vessel
     * @return
     */
    protected String findVesselType( LabVessel vessel ) {
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
    protected class EventTypeValuesExpression extends SearchTerm.Evaluator<List<ConstrainedValue>> {
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
    protected class EventTypeValueConversionExpression extends SearchTerm.Evaluator<Object> {
        @Override
        public Object evaluate(Object entity, Map<String, Object> context) {
            return Enum.valueOf(LabEventType.class, (String) context.get(SearchInstance.CONTEXT_KEY_SEARCH_STRING));
        }
    }
}
