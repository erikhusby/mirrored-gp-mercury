package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateful
public class EventEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    protected ProductOrderDao pdoDao;
    protected WorkflowConfigLookup workflowConfigLookup;

    public EventEtl() {
        entityClass = LabEvent.class;
        baseFilename = "event_fact";
    }

    @Inject
    public EventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao d, ProductOrderDao pdoDao) {
        this();
        dao = d;
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

        Collection<EventFactDto> facts = traverseGraph(entity);
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
        LabEvent labEvent;
        boolean noLabBatch;
        LabVessel labVessel;
        String eventName;
        SampleInstance sampleInstance;
        LabBatch labBatch;
        Long labBatchId;
        MercurySample sample;
        String productOrderKey;
        ProductOrder productOrder;
        WorkflowConfigDenorm wfDenorm;

        private EventFactDto(LabEvent labEvent, boolean noLabBatch, LabVessel labVessel, String eventName,
                             SampleInstance sampleInstance, LabBatch labBatch, Long labBatchId, MercurySample sample,
                             String productOrderKey) {
            this.labEvent = labEvent;
            this.noLabBatch = noLabBatch;
            this.labVessel = labVessel;
            this.eventName = eventName;
            this.sampleInstance = sampleInstance;
            this.labBatch = labBatch;
            this.labBatchId = labBatchId;
            this.sample = sample;
            this.productOrderKey = productOrderKey;
        }
    }

    @DaoFree
    private Collection<EventFactDto> traverseGraph(LabEvent entity) {
        Collection<EventFactDto> facts = new ArrayList<EventFactDto>();
        if (entity == null) {
            return facts;
        }

        if (entity.getLabEventType() == null) {
            logger.warn("Cannot ETL labEvent " + entity.getLabEventId() + " that has no LabEventType.");
            return facts;
        }

        boolean noLabBatch = entity.getLabBatch() == null;

        Collection<LabVessel> vessels = entity.getTargetLabVessels();
        if (vessels.size() == 0 && entity.getInPlaceLabVessel() != null) {
            vessels.add(entity.getInPlaceLabVessel());
        }
        if (vessels.size() == 0) {
            logger.warn("Cannot ETL event " + entity.getLabEventId() + " that has no vessels.");
            return facts;
        }

        String eventName = entity.getLabEventType().getName();

        for (LabVessel vessel : vessels) {

            Set<SampleInstance> sampleInstances = vessel.getSampleInstances();
            if (sampleInstances.size() == 0) {
                logger.warn("Cannot ETL event " + entity.getLabEventId() + " vessel " + vessel.getLabel() + " that has no SampleInstances.");
                continue;
            }

            for (SampleInstance si : sampleInstances) {

                LabBatch labBatch = noLabBatch ? si.getLabBatch() : entity.getLabBatch();
                Long labBatchId = labBatch != null ? labBatch.getLabBatchId() : null;

                MercurySample sample = si.getStartingSample();
                if (sample == null) {
                    logger.warn("Cannot find starting sample for sampleInstance " + si.toString());
                    continue;
                }
                String productOrderKey = si.getProductOrderKey();
                if (productOrderKey == null) {
                    logger.warn("Sample " + sample.getSampleKey() + " has null productOrderKey");
                    continue;
                }

                facts.add(new EventFactDto(entity, noLabBatch, vessel, eventName, si, labBatch, labBatchId,
                        sample, productOrderKey));

            }
        }
        return facts;
    }

    private void updateIds(Collection<EventFactDto> facts) {

        Map<String, ProductOrder> pdoMap = new HashMap<String, ProductOrder>();
        Map<String, WorkflowConfigDenorm> wfMap = new HashMap<String, WorkflowConfigDenorm>();

        // Spins through and updates pdo and workflow, reusing already looked up values.
        for (Iterator<EventFactDto> iter = facts.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();

            if (pdoMap.containsKey(fact.productOrderKey)) {
                fact.productOrder = pdoMap.get(fact.productOrderKey);
                fact.wfDenorm = wfMap.get(fact.productOrderKey);

                if (fact.productOrder == null || fact.wfDenorm == null) {
                    iter.remove();
                    continue;
                }

            } else {
                fact.productOrder = pdoDao.findByBusinessKey(fact.productOrderKey);
                pdoMap.put(fact.productOrderKey, fact.productOrder);

                if (fact.productOrder == null) {
                    logger.warn("ProductOrder " + fact.productOrderKey + " is missing.");
                    iter.remove();
                    continue;
                }

                fact.wfDenorm = workflowConfigLookup.lookupWorkflowConfig(fact.eventName, fact.productOrder,
                        fact.labEvent.getEventDate());
                wfMap.put(fact.productOrderKey, fact.wfDenorm);

                if (fact.wfDenorm == null) {
                    logger.warn("No workflow config for" +
                            " event " + fact.eventName +
                            " productOrder " + fact.productOrderKey +
                            " eventDate " + fact.labEvent.getEventDate());
                    iter.remove();
                    continue;
                }
            }
        }
    }

}
