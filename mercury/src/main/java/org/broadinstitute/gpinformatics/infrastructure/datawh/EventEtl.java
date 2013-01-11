package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.tools.javac.resources.version;
import junit.framework.Assert;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;

@Stateless
public class EventEtl extends GenericEntityEtl {
    @Inject
    LabEventDao dao;

    @Inject
    ProductOrderDao pdoDao;

    @Inject
    WorkflowConfig workflowConfig;

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
        return ((LabEvent)entity).getLabEventId();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        LabEvent entity = dao.findById(LabEvent.class, entityId);
        if (entity == null) {
            logger.info("Cannot export.  LabEvent having id " + entityId + " no longer exists.");
            return null;
        }
        return entityRecord(etlDateStr, isDelete, entity);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        Collection<String> recordList = new ArrayList<String>();
        List<LabEvent> entityList = dao.findAll(LabEvent.class,
                new GenericDao.GenericDaoCallback<Product>() {
                    @Override
                    public void callback(CriteriaQuery<Product> cq, Root<Product> root) {
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
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        Collection<String> records = new ArrayList<String>();

        LabBatch labBatch = entity.getLabBatch();
        String eventName = (labBatch != null && entity.getLabEventType() != null)
                ? entity.getLabEventType().getName() : null;
        Long labBatchId = labBatch != null ? labBatch.getLabBatchId() : null;
        JiraTicket ticket = labBatch != null ? labBatch.getJiraTicket() : null;
        String ticketName = ticket != null ? ticket.getTicketName() : null;

        Set<LabVessel> vessels = entity.getTargetLabVessels();
        if (vessels != null) {
            if (vessels.size() == 0) {
                vessels.add(entity.getInPlaceLabVessel());
            }
            for (LabVessel vessel : vessels) {

                Set<SampleInstance> sampleInstances = vessel.getSampleInstances();
                for (SampleInstance si : sampleInstances) {
                    MercurySample sample = si.getStartingSample();
                    String sampleKey = sample.getSampleKey();

                    records.add(genericRecord(etlDateStr, isDelete,
                            entity.getLabEventId(),
                            format(eventName),
                            format(lookupWorkflowConfigId(sample, entity.getEventDate())),
                            format(ticketName),
                            format(sampleKey),
                            format(labBatchId),
                            format(entity.getEventLocation()),
                            format(vessel.getLabVesselId()),
                            format(entity.getEventDate())
                    ));
                }
            }
        };
        // Makes at least one record for the event.
        if (records.size() == 0) {
            records.add(genericRecord(etlDateStr, isDelete,
                    entity.getLabEventId(),
                    format(eventName),
                    format((Long)null),
                    format(ticketName),
                    format((String)null),
                    format(labBatchId),
                    format(entity.getEventLocation()),
                    format((String)null),
                    format(entity.getEventDate())
            ));
        }
        return records;
    }

    /** Maps event name to a list of possible workflow records which must be resolved by desired date. */
    private Map<String, SortedSet<WorkflowConfigDenorm>> eventToWorkflowList = new HashMap<String, SortedSet<WorkflowConfigDenorm>>();

    /** Builds maps of event name to a list of possible workflow records. */
    void initWorkflowConfigDenorm() {
        Collection<WorkflowConfigDenorm> configs = WorkflowConfigDenorm.parse(workflowConfig);
        for (WorkflowConfigDenorm config : configs) {
            SortedSet<WorkflowConfigDenorm> workflows = eventToWorkflowList.get(config.getWorkflowStepEventName());
            if (workflows == null) {
                workflows = new TreeSet<WorkflowConfigDenorm>(new Comparator<WorkflowConfigDenorm>() {
                    // The denorm configs are sorted by descending effective date.
                    @Override
                    public int compare(WorkflowConfigDenorm o1, WorkflowConfigDenorm o2) {
                        return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                    }
                });
                eventToWorkflowList.put(config.getWorkflowStepEventName(), workflows);
            }
            workflows.add(config);
        }
    }

    /** Returns the id of the relevant WorkflowConfig denormalized record. */
    long lookupWorkflowConfigId(String eventName, MercurySample sample, Date eventDate) {
        if (sample == null) {
            logger.debug("Cannot lookup workflow for null sample");
            return 0L;
        }

        String productOrderKey = sample.getProductOrderKey();
        if (productOrderKey == null) {
            logger.debug("Sample " + sample.getSampleKey() + " has null productOrderKey");
            return 0L;
        }
        ProductOrder productOrder = pdoDao.findByBusinessKey(productOrderKey);
        if (productOrder == null) {
            logger.debug("Product " + productOrderKey + " has no entity");
            return 0L;
        }
        String workflowName = productOrder.getProduct().getWorkflowName();
        if (workflowName == null) {
            logger.debug("Product " + productOrderKey + " has no workflow");
            return 0L;
        }

        SortedSet<WorkflowConfigDenorm> set = eventToWorkflowList.get(eventName);
        for (WorkflowConfigDenorm denorm : set) {
            if (workflowName.equals(denorm.getProductWorkflowName())
                    && eventDate.after(denorm.getEffectiveDate())) {
                return denorm.getWorkflowConfigDenormId();
            }
        }

        return 0;
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }

}
