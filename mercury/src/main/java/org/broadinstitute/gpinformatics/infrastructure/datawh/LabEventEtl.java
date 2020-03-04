package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private WorkflowConfigLookup workflowConfigLookup;
    private BSPUserList bspUserList;
    private final Collection<EventFactDto> loggingDtos = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();
    private SequencingSampleFactEtl sequencingSampleFactEtl;
    private WorkflowLoader workflowLoader;
    public static final String NONE = "NONE";
    public static final String MULTIPLE = "MULTIPLE";
    public final String ancestorFileName = "library_ancestry";
    private EventAncestryEtlUtil eventAncestryEtlUtil;

    public LabEventEtl() {
    }

    @Inject
    public LabEventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao dao,
                       SequencingSampleFactEtl sequencingSampleFactEtl, WorkflowLoader workflowLoader, BSPUserList bspUserList) {
        super(LabEvent.class, "event_fact", "lab_event_aud", "lab_event_id", dao);
        this.workflowConfigLookup = workflowConfigLookup;
        this.sequencingSampleFactEtl = sequencingSampleFactEtl;
        this.workflowLoader = workflowLoader;
        this.bspUserList = bspUserList;
    }

    @Override
    Long entityId(LabEvent entity) {
        return entity.getLabEventId();
    }

    @Override
    Path rootId(Root<LabEvent> root) {
        return root.get(LabEvent_.labEventId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabEvent.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    public Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEvent entity) {

        Collection<String> eventFactRecords = new ArrayList<>();
        try {
            for (EventFactDto fact : makeEventFacts(entity)) {

                if (fact.canEtl()) {
                    eventFactRecords.add(
                        genericRecord(etlDateStr, isDelete,
                            format( fact.getEventId() ),
                            format( fact.getWfId() ),
                            format( fact.getWfProcessId() ),
                            format( fact.getEventType().getName() ),
                            format( fact.getPdoId() ),
                            format( fact.getPdoSampleId() ),
                            format( fact.getLcsetSampleId() ),
                            format( fact.getBatchName() ),
                            format( fact.getEventLocation() ),
                            format( fact.getVesselId() ),
                            format( fact.getVesselPosition() ),
                            ExtractTransform.formatTimestamp(fact.getEventDate()),
                            format(fact.getEventProgram()),
                            format(fact.getMolecularIndex()),
                            format(fact.isEtlLibrary()?fact.getLibraryName():""),
                                format(fact.getOperator()),
                            "E"
                        )
                    );

                    if( fact.getAncestryDtos() != null ) {
                        for (EventAncestryEtlUtil.AncestryFactDto ancestryDto : fact.getAncestryDtos()) {
                            eventFactRecords.add(genericRecord(etlDateStr, isDelete,
                                    format( ancestryDto.getAncestorEventId()),
                                    format( ancestryDto.getAncestorVesselId()),
                                    format( ancestryDto.getAncestorLibraryTypeName()),
                                    ExtractTransform.formatTimestamp(ancestryDto.getAncestorCreated()),
                                    format( ancestryDto.getChildEventId()),
                                    format( ancestryDto.getChildVesselId()),
                                    format( ancestryDto.getChildLibraryTypeName()),
                                    ExtractTransform.formatTimestamp(ancestryDto.getChildCreated()),
                                    "A"
                            ));
                        }
                    }


                }
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
            logger.error("Error doing lab event etl", e);
        }
        return eventFactRecords;
    }

    /**
     * Functionality in GenericEntityEtl is enhanced to write records to two different files.  <br/>
     * All delete records go in event fact table.  <br/>
     * Last character of line for update/insert records determines which file the record is written to: <br/>
     * "E" is an event fact record <br/>
     * "A" is an ancestry fact record <br/>
     * Scope relaxed from protected to public to allow a backfill service hook
     */
    @DaoFree
    @Override
    public int writeRecords(Collection<LabEvent> entities,
                               Collection<Long>deletedEntityIds,
                               String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile eventDataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile ancestryDataFile = new DataFile(dataFilename(etlDateStr, ancestorFileName));
        this.eventAncestryEtlUtil = new EventAncestryEtlUtil(workflowLoader.getWorkflowConfig());

        try {
            // Deletion records only contain the entityId field.
            for (Long entityId : deletedEntityIds) {
                String record = genericRecord(etlDateStr, true, entityId);
                eventDataFile.write(record);
            }
            // Writes the records.
            for (LabEvent entity : entities) {
                if (!deletedEntityIds.contains(dataSourceEntityId(entity))) {
                    try {
                        for (String record : dataRecords(etlDateStr, false, entity)) {
                            // Split file writes between events and ancestry ...
                            if( record.endsWith("E") ) {
                                eventDataFile.write(record);
                            } else if ( record.endsWith("A") ) {
                                ancestryDataFile.write(record);
                            }
                        }
                    } catch (Exception e) {
                        // Continues ETL and logs data-specific Mercury exceptions.  Re-throws systemic exceptions
                        // such as when BSP is down in order to stop this run of ETL.
                        if (!isSystemException(e)) {
                            if (errorException == null) {
                                errorException = e;
                            }
                            errorIds.add(dataSourceEntityId(entity));
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while writing " + eventDataFile.getFilename(), e);

        } finally {
            eventDataFile.close();
            ancestryDataFile.close();
        }
        return eventDataFile.getRecordCount() + ancestryDataFile.getRecordCount();
    }

    /**
     * Overridden to gather LabEvent objects for entity ids and pass to writer so the file writes are forked
     * into event or ancestry file <br/>
     * Scope relaxed from protected to public to allow a backfill service hook
     */
    @Override
    public int writeRecords(Collection<Long> deletedEntityIds,
                               Collection<Long> modifiedEntityIds,
                               Collection<Long> addedEntityIds,
                               Collection<RevInfoPair<LabEvent>> revInfoPairs,
                               String etlDateStr) {

        Collection<Long> nonDeletedIds = new ArrayList<>();
        nonDeletedIds.addAll(modifiedEntityIds);
        nonDeletedIds.addAll(addedEntityIds);

        Collection<LabEvent> eventList = new ArrayList<>();
        LabEvent event;
        for (Long entityId : nonDeletedIds) {
            event = dao.findById( LabEvent.class, entityId );
            if( event != null ) {
                eventList.add(event);
            }
        }
        return writeRecords( eventList, deletedEntityIds, etlDateStr );
    }

    /**
     * Holds extract data for lab event ETL.  Data should be considered immutable after constructor. <br />
     * Note: Hibernate causes stack overflow errors at commit stage due to what appears to be
     *    an old bug caused by recursive relationships.
     * To prevent, all entities are detached from persistence context at makeEventFacts method.
     * Any data access via entity lazy fetches or Hibernate proxies will fail
     */
    public static class EventFactDto {

        private boolean canEtl;
        private List<EventAncestryEtlUtil.AncestryFactDto> ancestryFactDtos;

        private Long labEventId;
        private LabEventType labEventType;
        private String eventLocation;
        private Date eventDate;
        private String programName;
        private String libraryName;
        private String molecularIndexName;
        private String batchName;
        private Date workflowEffectiveDate;
        private String workflowName;
        private Long workflowId;
        private String productWorkflowName;
        private Long workflowProcessId;
        private String workflowProcessName;
        private String workflowStepName;
        private Long productOrderId;
        private String productOrderName;
        private String pdoSampleId;
        private String lcsetSampleID;
        private Long labVesselId;
        private String barcode;
        private VesselPosition vesselPosition;
        private String rejectReason;
        private String operator;
        private boolean isEtlLibrary = false;

        EventFactDto(LabEvent labEvent, LabVessel labVessel, VesselPosition vesselPosition, String molecularIndexName, String batchName,
                     Date workflowEffectiveDate,
                     String workflowName, String pdoSampleId, String lcsetSampleID,
                     ProductOrder productOrder, WorkflowConfigDenorm wfDenorm, String operator, boolean canEtl) {

            this.labEventId = labEvent.getLabEventId();
            this.labEventType = labEvent.getLabEventType();
            this.eventLocation = labEvent.getEventLocation();
            this.eventDate = labEvent.getEventDate();
            this.programName = format(labEvent.getProgramName());
            this.libraryName = labEvent.getLabEventType().getLibraryType() == LabEventType.LibraryType.NONE_ASSIGNED ?
                    null : labEvent.getLabEventType().getLibraryType().getEtlDisplayName();
            this.molecularIndexName = molecularIndexName;
            this.batchName = batchName == null ? NONE : batchName;
            this.workflowEffectiveDate = workflowEffectiveDate;
            this.workflowName = workflowName;
            this.workflowId = wfDenorm == null ? null : wfDenorm.getWorkflowId();
            this.productWorkflowName = wfDenorm == null ? null : wfDenorm.getProductWorkflowName();
            this.workflowProcessId = wfDenorm == null ? null : wfDenorm.getProcessId();
            this.workflowProcessName = wfDenorm == null ? null : wfDenorm.getWorkflowProcessName();
            this.workflowStepName = wfDenorm == null ? null : wfDenorm.getWorkflowStepName();
            this.productOrderId = productOrder == null ? null : productOrder.getProductOrderId();
            this.productOrderName = productOrder == null ? null : productOrder.getBusinessKey();
            this.pdoSampleId = pdoSampleId;
            this.lcsetSampleID = lcsetSampleID;
            this.labVesselId = labVessel == null ? null : labVessel.getLabVesselId();
            this.barcode = labVessel == null ? null : labVessel.getLabel();
            this.vesselPosition = vesselPosition;
            this.operator = operator;

            this.canEtl = canEtl;
        }

        public static final Comparator<EventFactDto> BY_SAMPLE_KEY = new Comparator<EventFactDto>() {
            // Sorting a null sample key will put it after non-null sample keys.
            private final static String NULLS_LAST = "zzzzzzzzzz";
            @Override
            public int compare(EventFactDto lhs, EventFactDto rhs) {
                String s1 = lhs.getLcsetSampleId() == null || lhs.getLcsetSampleId().isEmpty() ?
                            NULLS_LAST : lhs.getLcsetSampleId();
                String s2 = rhs.getLcsetSampleId() == null || rhs.getLcsetSampleId().isEmpty() ?
                        NULLS_LAST : rhs.getLcsetSampleId();
                return s1.compareTo(s2);
            }
        };

        public boolean canEtl(){
            return canEtl;
        }

        public Long getEventId() {
            return labEventId;
        }

        public LabEventType getEventType() {
            return labEventType;
        }

        public String getEventLocation() {
            return eventLocation;
        }

        public Date getEventDate() {
            return eventDate;
        }

        public String getEventProgram() {
            return programName;
        }

        public String getLibraryName() {
            return libraryName;
        }

        public String getMolecularIndex() {
            return molecularIndexName;
        }

        public String getBatchName() {
            return batchName;
        }

        public Date getBatchDate() {
            return workflowEffectiveDate;
        }

        public String getWfName() {
            return productWorkflowName == null?workflowName:productWorkflowName;
        }

        public Long getWfId() {
            return workflowId;
        }

        public Long getWfProcessId() {
            return workflowProcessId;
        }

        public String getWfProcessName() {
            return workflowProcessName;
        }

        public String getWfStepName() {
            return workflowStepName;
        }

        public Long getPdoId() {
            return productOrderId;
        }

        public String getPdoName() {
            return productOrderName;
        }

        public String getPdoSampleId() {
            return pdoSampleId;
        }

        public String getLcsetSampleId() {
            return lcsetSampleID;
        }

        public Long getVesselId() {
            return labVesselId;
        }

        public String getVesselBarcode() {
            return barcode;
        }

        public String getVesselPosition(){
            return vesselPosition==null?"":vesselPosition.name();
        }

        public String getOperator() {
            return operator;
        }

        public void addAllAncestryDtos(List<EventAncestryEtlUtil.AncestryFactDto> ancestryFactDtos) {
            if( this.ancestryFactDtos == null ) {
                this.ancestryFactDtos = new ArrayList<>();
            }
            this.ancestryFactDtos.addAll(ancestryFactDtos);
        }

        public List<EventAncestryEtlUtil.AncestryFactDto> getAncestryDtos(){
            return ancestryFactDtos;
        }

        /**
         * Flags this event fact as of interest to analytics LIBRARY_LCSET_SAMPLE
         */
        public void setIsEtlLibrary(){
            isEtlLibrary = true;
        }
        public boolean isEtlLibrary(){
            return isEtlLibrary;
        }

        public void setRejectReason(String rejectReason ) {
            this.rejectReason = rejectReason;
        }

        /**
         * Get the specific reason for rejection of this event.
         * Should only be populated when record is rejected (canEtl is false)
          */
        public String getRejectReason() {
            if( canEtl ) {
                return "Record not rejected";
            } else if ( rejectReason == null ) {
                // Process neglected to populate value
                return "LabEvent ID: " + labEventId + " is rejected, but reason not documented";
            } else {
                return rejectReason;
            }
        }

    }

    /**
     * Makes one or more DTOs representing Event Fact records.  DTOs may have missing values.
     */
    public List<EventFactDto> makeEventFacts(long labEventId) {
        try {
            return makeEventFacts(dao.findById(LabEvent.class, labEventId));
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
            logger.error("Error making eventFact dto", e);
            return Collections.<EventFactDto>emptyList();
        }
    }

    private List<EventFactDto> makeEventFacts(LabEvent entity) {
        List<EventFactDto> dtos = new ArrayList<>();
        Set<SampleInstanceV2> sampleInstances;

        // Supports testing of just this method in a DBFree test
        if( eventAncestryEtlUtil == null ) {
            eventAncestryEtlUtil = new EventAncestryEtlUtil(workflowLoader.getWorkflowConfig());
        }

        // Sanity check bail out
        if (entity == null || entity.getLabEventType() == null) {
            logger.debug("Skipping ETL on labEvent is null or has no event type" );
            // Die here
            return dtos;
        }

        Collection<LabVessel> vessels = entity.getTargetLabVessels();

        if (vessels.isEmpty() && entity.getInPlaceLabVessel() != null) {
            vessels.add(entity.getInPlaceLabVessel());
        }

        String operator = null;
        if (entity.getEventOperator() != null) {
            operator = bspUserList.getById(entity.getEventOperator()).getUsername();
        }

        // If a synthetic event exists, record it, otherwise, skip it
        if (vessels.isEmpty()) {
            // Record ActivityBegin/End events (synthetic events)
            WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                    entity.getLabEventType().getName(), null, entity.getEventDate());
            if( wfDenorm != null ) {
                EventFactDto placeholderEventDto =
                        new EventFactDto(entity, null, null, null, null, null, null, null, null, null, wfDenorm, operator, true);
                dtos.add(placeholderEventDto);
            } else {
                // Record the failure
                EventFactDto rejectedDto =
                        new EventFactDto(entity, null, null, null, null, null, null, null, null, null, null, null, false);
                rejectedDto.setRejectReason("Skipping ETL on labEvent with no vessels " + entity.getLabEventId()
                                            + ": No synthetic workflow configured");
                dtos.add(rejectedDto);
                loggingDtos.add(rejectedDto);
            }
        } else {
            for (LabVessel vessel : vessels) {
                VesselContainer<? extends LabVessel> vesselContainer = vessel.getContainerRole();
                List<EventFactDto> eventVesselDtos = new ArrayList<>();

                if ( vesselContainer != null && !vesselContainer.getContainedVessels().isEmpty() ) {
                    // The container contains individual vessels, build a row for each
                    for (LabVessel containedVessel : vesselContainer.getContainedVessels()) {
                        VesselPosition position = vesselContainer.getPositionOfVessel(containedVessel);
                        sampleInstances = containedVessel.getSampleInstancesV2();
                        List<EventFactDto> vesselDTOs =
                                createDtoFromEventVessel(entity, containedVessel, position, sampleInstances, operator);
                        // Generate ancestor for each vessel in the container
                        eventAncestryEtlUtil.generateAncestryData(vesselDTOs, entity, containedVessel);
                        eventVesselDtos.addAll(vesselDTOs);
                    }
                } else if (vesselContainer != null ) {
                    // The container contains locations only (plate wells, lanes), build a row for each
                    for( VesselPosition targetPosition : vesselContainer.getPositions() ) {
                        sampleInstances = vesselContainer.getSampleInstancesAtPositionV2(targetPosition);
                        List<EventFactDto> vesselDTOs = createDtoFromEventVessel(entity, vesselContainer.getEmbedder(),
                                targetPosition, sampleInstances, operator);
                        eventVesselDtos.addAll(vesselDTOs );
                    }
                    // Generate ancestor for just the container
                    eventAncestryEtlUtil.generateAncestryData(eventVesselDtos, entity, vesselContainer.getEmbedder());
                } else {
                    // Build a row for the vessel (e.g barcoded tube)
                    sampleInstances = vessel.getSampleInstancesV2();
                    List<EventFactDto> vesselDTOs = createDtoFromEventVessel(entity, vessel, null, sampleInstances, operator);
                    eventAncestryEtlUtil.generateAncestryData(vesselDTOs, entity, vessel);
                    eventVesselDtos.addAll(vesselDTOs);
                }
                dtos.addAll(eventVesselDtos);
            }
            Collections.sort(dtos, EventFactDto.BY_SAMPLE_KEY);
        }

        postEtlLogging();

        return dtos;
    }

    /**
     * Create a row for each sample in an event lab vessel
     * Event ---> Vessel (1..n) ---> Sample (1..n)
     * @param labEvent The base event entity
     * @param vessel An event target vessel, possibly from a container
     * @param sampleInstances Any sample instances associated with the vessel
     * @return
     */
    private List<EventFactDto> createDtoFromEventVessel(
            LabEvent labEvent, LabVessel vessel,
            VesselPosition targetPosition, Set<SampleInstanceV2> sampleInstances, String operator) {
        List<EventFactDto> dtos = new ArrayList<>();

        if( sampleInstances.isEmpty() ) {
            // Reject for lack of any sample instances
            EventFactDto rejectedDto =
                    new EventFactDto(labEvent, vessel, null, null, null, null, null, null, null, null, null, null, false);
            rejectedDto.setRejectReason("Skipping ETL on labEvent: " + labEvent.getLabEventId() +
                                        ", vessel: " + vessel.getLabel() +
                                        (targetPosition == null ? "" : ", position: " + targetPosition) +
                                        " - No SampleInstanceV2 instances");
            dtos.add(rejectedDto);
            loggingDtos.add(rejectedDto);
        } else if (sampleInstances.size() == 1 && sampleInstances.iterator().next().isReagentOnly() ) {
            // Reject for sample instance having only reagent (Hybridization bait)
            EventFactDto rejectedDto =
                    new EventFactDto(labEvent, vessel, null, null, null, null, null, null, null, null, null, null, false);
            rejectedDto.setRejectReason("Skipping ETL on labEvent: " + labEvent.getLabEventId() +
                                        ", vessel: " + vessel.getLabel() +
                                        (targetPosition == null ? "" : ", position: " + targetPosition) +
                                        " - SampleInstanceV2 instance is reagent only");
            dtos.add(rejectedDto);
            loggingDtos.add(rejectedDto);
        } else {
            try {
                for (SampleInstanceV2 si : sampleInstances) {

                    // Extract ETL data from sample instance
                    SampleInstanceEtlData sampleInstanceEtlData = SampleInstanceEtlData.buildFromSampleInstance
                            ( si, labEvent.getEventDate(), true );

                    String pdoSampleID = sampleInstanceEtlData.getPdoSampleId();
                    ProductOrder pdo = sampleInstanceEtlData.getPdo();
                    LabBatch labBatch = sampleInstanceEtlData.getLabBatch();
                    String molecularIndexingSchemeName = sampleInstanceEtlData.getMolecularIndexingSchemeName();
                    String lcsetSampleID = sampleInstanceEtlData.getNearestSampleID();

                    // Pull workflow from lab batch
                    // Default to event date, overwritten if lab batch available
                    String batchName;
                    Date workflowEffectiveDate;
                    String workflowName;

                    if (labBatch != null) {
                        batchName = labBatch.getBatchName();
                        workflowEffectiveDate = labBatch.getCreatedOn();
                        workflowName = labBatch.getWorkflowName();
                    } else {
                        batchName = null;
                        workflowEffectiveDate = labEvent.getEventDate();
                        workflowName = null;
                    }

                    if (StringUtils.isBlank(workflowName) && pdo != null) {
                        workflowName = pdo.getProduct().getWorkflowName();
                    }

                    WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                            labEvent.getLabEventType().getName(), workflowName, workflowEffectiveDate);

                    dtos.add(new EventFactDto(labEvent, vessel, targetPosition, molecularIndexingSchemeName,
                            batchName, workflowEffectiveDate, workflowName,
                            pdoSampleID, lcsetSampleID, pdo, wfDenorm, operator, true));

                } // sampleInstances loop
            } catch (RuntimeException e) {
                logger.debug("Skipping ETL on labEvent " + labEvent.getLabEventId() +
                             " on vessel " + vessel.getLabel(), e);
            }
        }
        return dtos;
    }

    @Override
    public void postEtlLogging() {

        for( EventFactDto fact : loggingDtos ) {
            if (!fact.canEtl()) {
                logger.debug(fact.getRejectReason());
            }
        }
        loggingDtos.clear();

        // Logs any deleted events that currently require delete and re-etl of all later events.
        SortedSet<Long> deletedIds = new TreeSet<>();
        synchronized (loggingDeletedEventIds) {
            deletedIds.addAll(loggingDeletedEventIds);
            loggingDeletedEventIds.clear();
        }
        if (deletedIds.size() > 0) {
            logger.error("Manual etl required to fixup lab events downstream of deleted lab events " +
                         StringUtils.join(deletedIds, ", "));
        }
    }

}
