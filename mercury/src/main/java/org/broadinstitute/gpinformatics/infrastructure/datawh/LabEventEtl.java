package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

import javax.ejb.Stateful;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Stateful
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private WorkflowConfigLookup workflowConfigLookup;
    private final Collection<EventFactDto> loggingDtos = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();
    private SequencingSampleFactEtl sequencingSampleFactEtl;
    public static final String NONE = "NONE";
    public static final String MULTIPLE = "MULTIPLE";
    public final String ancestorFileName = "library_ancestry_fact";

    public LabEventEtl() {
    }

    @Inject
    public LabEventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao dao,
                       SequencingSampleFactEtl sequencingSampleFactEtl) {
        super(LabEvent.class, "event_fact", "lab_event_aud", "lab_event_id", dao);
        this.workflowConfigLookup = workflowConfigLookup;
        this.sequencingSampleFactEtl = sequencingSampleFactEtl;
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
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEvent entity) {

        Collection<String> eventFactRecords = new ArrayList<>();
        try {
            for (EventFactDto fact : makeEventFacts(entity)) {
                if (fact.canEtl()) {
                    eventFactRecords.add(
                        genericRecord(etlDateStr, isDelete,
                            format( fact.getEventId() ),
                            format( fact.getWfId() ),
                            format( fact.getWfProcessId() ),
                            format( fact.getPdoId() ),
                            format( fact.getSampleId() ),
                            format( fact.getBatchName() ),
                            format( fact.getEventLocation() ),
                            format( fact.getVesselId() ),
                            ExtractTransform.formatTimestamp(fact.getEventDate()),
                            format(fact.getEventProgram()),
                            format(fact.getMolecularIndex()),
                            "E"
                        )
                    );

                    if( fact.getAncestryFactDtos() != null ) {
                        for (EventAncestryEtlUtil.AncestryFactDto ancestryDto : fact.getAncestryFactDtos()) {
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
     * "A" is an ancestry fact record
     */
    @DaoFree
    @Override
    protected int writeRecords(Collection<LabEvent> entities,
                               Collection<Long>deletedEntityIds,
                               String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile eventDataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile ancestryDataFile = new DataFile(dataFilename(etlDateStr, ancestorFileName));

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
                        if (e.getCause() == null || e.getCause().getClass().getName().contains("broadinstitute")) {
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

    /**
     * Modifies the id lists and possibly also invokes sequencingSampleFact ETL, in order to fixup the downstream
     * event facts and sequencing facts when there are lab event deletions and modifications, which are due to a
     * manual fixup.
     *
     * @param deletedEntityIds the deleted event ids.
     * @param modifiedEntityIds the modified event ids, and the downstream event ids get added to this list.
     * @param etlDateStr the etl date.
     */
    @Override
    protected void processFixups(Collection<Long> deletedEntityIds,
                                 Collection<Long> modifiedEntityIds,
                                 String etlDateStr) {


        Set<Long> fixupEventIds = new HashSet<>(deletedEntityIds);
        fixupEventIds.addAll(modifiedEntityIds);

        // Gets the downstream events from first level vessels and their descendant vessels.
        Set<LabVessel> firstLevelVessels = new HashSet<>();
        for (Long entityId : fixupEventIds) {
            LabEvent entity = dao.findById(LabEvent.class, entityId);
            if (entity != null) {
                firstLevelVessels.addAll(entity.getTargetLabVessels());
                firstLevelVessels.add(entity.getInPlaceLabVessel());
            } else {
                loggingDeletedEventIds.add(entityId);
            }
        }

        Set<LabVessel> directAndDescendantVessels = new HashSet<>();
        for (LabVessel vessel : firstLevelVessels) {
            if (vessel != null) {
                directAndDescendantVessels.add(vessel);
                Collection<LabVessel> vessels = vessel.getDescendantVessels();
                if (!CollectionUtils.isEmpty(vessels)) {
                    directAndDescendantVessels.addAll(vessels);
                }
            }
        }

        Set<Long> descendantEventIds = new HashSet<>();
        Set<Long> descendantSequencingRunIds = new HashSet<>();

        for (LabVessel vessel : directAndDescendantVessels) {
            for (LabEvent event : vessel.getEvents()) {
                descendantEventIds.add(event.getLabEventId());
            }

            // Collects sequencing run ids from flowcell descendent vessels.
            if (vessel.getType().equals(LabVessel.ContainerType.FLOWCELL)) {
                if (OrmUtil.proxySafeIsInstance(vessel, RunCartridge.class)) {
                    RunCartridge runCartridge = (RunCartridge) vessel;
                    for (SequencingRun seqRun : runCartridge.getSequencingRuns()) {
                        descendantSequencingRunIds.add(seqRun.getSequencingRunId());
                    }
                }
            }
        }
        // Adds all except the deleted events to the modified list.
        modifiedEntityIds.addAll(descendantEventIds);
        modifiedEntityIds.removeAll(deletedEntityIds);

        if (descendantSequencingRunIds.size() > 0) {
            // Creates a sequencingSampleFact .dat file that contains the possibly modified sequencing runs.
            sequencingSampleFactEtl.writeEtlDataFile(
                    Collections.<Long>emptyList(),
                    descendantSequencingRunIds,
                    Collections.<Long>emptyList(),
                    Collections.<GenericEntityEtl<SequencingRun, SequencingRun>.RevInfoPair<SequencingRun>>emptyList(),
                    etlDateStr);
        }
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
        private Object[] data;

        private static final int COL_EVENT_ID       = 0;
        private static final int COL_WORKFLOW_ID    = 1;
        private static final int COL_PROCESS_ID     = 2;
        private static final int COL_PDO_ID         = 3;
        private static final int COL_SAMPLE_ID      = 4;
        private static final int COL_BATCH_NAME     = 5;
        private static final int COL_LOCATION       = 6;
        private static final int COL_VESSEL_ID      = 7;
        private static final int COL_EVENT_DATE     = 8;
        private static final int COL_PROGRAM_NAME   = 9;
        private static final int COL_INDEX_NAME     = 10;
        private static final int COL_LIBRARY_NAME   = 11;
        private static final int COL_VESSEL_BARCODE = 12;
        private static final int COL_WORKFLOW_NAME  = 13;
        private static final int COL_PROCESS_NAME   = 14;
        private static final int COL_PDO_NAME       = 15;
        private static final int COL_STEP_NAME      = 16;
        private static final int COL_EVENT_TYPE     = 17;
        private static final int COL_BATCH_DATE     = 18;

        EventFactDto(LabEvent labEvent, LabVessel labVessel, String molecularIndexName, String batchName,
                     Date workflowEffectiveDate,
                     String workflowName, MercurySample sample, ProductOrder productOrder,
                     WorkflowConfigDenorm wfDenorm, boolean canEtl) {

            data = new Object[19];

            // LabEvent never null
            data[COL_EVENT_ID] = labEvent.getLabEventId();
            data[COL_EVENT_TYPE] = labEvent.getLabEventType();
            data[COL_LOCATION] = labEvent.getEventLocation();
            data[COL_EVENT_DATE] = labEvent.getEventDate();
            data[COL_PROGRAM_NAME] = format(labEvent.getProgramName());
            data[COL_LIBRARY_NAME] =
                    labEvent.getLabEventType().getLibraryType() == LabEventType.LibraryType.NONE_ASSIGNED ?
                            null : labEvent.getLabEventType().getLibraryType().getDisplayName();

            data[COL_INDEX_NAME] = molecularIndexName;
            data[COL_BATCH_NAME] = batchName;

            data[COL_BATCH_DATE] = workflowEffectiveDate;
            data[COL_WORKFLOW_NAME] = workflowName;

            // Remaining entities can be null - validate before access
            data[COL_WORKFLOW_ID] = wfDenorm == null ? null : wfDenorm.getWorkflowId();
            data[COL_WORKFLOW_NAME] = wfDenorm == null ? null : wfDenorm.getProductWorkflowName();
            data[COL_PROCESS_ID] = wfDenorm == null ? null : wfDenorm.getProcessId();
            data[COL_PROCESS_NAME] = wfDenorm == null ? null : wfDenorm.getWorkflowProcessName();
            data[COL_STEP_NAME] = wfDenorm == null ? null : wfDenorm.getWorkflowStepName();
            data[COL_PDO_ID] = productOrder == null ? null : productOrder.getProductOrderId();
            data[COL_PDO_NAME] = productOrder == null ? null : productOrder.getBusinessKey();
            data[COL_SAMPLE_ID] = sample == null ? null : sample.getSampleKey();

            data[COL_VESSEL_ID] = labVessel == null ? null : labVessel.getLabVesselId();
            data[COL_VESSEL_BARCODE] = labVessel == null ? null : labVessel.getLabel();

            this.canEtl = canEtl;
        }

        public static final Comparator<EventFactDto> BY_SAMPLE_KEY = new Comparator<EventFactDto>() {
            // Sorting a null sample key will put it after non-null sample keys.
            private final static String NULLS_LAST = "zzzzzzzzzz";
            @Override
            public int compare(EventFactDto lhs, EventFactDto rhs) {
                String s1 = lhs.getSampleId() == null || lhs.getSampleId().isEmpty() ?
                            NULLS_LAST : lhs.getSampleId();
                String s2 = rhs.getSampleId() == null || rhs.getSampleId().isEmpty() ?
                        NULLS_LAST : rhs.getSampleId();
                return s1.compareTo(s2);
            }
        };

        public boolean canEtl(){
            return canEtl;
        }

        public Long getEventId() {
            return (Long) data[COL_EVENT_ID];
        }

        public LabEventType getEventType() {
            return (LabEventType) data[COL_EVENT_TYPE];
        }

        public String getEventLocation() {
            return (String) data[COL_LOCATION];
        }

        public Date getEventDate() {
            return (Date) data[COL_EVENT_DATE];
        }

        public String getEventProgram() {
            return (String) data[COL_PROGRAM_NAME];
        }

        public String getLibraryName() {
            return (String) data[COL_LIBRARY_NAME];
        }

        public String getMolecularIndex() {
            return (String) data[COL_INDEX_NAME];
        }

        public String getBatchName() {
            return (String) data[COL_BATCH_NAME];
        }

        public Date getBatchDate() {
            return (Date) data[COL_BATCH_NAME];
        }

        public String getWfName() {
            return (String) data[COL_WORKFLOW_NAME];
        }

        public Long getWfId() {
            return (Long) data[COL_WORKFLOW_ID];
        }

        public Long getWfProcessId() {
            return (Long) data[COL_PROCESS_ID];
        }

        public String getWfProcessName() {
            return (String) data[COL_PROCESS_NAME];
        }

        public String getWfStepName() {
            return (String) data[COL_STEP_NAME];
        }

        public Long getPdoId() {
            return (Long) data[COL_PDO_ID];
        }

        public String getPdoName() {
            return (String) data[COL_PDO_NAME];
        }

        public String getSampleId() {
            return (String) data[COL_SAMPLE_ID];
        }

        public Long getVesselId() {
            return (Long) data[COL_VESSEL_ID];
        }

        public String getVesselBarcode() {
            return (String) data[COL_VESSEL_BARCODE];
        }

        public void addAllAncestryDtos( List<EventAncestryEtlUtil.AncestryFactDto> ancestryFactDtos ) {
            if( this.ancestryFactDtos == null ) {
                this.ancestryFactDtos = new ArrayList<>();
            }
            this.ancestryFactDtos.addAll(ancestryFactDtos);
        }

        public List<EventAncestryEtlUtil.AncestryFactDto> getAncestryFactDtos(){
            return ancestryFactDtos;
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

        // Hand off event DTOs to see if ancestry should be added
        EventAncestryEtlUtil ancestryUtil = new EventAncestryEtlUtil();

        if (entity != null && entity.getLabEventType() != null) {
            String eventName = entity.getLabEventType().getName();
            Date workflowEffectiveDate;
            if( entity.getLabBatch() != null ) {
                workflowEffectiveDate = entity.getLabBatch().getCreatedOn();
            } else {
                workflowEffectiveDate = entity.getEventDate();
            }

            Collection<LabVessel> vessels = entity.getTargetLabVessels();
            if (vessels.isEmpty() && entity.getInPlaceLabVessel() != null) {
                vessels.add(entity.getInPlaceLabVessel());
            }

            if (vessels.isEmpty()) {
                // Use of the full constructor which in this case has multiple nulls is intentional
                // since exactly which fields are null is used as indicator in postEtlLogging, and this
                // pattern is used in other fact table etl that are exposed in ExtractTransformResource.
                WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                        eventName, null, workflowEffectiveDate);
                dtos.add(new EventFactDto(entity, null, null, null, null, null, null, null, wfDenorm, true));

                logger.debug("Skipping ETL on labEvent " + entity.getLabEventId() +
                             ": No event vessels" );
            }
            Set<SampleInstanceV2> sampleInstances;
            for (LabVessel vessel : vessels) {
                List<EventFactDto> dtosFromEvent;
                if( vessel.getContainerRole() != null ) {
                    if( !vessel.getContainerRole().getContainedVessels().isEmpty() ) {
                        for (LabVessel containedVessel : vessel.getContainerRole().getContainedVessels()) {
                            sampleInstances = containedVessel.getSampleInstancesV2();
                            dtosFromEvent = createDtoFromEventVessel(entity, containedVessel, sampleInstances, workflowEffectiveDate);
                            dtos.addAll(dtosFromEvent);
                            ancestryUtil.generateAncestryData( dtosFromEvent, entity, containedVessel );
                            dtosFromEvent.clear();
                        }
                    } else {
                        VesselContainer container = vessel.getContainerRole();
                        for( VesselPosition position : container.getEmbedder().getVesselGeometry().getVesselPositions() ) {
                            sampleInstances = container.getSampleInstancesAtPositionV2(position);
                            if( sampleInstances != null && !sampleInstances.isEmpty() ) {
                                dtosFromEvent = createDtoFromEventVessel(entity, vessel, sampleInstances,
                                        workflowEffectiveDate);
                                dtos.addAll(dtosFromEvent);
                                ancestryUtil.generateAncestryData( dtosFromEvent, entity, vessel );
                                dtosFromEvent.clear();
                            }
                        }
                    }
                } else {
                    sampleInstances = vessel.getSampleInstancesV2();
                    dtosFromEvent = createDtoFromEventVessel( entity, vessel, sampleInstances, workflowEffectiveDate );
                    dtos.addAll(dtosFromEvent);
                    ancestryUtil.generateAncestryData( dtosFromEvent, entity, vessel );
                    dtosFromEvent.clear();
                }
            }

        }

        Collections.sort(dtos, EventFactDto.BY_SAMPLE_KEY);

        synchronized (loggingDtos) {
            loggingDtos.addAll(dtos);
        }
        return dtos;
    }

    /**
     * Create a row for each sample in an event lab vessel
     * Event ---> Vessel (1..n) ---> Sample (1..n)
     * @param labEvent The base event entity
     * @param vessel An event target vessel, possibly from a container
     * @param sampleInstances Any sample instances associated with the vessel
     * @param workflowEffectiveDate Used to determines which workflow version to apply
     * @return
     */
    private List<EventFactDto> createDtoFromEventVessel( LabEvent labEvent, LabVessel vessel, Set<SampleInstanceV2> sampleInstances, Date workflowEffectiveDate ) {
        List<EventFactDto> dtos = new ArrayList<>();
        try {
            if (!sampleInstances.isEmpty()) {
                for (SampleInstanceV2 si : sampleInstances) {

                    // Null bucket entry is interpreted as either a pre-Mercury (BSP) event
                    //  or a control sample which ETL ignores.
                    LabBatch labBatch = si.getSingleBatch();
                    BucketEntry bucketEntry = si.getSingleBucketEntry();
                    ProductOrder pdo = bucketEntry != null ? bucketEntry.getProductOrder() : null;
                    String molecularIndexingSchemeName =  si.getMolecularIndexingScheme() != null ?
                            si.getMolecularIndexingScheme().getName() : null;

                    String batchName = labBatch != null ? labBatch.getBatchName() : NONE;
                    if( labBatch != null ){
                        workflowEffectiveDate = labBatch.getCreatedOn();
                    }
                    String workflowName = labBatch != null ? labBatch.getWorkflowName() : null;
                    if (StringUtils.isBlank(workflowName) && pdo != null) {
                        workflowName = pdo.getProduct().getWorkflow().getWorkflowName();
                    }
                    WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                            labEvent.getLabEventType().getName(), workflowName, workflowEffectiveDate);
                    boolean canEtl = wfDenorm != null &&
                                     (labBatch != null && labBatch.getLabBatchType() == LabBatchType.WORKFLOW
                                      || !wfDenorm.isBatchNeeded()) &&
                                     (pdo != null || !wfDenorm.isProductOrderNeeded());
                    if( !canEtl ) {
                        logger.debug("Skipping ETL on labEvent " + labEvent.getLabEventId() +
                                     ": batch not workflow, no PDO, or no worflow step for event" );
                    }

                    MercurySample sample = si.getRootOrEarliestMercurySample();
                    if (sample != null) {
                        dtos.add( new EventFactDto(labEvent, vessel, molecularIndexingSchemeName, batchName, workflowEffectiveDate, workflowName,
                                sample, pdo, wfDenorm, canEtl) );
                    } else {
                        // Use of the full constructor which in this case has multiple nulls is intentional
                        // since exactly which fields are null is used as indicator in postEtlLogging, and this
                        // pattern is used in other fact table etl that are exposed in ExtractTransformResource.

                        dtos.add( new EventFactDto(labEvent, vessel,
                                MolecularIndexReagent.getIndexesString(vessel.getIndexesForSampleInstance(si)).trim(),
                                null, null, null, null, pdo, null, false) );
                        logger.debug("Skipping ETL on labEvent " + labEvent.getLabEventId() +
                                     " on vessel " + vessel.getLabel() + ": RootOrEarliestMercurySample is null" );
                    }
                }
            } else {
                // Use of the full constructor which in this case has multiple nulls is intentional
                // since exactly which fields are null is used as indicator in postEtlLogging, and this
                // pattern is used in other fact table etl that are exposed in ExtractTransformResource.
                dtos.add( new EventFactDto(labEvent, vessel, null, null, null, null, null, null, null, false) );
                logger.debug("Skipping ETL on labEvent " + labEvent.getLabEventId() +
                             " on vessel " + vessel.getLabel() + ": No SampleInstanceV2 instances" );
            }
        } catch (RuntimeException e) {
            logger.debug("Skipping ETL on labEvent " + labEvent.getLabEventId() +
                         " on vessel " + vessel.getLabel(), e);
        }
        return dtos;
    }

    @Override
    public void postEtlLogging() {
        super.postEtlLogging();
        
        List<EventFactDto> dtos = new ArrayList<>();
        synchronized (loggingDtos) {
            dtos.addAll(loggingDtos);
            loggingDtos.clear();
        }
        // Aggregates errors by the appropriate record identifier, depending on what the missing value is.
        SortedSet<Long> errorIds = new TreeSet<>();
        SortedSet<Long> otherIds = new TreeSet<>();

        //  Only logs dtos that did not get exported.  Only logs each dto once.  Attempts to show the most basic flaw.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.canEtl()) {
                iter.remove();
            }
        }

        // No vessel on event.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getVesselId() == null) {
                errorIds.add(fact.getEventId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing vessel for labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // No sampleInstance on vessel.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getPdoId() == null && fact.getMolecularIndex() == null ) {
                errorIds.add( fact.getEventId() );
                otherIds.add( fact.getVesselId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing sampleInstances in vessels: " + StringUtils.join(otherIds, ", ") +
                         " in labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // No pdo on sampleInstance, or no pdo entity for pdoKey.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getPdoId() == null ) {
                errorIds.add( fact.getEventId() );
                otherIds.add( fact.getVesselId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing productOrder for sampleInstances in vessels: " + StringUtils.join(otherIds, ", ")
                         + " in labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // No starting sample on sampleInstance.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getSampleId() == null) {
                errorIds.add( fact.getEventId() );
                otherIds.add( fact.getVesselId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing starting sample for sampleInstances in vessels: "
                         + StringUtils.join(otherIds, ", ") + " in labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // No lab batch.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getBatchName() == null || fact.getBatchName().equals(NONE) ) {
                errorIds.add( fact.getEventId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing labBatch for labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // Multiple lab batch.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getBatchName().equals(MULTIPLE)) {
                errorIds.add( fact.getEventId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Multiple labBatch for labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

        // No workflowConfig for (eventName, workflowName, eventDate).
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getWfId() == null) {
                errorIds.add( fact.getEventId() );
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing workflow for labEvents: " + StringUtils.join(errorIds, ", "));
        }
        errorIds.clear();
        otherIds.clear();

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
