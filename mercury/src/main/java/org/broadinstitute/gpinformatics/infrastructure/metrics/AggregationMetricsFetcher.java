package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Data access object for fetching Picard aggregation metrics.
 * <p/>
 * Not a {@link GenericDao} because it uses a different persistence unit.
 */
@Stateful
public class AggregationMetricsFetcher {

    private static final Log log = LogFactory.getLog(AggregationMetricsFetcher.class);

    public static final String SAMPLE_COLUMN = "sample";
    public static final String PROJECT_COLUMN = "project";
    public static final String VERSION_COLUMN = "version";

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public List<Aggregation> fetch(List<String> projects, List<String> samples, List<Integer> versions) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);

        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);

        List<Predicate> predicateOfOrs = new ArrayList<>();
        Iterator<String> projectIterator = projects.iterator();
        Iterator<String> sampleIterator = samples.iterator();
        Iterator<Integer> versionIterator = versions.iterator();
        while (projectIterator.hasNext() && sampleIterator.hasNext() && versionIterator.hasNext()) {
            String project = projectIterator.next();
            String sample = sampleIterator.next();
            Integer version = versionIterator.next();
            List<Predicate> predicate = new ArrayList<>();

            predicate.add(criteriaBuilder.equal(root.get(Aggregation_.project), project));
            if (sample != null) {
                predicate.add(criteriaBuilder.equal(root.get(Aggregation_.sample), sample));
            }
            predicate.add(criteriaBuilder.equal(root.get(Aggregation_.version), version));

        /*
         * Look for the row where LIBRARY is NULL because otherwise there would be multiple results. Kathleen Tibbetts
         * said that this is the way to narrow it down to one.
         */

//            predicate.add(criteriaBuilder.isNull(root.get("library")));

            predicateOfOrs.add(criteriaBuilder.and(predicate.toArray(new Predicate[predicate.size()])));
        }

        criteriaQuery.where(criteriaBuilder.or(predicateOfOrs.toArray(new Predicate[predicateOfOrs.size()])),
                criteriaBuilder.isNull(root.get("library")));

        TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);
        List<Aggregation> aggregations = new ArrayList<>();
        try {
            aggregations = query.getResultList();
        } catch (NoResultException e) {
            log.info("Unable to retrieve aggregations based on given criteria");
        }
        for(Aggregation aggregation:aggregations) {
            try {
                TypedQuery<LevelOfDetection> lodQuery =
                        entityManager.createNamedQuery(LevelOfDetection.LOD_QUERY_NAME, LevelOfDetection.class)
                                .setParameter(SAMPLE_COLUMN, aggregation.getSample())
                                .setParameter(PROJECT_COLUMN, aggregation.getProject())
                                .setParameter(VERSION_COLUMN, aggregation.getVersion());
                aggregation.setLevelOfDetection(lodQuery.getSingleResult());
            } catch (NoResultException e) {
                log.info(String.format("Unable to retrieve LOD info for Aggregation based on : " +
                                       "Project %s, Sample %s, Version %d", aggregation.getProject(),
                        aggregation.getSample(), aggregation.getVersion()));
            }
        }
        return aggregations;
    }

}
