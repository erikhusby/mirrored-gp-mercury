package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * Queue Priority Types are utilized by the EnqueueOverride classes to jump new queuegroupings higher into the queue
 * based on Queue specific logic defined in their subclasses.
 *
 * NOTE:  This isn't just for Pico, all Queues can implement their own priority types by adding
 * them here.
 *
 * Because each defines the items themselves, we don't need to worry about Types which aren't used by a particular queue.
 */
public enum QueuePriority {
    STANDARD,
    EXOME_EXPRESS,
    CLIA,
    ALTERED
}
