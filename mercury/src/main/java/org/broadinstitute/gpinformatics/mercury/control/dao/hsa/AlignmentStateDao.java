package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.SequencingDemultiplexMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AlignmentStateDao extends GenericDao {

    public AlignmentState findByIdentifier(String stateName) {
        return findSingle(AlignmentState.class, AlignmentState_.stateName, stateName);
    }

    public List<AlignmentState> findCompletedAlignmentsForSample(Collection<MercurySample> samples) {
        if (samples == null || samples.isEmpty()) {
            return Collections.emptyList();
        }

        List<AlignmentState> resultList = new ArrayList<>();

        Set<String> sampleAlias = samples.stream().map(MercurySample::getSampleKey).collect(Collectors.toSet());

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<AlignmentState> query = cb.createQuery(AlignmentState.class);
        Root<AlignmentState> root = query.from(AlignmentState.class);

        final SetJoin<AlignmentState, MercurySample> setJoin =
                root.join(AlignmentState_.mercurySamples);

        // Select states with completed tasks only
        Subquery<State> sq = query.subquery(State.class);
        Root<Task> subRoot = sq.from(Task.class);
        sq.select(subRoot.get(Task_.state));
        Predicate taskStatePred = cb.equal(root.get(State_.stateId), subRoot.get(Task_.state));
        Predicate taskComplete = cb.equal(subRoot.get(Task_.status), Status.COMPLETE);
        sq.where(cb.and(taskStatePred, taskComplete));
        Path<Long> stateId = root.get(AlignmentState_.stateId);
        Predicate subQueryPredicate = stateId.in(sq);

        Expression<String> parentExpression = setJoin.get(MercurySample_.sampleKey);
        Predicate parentPredicate = parentExpression.in(sampleAlias);

        query.where(cb.and(parentPredicate, subQueryPredicate));
        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }
}
