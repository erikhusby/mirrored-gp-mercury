package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.QueueValidationHandler;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Stateful
@RequestScoped
public class QueueEjb {

    public QueueEjb() {
    }

    public QueueEjb(GenericQueueDao genericQueueDao, QueueValidationHandler queueValidationHandler) {
        this.genericQueueDao = genericQueueDao;
        this.queueValidationHandler = queueValidationHandler;
    }

    @Inject
    private GenericQueueDao genericQueueDao;

    @Inject
    private LabVesselDao labVesselDao;

    private QueueValidationHandler queueValidationHandler = new QueueValidationHandler();


    /**
     * Adds either a container lab vessel with its tubes, or a larger list of lab vessels of any type to a queue as a
     * single queue group.  The general intent is for all lab vessels added together to stay together.
     *
     * @param vesselList        List of vessels to queue up as a single group.
     * @param readableText      Text displayed on the queue row for this item.  If none is there, default text will be
     *                          provided.  Recommended that if a single container is utilized, you use the barcode of
     *                          the container lab vessel.
     * @param queueType         Type of Queue to add the lab vessels to.
     */
    public Long enqueueLabVessels(@Nonnull Collection<LabVessel> vesselList,
                                  @Nonnull QueueType queueType, @Nullable String readableText,
                                  @Nonnull MessageCollection messageCollection) {

        GenericQueue genericQueue = findQueueByType(queueType);

        if (genericQueue.getQueueGroupings() == null) {
            genericQueue.setQueueGroupings(new TreeSet<>(QueueGrouping.BY_SORT_ORDER));
        }
        List<Long> vesselIds = getApplicableLabVesselIds(vesselList);

        boolean isUniqueSetOfActiveVessels = true;

        for (QueueGrouping queueGrouping : genericQueue.getQueueGroupings()) {
            if (queueGrouping.getQueuedEntities().size() == vesselList.size()) {
                // Make a copy of the vessel ids list so we can try to add all the vessel ids and see if there are any differences
                Set<Long> verifyingVesselIds = new HashSet<>(vesselIds);
                for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                    // If the status isn't active, already there is a difference so cut out.
                    if (queueEntity.getQueueStatus() != QueueStatus.Active) {
                        verifyingVesselIds.clear();
                        break;
                    }
                    // try to add the vessel id - if successful it is different list so we can cut out.
                    if (verifyingVesselIds.add(queueEntity.getLabVessel().getLabVesselId())) {
                        break;
                    }
                }
                // Since the # of the entities in the group is the same as the vessels, and the Ids are identical in both
                // then we know it is a duplicate request and can stop trying to enqueue.
                if (verifyingVesselIds.size() == vesselList.size()) {
                    isUniqueSetOfActiveVessels = false;
                }
            }
        }

        if (isUniqueSetOfActiveVessels) {
            QueueGrouping queueGrouping = createGroupingAndSetInitialOrder(readableText, genericQueue, vesselList);

            genericQueue.getQueueGroupings().add(queueGrouping);
            try {
                queueValidationHandler.validate(vesselList, queueType, messageCollection);
            } catch (Exception e) {
                messageCollection.addWarning("Internal error trying to validate: " + e.getMessage());
            }
            return queueGrouping.getQueueGroupingId();
        }

        return null;
    }

    @NotNull
    private List<Long> getApplicableLabVesselIds(@Nonnull Collection<LabVessel> vesselList) {
        List<Long> vesselIds = new ArrayList<>();
        for (LabVessel labVessel : vesselList) {

            if (labVessel.getContainerRole() != null) {
                for (LabVessel vessel : labVessel.getContainerRole().getMapPositionToVessel().values()) {
                    vesselIds.add(vessel.getLabVesselId());
                }
            } else {
                vesselIds.add(labVessel.getLabVesselId());
            }
        }
        return vesselIds;
    }

    /**
     * Removes Lab Vessels from a queue.  It is expected only to utilize tubes and not full containers in the dequeue
     * process.
     *
     * @param labVessels            Vessels to Dequeue.
     * @param queueType             Queue Type to remove from.
     * @param messageCollection     Messages back to the user.
     * @param dequeueingOptions     Dequeueing Options
     */
    @SuppressWarnings("WeakerAccess")
    public void dequeueLabVessels(Collection<LabVessel> labVessels, QueueType queueType,
                                  MessageCollection messageCollection, DequeueingOptions dequeueingOptions) {

        List<Long> labVesselIds = getApplicableLabVesselIds(labVessels);

        // Finds all the Active entities by the vessel Ids
        List<QueueEntity> entitiesByVesselIds = genericQueueDao.findActiveEntitiesByVesselIds(queueType, labVesselIds);

        // Check for completeness, then if somplete update status.
        for (QueueEntity queueEntity : entitiesByVesselIds) {
            if (!queueValidationHandler.isComplete(queueEntity.getLabVessel(), queueType, messageCollection)
                            && dequeueingOptions == DequeueingOptions.DEFAULT_DEQUEUE_RULES) {
                messageCollection.addWarning(queueEntity.getLabVessel().getLabel()
                        + " has been denoted as not yet completed"
                        + " from the " + queueType.getTextName() + " queue.");
            } else {
                updateQueueEntityStatus(messageCollection, queueEntity, QueueStatus.Completed);
            }
        }
    }

    /**
     * Changes the ordering of the queue to whatever is passed in by the user.
     * @param queueGroupingId
     * @param positionToMoveTo
     * @param queueType             Queue to re-order
     * @param messageCollection
     */
    public void reOrderQueue(Long queueGroupingId, Integer positionToMoveTo, QueueType queueType, MessageCollection messageCollection) {

        GenericQueue genericQueue = findQueueByType(queueType);

        long currentIndex = 1;

        QueueGrouping queueGroupingBeingMoved = null;

        List<QueueGrouping> queueGroupings = new ArrayList<>(genericQueue.getQueueGroupings());

        for (QueueGrouping queueGrouping : queueGroupings) {
            if (queueGrouping.getQueueGroupingId().equals(queueGroupingId)) {
                queueGroupingBeingMoved = queueGrouping;
            }
        }

        if (queueGroupingBeingMoved == null) {
            messageCollection.addError("Error finding the Queue'd item you wish to move within the queue.");
        } else {

            for (QueueGrouping queueGrouping : queueGroupings) {
                if (positionToMoveTo.longValue() == currentIndex) {
                    queueGroupingBeingMoved.setSortOrder(currentIndex++);
                    queueGroupingBeingMoved.setQueuePriority(QueuePriority.ALTERED);
                }

                if (!queueGrouping.getQueueGroupingId().equals(queueGroupingId)) {
                    queueGrouping.setSortOrder(currentIndex++);
                }
            }
        }
    }

    public void excludeItems(Collection<? extends LabVessel> labVesselsToExclude, QueueType queueType, MessageCollection messageCollection) {

        GenericQueue genericQueue = findQueueByType(queueType);

        for (LabVessel labVessel : labVesselsToExclude) {
            for (QueueGrouping queueGrouping : genericQueue.getQueueGroupings()) {
                for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                    if (getApplicableLabVesselIds(Collections.singleton(queueEntity.getLabVessel())).contains(labVessel.getLabVesselId())) {
                        updateQueueEntityStatus(messageCollection, queueEntity, QueueStatus.Excluded);
                    }
                }
            }
        }
    }

    private void updateQueueEntityStatus(MessageCollection messageCollection, QueueEntity queueEntity, QueueStatus queueStatus) {

        switch (queueStatus) {
            case Completed:
            case Excluded:
                if (queueEntity.getQueueStatus() == QueueStatus.Active) {
                    queueEntity.setQueueStatus(queueStatus);
                } else {
                    messageCollection.addInfo(queueEntity.getLabVessel().getLabel() + " was attempted to be "
                            + queueStatus.name() + " but was not active, it currently is: " + queueEntity.getQueueStatus().name());
                }
                break;
            default:
                throw new RuntimeException("Unexpected update status.");
        }
    }

    public QueueGrouping createGroupingAndSetInitialOrder(@Nullable String readableText, GenericQueue genericQueue, Collection<LabVessel> vesselList) {
        QueueGrouping queueGrouping = new QueueGrouping(readableText, genericQueue);

        queueGrouping.setAssociatedQueue(genericQueue);
        persist(queueGrouping);
        genericQueueDao.flush();

        for (LabVessel labVessel : vesselList) {

            QueueEntity queueEntity = new QueueEntity(queueGrouping, labVessel);
            queueGrouping.getQueuedEntities().add(queueEntity);
            persist(queueEntity);
        }
        setInitialOrder(queueGrouping);
        return queueGrouping;
    }

    private void setInitialOrder(QueueGrouping queueGrouping) {
        try {
            AbstractEnqueueOverride enqueueOverride = queueGrouping.getAssociatedQueue().getQueueType().getEnqueueOverrideClass().newInstance();

            // Find the vessel ids which already have been in the queue.  These would get standard priority.
            List<Long> vesselIds = new ArrayList<>();
            // Grab the vessel is from the queue entity
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                vesselIds.add(queueEntity.getLabVessel().getLabVesselId());

                // grab the vessel id from the root mercury samples
                for (SampleInstanceV2 sampleInstanceV2 : queueEntity.getLabVessel().getSampleInstancesV2()) {
                    for (MercurySample mercurySample : sampleInstanceV2.getRootMercurySamples()) {
                        for (LabVessel labVessel : mercurySample.getLabVessel()) {
                            vesselIds.add(labVessel.getLabVesselId());
                        }
                    }
                }
            }

            // Find the existing entities
            List<QueueEntity> entitiesByVesselIds = genericQueueDao.findEntitiesByVesselIds(queueGrouping.getAssociatedQueue().getQueueType(), vesselIds);

            Set<Long> uniqueVesselIdsAlreadyInQueue = new HashSet<>();
            for (QueueEntity entity : entitiesByVesselIds) {
                uniqueVesselIdsAlreadyInQueue.add(entity.getLabVessel().getLabVesselId());
            }

            enqueueOverride.setInitialOrder(queueGrouping, uniqueVesselIdsAlreadyInQueue);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void persist(QueueEntity queueEntity) {
        genericQueueDao.persist(queueEntity);
    }

    private void persist(QueueGrouping queueGrouping) {
        genericQueueDao.persist(queueGrouping);
    }

    public GenericQueue findQueueByType(QueueType queueType) {
        return genericQueueDao.findQueueByType(queueType);
    }

    public void moveToTop(QueueType queueType, Long queueGroupingId) {

        GenericQueue queue = findQueueByType(queueType);

        List<QueueGrouping> groupingsInCurrentOrder = new ArrayList<>(queue.getQueueGroupings());

        long i = 2;
        for (QueueGrouping grouping : groupingsInCurrentOrder) {
            if (grouping.getQueueGroupingId().equals(queueGroupingId)) {
                grouping.setSortOrder(1L);
                break;
            } else {
                grouping.setSortOrder(i++);
            }
        }
    }

    public void moveToBottom(QueueType queueType, Long queueGroupingId) {

        GenericQueue queue = findQueueByType(queueType);

        List<QueueGrouping> groupingsInCurrentOrder = new ArrayList<>(queue.getQueueGroupings());

        long i = 1;

        QueueGrouping groupingToMakeLast = null;
        for (QueueGrouping grouping : groupingsInCurrentOrder) {
            if (grouping.getQueueGroupingId().equals(queueGroupingId)) {
                groupingToMakeLast = grouping;
            } else {
                grouping.setSortOrder(i++);
            }
        }

        if (groupingToMakeLast != null) {
            groupingToMakeLast.setSortOrder(i);
        }
    }

    public void dequeueLabVessels(LabMetricRun labMetricRun, QueueType queueType, MessageCollection messageCollection) {
        List<LabVessel> passingLabVessels = new ArrayList<>();
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            if (labMetric.getLabMetricDecision().getDecision() == LabMetricDecision.Decision.PASS) {
                passingLabVessels.add(labMetric.getLabVessel());
            }
        }

        dequeueLabVessels(passingLabVessels, queueType, messageCollection, DequeueingOptions.OVERRIDE);
    }

    public void excludeItemsById(List<String> excludeVessels, QueueType queueType, MessageCollection messageCollection) {
        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(excludeVessels);

        vessels.addAll(labVesselDao.findByBarcodes(excludeVessels).values());
        vessels.removeAll(Collections.singletonList(null));

        excludeItems(vessels, queueType, messageCollection);
    }
}
