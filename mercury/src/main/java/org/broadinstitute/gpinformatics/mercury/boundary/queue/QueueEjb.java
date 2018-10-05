package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.QueueValidationHandler;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
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

        GenericQueue genericQueue = findQueueByType(queueType);

        if (genericQueue.getQueueGroupings() == null) {
            genericQueue.setQueueGroupings(new TreeSet<>(QueueGrouping.BY_SORT_ORDER));
        }

        QueueGrouping queueGrouping = createGroupingAndSetInitialOrder(containerVessel, readableText);

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
     *
     * @param groupingIdToNewOrder      Map of the grouping ID ot the new order # to set it to
     * @param queueType                 Queue to re-order
     */
    public void reOrderQueue(Map<Long, Long> groupingIdToNewOrder, QueueType queueType,
                             MessageCollection messageCollection) throws ReorderException {

        GenericQueue genericQueue = findQueueByType(queueType);

        Set<Long> testingSet = new HashSet<>(groupingIdToNewOrder.values());
        if (testingSet.size() != groupingIdToNewOrder.size()) {
            messageCollection.addError("The same sample sorting order was assigned to multiple samples. Please fix and try again.");
        }

        if (groupingIdToNewOrder.size() != genericQueue.getQueueGroupings().size()) {
            messageCollection.addError("Not all samples were assigned a sorting order.  Please fix this and try again.");
        }

        if (messageCollection.hasErrors()) {
            throw new ReorderException("Errors in reordering.");
        }

        for (QueueGrouping queueGrouping : genericQueue.getQueueGroupings()) {

            if (groupingIdToNewOrder.containsKey(queueGrouping.getQueueGroupingId())) {
                queueGrouping.setSortOrder(groupingIdToNewOrder.get(queueGrouping.getQueueGroupingId()));
            } else {
                messageCollection.addUniqueError(messageCollection, "Not all queued items had an order" +
                        " number set upon them.");
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

    private QueueGrouping createGroupingAndSetInitialOrder(@Nullable LabVessel containerVessel, @Nullable String readableText) {
        QueueGrouping queueGrouping = new QueueGrouping(containerVessel, readableText);
        persist(queueGrouping);
        genericQueueDao.flush();
        queueGrouping.setSortOrder(queueGrouping.getQueueGroupingId());
        return queueGrouping;
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

    public void moveToTops(GenericQueue queue, Long queueGroupingId) {

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

    public void moveToBottom(GenericQueue queue, Long queueGroupingId) {

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
