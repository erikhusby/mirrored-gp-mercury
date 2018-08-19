package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;

@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class SampleKitRequestDao  extends GenericDao {
    /**
     * Finds one record matching the email and all non-blank values in the lookupKey.
     * Blank values they are ignored and so they match any value.  If multiple matches
     * are found the most recent one is returned. If no matches, null is returned.
     */
    public SampleKitRequest find(final SampleKitRequest.SampleKitRequestKey key) {
        final CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<SampleKitRequest> criteriaQuery = criteriaBuilder.createQuery(SampleKitRequest.class);
        final Root<SampleKitRequest> root = criteriaQuery.from(SampleKitRequest.class);
        // Query uses the index on email.
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(root.get(SampleKitRequest_.email),
                key.getEmail())));
        try {
            Optional<SampleKitRequest> result = getEntityManager().createQuery(criteriaQuery).getResultList().stream().
                    filter(entity -> blankKeyOrMatch(key.getFirstName(), entity.getFirstName()) &&
                            blankKeyOrMatch(key.getLastName(), entity.getLastName()) &&
                            blankKeyOrMatch(key.getOrganization(), entity.getOrganization()) &&
                            blankKeyOrMatch(key.getAddress(), entity.getAddress()) &&
                            blankKeyOrMatch(key.getCity(), entity.getCity()) &&
                            blankKeyOrMatch(key.getState(), entity.getState()) &&
                            blankKeyOrMatch(key.getPostalCode(), entity.getPostalCode()) &&
                            blankKeyOrMatch(key.getCountry(), entity.getCountry()) &&
                            blankKeyOrMatch(key.getPhone(), entity.getPhone()) &&
                            blankKeyOrMatch(key.getCommonName(), entity.getCommonName()) &&
                            blankKeyOrMatch(key.getGenus(), entity.getGenus()) &&
                            blankKeyOrMatch(key.getSpecies(), entity.getSpecies()) &&
                            blankKeyOrMatch(key.getIrbApprovalRequired(), entity.getIrbApprovalRequired())).
                    sorted((o1, o2) -> {
                        // Sorted by descending entity id (most recent entity is first).
                        return (int)(o2.getSampleKitRequestId() - o1.getSampleKitRequestId());
                    }).
                    findFirst();
            return result.isPresent() ? result.get() : null;
        } catch (NoResultException ignored) {
            // Swallows the exception.
        }
        return null;
    }

    private boolean blankKeyOrMatch(String keyValue, String rowValue) {
        return StringUtils.isBlank(keyValue) ||
                StringUtils.trimToEmpty(keyValue).equals(StringUtils.trimToEmpty(rowValue));
    }
}