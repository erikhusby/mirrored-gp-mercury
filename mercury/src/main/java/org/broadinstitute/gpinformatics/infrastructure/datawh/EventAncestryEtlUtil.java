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
 * In the workflow configuration hierarchy:
 *    WorkflowConfig -> ProductWorkflowDef -> WorkflowProcessDef -> WorkflowProcessDefVersion -> WorkflowStepDef
 *
 * If the WorkflowStepDef for the event is not flagged with ancestryEtlFlag the event is ignored.
 *
 */
public class EventAncestryEtlUtil {

    public EventAncestryEtlUtil() {
    }

    /**
     * Examines the event hierarchy for events flagged for ancestry ETL and appends ancestors to event facts.
     * Note:  Only appends ancestry to the first unique Event --> Vessel combination, ancestry is not denormalized at the sample level.
     * @param eventFacts A list of event fact entries to (conditionally) append ancestry to
     */
    public void generateAncestryData( List<LabEventEtl.EventFactDto> eventFacts ){

        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig wfconfig = workflowLoader.load();

        // Avoid the repeated overhead of parsing workflow for the same event if it is not flagged for ancestry
        Set<Long> ignoreAncestryForEventList = new HashSet();

        // Avoid gathering ancestry for other rows for the same vessel (a vessel is duplicated for multiple samples)
        Set<Long> ignoreForDuplicateVesselList = new HashSet();

        Long previousEventId = -1L;
        List<WorkflowStepDef> workflowStepList = null;

        for( LabEventEtl.EventFactDto eventFact : eventFacts ) {

            // Don't bother with non-etl events
            if( !eventFact.canEtl() || eventFact.getLabVessel() == null ) {
                continue;
            }

            // Data is fully denormalized on Event-Vessel-Sample. Don't re-analyze the same event for ancestry flag
            if( ignoreAncestryForEventList.contains(eventFact.getLabEvent().getLabEventId()) ) {
                continue;
            }

            // No need to keep rebuilding workflow for the same event ...
            if( !eventFact.getLabEvent().getLabEventId().equals(previousEventId)) {
                workflowStepList = getWorkflowStepsUpToEvent(eventFact, wfconfig);
                previousEventId = eventFact.getLabEvent().getLabEventId();
            }

            // Ignore events for which ancestry etl on current event is not flagged
            List<LabEventType> nextAncestorEventTypes = new ArrayList<>();
            if( workflowStepList.isEmpty() || ! workflowStepList.get( workflowStepList.size() - 1 ).doAncestryEtl() ) {
                // Skip any subsequent denormalized events
                ignoreAncestryForEventList.add( eventFact.getLabEvent().getLabEventId() );
                continue;
            } else {
                // Find the next nearest ancestor event type(s)
                nextAncestorEventTypes.addAll( findNearestAncestorEventType(workflowStepList) );
            }

            if( !ignoreForDuplicateVesselList.contains(eventFact.getLabVessel().getLabVesselId() ) ) {
                eventFact.addAllAncestryDtos(buildAncestryFacts(eventFact, nextAncestorEventTypes));
                ignoreForDuplicateVesselList.add( eventFact.getLabVessel().getLabVesselId() );
            }
        }
    }

    /**
     * Navigate the event vessel ancestry and find the first ancestor event of the specified types
     * (workflow allows multiple event types at a step, hopefully only a single type if flagged for ancestry)
     * @param eventFact Objects related to the child event
     * @param nextAncestorEventTypes Event type(s) to search the ancestry for
     * @return Any ancestry facts found for the event-vessel combination, empty list if none
     */
    private List<AncestryFactDto> buildAncestryFacts(LabEventEtl.EventFactDto eventFact,
                                                     List<LabEventType> nextAncestorEventTypes) {
        List<AncestryFactDto> ancestryFactDtos = new ArrayList<>();

        if( nextAncestorEventTypes.isEmpty() ) {
            return ancestryFactDtos;
        }

        LabVessel childLabVessel = eventFact.getLabVessel();
        LabEvent childLabEvent = eventFact.getLabEvent();
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
                                    childLabVessel,
                                    childLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                                    childLabEvent.getEventDate()));
                        }
                    }
                } else {
                    ancestryFactDtos.add(new AncestryFactDto(
                            ancestorLabEvent.getLabEventId(),
                            ancestorLabVessel,
                            ancestorLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                            ancestorLabEvent.getEventDate(),
                            childLabVessel,
                            childLabEvent.getLabEventType().getLibraryType().getDisplayName(),
                            childLabEvent.getEventDate()));
                }
            }
        }

        return ancestryFactDtos;
    }

    /**
     * Given sequential workflow steps, walk backwards until the first ancestry etl step is encountered
     * @param workflowStepList The product workflow steps up to and including current step
     * @return The lab event types associated with first ancestor step in process with ancestry flagged
     */
    private List<LabEventType> findNearestAncestorEventType(List<WorkflowStepDef> workflowStepList) {
        List<LabEventType> eventTypes;

        // Work backward for ancestor
        for( int i = workflowStepList.size() - 2; i >=0; i-- ) {
            if( workflowStepList.get(i).doAncestryEtl() ) {
                return workflowStepList.get(i).getLabEventTypes();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Return a sequential list of workflow steps leading up to current event
     * @param eventFact Event and workflow data structure built by LabEventEtl
     * @param wfconfig Workflow configuration object model
     * @return The workflow steps leading up to the current event
     */
    private List<WorkflowStepDef> getWorkflowStepsUpToEvent( LabEventEtl.EventFactDto eventFact, WorkflowConfig wfconfig){

        Date eventEffectiveDate;
        if( eventFact.getLabEvent().getLabBatch() != null ) {
            eventEffectiveDate = eventFact.getLabEvent().getLabBatch().getCreatedOn();
        } else {
            eventEffectiveDate = eventFact.getLabEvent().getEventDate();
        }

        String workflowName = eventFact.getWfDenorm().getProductWorkflowName();

        // Workflow step list
        List<WorkflowStepDef> workflowStepList = wfconfig.getSequentialWorkflowSteps( workflowName, eventEffectiveDate );

        int indexOfCurrentEvent = 0;

        LabEventType eventType = eventFact.getLabEvent().getLabEventType();
        for( WorkflowStepDef workflowStepDef : workflowStepList ) {
            indexOfCurrentEvent++;
            if (workflowStepDef.getLabEventTypes().contains( eventType ) ){
                break;
            }
        }

        return workflowStepList.subList(0, indexOfCurrentEvent);
    }

    public class AncestryFactDto {
        private Long ancestorEventId;
        private LabVessel ancestorVessel;
        private LabVessel childVessel;
        private String ancestorLibraryTypeName;
        private String childLibraryTypeName;
        private Date childLibraryCreated;
        private Date  ancestorLibraryCreated;

        public AncestryFactDto(Long ancestorEventId, LabVessel ancestorVessel, String ancestorLibraryTypeName,
                               Date ancestorLibraryCreated, LabVessel childVessel,
                               String childLibraryTypeName, Date childLibraryCreated ){
            this.ancestorEventId = ancestorEventId;
            this.ancestorVessel = ancestorVessel;
            this.ancestorLibraryTypeName = ancestorLibraryTypeName;
            this.ancestorLibraryCreated = ancestorLibraryCreated;
            this.childVessel = childVessel;
            this.childLibraryTypeName = childLibraryTypeName;
            this.childLibraryCreated = childLibraryCreated;
        }

        public Long getAncestorEventId(){
            return ancestorEventId;
        }

        public LabVessel getAncestorVessel(){
            return ancestorVessel;
        }

        public LabVessel getChildVessel(){
            return childVessel;
        }
        public String getAncestorLibraryTypeName(){
            return ancestorLibraryTypeName;
        }

        public String getChildLibraryTypeName(){
            return childLibraryTypeName;
        }
        public Date getAncestorCreated(){
            return ancestorLibraryCreated;
        }

        public Date getChildCreated(){
            return childLibraryCreated;
        }
    }

}
