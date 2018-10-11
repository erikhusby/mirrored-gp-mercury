package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity_;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.hibernate.Criteria;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class GenericQueueDao extends GenericDao {

    public GenericQueue findQueueByType(QueueType queueType) {
        return findSingle(GenericQueue.class, GenericQueue_.queueType, queueType);
    }

    public List<QueueEntity> findEntitiesByVesselIds(List<Long> vesselIds) {
        if (CollectionUtils.isEmpty(vesselIds)) {
            return new ArrayList<>();
        }
        return findAll(QueueEntity.class, new GenericDaoCallback<QueueEntity>() {
            @Override
            public void callback(CriteriaQuery<QueueEntity> criteriaQuery, Root<QueueEntity> root) {
                CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
                Path<LabVessel> labVesselPath = root.get(QueueEntity_.labVessel);
                criteriaQuery.where(criteriaBuilder.in(labVesselPath.get(LabVessel_.labVesselId)).in(vesselIds));
            }
        });
    }
}
