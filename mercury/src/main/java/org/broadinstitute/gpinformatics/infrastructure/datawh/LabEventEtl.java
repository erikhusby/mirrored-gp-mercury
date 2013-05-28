package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.text.SimpleDateFormat;
import java.util.*;

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
                    fact.labEvent.getLabEventId(),
                    format(fact.wfDenorm == null ? null : fact.wfDenorm.getWorkflowId()),
                    format(fact.wfDenorm == null ? null : fact.wfDenorm.getProcessId()),
                    format(fact.productOrder == null ? null : fact.productOrder.getProductOrderId()),
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
        Set<String> missingSampleInstances = new HashSet<String>();
        Set<String> missingStartingSamples = new HashSet<String>();

        if (entity != null && entity.getLabEventType() != null) {
            String eventName = entity.getLabEventType().getName();

            Collection<LabVessel> vessels = entity.getTargetLabVessels();
            if (vessels.size() == 0 && entity.getInPlaceLabVessel() != null) {
                vessels.add(entity.getInPlaceLabVessel());
            }

            for (LabVessel vessel : vessels) {
                try {
                    Set<SampleInstance> sampleInstances =
                            vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW);
                    if (sampleInstances.size() > 0) {
                        for (SampleInstance si : sampleInstances) {
                            MercurySample sample = si.getStartingSample();
                            if (sample != null) {
                                for (LabBatch labBatch : si.getAllWorkflowLabBatches()) {
                                    facts.add(new EventFactDto(entity, vessel, eventName, si, labBatch, sample,
                                            si.getProductOrderKey()));
                                }
                            } else {
                                missingStartingSamples.add(si.toString());
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
        if (missingSampleInstances.size() > 0) {
            String missing = StringUtils.join(missingSampleInstances, ", ");
            logger.debug("Vessels having no SampleInstance: " + missing);
        }
        if (missingStartingSamples.size() > 0) {
            String missing = StringUtils.join(missingStartingSamples, ", ");
            logger.debug("SampleInstances having no starting sample: " + missing);
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
        for (Iterator<EventFactDto> iter = facts.iterator(); iter.hasNext(); ) {
            EventFactDto fact = iter.next();

            if (fact.productOrderKey != null) {
                // First checks the local cache.
                if (pdoMap.containsKey(fact.productOrderKey)) {
                    fact.productOrder = pdoMap.get(fact.productOrderKey);
                    fact.wfDenorm = wfMap.get(fact.productOrderKey);

                } else {
                    // Does the product order lookup.
                    fact.productOrder = pdoDao.findByBusinessKey(fact.productOrderKey);
                    if (fact.productOrder != null) {
                        pdoMap.put(fact.productOrderKey, fact.productOrder);

                        // Does the workflow lookup.
                        fact.wfDenorm = workflowConfigLookup.lookupWorkflowConfig(fact.eventName, fact.productOrder,
                                fact.labEvent.getEventDate());
                        wfMap.put(fact.productOrderKey, fact.wfDenorm);

                        if (fact.wfDenorm == null) {
                            String epe = "(event " + fact.eventName + " productOrder " + fact.productOrderKey +
                                    " eventDate " + sdf.format(fact.labEvent.getEventDate()) + ")";
                            missingWorkflows.add(epe);
                        }

                    } else {
                        missingPdoEntities.add(fact.productOrderKey);
                    }

                }
            }
        }
        // Aggregates log messages.
        if (missingPdoEntities.size() > 0) {
            String missing = StringUtils.join(missingPdoEntities.iterator(), ", ");
            logger.warn("Cannot find product order entity for: " + missing);
        }
        if (missingWorkflows.size() > 0) {
            String missing = StringUtils.join(missingPdoEntities.iterator(), ", ");
            logger.warn("Cannot find workflow config for: " + missing);
        }
    }

}
