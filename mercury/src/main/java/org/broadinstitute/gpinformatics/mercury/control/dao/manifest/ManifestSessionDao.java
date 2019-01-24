package org.broadinstitute.gpinformatics.mercury.control.dao.manifest;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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
                ManifestSession.SessionStatus.PENDING_SAMPLE_INFO));
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

    /** Returns the manifests that contain the given package id. */
    public List<ManifestSession> getSessionsForPackage(String packageId) {
        return findList(ManifestSession.class, ManifestSession_.sessionPrefix, packageId);
    }
}
