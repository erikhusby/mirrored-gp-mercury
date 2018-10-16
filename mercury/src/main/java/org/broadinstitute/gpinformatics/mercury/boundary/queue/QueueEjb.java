package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.QueueValidationHandler;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    private QueueValidationHandler queueValidationHandler = new QueueValidationHandler();


    /**
     * Adds either a container lab vessel with its tubes, or a larger list of lab vessels of any type to a queue as a
     * single queue group.  The general intent is for all lab vessels added together to stay together.
     *
     * @param containerVessel   Container vessel if there is a single container being added.
     * @param vesselList        List of vessels to queue up as a single group.
     * @param readableText      Text displayed on the queue row for this item.  If none is there, default text will be
     *                          provided.  Recommended that if a single container is utilized, you use the barcode of
     *                          the container lab vessel.
     * @param queueType         Type of Queue to add the lab vessels to.
     */
    public void enqueueLabVessels(@Nullable LabVessel containerVessel, @Nonnull Collection<? extends LabVessel> vesselList,
                                  @Nonnull QueueType queueType, @Nullable String readableText,
                                  @Nonnull MessageCollection messageCollection) {

        // TODO:  If containerVessel is null, error if readabletext is null.  If containerVessel is not null, and
        // TODO:  readable text is null, then stilck the barcode from containerVessel into readable text


        // TODO:  Verify that the overrides are only done IF it is the first time a vessel is put into the queue.

        GenericQueue genericQueue = findQueueByType(queueType);

        if (genericQueue.getQueueGroupings() == null) {
            genericQueue.setQueueGroupings(new TreeSet<>(QueueGrouping.BY_SORT_ORDER));
        }

        QueueGrouping queueGrouping = createGroupingAndSetInitialOrder(containerVessel, readableText, genericQueue);

        queueGrouping.setAssociatedQueue(genericQueue);
        genericQueue.getQueueGroupings().add(queueGrouping);

        for (LabVessel labVessel : vesselList) {
            try {
                queueValidationHandler.validate(labVessel, queueType, messageCollection);
            } catch (Exception e) {
                messageCollection.addWarning("Internal error trying to validate " + labVessel.getLabel());
            }
            QueueEntity queueEntity = new QueueEntity(queueGrouping, labVessel);
            queueGrouping.getQueuedEntities().add(queueEntity);
            persist(queueEntity);
        }
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
    public void dequeueLabVessels(Collection<? extends LabVessel> labVessels, QueueType queueType,
                                  MessageCollection messageCollection, DequeueingOptions dequeueingOptions) {

        Iterator<? extends LabVessel> iterator = labVessels.iterator();
        GenericQueue genericQueue = findQueueByType(queueType);

        for (QueueGrouping queueGrouping : genericQueue.getQueueGroupings()) {
            if (!iterator.hasNext()) {
                break;
            }
            boolean found = false;
            LabVessel labVessel = iterator.next();
            if (!queueValidationHandler.isComplete(labVessel, queueType, messageCollection) && dequeueingOptions == DequeueingOptions.DEFAULT_DEQUEUE_RULES) {
                messageCollection.addWarning(labVessel.getLabel() + " has been denoted as not yet completed" +
                        " from the " + queueType.getTextName() + " queue.");
            } else if (queueGrouping.getContainerVessel() != null
                            && queueGrouping.getContainerVessel().getLabVesselId().equals(labVessel.getLabVesselId())) {
                messageCollection.addWarning("The lab vessel " + labVessel.getLabel()
                                           + " is a container vessel and not allowed to be utilized during the"
                                           + " dequeueing process " + queueType.getTextName() + ".");
            } else {
                for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                    if (queueEntity.getLabVessel().getLabVesselId().equals(labVessel.getLabVesselId())) {
                        updateQueueEntityStatus(messageCollection, queueEntity, QueueStatus.Completed);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                messageCollection.addWarning("The lab vessel " + labVessel.getLabel()
                        + " was not found in the " + queueType.getTextName() + " queue.");
            }
        }
    }

    /**
     * Changes the ordering of the queue to whatever is passed in by the user.
     *  @param queueGroupingId
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
                    queueGroupingBeingMoved.setQueuePriority(QueuePriority.HEIGHTENED);
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
                    if (queueEntity.getLabVessel().getLabVesselId().equals(labVessel.getLabVesselId())) {
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

    public QueueGrouping createGroupingAndSetInitialOrder(@Nullable LabVessel containerVessel, @Nullable String readableText, GenericQueue genericQueue) {
        QueueGrouping queueGrouping = new QueueGrouping(containerVessel, readableText, genericQueue);
        persist(queueGrouping);
        genericQueueDao.flush();
        setInitialOrder(queueGrouping);
        return queueGrouping;
    }

    private void setInitialOrder(QueueGrouping queueGrouping) {
        try {
            AbstractEnqueueOverride enqueueOverride = queueGrouping.getAssociatedQueue().getQueueType().getEnqueueOverrideClass().newInstance();

            // Find the vessel ids which already have been in the queue.  These would get standard priority.
            List<Long> vesselIds = new ArrayList<>();
            List<QueueEntity> entitiesByVesselIds = genericQueueDao.findEntitiesByVesselIds(vesselIds);

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
}
