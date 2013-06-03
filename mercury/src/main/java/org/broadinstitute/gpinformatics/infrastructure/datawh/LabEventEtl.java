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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Stateful
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private ProductOrderDao pdoDao;
    private WorkflowConfigLookup workflowConfigLookup;
    private SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");

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
    Path rootId(Root root) {
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

        Collection<EventFactDto> facts = makeEventFacts(entity);
        updateIds(facts);

        Collection<String> records = new ArrayList<String>();
        for (EventFactDto fact : facts) {
            records.add(genericRecord(etlDateStr, isDelete,
                    fact.getLabEvent().getLabEventId(),
                    format(fact.getWfDenorm() == null ? null : fact.getWfDenorm().getWorkflowId()),
                    format(fact.getWfDenorm() == null ? null : fact.getWfDenorm().getProcessId()),
                    format(fact.getProductOrder() == null ? null : fact.getProductOrder().getProductOrderId()),
                    format(fact.getSample().getSampleKey()),
                    format(fact.getLabBatchId()),
                    format(fact.getLabEvent().getEventLocation()),
                    format(fact.getLabVessel().getLabVesselId()),
                    format(ExtractTransform.secTimestampFormat.format(fact.getLabEvent().getEventDate()))
            ));
        }
        return records;
    }

    private static class EventFactDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private String eventName;
        private LabBatch labBatch;
        private Long labBatchId;
        private MercurySample sample;
        private String productOrderKey;
        private ProductOrder productOrder;
        private WorkflowConfigDenorm wfDenorm;

        private EventFactDto(LabEvent labEvent, LabVessel labVessel, String eventName,
                             LabBatch labBatch, MercurySample sample, String productOrderKey) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.eventName = eventName;
            this.labBatch = labBatch;
            this.labBatchId = labBatch == null ? null : labBatch.getLabBatchId();
            this.sample = sample;
            this.productOrderKey = productOrderKey;
        }

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public String getEventName() {
            return eventName;
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

        public String getProductOrderKey() {
            return productOrderKey;
        }

        public ProductOrder getProductOrder() {
            return productOrder;
        }

        public WorkflowConfigDenorm getWfDenorm() {
            return wfDenorm;
        }

        public void setProductOrder(ProductOrder productOrder) {
            this.productOrder = productOrder;
        }

        public void setWfDenorm(WorkflowConfigDenorm wfDenorm) {
            this.wfDenorm = wfDenorm;
        }
    }

    public Collection<EventFactDto> makeEventFacts(LabEvent entity) {
        Collection<EventFactDto> facts = new ArrayList<EventFactDto>();
        Set<String> missingSampleInstances = new HashSet<String>();
        Set<String> missingStartingSamples = new HashSet<String>();
        Set<String> missingPdoKeys = new HashSet<String>();

        if (entity != null && entity.getLabEventType() != null) {
            String eventName = entity.getLabEventType().getName();

            Collection<LabVessel> vessels = entity.getTargetLabVessels();
            if (vessels.isEmpty() && entity.getInPlaceLabVessel() != null) {
                vessels.add(entity.getInPlaceLabVessel());
            }

            for (LabVessel vessel : vessels) {
                try {
                    Set<SampleInstance> sampleInstances =
                            vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW);
                    if (!sampleInstances.isEmpty()) {
                        for (SampleInstance si : sampleInstances) {
                            if (si.getProductOrderKey() != null) {
                                MercurySample sample = si.getStartingSample();
                                if (sample != null) {
                                    for (LabBatch labBatch : si.getAllWorkflowLabBatches()) {
                                        facts.add(new EventFactDto(entity, vessel, eventName, labBatch, sample,
                                                si.getProductOrderKey()));
                                    }
                                } else {
                                    // todo jmt why calling default toString()?
                                    missingStartingSamples.add(si.toString());
                                }
                            } else {
                                // todo jmt why calling default toString()?
                                missingPdoKeys.add(si.toString());
                            }
                        }
                    } else {
                        missingSampleInstances.add(vessel.getLabel());
                    }
                } catch (RuntimeException e) {
                    logger.debug("Skipping ETL on vessel " + vessel.getLabel() + " due to: " + e);
                }
            }
        }
        // Aggregates log messages.
        if (!missingSampleInstances.isEmpty()) {
            logger.debug(missingSampleInstances.size() + " vessels have no SampleInstance.");
        }
        if (!missingStartingSamples.isEmpty()) {
            logger.debug(missingStartingSamples.size() + " sampleInstances have no starting sample.");
        }
        if (!missingPdoKeys.isEmpty()) {
            logger.debug(missingPdoKeys.size() + " sampleInstances have no product order.");
        }
        return facts;
    }

    // Looks up workflow and product and updates the fact.
    private void updateIds(Collection<EventFactDto> facts) {

        Map<String, ProductOrder> pdoMap = new HashMap<String, ProductOrder>();
        Map<String, WorkflowConfigDenorm> wfMap = new HashMap<String, WorkflowConfigDenorm>();
        Set<String> missingWorkflows = new HashSet<String>();
        Set<String> missingPdoEntities = new HashSet<String>();

        // Spins through and updates pdo and workflow, reusing already looked up values.
        for (EventFactDto fact : facts) {
            if (fact.getProductOrderKey() != null) {
                // First checks the local cache.
                if (pdoMap.containsKey(fact.getProductOrderKey())) {
                    fact.setProductOrder(pdoMap.get(fact.getProductOrderKey()));
                    fact.setWfDenorm(wfMap.get(fact.getProductOrderKey()));
                } else {
                    // Does the product order lookup.
                    fact.setProductOrder(pdoDao.findByBusinessKey(fact.getProductOrderKey()));
                    if (fact.getProductOrder() != null) {
                        pdoMap.put(fact.getProductOrderKey(), fact.getProductOrder());

                        // Does the workflow lookup.
                        fact.setWfDenorm(workflowConfigLookup.lookupWorkflowConfig(fact.getEventName(),
                                fact.getLabBatch(), fact.getLabEvent().getEventDate()));
                        wfMap.put(fact.getProductOrderKey(), fact.getWfDenorm());

                        if (fact.getWfDenorm() == null) {
                            String epe = "(event " + fact.getEventName() + " productOrder " + fact.getProductOrderKey()
                                         + " eventDate " + sdf.format(fact.getLabEvent().getEventDate()) + ")";
                            missingWorkflows.add(epe);
                        }

                    } else {
                        missingPdoEntities.add(fact.getProductOrderKey());
                    }

                }
            }
        }
        // Aggregates log messages.
        if (!missingPdoEntities.isEmpty()) {
            String missing = StringUtils.join(missingPdoEntities.iterator(), ", ");
            logger.debug("Cannot find product order entity for: " + missing);
        }
        if (!missingWorkflows.isEmpty()) {
            String missing = StringUtils.join(missingPdoEntities.iterator(), ", ");
            logger.debug("Cannot find workflow config for: " + missing);
        }
    }

}
