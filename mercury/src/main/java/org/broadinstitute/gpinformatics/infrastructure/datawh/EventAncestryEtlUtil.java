package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class relies on the heavy lifting done in LabEventEtl process which gathers and denormalizes
 * the [event]-[vessel/contained vessel]-[sample] data.
 *
 * It is intended to be constructed exactly once for each event being processed
 *
 * In the workflow configuration hierarchy:
 *    WorkflowConfig -> ProductWorkflowDef -> WorkflowProcessDef -> WorkflowProcessDefVersion -> WorkflowStepDef
 *
 * If the WorkflowStepDef for the event is not flagged with ancestryEtlFlag the event is ignored.
 *
 */
public class EventAncestryEtlUtil {

    public EventAncestryEtlUtil() {
    }

    // Avoid the repeated overhead of parsing workflow for the same event if it is not flagged for ancestry
    Set<Long> ignoreAncestryForEventList = new HashSet();

    // Avoid gathering ancestry for other rows for the same vessel.
    //    A vessel (e.g. pool) is duplicated for multiple samples
    Set<Long> ignoreForDuplicateVesselList = new HashSet();

    /**
     * Examines the event hierarchy for events flagged for ancestry ETL and appends ancestors to event facts.
     * Note:  Only appends ancestry to the first unique Event --> Vessel combination, ancestry is not denormalized at the sample level.
     * @param eventFacts Denormalized list of event facts generated for single event
     * @param labEvent The child (current) event (as opposed to ancestor event)
     * @param labVessel A vessel associated with the child (current) event
     */
    public void generateAncestryData( List<LabEventEtl.EventFactDto> eventFacts, LabEvent labEvent, LabVessel labVessel ){

        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig wfconfig = workflowLoader.load();

        Long previousEventId = -1L;
        List<WorkflowStepDef> workflowStepList = null;

        for( LabEventEtl.EventFactDto eventFact : eventFacts ) {

            // Don't bother with non-etl events
            if( !eventFact.canEtl() || labVessel == null ) {
                continue;
            }

            if( WorkflowConfigLookup.isSynthetic(eventFact.getWfName() ) ) {
                continue;
            }

            // Data is fully denormalized on Event-Vessel-Sample. Don't re-analyze the same event for ancestry flag
            if( ignoreAncestryForEventList.contains(eventFact.getEventId()) ) {
                continue;
            }

            // No need to keep rebuilding workflow for the same event ...
            if( !eventFact.getEventId().equals(previousEventId)) {
                workflowStepList = getWorkflowStepsUpToEvent(eventFact, wfconfig);
                previousEventId = eventFact.getEventId();
            }

            // Ignore events for which ancestry etl on current event is not flagged
            List<LabEventType> nextAncestorEventTypes = new ArrayList<>();
            if( workflowStepList.isEmpty() || ! workflowStepList.get( workflowStepList.size() - 1 ).doAncestryEtl() ) {
                // Skip any subsequent denormalized events
                ignoreAncestryForEventList.add( eventFact.getEventId() );
                continue;
            } else {
                // Find the next nearest ancestor event type(s)
                nextAncestorEventTypes.addAll( findAllAncestorEtlEventTypes(workflowStepList) );
            }

            if( !ignoreForDuplicateVesselList.contains(eventFact.getVesselId()) ) {
                eventFact.addAllAncestryDtos(buildAncestryFacts(labEvent, labVessel, nextAncestorEventTypes));
                ignoreForDuplicateVesselList.add( eventFact.getVesselId() );
            }
        }
    }

    /**
     * Navigate the event vessel ancestry and find the first ancestor event of the specified types
     * (workflow allows multiple event types at a step, hopefully only a single type if flagged for ancestry)
     * @param childLabEvent The child (current) event (as opposed to ancestor event)
     * @param childLabVessel A vessel associated with the child (current) event
     * @param nextAncestorEventTypes Event type(s) to search the ancestry for
     * @return Any ancestry facts found for the event-vessel combination, empty list if none
     */
    private List<AncestryFactDto> buildAncestryFacts(LabEvent childLabEvent, LabVessel childLabVessel,
                                                     List<LabEventType> nextAncestorEventTypes) {
        List<AncestryFactDto> ancestryFactDtos = new ArrayList<>();

        if( nextAncestorEventTypes.isEmpty() ) {
            return ancestryFactDtos;
        }

        Map<LabEvent, Set<LabVessel>> ancestorEventVesselMap = childLabVessel.findVesselsForLabEventTypes(
                nextAncestorEventTypes, Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors), true);

        for( LabEvent ancestorLabEvent : ancestorEventVesselMap.keySet() ) {
            for( LabVessel ancestorLabVessel : ancestorEventVesselMap.get(ancestorLabEvent) ) {
                if( ancestorLabVessel.getContainerRole() != null ) {
                    VesselContainer containerVessel = ancestorLabVessel.getContainerRole();
                    // Use embedder positions
                    VesselGeometry geometry = containerVessel.getEmbedder().getVesselGeometry();
                    for (VesselPosition vesselPosition : geometry.getVesselPositions()) {
                        LabVessel containedVessel = containerVessel.getVesselAtPosition(vesselPosition);
                        if( containedVessel != null ) {
                            ancestryFactDtos.add(new AncestryFactDto(
                                    ancestorLabEvent.getLabEventId(),
                                    containedVessel,
                                    ancestorLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                                    ancestorLabEvent.getEventDate(),
                                    childLabEvent.getLabEventId(),
                                    childLabVessel,
                                    childLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                                    childLabEvent.getEventDate()));
                        }
                    }
                } else {
                    // Vessel is not a container...
                    ancestryFactDtos.add(new AncestryFactDto(
                            ancestorLabEvent.getLabEventId(),
                            ancestorLabVessel,
                            ancestorLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                            ancestorLabEvent.getEventDate(),
                            childLabEvent.getLabEventId(),
                            childLabVessel,
                            childLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                            childLabEvent.getEventDate()));
                }
            }
        }

        return ancestryFactDtos;
    }

    /**
     * Given sequential workflow steps, walk backwards to find ancestry etl steps
     * @param workflowStepList The product workflow steps up to and including current step
     * @return The lab event types associated with ancestor steps in process with ancestry flagged
     *      , ordered from nearest to farthest ancestry
     */
    private List<LabEventType> findAllAncestorEtlEventTypes(List<WorkflowStepDef> workflowStepList) {
        List<LabEventType> eventTypes = new ArrayList<>();

        // Work backward for ancestor
        for( int i = workflowStepList.size() - 2; i >=0; i-- ) {
            if( workflowStepList.get(i).doAncestryEtl() ) {
                eventTypes.addAll(workflowStepList.get(i).getLabEventTypes());
            }
        }
        return eventTypes;
    }

    /**
     * Return a sequential list of workflow steps leading up to current event
     * @param eventFact Event and workflow data structure built by LabEventEtl
     * @param wfconfig Workflow configuration object model
     * @return The workflow steps leading up to the current event
     */
    private List<WorkflowStepDef> getWorkflowStepsUpToEvent( LabEventEtl.EventFactDto eventFact, WorkflowConfig wfconfig){

        Date eventEffectiveDate;
        if( eventFact.getBatchName().isEmpty() ) {
            eventEffectiveDate = eventFact.getBatchDate();
        } else {
            eventEffectiveDate = eventFact.getEventDate();
        }

        String workflowName = eventFact.getWfName();

        // Workflow step list
        List<WorkflowStepDef> workflowStepList = wfconfig.getSequentialWorkflowSteps( workflowName, eventEffectiveDate );

        int indexOfCurrentEvent = 0;

        LabEventType eventType = eventFact.getEventType();
        for( WorkflowStepDef workflowStepDef : workflowStepList ) {
            indexOfCurrentEvent++;
            if (workflowStepDef.getLabEventTypes().contains( eventType ) ){
                break;
            }
        }

        return workflowStepList.subList(0, indexOfCurrentEvent);
    }

    public class AncestryFactDto {

        private Object[] data;

        private static final int COL_ANCESTOR_EVENT_ID     = 0;
        private static final int COL_CHILD_EVENT_ID        = 1;
        private static final int COL_ANCESTOR_VESSEL_ID    = 2;
        private static final int COL_ANCESTOR_LIBRARY_NAME = 3;
        private static final int COL_ANCESTOR_CREATED      = 4;
        private static final int COL_CHILD_VESSEL_ID       = 5;
        private static final int COL_CHILD_LIBRARY_NAME    = 6;
        private static final int COL_CHILD_CREATED         = 7;

        public AncestryFactDto(Long ancestorEventId, LabVessel ancestorVessel, String ancestorLibraryTypeName,
                               Date ancestorLibraryCreated, Long childEventId, LabVessel childVessel,
                               String childLibraryTypeName, Date childLibraryCreated ){
            data = new Object[8];
            data[COL_ANCESTOR_EVENT_ID]     = ancestorEventId;
            data[COL_ANCESTOR_VESSEL_ID]    = ancestorVessel.getLabVesselId();
            data[COL_ANCESTOR_LIBRARY_NAME] = ancestorLibraryTypeName;
            data[COL_ANCESTOR_CREATED]      = ancestorLibraryCreated;
            data[COL_CHILD_EVENT_ID]        = childEventId;
            data[COL_CHILD_VESSEL_ID]       = childVessel.getLabVesselId();
            data[COL_CHILD_LIBRARY_NAME]    = childLibraryTypeName;
            data[COL_CHILD_CREATED]         = childLibraryCreated;
        }

        public Long getAncestorEventId(){
            return (Long) data[COL_ANCESTOR_EVENT_ID];
        }

        public Long getAncestorVesselId(){
            return (Long) data[COL_ANCESTOR_VESSEL_ID];
        }

        public String getAncestorLibraryTypeName(){
            return (String) data[COL_ANCESTOR_LIBRARY_NAME];
        }

        public Date getAncestorCreated(){
            return (Date) data[COL_ANCESTOR_CREATED];
        }

        public Long getChildEventId(){
            return (Long) data[COL_CHILD_EVENT_ID];
        }

        public Long getChildVesselId(){
            return (Long) data[COL_CHILD_VESSEL_ID];
        }

        public String getChildLibraryTypeName(){
            return (String) data[COL_CHILD_LIBRARY_NAME];
        }

        public Date getChildCreated(){
            return (Date) data[COL_CHILD_CREATED];
        }
    }

}
