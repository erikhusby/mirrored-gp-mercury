package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

/**
 * Access to Mercury AuditReader for data warehouse.
 */
@ApplicationScoped
public class AuditReaderDao extends GenericDao {
    private final long MSEC_IN_SEC = 1000L;

    private AuditReader getAuditReader() {
        return AuditReaderFactory.get(getEntityManager());
    }

    /**
     * Finds the audit info ids for data changes that happened in the given interval.
     *
     * @param startTimeSec     start of interval, in seconds
     * @param endTimeSec  end of interval, in seconds
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public Collection<Long> fetchAuditIds(long startTimeSec, long endTimeSec) {
        Date startDate = new Date(startTimeSec * MSEC_IN_SEC);
        Date endDate = new Date(endTimeSec * MSEC_IN_SEC);

        // Typical AuditReader usage would use audit rev id to determine where to start
        // the new etl.  But in Mercury the ids were observed to be not always monotonic,
        // so the audit timestamp must be used instead.

        // Audit revDate has 10 mSec resolution but JPA on our RevInfo table empirically only
        // permits comparing revDate with a param that has whole second resolution.  That
        // means etl must have a time interval defined on whole second boundaries.  This
        // can possibly leave the very most recent events to be picked up in the next etl.

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<RevInfo> root = criteriaQuery.from(RevInfo.class);
        criteriaQuery.select(root.get(RevInfo_.revInfoId));
        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.greaterThanOrEqualTo(root.get(RevInfo_.revDate), startDate),
                criteriaBuilder.lessThan(root.get(RevInfo_.revDate), endDate));
        criteriaQuery.where(predicate);

        try {
            Collection<Long> revList = getEntityManager().createQuery(criteriaQuery).getResultList();
            return revList;

        } catch (NoResultException ignored) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Finds and records all data changes for the given audit revision ids.
     *
     * @param revIds      audit revision ids
     * @param entityClass the class of entity to process
     */
    public List<Object[]> fetchDataChanges(Collection<Long> revIds, Class entityClass) {
        List<Object[]> dataChanges = new ArrayList<Object[]>();

        if (revIds == null || revIds.size() == 0) {
            return dataChanges;
        }
        // TODO Splitterize
        // Chunks revIds as necessary to limit sql "in" clause to 1000 elements.
        final int IN_CLAUSE_LIMIT = 1000;
        Collection<Long> sublist = new ArrayList<Long>();
        for (Long id : revIds) {
            sublist.add(id);
            if (sublist.size() == IN_CLAUSE_LIMIT || sublist.size() == revIds.size()) {

                // Processes and flushes sublist.
                AuditQuery query = getAuditReader().createQuery()
                        .forRevisionsOfEntity(entityClass, false, true)
                        .add(AuditEntity.revisionNumber().in(sublist));
                try {
                    dataChanges.addAll(query.getResultList());
                } catch (NoResultException e) {
                    // Ignore, continue querying.
                }
                sublist.clear();
            }
        }
        return dataChanges;
    }

}
