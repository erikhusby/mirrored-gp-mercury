package org.broadinstitute.gpinformatics.infrastructure.cognos;

import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.SampleCoverageFirstMet;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.SampleCoverageFirstMet_;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Stateful
public class SampleCoverageFirstMetFetcher {

    @PersistenceContext(unitName = "metrics_pu")
    private EntityManager entityManager;

    public Map<String, SampleCoverageFirstMet> getCoverageFirstMetBySampleForPdo(String productOrderKey) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SampleCoverageFirstMet> query = criteriaBuilder.createQuery(SampleCoverageFirstMet.class);
        Root<SampleCoverageFirstMet> root = query.from(SampleCoverageFirstMet.class);
        query.where(criteriaBuilder.equal(root.get(SampleCoverageFirstMet_.id).get("pdoName"), productOrderKey));

        List<SampleCoverageFirstMet> resultList = entityManager.createQuery(query).getResultList();

        Map<String, SampleCoverageFirstMet> result = new HashMap<>();
        for (SampleCoverageFirstMet sampleCoverageFirstMet : resultList) {
            result.put(sampleCoverageFirstMet.getExternalSampleId(), sampleCoverageFirstMet);
        }

        return result;
    }
}
