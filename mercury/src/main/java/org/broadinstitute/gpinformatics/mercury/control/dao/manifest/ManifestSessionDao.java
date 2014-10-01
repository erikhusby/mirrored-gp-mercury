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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
                ManifestSession.SessionStatus.OPEN, ManifestSession.SessionStatus.ACCESSIONING));
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
        Predicate tubesRemainingToBeTransferred = criteriaBuilder.notEqual(
                root.get(ManifestSession_.tubesRemainingToBeTransferred), 0);

        query.where(completedStatus, tubesRemainingToBeTransferred);

        return getEntityManager().createQuery(query).getResultList();
    }
}
