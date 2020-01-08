package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for QueueGroupings.
 */
@Stateful
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
            Join<QueueGrouping, GenericQueue> genericQueueJoin = root.join(QueueGrouping_.associatedQueue, JoinType.INNER);
            criteriaQuery.where(
                    criteriaBuilder.equal(genericQueueJoin.get(GenericQueue_.queueType), queueType),
                    root.get(QueueGrouping_.queueStatus).in(QueueStatus.Active, QueueStatus.Repeat));
            criteriaQuery.orderBy(criteriaBuilder.asc(root.get(QueueGrouping_.sortOrder)));
        });
    }

    /**
     * For a given queue group, get counts of vessels for each QueueStatus
     */
    public Map<QueueStatus, Long> getEntityStatusCounts(Long queueGroupingId) {
        Map<QueueStatus, Long> entitiesInQueueByPriority = new HashMap<>();

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<Tuple> qry = criteriaBuilder.createTupleQuery();
        Root<QueueEntity> root = qry.from(QueueEntity.class);
        qry.multiselect(root.get(QueueEntity_.queueStatus), criteriaBuilder.count(root.get(QueueEntity_.queueEntityId)));
        qry.groupBy(root.get(QueueEntity_.queueStatus));
        qry.where(criteriaBuilder.equal(root.get(QueueEntity_.queueGrouping), queueGroupingId));
        List<Tuple> results = getEntityManager().createQuery(qry).getResultList();

        for (Tuple vals : results) {
            entitiesInQueueByPriority.put((QueueStatus) vals.get(0), (Long) vals.get(1));
        }

        return entitiesInQueueByPriority;
    }

    /**
     * Rename queue grouping
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void renameGrouping(Long queueGroupingId, String newName) throws IllegalArgumentException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery qry = criteriaBuilder.createQuery();
        Root<QueueGrouping> root = qry.from(QueueGrouping.class);
        qry.select(root.get(QueueGrouping_.queueGroupingId));
        qry.where(criteriaBuilder.equal(root.get(QueueGrouping_.queueGroupingText), newName));
        List<Object> results = getEntityManager().createQuery(qry).getResultList();
        if (results.size() > 0) {
            throw new IllegalArgumentException("Queue grouping name already exists.");
        }

        List<QueueGrouping> groups = findAll(QueueGrouping.class, (criteriaQuery, groupRoot) -> {
            criteriaQuery.where(
                    criteriaBuilder.equal(groupRoot.get(QueueGrouping_.queueGroupingId), queueGroupingId));
        });

        if (groups.size() == 0) {
            throw new IllegalArgumentException("No grouping with ID " + queueGroupingId + " exists.");
        }

        groups.get(0).setQueueGroupingText(newName);
        flush();
    }

}
