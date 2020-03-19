package org.broadinstitute.gpinformatics.mercury.control.dao.manifest;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO for ManifestSessions.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ManifestSessionDao extends GenericDao {

    public ManifestSession find(long id) {
        return findById(ManifestSession.class, id);
    }

    public List<ManifestSession> findOpenSessions() {
        return findListByList(ManifestSession.class, ManifestSession_.status, EnumSet.of(
                ManifestSession.SessionStatus.OPEN, ManifestSession.SessionStatus.ACCESSIONING,
                ManifestSession.SessionStatus.PENDING_SAMPLE_INFO)).stream().
                // Excludes CRSP manifest sessions without a research project such as Mayo manifests.
                filter(manifestSession -> manifestSession.getResearchProject() != null
                                          || (manifestSession.getAccessioningProcessType() == ManifestSessionEjb.AccessioningProcessType.COVID)).
                collect(Collectors.toList());
    }

    /**
     * Return ManifestSessions in COMPLETED status having tubes remaining to be transferred (manifest records with
     * status SAMPLE_TRANSFERRED_TO_TUBE).
     */
    public List<ManifestSession> findSessionsEligibleForTubeTransfer() {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        final CriteriaQuery<ManifestSession> query = criteriaBuilder.createQuery(ManifestSession.class);
        query.distinct(true);

        Root<ManifestSession> root = query.from(ManifestSession.class);

        Predicate completedStatus =
                criteriaBuilder.equal(root.get(ManifestSession_.status), ManifestSession.SessionStatus.COMPLETED);

        // The two CriteriaBuilder#diff calls below calculate the result of
        //
        // totalNumberOfRecords - numberOfQuarantinedRecords - numberOfTubesTransferred
        //
        // If the result of this expression is not zero and the session is not in COMPLETED status, the session should
        // be considered eligible for tube transfer.
        Expression<Integer> totalMinusQuarantined =
                criteriaBuilder.diff(root.get(ManifestSession_.totalNumberOfRecords),
                        root.get(ManifestSession_.numberOfQuarantinedRecords));

        Expression<Integer> numberOfTubesRemainingToBeTransferred = criteriaBuilder
                .diff(totalMinusQuarantined, root.get(ManifestSession_.numberOfTubesTransferred));

        Predicate tubesRemainingToBeTransferred =
                criteriaBuilder.and(
                        criteriaBuilder.notEqual(criteriaBuilder.toInteger(numberOfTubesRemainingToBeTransferred), 0),
                        criteriaBuilder.equal(root.get(ManifestSession_.fromSampleKit), Boolean.FALSE));

        query.where(completedStatus, tubesRemainingToBeTransferred);

        return getEntityManager().createQuery(query).getResultList();
    }

    public List<ManifestSession> getSessionsForReceiptTicket(String receiptTicket) {

        return findListByList(ManifestSession.class, ManifestSession_.receiptTicket,
                Collections.singleton(receiptTicket), new GenericDaoCallback<ManifestSession>() {
                    @Override
                    public void callback(CriteriaQuery<ManifestSession> criteriaQuery, Root<ManifestSession> root) {
                        criteriaQuery.where(getCriteriaBuilder().equal(root.get(ManifestSession_.status),
                                ManifestSession.SessionStatus.COMPLETED));
                    }
                });
    }

    /**
     * Returns the manifest for the given sessionPrefix. For Mayo manifests there will be one, or none.
     */
    public ManifestSession getSessionByPrefix(String prefix) {
        return findSingle(ManifestSession.class, ManifestSession_.sessionPrefix, prefix);
    }

    /**
     * Returns the manifest for the given vessel label. For Mayo manifests there will be one, or none.
     */
    public ManifestSession getSessionByVesselLabel(String label) {
        // FYI this code was observed to be doing an inner join with manifest_vessel_labels
        // using the label in the where clause, so it will be an efficient indexed lookup.
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<ManifestSession> query = builder.createQuery(ManifestSession.class);
        Root<ManifestSession> root = query.from(ManifestSession.class);
        SetJoin<ManifestSession, String> vesselLabels = root.join(ManifestSession_.vesselLabels);
        ParameterExpression<String> param = builder.parameter(String.class);
        query.where(builder.equal(vesselLabels, param));
        TypedQuery<ManifestSession> manifestQuery = getEntityManager().createQuery(query);
        manifestQuery.setParameter(param, label);
        return manifestQuery.getResultList().stream().findFirst().orElse(null);
    }
}
