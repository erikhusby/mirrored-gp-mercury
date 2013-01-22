package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections.map.LRUMap;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateless
public class EventEtl extends GenericEntityEtl {

    private LabEventDao dao;
    private ProductOrderDao pdoDao;
    private WorkflowLoader workflowLoader;

    @Inject
    public void setLabEventDao(LabEventDao dao) {
        this.dao = dao;
    }

    @Inject
    public void setProductOrderDao(ProductOrderDao pdoDao) {
        this.pdoDao = pdoDao;
    }

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
        initWorkflowConfigDenorm();
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
                        if (startId > 0 || endId < Long.MAX_VALUE) {
                            CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                            cq.where(cb.between(root.get(LabEvent_.labEventId), startId, endId));
                        }
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
        if (vessels.size() == 0) {
            vessels.add(entity.getInPlaceLabVessel());
        }
        if (vessels.size() == 0) {
            logger.warn("Cannot ETL event " + entity.getLabEventId() + " that has no vessels.");
            return records;
        }

        String eventName = entity.getLabEventType().getName();

        for (LabVessel vessel : vessels) {

            Set<SampleInstance> sampleInstances = vessel.getSampleInstances();
            for (SampleInstance si : sampleInstances) {

                // Should get batch from sample instance if not on labEvent.
                // But for now, just ETL a null lab batch.
                Long labBatchId = noLabBatch ? null : entity.getLabBatch().getLabBatchId();

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

                Long workflowConfigId = lookupWorkflowConfigId(eventName, productOrder, entity.getEventDate());

                records.add(genericRecord(etlDateStr, isDelete,
                        entity.getLabEventId(),
                        format(eventName),
                        format(workflowConfigId),
                        format(productOrder.getProductOrderId()),
                        format(sampleKey),
                        format(labBatchId),
                        format(entity.getEventLocation()),
                        format(vessel.getLabVesselId()),
                        format(ExtractTransform.secTimestampFormat.format(entity.getEventDate()))
                ));
            }

            // Makes a record for the event when there are no samples.
            if (records.size() == 0) {
                records.add(genericRecord(etlDateStr, isDelete,
                        entity.getLabEventId(),
                        format(eventName),
                        format((Long) null),
                        format((String) null),
                        format((String) null),
                        format(noLabBatch ? (Long)null : entity.getLabBatch().getLabBatchId()),
                        format(entity.getEventLocation()),
                        format((String) null),
                        format(ExtractTransform.secTimestampFormat.format(entity.getEventDate()))
                ));
            }
        }

        return records;
    }


    Map<String, List<WorkflowConfigDenorm>> mapEventToWorkflows = null;

    /**
     * Builds 1:N mapping of event name to denorm workflow configs that contain that event name.
     */
    void initWorkflowConfigDenorm() {
        mapEventToWorkflows = new HashMap<String, List<WorkflowConfigDenorm>>();
        WorkflowConfig workflowConfig = workflowLoader.load();
        Collection<WorkflowConfigDenorm> configs = WorkflowConfigDenorm.parse(workflowConfig);
        for (WorkflowConfigDenorm config : configs) {
            List<WorkflowConfigDenorm> workflows = mapEventToWorkflows.get(config.getWorkflowStepEventName());
            if (workflows == null) {
                workflows = new ArrayList<WorkflowConfigDenorm>();
                mapEventToWorkflows.put(config.getWorkflowStepEventName(), workflows);
            }
            workflows.add(config);
        }

        // Sorts each of the workflow lists by descending effective date for easier lookup by date.
        for (List<WorkflowConfigDenorm> workflowList : mapEventToWorkflows.values()) {
            Collections.sort(workflowList, new Comparator<WorkflowConfigDenorm>() {
                @Override
                public int compare(WorkflowConfigDenorm o1, WorkflowConfigDenorm o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }
    }


    private final int CONFIG_ID_CACHE_SIZE = 4;
    private LRUMap configIdCache = new LRUMap(CONFIG_ID_CACHE_SIZE);
    private final Object cacheMutex = new Object();

    /**
     * Returns the id of the relevant WorkflowConfig denormalized record.
     *
     * @return null if no id found
     */
    Long lookupWorkflowConfigId(String eventName, ProductOrder productOrder, Date eventDate) {

        // Checks for a cache hit, which may be a null.
        String cacheKey = eventName + productOrder.getBusinessKey() + eventDate.toString();
        synchronized (configIdCache) {
            if (configIdCache.containsKey(cacheKey)) {
                return (Long) configIdCache.get(cacheKey);
            }
        }

        String workflowName = productOrder.getProduct().getWorkflowName();
        if (workflowName == null) {
            logger.warn("Product " + productOrder.getBusinessKey() + " has no workflow name");
            synchronized (configIdCache) {
                configIdCache.put(cacheKey, null);
            }
            return null;
        }

        // Iterates on the sorted list of workflow configs to find a match having latest effective date.
        List<WorkflowConfigDenorm> denormConfigs = mapEventToWorkflows.get(eventName);
        if (denormConfigs == null) {
            logger.warn("No WorkflowConfig records have event " + eventName);
            synchronized (configIdCache) {
                configIdCache.put(cacheKey, null);
            }
            return null;
        }

        for (WorkflowConfigDenorm denorm : denormConfigs) {
            if (workflowName.equals(denorm.getProductWorkflowName()) && eventDate.after(denorm.getEffectiveDate())) {
                Long id = denorm.getWorkflowConfigDenormId();
                synchronized (configIdCache) {
                    configIdCache.put(cacheKey, id);
                }
                return id;
            }
        }
        logger.warn("No denormalized workflow config for product " + workflowName + " having eventName " + eventName
                + " on date " + eventDate.toString());
        synchronized (configIdCache) {
            configIdCache.put(cacheKey, null);
        }
        return null;
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
