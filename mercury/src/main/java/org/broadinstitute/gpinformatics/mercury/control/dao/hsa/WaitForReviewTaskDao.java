package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class WaitForReviewTaskDao extends GenericDao {

    public List<WaitForReviewTask> findAllByStatus(Status status) {
        return findList(WaitForReviewTask.class, WaitForReviewTask_.status, status);
    }

    public List<WaitForReviewTask> findRunningWithSamples() {
        List<WaitForReviewTask> resultList = new ArrayList<>();

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<WaitForReviewTask> query = cb.createQuery(WaitForReviewTask.class);
        Root<WaitForReviewTask> root = query.from(WaitForReviewTask.class);

        final Join<WaitForReviewTask, State> join =
                root.join(WaitForReviewTask_.state);

        Predicate notEmpty = cb.isNotEmpty(join.get(State_.MERCURY_SAMPLES));
        Predicate running = cb.equal(root.get(WaitForReviewTask_.status), Status.RUNNING);
        query.where(cb.and(running, notEmpty));

        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
        }
        return resultList;
    }

}
