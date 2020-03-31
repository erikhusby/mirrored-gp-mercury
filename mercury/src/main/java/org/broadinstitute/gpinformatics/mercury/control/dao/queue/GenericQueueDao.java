package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

/**
 * DAO for Generic Queues
 */
@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class GenericQueueDao extends GenericDao {

    /**
     * Finds and loads the Queue by its type.
     *
     * @param queueType     Type of Queue to load.
     * @return              The loaded Queue.
     */
    public GenericQueue findQueueByType(QueueType queueType) {
        return findSingle(GenericQueue.class, GenericQueue_.queueType, queueType);
    }
}
