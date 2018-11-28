package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

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

    // All workflow event types which are ETL flagged and have library names assigned
    private Set<LabEventType> libraryEventTypes = new HashSet<>();
    private List<LabEventType> eventTypeList = new ArrayList<>();


    public EventAncestryEtlUtil(WorkflowConfig workflowConfig) {
        // Build out workflow step list
        for(ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs() ) {
            for( ProductWorkflowDefVersion workflowDefVersion : workflowDef.getWorkflowVersionsDescEffDate() ) {
                for( WorkflowProcessDef processDef : workflowDefVersion.getWorkflowProcessDefs()) {
                    for( WorkflowProcessDefVersion processDefVersion : processDef.getProcessVersionsDescEffDate() ) {
                        for( WorkflowStepDef workflowStepDef : processDefVersion.getWorkflowStepDefs() ) {
                            if( workflowStepDef.doAncestryEtl() ) {
                                for( LabEventType labEventType : workflowStepDef.getLabEventTypes() ) {
                                    if( labEventType.getLibraryType() != LabEventType.LibraryType.NONE_ASSIGNED ) {
                                        libraryEventTypes.add(labEventType);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        eventTypeList.addAll(libraryEventTypes);
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

        // Skip duplicate entries for same vessel (e.g. static plate)
        Set<LabVessel> vesselsToSkip = new HashSet<>();

        for( LabEventEtl.EventFactDto eventFact : eventFacts ) {

            // Don't bother with non-etl events or duplicate event fact vessel rows (static plate, flowcell, strip tube)
            if( !eventFact.canEtl() || labVessel == null || vesselsToSkip.contains(labVessel) ) {
                continue;
            }

            // Ignore events for which ancestry etl on current event is not flagged
            if( !libraryEventTypes.contains(eventFact.getEventType()) ) {
                continue;
            }

            // No need to keep re-parsing workflow for the same event ...
            if( !eventFact.getEventId().equals(previousEventId)) {
                previousEventId = eventFact.getEventId();
                // Clear out vessels to skip upon hitting new event
                vesselsToSkip.clear();
            }

            // Find ancestor library event(s) of interest
            // Build the ancestry dtos
            eventFact.addAllAncestryDtos(buildAncestryFacts(labEvent, labVessel));
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
     * @return Any ancestry facts found for the event-vessel combination, empty list if none
     */
    private List<AncestryFactDto> buildAncestryFacts(LabEvent labEvent, LabVessel labVessel) {
        List<AncestryFactDto> ancestryFactDtos = new ArrayList<>();

        Map<LabEvent, Set<LabVessel>> ancestorEventVesselMap = labVessel.findVesselsForLabEventTypes(
                eventTypeList, Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors), true);

        // Remove 'current' event
        ancestorEventVesselMap.remove(labEvent);

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
