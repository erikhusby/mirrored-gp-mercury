package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This traversal evaluator gets vessels associated with specific ancestor and/or descendant event types as specified by user
 */
public class VesselByEventTypeTraversalEvaluator extends CustomTraversalEvaluator {

    public VesselByEventTypeTraversalEvaluator(){
        super("Vessels by Event Type", "vesselEventTypeTraverser", new ParamConfiguration() );
    }

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, TransferTraverserCriteria.TraversalDirection traversalDirection, SearchInstance searchInstance) {

        ResultParamValues resultParamValues = searchInstance.getCustomTraversalOptionParams();
        if( resultParamValues == null || resultParamValues.getMultiValues(ParamConfiguration.DataField.EVENT_TYPE.getFormFieldId()).size() == 0 ) {
            throw new IllegalStateException("No user defined parameters provided with " + getLabel() + " traverser");
        }

        Set<Object> searchResultVessels = new TreeSet<>( new Comparator() {
            @Override
            public int compare(Object first, Object second) {
                return ((LabVessel)first).getLabel().compareTo(((LabVessel)second).getLabel());
            }
        });

        List<LabEventType> eventTypes = new ArrayList<>();
        for( String typeName : resultParamValues.getMultiValues(ParamConfiguration.DataField.EVENT_TYPE.getFormFieldId())) {
            eventTypes.add(LabEventType.valueOf(typeName));
        }

        // If the user didn't choose any events, don't waste time traversing
        if( eventTypes.isEmpty() ) {
            return searchResultVessels;
        }

        boolean isTargetVesselSelected = "target".equals( resultParamValues.getSingleValue( ParamConfiguration.DataField.SRC_OR_TARGET.getFormFieldId() ) );
        boolean isStopAtFirstFind = "captureNearest".equals( resultParamValues.getSingleValue( ParamConfiguration.DataField.NEAREST.getFormFieldId() ) );

        PaginationUtil.Pagination pagination = searchInstance.getEvalContext().getPagination();

        for( LabVessel startingVessel : (List<LabVessel>) rootEntities ) {

            TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                    = new TransferTraverserCriteria.VesselForEventTypeCriteria(eventTypes, isTargetVesselSelected, isStopAtFirstFind);

            // Initialize with starting vessel = starting vessel
            pagination.addExtraIdInfo( startingVessel.getLabel(), startingVessel.getLabel() );

            VesselContainer<?> vesselContainer = startingVessel.getContainerRole();
            if( vesselContainer != null ) {
                startingVessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria, traversalDirection);
            } else {
                startingVessel.evaluateCriteria(eventTypeCriteria, traversalDirection);
            }

            for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
                for( LabVessel eventVessel : eventEntry.getValue() ) {
                    searchResultVessels.add( eventVessel );
                    // Handle starting vessel
                    pagination.addExtraIdInfo( eventVessel.getLabel(), startingVessel.getLabel() );
                }
            }
        }

        return searchResultVessels;
    }

    @Override
    public List<Object> buildEntityIdList(Set<? extends Object> entities) {
        List<Object> idList = new ArrayList<>();

        // Filter out the containers, otherwise we get the DNA plate container in addition to the wells
        for( LabVessel vessel : (Set<LabVessel>) entities ) {
            idList.add(vessel.getLabel());
        }

        return idList;
    }


    /**
     * The configuration for the user selectable parameters tied to this traverser <br />
     * Used to produce UI configuration via SearchTerm.getResultParamConfigurationExpression. (All input value included) <br />
     * Used to store value for use in SearchTerm.getDisplayValueExpression, input value not included. (ParamInput names tightly coupled to a specific displayValueExpression)
     */
    public static class ParamConfiguration extends ResultParamConfiguration {

        public enum DataField {
            EVENT_TYPE("eventTypes", InputType.MULTI_PICKLIST, "Capture Vessel(s) for Event Type(s)"),
            SRC_OR_TARGET("srcOrTarget", InputType.RADIO, "Capture" ),
            NEAREST("captureNearest", InputType.CHECKBOX, "Capture Vessel(s) for Nearest Event Only");

            private String formFieldId;
            private InputType inputType;
            private String label;

            private DataField( String formFieldId, InputType inputType, String label ) {
                this.formFieldId = formFieldId;
                this.inputType = inputType;
                this.label = label;
            }

            public String getFormFieldId() {
                return formFieldId;
            }

            public InputType getInputType() {
                return inputType;
            }

            public String getLabel() {
                return label;
            }
        }

        public ParamConfiguration() {
            ParamInput captureNearestInput = new ParamInput(DataField.NEAREST.getFormFieldId(),DataField.NEAREST.getInputType(), DataField.NEAREST.getLabel());
            addParamInput(captureNearestInput);

            ParamInput srcTargetInput = new ParamInput(DataField.SRC_OR_TARGET.getFormFieldId(),DataField.SRC_OR_TARGET.getInputType(), DataField.SRC_OR_TARGET.getLabel());
            List<ConstrainedValue> options = new ArrayList<>();
            options.add(new ConstrainedValue("target", "Event Target Vessel(s)"));
            options.add(new ConstrainedValue("source", "Event Source Vessel(s)"));
            srcTargetInput.addDefaultValue("target");
            srcTargetInput.setOptionItems(options);
            addParamInput(srcTargetInput);

            ParamInput eventTypeInput = new ParamInput(DataField.EVENT_TYPE.getFormFieldId(),DataField.EVENT_TYPE.getInputType(), DataField.EVENT_TYPE.getLabel());
            eventTypeInput.setOptionItems(SearchDefinitionFactory.EventTypeValuesExpression.getConstrainedValues());
            addParamInput(eventTypeInput);

        }
    }

}