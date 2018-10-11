package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles the enqueue logic specific to each type of Queue.  Example: Pico has Clia first, then ExEx, then Standard
 * so any ExEx or Clia samples jumps places in the queue at enqueue time.
 */
public abstract class AbstractEnqueueOverride {

    private static final int DEFAULT_PRIORITY_INDEX = Integer.MAX_VALUE;

    /**
     * Logic for setting the inial order on a queue grouping.
     *
     * @param queueGrouping     Grouping to insert into the queue.
     * @param uniqueVesselIdsAlreadyInQueue
     */
    public final void setInitialOrder(QueueGrouping queueGrouping, Set<Long> uniqueVesselIdsAlreadyInQueue) {
        queueGrouping.setQueuePriority(determineQueuePriorityType(queueGrouping, uniqueVesselIdsAlreadyInQueue));

        insertQueueGroupingIntoQueue(queueGrouping);
    }

    private QueuePriority determineQueuePriorityType(QueueGrouping queueGrouping, Set<Long> uniqueVesselIdsAlreadyInQueue) {
        // Check to see if ALL of the items in the grouping have been through pico before.  If so, give it standard
        // otherwise utilize the queue specific logic for determining the priority.
        int countFound = 0;
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            if (uniqueVesselIdsAlreadyInQueue.contains(queueEntity.getLabVessel().getLabVesselId())) {
                countFound++;
            }
        }

        if (countFound == queueGrouping.getQueuedEntities().size()) {
            return QueuePriority.STANDARD;
        }

        return determineQueuePriorityType(queueGrouping);
    }

    /**
     * Determine queue priority Type for the queue grouping being enqueued.
     *
     * @param queueGrouping     QueueGrouping to determine the Queue Priority type for.
     * @return                  QueuePriorityType for the QueueGrouping passed in.
     */
    protected abstract QueuePriority determineQueuePriorityType(QueueGrouping queueGrouping);

    /**
     * Priority Order for the Queue Priority for the queue being implemented.  Additionally, the HEIGHTENED and STANDARD
     * priorities can be skipped as they'll. 
     * 
     * NOTE: Cannot be null, however it can be empty if only HEIGHTENED and STANDARD are in use.
     *
     * @return Priority order for the queue excluding HEIGHTENED and STANDARD
     */
    @Nonnull
    public abstract QueuePriority[] getQueuePriorityOrder();

    /**
     * Sets the sort order for the grouping, and updates all QueueGroupings which should be set after the newly enqueued item.
     *
     * @param queueGrouping     QueueGrouping being enqueued.
     */
    @SuppressWarnings("Duplicates")
    private void insertQueueGroupingIntoQueue(QueueGrouping queueGrouping) {
        long currentSortOrder = 1;

        QueuePriority[] queuePriorityOrder = getQueuePriorityOrder();
        int insertedItemPriorityIndex = DEFAULT_PRIORITY_INDEX;

        for (int i = 0; i < queuePriorityOrder.length; i++) {
            if (queueGrouping.getQueuePriority() == queuePriorityOrder[i]) {
                insertedItemPriorityIndex = i;
                break;
            }
        }

        boolean newItemAdded = false;
        // To avoid even the minor possibility of modifying the contents of the collection being iterated.
        List<QueueGrouping> queueGroupings = new ArrayList<>(queueGrouping.getAssociatedQueue().getQueueGroupings());
        for (QueueGrouping grouping : queueGroupings) {

            if (grouping.getQueuePriority() == QueuePriority.HEIGHTENED) {
                grouping.setSortOrder(currentSortOrder++);
            } else {

                int currentPriorityIndex = Integer.MAX_VALUE; 
                for (int priorityIndex = 0; priorityIndex < queuePriorityOrder.length; priorityIndex++) {
                    if (grouping.getQueuePriority() == queuePriorityOrder[priorityIndex]) {
                        currentPriorityIndex = priorityIndex;
                        break;
                    }
                }
                if (insertedItemPriorityIndex < currentPriorityIndex && !newItemAdded) {
                    queueGrouping.setSortOrder(currentSortOrder++);
                    newItemAdded = true;
                }
                grouping.setSortOrder(currentSortOrder++);
            }
        }

        if (!newItemAdded) {
            useDefaultOrdering(queueGrouping);
        }
    }

    /**
     * Utilizes the default ordering rules and sets the sort order to be the QueueGroupingId of the item being enqueued.
     * This will ensure that if multiple standard enqueues happen at the same time they won't interfere with each other.
     *
     * @param queueGrouping     QueueGrouping being enqueued.
     */
    @SuppressWarnings("WeakerAccess")
    final void useDefaultOrdering(QueueGrouping queueGrouping) {
        queueGrouping.setSortOrder(queueGrouping.getQueueGroupingId());
    }
}
