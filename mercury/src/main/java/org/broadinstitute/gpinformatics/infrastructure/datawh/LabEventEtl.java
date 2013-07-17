package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Stateful
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private ProductOrderDao pdoDao;
    private WorkflowConfigLookup workflowConfigLookup;
    private final Map<String, ProductOrder> cachedPdo = new HashMap<>();
    private final Collection<EventFactDto> loggingDtos = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();
    private SequencingSampleFactEtl sequencingSampleFactEtl;
    public static final String NONE = "NONE";
    public static final String MULTIPLE = "MULTIPLE";

    public LabEventEtl() {
    }

    @Inject
    public LabEventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao dao, ProductOrderDao pdoDao,
                       SequencingSampleFactEtl sequencingSampleFactEtl) {
        super(LabEvent.class, "event_fact", dao);
        this.workflowConfigLookup = workflowConfigLookup;
        this.pdoDao = pdoDao;
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

        Collection<String> records = new ArrayList<>();
        try {
            for (EventFactDto fact : makeEventFacts(entity)) {
                if (fact.isCanEtl()) {
                    ProductOrder pdo = fact.getProductOrder();
                    records.add(genericRecord(etlDateStr, isDelete,
                            fact.getLabEvent().getLabEventId(),
                            format(fact.getWfDenorm().getWorkflowId()),
                            format(fact.getWfDenorm().getProcessId()),
                            format(pdo != null ? pdo.getProductOrderId() : null),
                            format(fact.getSample().getSampleKey()),
                            format(fact.getBatchName()),
                            format(fact.getLabEvent().getEventLocation()),
                            format(fact.getLabVessel().getLabVesselId()),
                            format(ExtractTransform.secTimestampFormat.format(fact.getLabEvent().getEventDate()))
                    ));
                }
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
            logger.error("Error doing lab event etl", e);
        }
        return records;
    }

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

        Set<LabVessel> directAndDescendantVessels = new HashSet<>(firstLevelVessels);
        for (LabVessel vessel : firstLevelVessels) {
            directAndDescendantVessels.addAll(vessel.getDescendantVessels());
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



    public static class EventFactDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private String sampleInstanceIndexes;
        private String batchName;
        private String workflowName;
        private MercurySample sample;
        private ProductOrder productOrder;
        private WorkflowConfigDenorm wfDenorm;
        boolean canEtl;

        EventFactDto(LabEvent labEvent, LabVessel labVessel, String sampleInstanceIndexes, String batchName,
                     String workflowName, MercurySample sample, ProductOrder productOrder,
                     WorkflowConfigDenorm wfDenorm, boolean canEtl) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.sampleInstanceIndexes = sampleInstanceIndexes;
            this.batchName = batchName;
            this.workflowName = workflowName;
            this.sample = sample;
            this.productOrder = productOrder;
            this.wfDenorm = wfDenorm;
            this.canEtl = canEtl;
        }


        public static final Comparator<EventFactDto> BY_SAMPLE_KEY = new Comparator<EventFactDto>() {
            // Sorting a null sample key will put it after non-null sample keys.
            private final static String NULLS_LAST = "zzzzzzzzzz";
            @Override
            public int compare(EventFactDto lhs, EventFactDto rhs) {
                String s1 = lhs.getSample() == null || lhs.getSample().getSampleKey() == null ?
                            NULLS_LAST : lhs.getSample().getSampleKey();
                    String s2 = rhs.getSample() == null || rhs.getSample().getSampleKey() == null ?
                            NULLS_LAST : rhs.getSample().getSampleKey();
                    return s1.compareTo(s2);
            }
        };

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public String getSampleInstanceIndexes() {
            return sampleInstanceIndexes;
        }

        public String getBatchName() {
            return batchName;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public MercurySample getSample() {
            return sample;
        }

        public ProductOrder getProductOrder() {
            return productOrder;
        }

        public WorkflowConfigDenorm getWfDenorm() {
            return wfDenorm;
        }

        public boolean isCanEtl() {
            return canEtl;
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

        if (entity != null && entity.getLabEventType() != null) {
            String eventName = entity.getLabEventType().getName();

            Collection<LabVessel> vessels = entity.getTargetLabVessels();
            if (vessels.isEmpty() && entity.getInPlaceLabVessel() != null) {
                vessels.add(entity.getInPlaceLabVessel());
            }

            if (vessels.isEmpty()) {
                dtos.add(new EventFactDto(entity, null, null, null, null, null, null, null, false));
            }

            // For quant events on a plate, instead of reporting on the plate vessel, substitute in the
            // direct ancestor tube vessels in order to join to labMetrics on vesselId.
            vessels = substituteVessels(entity, vessels);

            for (LabVessel vessel : vessels) {
                try {
                    Set<SampleInstance> sampleInstances =
                            vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW);

                    if (!sampleInstances.isEmpty()) {
                        for (SampleInstance si : sampleInstances) {

                            String pdoKey = si.getProductOrderKey();
                            if (pdoKey != null) {
                                ProductOrder pdo;
                                synchronized (cachedPdo) {
                                    if (cachedPdo.containsKey(pdoKey)) {
                                        pdo = cachedPdo.get(pdoKey);
                                    } else {
                                        pdo = pdoDao.findByBusinessKey(pdoKey);
                                        cachedPdo.put(pdoKey, pdo);
                                    }
                                }

                                MercurySample sample = si.getStartingSample();
                                if (sample != null) {

                                    Collection<LabBatch> batches = si.getAllWorkflowLabBatches();
                                    LabBatch labBatch = batches.size() == 1 ? batches.iterator().next() : null;
                                    String batchName = labBatch != null ? labBatch.getBatchName() :
                                            batches.size() == 0 ? NONE : MULTIPLE;

                                    String workflowName = labBatch != null ? labBatch.getWorkflowName() : null;
                                    if (StringUtils.isBlank(workflowName)) {
                                        workflowName = pdo.getProduct().getWorkflowName();
                                    }
                                    WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                                            eventName, workflowName, entity.getEventDate());

                                    boolean canEtl = wfDenorm != null &&
                                                     (pdo != null || !wfDenorm.isProductOrderNeeded());

                                    dtos.add(new EventFactDto(entity, vessel, null, batchName, workflowName,
                                            sample, pdo, wfDenorm, canEtl));
                                } else {
                                    dtos.add(new EventFactDto(entity, vessel, vessel.getIndexesString(si),
                                            null, null, null, pdo, null, false));
                                }
                            } else {
                                dtos.add(new EventFactDto(entity, vessel, vessel.getIndexesString(si),
                                        null, null, null, null, null, false));
                            }
                        }
                    } else {
                        dtos.add(new EventFactDto(entity, vessel, null, null, null, null, null, null, false));
                    }
                } catch (RuntimeException e) {
                    logger.debug("Skipping ETL on labEvent " + entity.getLabEventId() +
                                 " on vessel " + vessel.getLabel(), e);
                }
            }

        }
        Collections.sort(dtos, EventFactDto.BY_SAMPLE_KEY);

        synchronized (loggingDtos) {
            loggingDtos.addAll(dtos);
        }
        return dtos;
    }

    // For quant events, looks up the immediate tube ancestors, and puts them in the collection of vessels.
    private Collection<LabVessel> substituteVessels(LabEvent event, Collection<LabVessel> vessels) {
        if (LabMetricEtl.mapMetricTypeToEventType.values().contains(event.getLabEventType())
            && vessels.size() == 1
            && vessels.iterator().next().getContainerRole() != null) {

            LabVessel plate = vessels.iterator().next();
            Set<LabVessel> directAncestors = new HashSet<>();

            for (LabEvent transferFromEvent : plate.getTransfersTo()) {
                for (LabVessel vessel : transferFromEvent.getSourceLabVessels()) {
                    if (vessel.getContainerRole() != null) {
                        directAncestors.addAll(vessel.getContainerRole().getContainedVessels());
                    }
                }
            }
            return directAncestors;
        } else {
            return vessels;
        }
    }

    @Override
    public void postEtlLogging() {
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
            if (fact.isCanEtl()) {
                iter.remove();
            }
        }

        // No vessel on event.
        for (Iterator<EventFactDto> iter = dtos.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();
            if (fact.getLabVessel() == null) {
                errorIds.add(fact.getLabEvent().getLabEventId());
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
            if (fact.getProductOrder() == null && fact.getSampleInstanceIndexes() == null) {
                errorIds.add(fact.getLabEvent().getLabEventId());
                otherIds.add(fact.getLabVessel().getLabVesselId());
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
            if (fact.getProductOrder() == null) {
                errorIds.add(fact.getLabEvent().getLabEventId());
                otherIds.add(fact.getLabVessel().getLabVesselId());
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
            if (fact.getSample() == null) {
                errorIds.add(fact.getLabEvent().getLabEventId());
                otherIds.add(fact.getLabVessel().getLabVesselId());
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
            if (fact.getBatchName() == null || fact.getBatchName().equals(NONE)) {
                errorIds.add(fact.getLabEvent().getLabEventId());
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
                errorIds.add(fact.getLabEvent().getLabEventId());
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
            if (fact.getWfDenorm() == null) {
                errorIds.add(fact.getLabEvent().getLabEventId());
                iter.remove();
            }
        }
        if (errorIds.size() > 0) {
            logger.debug("Missing workflowConfig for labEvents: " + StringUtils.join(errorIds, ", "));
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
