package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LabBatchDao extends GenericDao {

    public LabBatch findByName(String batchName) {
        return findSingle(LabBatch.class, LabBatch_.batchName, batchName);
    }

    public List<LabBatch> findByListIdentifier(List<String> searchList) {
        return findListByList(LabBatch.class, LabBatch_.batchName, searchList);
    }

    /**
     * @param labBatchType The desired LabBatch.LabBatchType
     * @return A list of lab batches sorted descending by created date
     */
    public List<LabBatch> findByType(LabBatch.LabBatchType labBatchType) {
        return findByTypeAndActiveStatus(labBatchType, null);
    }

    /**
     * @param labBatchType The desired LabBatch.LabBatchType
     * @param isActive     The desired active status, null to ignore status and return all statuses
     * @return A list of lab batches sorted descending by created date
     */
    public List<LabBatch> findByTypeAndActiveStatus(LabBatch.LabBatchType labBatchType, Boolean isActive) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<LabBatch> criteriaQuery = criteriaBuilder.createQuery(LabBatch.class);
        Root<LabBatch> root = criteriaQuery.from(LabBatch.class);
        Predicate[] predicates;
        if (isActive == null) {
            predicates = new Predicate[]{criteriaBuilder.equal(root.get(LabBatch_.labBatchType), labBatchType)};
        } else {
            predicates = new Predicate[]{
                    criteriaBuilder.equal(root.get(LabBatch_.labBatchType), labBatchType),
                    criteriaBuilder.equal(root.get(LabBatch_.isActive), isActive)
            };
        }

        criteriaQuery.where(predicates)
                .orderBy(criteriaBuilder.desc(root.get(LabBatch_.createdOn)));

        try {
            return getQuery(criteriaQuery, LockModeType.NONE).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    public LabBatch findByBusinessKey(String businessKey) {
        return findByName(businessKey);
    }
}
