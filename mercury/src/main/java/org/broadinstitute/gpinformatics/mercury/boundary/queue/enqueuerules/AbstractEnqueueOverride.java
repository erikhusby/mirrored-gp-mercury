package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Handles the enqueue logic specific to each type of Queue.  Example: Pico has Clia first, then ExEx, then the default
 * so any ExEx or Clia samples jumps places in the queue at enqueue time.
 */
public abstract class AbstractEnqueueOverride {

    /**
     * We give it the max value so that the when we check the index on it being added to the queue it  will be assigned a proper priority
     */
    private static final int DEFAULT_PRIORITY_INDEX = Integer.MAX_VALUE;

    /**
     * Logic for setting the inial order on a queue grouping.
     *
     * @param queueGrouping                     Grouping to insert into the queue.
     * @param uniqueVesselIdsAlreadyInQueue     Set of VesselIds which have been in the Queue in the past.
     */
    public final void setInitialOrder(QueueGrouping queueGrouping, Set<Long> uniqueVesselIdsAlreadyInQueue) {
        queueGrouping.setQueuePriority(determineQueuePriority(queueGrouping, uniqueVesselIdsAlreadyInQueue));

        insertQueueGroupingIntoQueue(queueGrouping);
    }

    /**
     * Checks to see if "ALL" of the vessels have been in the queue in the past, if not, finds the priority of any
     * vessels which are new to the queue.
     *
     * @param queueGrouping                     Grouping to insert into the queue.
     * @param uniqueVesselIdsAlreadyInQueue     Set of VesselIds which have been in the Queue in the past.
     * @return                                  Algorithmically determined Queue Priority
     */
    protected final QueuePriority determineQueuePriority(QueueGrouping queueGrouping, Set<Long> uniqueVesselIdsAlreadyInQueue) {
        // Check to see if ALL of the items in the grouping have been through pico before.  If so, give it default
        // otherwise utilize the queue specific logic for determining the priority.
        if (checkForPreExistingEntries(queueGrouping, uniqueVesselIdsAlreadyInQueue)) {
            return getDefaultPriority();
        }

        return determineQueuePriority(queueGrouping);
    }

    private boolean checkForPreExistingEntries(QueueGrouping queueGrouping, Set<Long> uniqueVesselIdsAlreadyInQueue) {
        int countFound = 0;
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {

            if (uniqueVesselIdsAlreadyInQueue.contains(queueEntity.getLabVessel().getLabVesselId())) {
                countFound++;
            }
        }

        // If ALL samples have been in the queue in the past, it will stay default regardless of what would have
        // happened the first time it was put into the queue.
        if (countFound == queueGrouping.getQueuedEntities().size()) {
            return true;
        }
        return false;
    }

    /**
     * Determine queue priority Type for the queue grouping being enqueued.
     *
     * @param queueGrouping     QueueGrouping to determine the Queue Priority type for.
     * @return                  QueuePriorityType for the QueueGrouping passed in.
     */
    protected final QueuePriority determineQueuePriority(QueueGrouping queueGrouping) {
        QueuePriority finalPriorityType = getDefaultPriority();
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            QueuePriority priorityType = checkForSpecialPriorityType(queueEntity.getLabVessel().getMercurySamples());
            // Clia has highest priority, so we drop as soon as we find.
            if (priorityType == getQueuePriorityOrder()[0]) {
                return priorityType;
            } else if (getPriorityIndex(getQueuePriorityOrder(), finalPriorityType) < getPriorityIndex(getQueuePriorityOrder(), priorityType)) {
                // Ex Ex. isn't the highest, so we cache to return later
                finalPriorityType = priorityType;
            }

            // This was suggested as a good place to look secondarily. It is possible that the "root" sample has the
            // correct info while the labvessel in question doesn't.  I Assume this crawls up the chain so it will
            // look at each sample properly, but if not I may need to change this.
            for (SampleInstanceV2 sampleInstanceV2 : queueEntity.getLabVessel().getSampleInstancesV2()) {
                priorityType = checkForSpecialPriorityType(sampleInstanceV2.getRootMercurySamples());
                // Clia has highest priority, so we drop as soon as we find.
                if (priorityType == getQueuePriorityOrder()[0]) {
                    return priorityType;
                } else if (getPriorityIndex(getQueuePriorityOrder(), finalPriorityType) < getPriorityIndex(getQueuePriorityOrder(), priorityType)) {
                    // Ex Ex. isn't the highest, so we cache to return later
                    finalPriorityType = priorityType;
                }
            }
        }

        return finalPriorityType;
    }

    private int getPriorityIndex(QueuePriority[] queuePriorityOrder, QueuePriority queuePriority) {

        for (int i = 0; i < queuePriorityOrder.length; i++) {
            if (queuePriorityOrder[i] == queuePriority) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Priority Order for the Queue Priority for the queue being implemented.  Additionally, the ALTERED and default (STANDARD)
     * priorities can be skipped as they'll. 
     * 
     * NOTE: Cannot be null, however it can be empty if only ALTERED and default (STANDARD) are in use.
     *
     * @return Priority order for the queue excluding ALTERED and default (STANDARD)
     */
    @Nonnull
    public abstract QueuePriority[] getQueuePriorityOrder();

    /**
     * Sets the sort order for the grouping, and updates all QueueGroupings which should be set after the newly enqueued item.
     *
     * @param newGrouping     QueueGrouping being enqueued.
     */
    @SuppressWarnings("Duplicates")
    private final void insertQueueGroupingIntoQueue(QueueGrouping newGrouping) {
        long currentSortOrder = 1;

        QueuePriority[] queuePriorityOrder = getQueuePriorityOrder();
        int insertedItemPriorityIndex = DEFAULT_PRIORITY_INDEX;

        for (int i = 0; i < queuePriorityOrder.length; i++) {
            if (newGrouping.getQueuePriority() == queuePriorityOrder[i]) {
                insertedItemPriorityIndex = i;
                break;
            }
        }

        boolean newItemAdded = false;
        // To avoid even the minor possibility of modifying the contents of the collection being iterated.
        List<QueueGrouping> queueGroupings = new ArrayList<>(newGrouping.getAssociatedQueue().getQueueGroupings());
        for (QueueGrouping existingGrouping : queueGroupings) {

            if (existingGrouping.shouldSkipPriorityCheck()) {
                existingGrouping.setSortOrder(currentSortOrder++);
            } else {

                int currentPriorityIndex = Integer.MAX_VALUE; 
                for (int priorityIndex = 0; priorityIndex < queuePriorityOrder.length; priorityIndex++) {
                    if (existingGrouping.getQueuePriority() == queuePriorityOrder[priorityIndex]) {
                        currentPriorityIndex = priorityIndex;
                        break;
                    }
                }
                if (insertedItemPriorityIndex < currentPriorityIndex && !newItemAdded) {
                    newGrouping.setSortOrder(currentSortOrder++);
                    newItemAdded = true;
                }
                existingGrouping.setSortOrder(currentSortOrder++);
            }
        }

        if (!newItemAdded) {
            useDefaultOrdering(newGrouping);
        }
    }

    /**
     * Utilizes the default ordering rules and sets the sort order to be the QueueGroupingId of the item being enqueued.
     * This will ensure that if multiple default enqueues happen at the same time they won't interfere with each other.
     *
     * @param queueGrouping     QueueGrouping being enqueued.
     */
    final void useDefaultOrdering(QueueGrouping queueGrouping) {
        queueGrouping.setSortOrder(queueGrouping.getQueueGroupingId());
    }

    /**
     * Checks the MercurySamples passed in for any of the two special cases.
     *
     * @param mercurySamples    All the MercurySample objects to review
     * @return                  Priority Type found
     */
    protected abstract QueuePriority checkForSpecialPriorityType(Collection<MercurySample> mercurySamples);

    @NotNull
    static QueuePriority getDefaultPriority() {
        return QueuePriority.STANDARD;
    }
}
