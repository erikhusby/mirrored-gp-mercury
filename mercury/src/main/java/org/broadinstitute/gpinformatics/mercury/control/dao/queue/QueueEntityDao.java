package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for QueueEntities.
 */
@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class QueueEntityDao extends GenericDao {

    /**
     * Finds all the active entities by the Vessel Ids passed in.
     *
     * @param queueType     Queue to search for Active Queue Entities within.
     * @param vesselIds     List of Vessel IDs to search for.
     * @return              The active entities within the requested Queue.
     */
    public List<QueueEntity> findActiveEntitiesByVesselIds(QueueType queueType, List<Long> vesselIds) {
        if (CollectionUtils.isEmpty(vesselIds)) {
            return new ArrayList<>();
        }
        return findAll(QueueEntity.class, (criteriaQuery, root) -> {
            CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
            Path<LabVessel> labVesselPath = root.get(QueueEntity_.labVessel);
            Path<QueueGrouping> queueGroupingPath = root.get(QueueEntity_.queueGrouping);
            Path<GenericQueue> genericQueuePath = queueGroupingPath.get(QueueGrouping_.associatedQueue);
            criteriaQuery.where(labVesselPath.get(LabVessel_.labVesselId).in(vesselIds),
                    criteriaBuilder.equal(genericQueuePath.get(GenericQueue_.queueType), queueType),
                    criteriaBuilder.equal(root.get(QueueEntity_.queueStatus), QueueStatus.Active));
        });
    }
}
