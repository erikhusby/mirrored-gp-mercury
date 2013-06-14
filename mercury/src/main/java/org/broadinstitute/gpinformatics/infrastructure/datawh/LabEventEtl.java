package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
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
import java.util.*;

@Stateful
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private ProductOrderDao pdoDao;
    private WorkflowConfigLookup workflowConfigLookup;
    private Map<String, ProductOrder> cachedPdo = new HashMap<>();
    private Collection<EventFactDto> loggingDtos = new ArrayList<>();

    public LabEventEtl() {
    }

    @Inject
    public LabEventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao dao, ProductOrderDao pdoDao) {
        super(LabEvent.class, "event_fact", dao);
        this.workflowConfigLookup = workflowConfigLookup;
        this.pdoDao = pdoDao;
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

        for (EventFactDto fact : makeEventFacts(entity)) {
            if (fact.isComplete()) {
                ProductOrder pdo = fact.getProductOrder();
                records.add(genericRecord(etlDateStr, isDelete,
                        fact.getLabEvent().getLabEventId(),
                        format(fact.getWfDenorm().getWorkflowId()),
                        format(fact.getWfDenorm().getProcessId()),
                        format(pdo != null ? pdo.getProductOrderId() : null),
                        format(fact.getSample().getSampleKey()),
                        format(fact.getLabBatchId()),
                        format(fact.getLabEvent().getEventLocation()),
                        format(fact.getLabVessel().getLabVesselId()),
                        format(ExtractTransform.secTimestampFormat.format(fact.getLabEvent().getEventDate()))
                ));
            }
        }
        return records;
    }

    public static class EventFactDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private String sampleInstanceIndexes;
        private LabBatch labBatch;
        private Long labBatchId;
        private MercurySample sample;
        private ProductOrder productOrder;
        private WorkflowConfigDenorm wfDenorm;
        boolean isComplete;

        EventFactDto(LabEvent labEvent, LabVessel labVessel, String sampleInstanceIndexes, LabBatch labBatch,
                     MercurySample sample, ProductOrder productOrder, WorkflowConfigDenorm wfDenorm, boolean isComplete) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.sampleInstanceIndexes = sampleInstanceIndexes;
            this.labBatch = labBatch;
            this.labBatchId = labBatch == null ? null : labBatch.getLabBatchId();
            this.sample = sample;
            this.productOrder = productOrder;
            this.wfDenorm = wfDenorm;
            this.isComplete = isComplete;
        }

        private final static String NULLS_LAST = "zzzzzzzzzz";

        public static Comparator sampleKeyComparator() {
            return new Comparator<EventFactDto>() {
                @Override
                public int compare(EventFactDto o1, EventFactDto o2) {
                    String s1 = o1.getSample() == null || o1.getSample().getSampleKey() == null ?
                            NULLS_LAST : o1.getSample().getSampleKey();
                    String s2 = o2.getSample() == null || o2.getSample().getSampleKey() == null ?
                            NULLS_LAST : o2.getSample().getSampleKey();
                    return s1.compareTo(s2);
                }
            };
        }

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public String getSampleInstanceIndexes() {
            return sampleInstanceIndexes;
        }

        public LabBatch getLabBatch() {
            return labBatch;
        }

        public Long getLabBatchId() {
            return labBatchId;
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

        public boolean isComplete() {
            return isComplete;
        }

    }

    /** Makes one or more DTOs representing Event Fact records.  DTOs may have missing values. */
    public List<EventFactDto> makeEventFacts(long labEventId) {
        return makeEventFacts(dao.findById(LabEvent.class, labEventId));
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
                dtos.add(new EventFactDto(entity, null, null, null, null, null, null, false));
            }

            for (LabVessel vessel : vessels) {
                try {
                    Set<SampleInstance> sampleInstances =
                            vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW);

                    if (!sampleInstances.isEmpty()) {
                        for (SampleInstance si : sampleInstances) {

                            String pdoKey = si.getProductOrderKey();
                            if (pdoKey != null) {
                                ProductOrder pdo = null;

                                if (cachedPdo.containsKey(pdoKey)) {
                                    pdo = cachedPdo.get(pdoKey);
                                } else {
                                    pdo = pdoDao.findByBusinessKey(pdoKey);
                                    cachedPdo.put(pdoKey, pdo);
                                }

                                MercurySample sample = si.getStartingSample();
                                if (sample != null) {
                                    for (LabBatch labBatch : si.getAllWorkflowLabBatches()) {
                                        String workflowName = labBatch.getWorkflowName();
                                        WorkflowConfigDenorm wfDenorm = workflowConfigLookup.lookupWorkflowConfig(
                                                eventName, workflowName, entity.getEventDate());

                                        boolean canExportToEtl =
                                                wfDenorm != null && (pdo != null || !wfDenorm.isProductOrderNeeded());

                                        dtos.add(new EventFactDto(entity, vessel, null, labBatch, sample, pdo,
                                                wfDenorm, canExportToEtl));
                                    }
                                } else {
                                    dtos.add(new EventFactDto(entity, vessel, vessel.getIndexesString(si),
                                            null, null, pdo, null, false));
                                }
                            } else {
                                dtos.add(new EventFactDto(entity, vessel, vessel.getIndexesString(si),
                                        null, null, null, null, false));
                            }
                        }
                    } else {
                        dtos.add(new EventFactDto(entity, vessel, null, null, null, null, null, false));
                    }
                } catch (RuntimeException e) {
                    logger.debug("Skipping ETL on labEvent " + entity.getLabEventId() +
                            " on vessel " + vessel.getLabel(), e);
                }
            }
        }
        Collections.sort(dtos, EventFactDto.sampleKeyComparator());

        synchronized(loggingDtos) {
            loggingDtos.clear();
            loggingDtos.addAll(dtos);
        }
        return dtos;
    }

    @Override
    public void postEtlLogging() {
        synchronized(loggingDtos) {
            // Aggregates errors by the appropriate record identifier, depending on what the missing value is.
            Set<Long> errorIds = new HashSet<>();
            Set<Long> otherIds = new HashSet<>();

            // Keep track of reported errors so we log an entity once, showing the most basic flaw.
            Set<EventFactDto> reportedErrors = new HashSet<>();

            // No vessel on event.
            for (EventFactDto fact : loggingDtos) {
                if (!fact.isComplete() && !reportedErrors.contains(fact) &&
                        fact.getLabVessel() == null) {
                    reportedErrors.add(fact);
                    errorIds.add(fact.getLabEvent().getLabEventId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing vessel for labEvents: " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();
            otherIds.clear();

            // No sampleInstance on vessel.
            for (EventFactDto fact : loggingDtos) {
                if (!fact.isComplete() && !reportedErrors.contains(fact) &&
                        fact.getProductOrder() == null && fact.getSampleInstanceIndexes() == null) {
                    reportedErrors.add(fact);
                    errorIds.add(fact.getLabEvent().getLabEventId());
                    otherIds.add(fact.getLabVessel().getLabVesselId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing sampleInstances in vessels: " + StringUtils.join(otherIds, ", ") +
                        " in labEvents: " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();
            otherIds.clear();

            // No pdo on sampleInstance, or no pdo entity for pdoKey.
            for (EventFactDto fact : loggingDtos) {
                if (!fact.isComplete() && !reportedErrors.contains(fact) &&
                        fact.getProductOrder() == null) {
                    reportedErrors.add(fact);
                    errorIds.add(fact.getLabEvent().getLabEventId());
                    otherIds.add(fact.getLabVessel().getLabVesselId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing productOrder for sampleInstances in vessels: " + StringUtils.join(otherIds, ", ")
                        + " in labEvents: " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();
            otherIds.clear();

            // No starting sample on sampleInstance.
            for (EventFactDto fact : loggingDtos) {
                if (!fact.isComplete() && !reportedErrors.contains(fact) &&
                        fact.getSample() == null) {
                    reportedErrors.add(fact);
                    errorIds.add(fact.getLabEvent().getLabEventId());
                    otherIds.add(fact.getLabVessel().getLabVesselId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing starting sample for sampleInstances in vessels: "
                        + StringUtils.join(otherIds, ", ") + " in labEvents: " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();
            otherIds.clear();

            // No workflowConfig for (eventName, workflowName, eventDate).
            for (EventFactDto fact : loggingDtos) {
                if (!fact.isComplete() && fact.getWfDenorm() == null) {
                    errorIds.add(fact.getLabEvent().getLabEventId());
                }
            }
            if (errorIds.size() > 0) {
                logger.debug("Missing workflowConfig for labEvents: " + StringUtils.join(errorIds, ", "));
            }
            errorIds.clear();
            otherIds.clear();
        }
    }

}
