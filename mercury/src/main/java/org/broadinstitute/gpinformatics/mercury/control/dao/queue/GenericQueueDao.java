package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class GenericQueueDao extends GenericDao {

    public GenericQueue findQueueByType(QueueType queueType) {
        return findSingle(GenericQueue.class, GenericQueue_.queueType, queueType);
    }
}
