package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
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
    private WorkflowConfig workflowConfig;

    // Avoid the repeated overhead of parsing workflow for the same event if it is not flagged for ancestry
    Set<Long> ignoreAncestryForEventList = new HashSet();

    @Inject
    public EventAncestryEtlUtil(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    /**
     * Examines the event hierarchy for events flagged for ancestry ETL and appends ancestors to event facts.
     * Note:  Only appends ancestry to the first unique Event --> Vessel combination, ancestry is not denormalized at the sample level.
     * @param eventFacts Denormalized list of event facts generated for single event
     * @param labEvent The child (current) event (as opposed to ancestor event)
     * @param labVessel A vessel associated with the child (current) event
     */
    public void generateAncestryData( List<LabEventEtl.EventFactDto> eventFacts, LabEvent labEvent, LabVessel labVessel ){

        Long previousEventId = -1L;
        List<WorkflowStepDef> workflowStepList = null;

        // Skip duplicate entries for same vessel (e.g. static plate)
        Set<LabVessel> vesselsToSkip = new HashSet<>();

        for( LabEventEtl.EventFactDto eventFact : eventFacts ) {

            // Don't bother with non-etl events or duplicate event fact vessel rows (static plate, flowcell, strip tube)
            if( !eventFact.canEtl() || labVessel == null || vesselsToSkip.contains(labVessel) ) {
                continue;
            }

            // Can't do anything with a bad workflow
            if( eventFact.getWfName() == null || eventFact.getWfName().isEmpty() ) {
                continue;
            }

            if( WorkflowConfigLookup.isSynthetic(eventFact.getWfName() ) ) {
                continue;
            }

            // Data is fully denormalized on Event-Vessel-Sample.
            // Don't re-analyze the same event if ancestry flag is false
            // Don't keep re-analyzing same vessel in same event
            if( ignoreAncestryForEventList.contains(eventFact.getEventId() ) ) {
                continue;
            }

            // No need to keep re-parsing workflow for the same event ...
            if( !eventFact.getEventId().equals(previousEventId)) {
                workflowStepList = getWorkflowStepsUpToEvent(eventFact, workflowConfig);
                previousEventId = eventFact.getEventId();
                // Clear out vessels to skip upon hitting new event
                vesselsToSkip.clear();
            }

            // Ignore events for which ancestry etl on current event is not flagged
            List<LabEventType> ancestorEventTypesToFind = new ArrayList<>();
            if( workflowStepList.isEmpty() || ! workflowStepList.get( workflowStepList.size() - 1 ).doAncestryEtl() ) {
                // Skip any subsequent denormalized events
                ignoreAncestryForEventList.add( eventFact.getEventId() );
                continue;
            }

            // Find the next nearest ancestor event type(s)
            ancestorEventTypesToFind.addAll(findAllAncestorEtlEventTypes(workflowStepList));
            // Build the ancestry dtos
            eventFact.addAllAncestryDtos(buildAncestryFacts(labEvent, labVessel, ancestorEventTypesToFind));
            eventFact.setIsEtlLibrary();
            vesselsToSkip.add(labVessel);
        }
    }

    /**
     * Navigate the event vessel ancestry, find all ancestor events of the specified types,
     *   and create ancestry records linking to labEvent. <br />
     * Note:  At this point, the labEvent has already been vetted as being flagged for ancestry
     * @param labEvent The event from which to start ancestry search
     * @param labVessel The vessel associated with the labEvent argument
     * @param ancestorEventTypes Event type(s) to search the ancestry for
     * @return Any ancestry facts found for the event-vessel combination, empty list if none
     */
    private List<AncestryFactDto> buildAncestryFacts(LabEvent labEvent, LabVessel labVessel,
                                                     List<LabEventType> ancestorEventTypes) {
        List<AncestryFactDto> ancestryFactDtos = new ArrayList<>();

        if( ancestorEventTypes.isEmpty() ) {
            return ancestryFactDtos;
        }

        Map<LabEvent, Set<LabVessel>> ancestorEventVesselMap = labVessel.findVesselsForLabEventTypes(
                ancestorEventTypes, Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors), true);

        for( LabEvent ancestorLabEvent : ancestorEventVesselMap.keySet() ) {
            Set<LabVessel> eventVessels = ancestorEventVesselMap.get(ancestorLabEvent);
            for( LabVessel ancestorLabVessel : eventVessels ) {
                if( ancestorLabVessel.getContainerRole() != null ) {
                    VesselContainer containerVessel = ancestorLabVessel.getContainerRole();
                    if( containerVessel.hasAnonymousVessels() ) {
                        // Use container vessel
                        ancestryFactDtos.add(new AncestryFactDto(
                                ancestorLabEvent.getLabEventId(),
                                containerVessel.getEmbedder(),
                                ancestorLabEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                                ancestorLabEvent.getEventDate(),
                                labEvent.getLabEventId(),
                                labVessel,
                                labEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                                labEvent.getEventDate()));
                    } else {
                        // Use vessels at positions
                        VesselGeometry geometry = containerVessel.getEmbedder().getVesselGeometry();
                        for (VesselPosition vesselPosition : geometry.getVesselPositions()) {
                            LabVessel containedVessel = containerVessel.getVesselAtPosition(vesselPosition);
                            // Vessel set may include a tube and the tube formation it's in, exclude duplicates
                            // TODO JMS Investigate why vessel/container traversal getAncestor()/getDescendant() logic does this.
                            if( containedVessel != null && !eventVessels.contains(containedVessel)) {
                                ancestryFactDtos.add(new AncestryFactDto(
                                        ancestorLabEvent.getLabEventId(),
                                        containedVessel,
                                        ancestorLabEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                                        ancestorLabEvent.getEventDate(),
                                        labEvent.getLabEventId(),
                                        labVessel,
                                        labEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                                        labEvent.getEventDate()));
                            }
                        }
                    }
                } else {
                    // Vessel is not a container, use vessel
                    ancestryFactDtos.add(new AncestryFactDto(
                            ancestorLabEvent.getLabEventId(),
                            ancestorLabVessel,
                            ancestorLabEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                            ancestorLabEvent.getEventDate(),
                            labEvent.getLabEventId(),
                            labVessel,
                            labEvent.getLabEventType().getLibraryType().getEtlDisplayName(),
                            labEvent.getEventDate()));
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
        if( eventFact.getBatchName().equals(LabEventEtl.NONE ) ) {
            eventEffectiveDate = eventFact.getEventDate();
        } else {
            eventEffectiveDate = eventFact.getBatchDate();
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

        private Long ancestorEventId;
        private Long childEventId;
        private Long ancestorVesselId;
        private String ancestorLibraryName;
        private Date ancestorLibraryCreated;
        private Long childVesselId;
        private String childLibraryName;
        private Date childLibraryCreated;

        public AncestryFactDto(Long ancestorEventId, LabVessel ancestorVessel, String ancestorLibraryTypeName,
                               Date ancestorLibraryCreated, Long childEventId, LabVessel childVessel,
                               String childLibraryTypeName, Date childLibraryCreated ){
            this.ancestorEventId        = ancestorEventId;
            this.ancestorVesselId       = ancestorVessel.getLabVesselId();
            this.ancestorLibraryName    = ancestorLibraryTypeName;
            this.ancestorLibraryCreated = ancestorLibraryCreated;
            this.childEventId           = childEventId;
            this.childVesselId          = childVessel.getLabVesselId();
            this.childLibraryName       = childLibraryTypeName;
            this.childLibraryCreated    = childLibraryCreated;
        }

        public Long getAncestorEventId(){
            return ancestorEventId;
        }

        public Long getAncestorVesselId(){
            return ancestorVesselId;
        }

        public String getAncestorLibraryTypeName(){
            return ancestorLibraryName;
        }

        public Date getAncestorCreated(){
            return ancestorLibraryCreated;
        }

        public Long getChildEventId(){
            return childEventId;
        }

        public Long getChildVesselId(){
            return childVesselId;
        }

        public String getChildLibraryTypeName(){
            return childLibraryName;
        }

        public Date getChildCreated(){
            return childLibraryCreated;
        }
    }

}
