package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateful
public class EventEtl extends GenericEntityEtl {

    @Inject
    private LabEventDao dao;
    @Inject
    private ProductOrderDao pdoDao;
    @Inject
    private WorkflowConfigLookup workflowConfigLookup;

    public EventEtl() {}

    public EventEtl(WorkflowConfigLookup workflowConfigLookup, LabEventDao dao, ProductOrderDao pdoDao) {
        this.workflowConfigLookup = workflowConfigLookup;
        this.dao = dao;
        this.pdoDao = pdoDao;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return LabEvent.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "event_fact";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((LabEvent) entity).getLabEventId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        Collection<String> recordList = new ArrayList<String>();
        LabEvent entity = dao.findById(LabEvent.class, entityId);
        if (entity != null) {
            recordList.addAll(entityRecord(etlDateStr, isDelete, entity));
        } else {
            logger.info("Cannot export. " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
        }
        return recordList;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<LabEvent> entityList = dao.findAll(getEntityClass(),
                new GenericDao.GenericDaoCallback<LabEvent>() {
                    @Override
                    public void callback(CriteriaQuery<LabEvent> cq, Root<LabEvent> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(root.get(LabEvent_.labEventId), startId, endId));
                    }
                });
        for (LabEvent entity : entityList) {
            recordList.addAll(entityRecord(etlDateStr, isDelete, entity));
        }
        return recordList;
    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     *
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        Collection<String> records = new ArrayList<String>();

        if (entity.getLabEventType() == null) {
            logger.warn("Cannot ETL labEvent " + entity.getLabEventId() + " that has no LabEventType.");
            return records;
        }

        boolean noLabBatch = entity.getLabBatch() == null;

        Collection<LabVessel> vessels = entity.getTargetLabVessels();
        if (vessels.size() == 0 && entity.getInPlaceLabVessel() != null) {
            vessels.add(entity.getInPlaceLabVessel());
        }
        if (vessels.size() == 0) {
            logger.warn("Cannot ETL event " + entity.getLabEventId() + " that has no vessels.");
            return records;
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
                String sampleKey = sample.getSampleKey();
                String productOrderKey = sample.getProductOrderKey();
                if (productOrderKey == null) {
                    logger.warn("Sample " + sample.getSampleKey() + " has null productOrderKey");
                    continue;
                }
                ProductOrder productOrder = pdoDao.findByBusinessKey(productOrderKey);
                if (productOrder == null) {
                    logger.warn("Product " + productOrderKey + " has no entity");
                    continue;
                }

                WorkflowConfigDenorm denorm = workflowConfigLookup.lookupWorkflowConfig(eventName, productOrder, entity.getEventDate());

                records.add(genericRecord(etlDateStr, isDelete,
                        entity.getLabEventId(),
                        format(denorm.getWorkflowId()),
                        format(denorm.getProcessId()),
                        format(productOrder.getProductOrderId()),
                        format(sampleKey),
                        format(labBatchId),
                        format(entity.getEventLocation()),
                        format(vessel.getLabVesselId()),
                        format(ExtractTransform.secTimestampFormat.format(entity.getEventDate()))
                ));
            }
        }

        return records;
    }

    /**
     * This entity does not make status records.
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /**
     * This entity does support add/modify records via primary key.
     */
    @Override
    boolean isEntityEtl() {
        return true;
    }

}
