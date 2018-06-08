package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User selectable result column configurations for when a constrainedResultParamsExpression is attached <br />
 * Used to produce UI configuration via SearchTerm.getResultParamConfigurationExpression. (All input value included) <br />
 * Used to store value for use in SearchTerm.getDisplayValueExpression, input value not included. (ParamInput names tightly coupled to a specific displayValueExpression)
 */
public class VesselEventResultParamConfiguration extends ResultParamConfiguration {
    
    public enum ResultEventData{
        BARCODE,
        POSITION,
        DATE
    }

    public VesselEventResultParamConfiguration() {
        ResultParamConfiguration.ParamInput captureNearestInput = new ResultParamConfiguration.ParamInput("captureNearest", ResultParamConfiguration.InputType.CHECKBOX, "Capture Vessel(s) for Nearest Event Only");
        addParamInput(captureNearestInput);

        ResultParamConfiguration.ParamInput srcTargetInput = new ResultParamConfiguration.ParamInput("srcOrTarget", ResultParamConfiguration.InputType.RADIO, "Capture");
        List<ConstrainedValue> options = new ArrayList<>();
        options.add(new ConstrainedValue("target", "Event Target Vessel(s)"));
        options.add(new ConstrainedValue("source", "Event Source Vessel(s)"));
        srcTargetInput.addDefaultValue("target");
        srcTargetInput.setOptionItems(options);
        addParamInput(srcTargetInput);

        ResultParamConfiguration.ParamInput eventTypeInput = new ResultParamConfiguration.ParamInput("eventTypes", ResultParamConfiguration.InputType.MULTI_PICKLIST, "Capture Vessel(s) for Event Type(s)");
        eventTypeInput.setOptionItems(SearchDefinitionFactory.EventTypeValuesExpression.getConstrainedValues());
        addParamInput(eventTypeInput);
        
    }

    public static Set<String> getDisplayValue(Object entity, SearchContext context, ResultEventData resultEventData) {
        Set<String> results = new HashSet<>();
        ResultParamValues columnParams = context.getColumnParams();
        if( columnParams == null ) {
            results.add("(Params required)");
            return results;
        }
        List<LabEventType> eventTypes = new ArrayList<>();
        for(ResultParamValues.ParamValue value : columnParams.getParamValues() ) {
            if( !value.getName().equals("eventTypes")) {
                continue;
            } else {
                try {
                    LabEventType type = LabEventType.valueOf(value.getValue());
                    eventTypes.add(type);
                } catch (Exception ex) {
                    results.clear();
                    results.add("(No event type for param: " + value.getValue() + ")");
                    return results;
                }
            }
        }

        boolean captureTarget = true;  // Default
        for(ResultParamValues.ParamValue value : columnParams.getParamValues() ) {
            if (value.getName().equals("srcOrTarget")) {
                if ("source".equals(value.getValue())) {
                    captureTarget = false;
                }
                break;
            }
        }

        boolean captureNearest = false; // Default
        for(ResultParamValues.ParamValue value : columnParams.getParamValues() ) {
            if (value.getName().equals("captureNearest")) {
                captureNearest = true;
                break;
            }
        }

        LabVessel labVessel = (LabVessel) entity;

        LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(eventTypes,captureNearest,captureTarget);
        if( resultEventData == ResultEventData.DATE) {
            eval.captureAllEvents();
        }

        if( labVessel.getContainerRole() != null ) {
            labVessel.getContainerRole().applyCriteriaToAllPositions(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
        } else {
            labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
        }

        switch( resultEventData ) {
            case BARCODE:
                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    results.add(labVesselAndPositions.getKey().getLabel());
                }
                break;
            case POSITION:
                for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions
                        : eval.getPositions().asMap().entrySet()) {
                    for (VesselPosition position : labVesselAndPositions.getValue()) {
                        results.add(position.toString());
                    }
                }
                break;
            case DATE:
                for (LabEvent event : eval.getAllEvents()) {
                    results.add(ColumnValueType.DATE.format(event.getEventDate(), ""));
                }
                break;
        }

        return results;
    }
}
