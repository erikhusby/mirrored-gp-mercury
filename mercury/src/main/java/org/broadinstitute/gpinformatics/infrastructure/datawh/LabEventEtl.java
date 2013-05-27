package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateful
public class LabEventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private ProductOrderDao pdoDao;
    private WorkflowConfigLookup workflowConfigLookup;

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
                    fact.labEvent.getLabEventId(),
                    format(fact.wfDenorm.getWorkflowId()),
                    format(fact.wfDenorm.getProcessId()),
                    format(fact.productOrder.getProductOrderId()),
                    format(fact.sample.getSampleKey()),
                    format(fact.labBatchId),
                    format(fact.labEvent.getEventLocation()),
                    format(fact.labVessel.getLabVesselId()),
                    format(ExtractTransform.secTimestampFormat.format(fact.labEvent.getEventDate()))
            ));
        }
        return records;
    }

    private class EventFactDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private String eventName;
        private SampleInstance sampleInstance;
        private LabBatch labBatch;
        private Long labBatchId;
        private MercurySample sample;
        private String productOrderKey;
        private ProductOrder productOrder;
        private WorkflowConfigDenorm wfDenorm;

        private EventFactDto(LabEvent labEvent, LabVessel labVessel, String eventName, SampleInstance sampleInstance,
                             LabBatch labBatch, MercurySample sample, String productOrderKey) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.eventName = eventName;
            this.sampleInstance = sampleInstance;
            this.labBatch = labBatch;
            this.labBatchId = labBatch == null ? null : labBatch.getLabBatchId();
            this.sample = sample;
            this.productOrderKey = productOrderKey;
        }
    }

    public Collection<EventFactDto> makeEventFacts(LabEvent entity) {
        Collection<EventFactDto> facts = new ArrayList<EventFactDto>();
        if (entity != null && entity.getLabEventType() != null) {
            String eventName = entity.getLabEventType().getName();

            Collection<LabVessel> vessels = entity.getTargetLabVessels();
            if (vessels.size() == 0 && entity.getInPlaceLabVessel() != null) {
                vessels.add(entity.getInPlaceLabVessel());
            }

            for (LabVessel vessel : vessels) {
                try {
                    Set<SampleInstance> sampleInstances = vessel.getSampleInstances();
                    if (sampleInstances.size() > 0) {
                        for (SampleInstance si : sampleInstances) {

                            LabBatch labBatch = si.getLabBatch() != null ? si.getLabBatch() : entity.getLabBatch();
                            if (labBatch != null) {

                                MercurySample sample = si.getStartingSample();
                                if (sample != null) {
                                    String productOrderKey = si.getProductOrderKey();
                                    if (productOrderKey != null) {

                                        facts.add(new EventFactDto(entity, vessel, eventName, si, labBatch, sample,
                                                productOrderKey));

                                    } else {
                                        if (labBatch.getLabBatchType() == LabBatchType.WORKFLOW) {
                                            logger.debug("Sample " + sample.getSampleKey() + " in " +
                                                    labBatch.getBusinessKey() + " has no product order");
                                        }
                                    }
                                } else {
                                    logger.debug("SampleInstance " + si.toString() + " has no starting sample.");
                                }
                            }
                        }
                    } else {
                        logger.debug("Vessel " + vessel.getLabel() + " has no SampleInstances.");
                    }
                } catch (RuntimeException e) {
                    logger.debug("Skipping ETL on vessel " + vessel.getLabel() + " due to: " + e);
                }
            }
        }
        return facts;
    }

    // Updates fields in each EventFactDto, and removes it from the collection if unusable.
    private void updateIds(Collection<EventFactDto> facts) {

        Map<String, ProductOrder> pdoMap = new HashMap<String, ProductOrder>();
        Map<String, WorkflowConfigDenorm> wfMap = new HashMap<String, WorkflowConfigDenorm>();

        // Spins through and updates pdo and workflow, reusing already looked up values.
        for (Iterator<EventFactDto> iter = facts.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();

            // First checks the map to reuse any lookups that were already done.
            if (pdoMap.containsKey(fact.productOrderKey)) {
                fact.productOrder = pdoMap.get(fact.productOrderKey);
                fact.wfDenorm = wfMap.get(fact.productOrderKey);
            } else {

                // Does the lookups and warns once if entity is missing.
                fact.productOrder = pdoDao.findByBusinessKey(fact.productOrderKey);
                pdoMap.put(fact.productOrderKey, fact.productOrder);

                if (fact.productOrder == null) {
                    logger.warn("ProductOrder " + fact.productOrderKey + " is missing.");
                } else {

                    fact.wfDenorm = workflowConfigLookup.lookupWorkflowConfig(fact.eventName, fact.productOrder,
                            fact.labEvent.getEventDate());
                    wfMap.put(fact.productOrderKey, fact.wfDenorm);

                    if (fact.wfDenorm == null) {
                        logger.warn("No workflow config for" +
                                " event " + fact.eventName +
                                " productOrder " + fact.productOrderKey +
                                " eventDate " + fact.labEvent.getEventDate());
                    }
                }
            }
            if (fact.productOrder == null || fact.wfDenorm == null) {
                iter.remove();
            }
        }
    }

}
