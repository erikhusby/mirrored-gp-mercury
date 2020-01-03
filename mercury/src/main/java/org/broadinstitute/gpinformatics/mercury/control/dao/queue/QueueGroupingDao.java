package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for QueueGroupings.
 */
@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class QueueGroupingDao extends GenericDao {

    /**
     * Finds all the active entities by queue type.
     *
     * @param queueType Queue to search for Active Queue Entities within.
     * @return The active groupings within the requested Queue, sorted by priority.
     */
    public List<QueueGrouping> findActiveGroupsByQueueType(QueueType queueType) {
        return findAll(QueueGrouping.class, (criteriaQuery, root) -> {
            CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
            Path<GenericQueue> genericQueuePath = root.get(QueueGrouping_.associatedQueue);
            criteriaQuery.where(
                    criteriaBuilder.equal(genericQueuePath.get(GenericQueue_.queueType), queueType),
                    root.get(QueueGrouping_.queueStatus).in(Arrays.asList(QueueStatus.Active, QueueStatus.Repeat)));
            criteriaQuery.orderBy(criteriaBuilder.asc(root.get(QueueGrouping_.sortOrder)));
        });
    }

}
