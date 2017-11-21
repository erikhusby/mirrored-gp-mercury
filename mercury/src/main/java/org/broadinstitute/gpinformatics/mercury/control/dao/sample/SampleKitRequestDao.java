package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest_;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public class SampleKitRequestDao  extends GenericDao {

    /** Returns one record having the specified email and organization. Also uses lastName & firstName if non-null. */
    public SampleKitRequest find(final String email, final String organization, final String lastName,
            final String firstName) {
        final CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<SampleKitRequest> criteriaQuery = criteriaBuilder.createQuery(SampleKitRequest.class);
        final Root<SampleKitRequest> root = criteriaQuery.from(SampleKitRequest.class);
        List<Predicate> predicates = new ArrayList<Predicate>() {{
            add(criteriaBuilder.equal(root.get(SampleKitRequest_.email), email));
            add(criteriaBuilder.equal(root.get(SampleKitRequest_.organization), organization));
            if (StringUtils.isNotBlank(lastName)) {
                add(criteriaBuilder.equal(root.get(SampleKitRequest_.lastName), lastName));
            }
            if (StringUtils.isNotBlank(firstName)) {
                add(criteriaBuilder.equal(root.get(SampleKitRequest_.firstName), firstName));
            }
        }};
        criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        // In case there are multiple matching records, returns the one having lowest entity id.
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get(SampleKitRequest_.sampleKitRequestId)));
        try {
            return getEntityManager().createQuery(criteriaQuery).setFirstResult(0).setMaxResults(1).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /** Returns one record having the specified email and organization. */
    public SampleKitRequest find(String email, String organization) {
        return find(email, organization, null, null);
    }
}
