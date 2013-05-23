package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import com.sun.xml.ws.developer.Stateful;
import org.apache.commons.collections15.SortedBidiMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * Access to Mercury AuditReader for data warehouse.
 */
@Stateful
@RequestScoped
public class AuditReaderDao extends GenericDao {
    private final long MSEC_IN_SEC = 1000L;
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);

    private AuditReader getAuditReader() {
        return AuditReaderFactory.get(getEntityManager());
    }

    /**
     * Finds the audit info ids for data changes that happened in the given interval.
     *
     * @param startTimeSec     start of interval, in seconds
     * @param endTimeSec  end of interval, in seconds
     * @return Map of rev info id and the rev's timestamp.
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public SortedMap<Long, Date> fetchAuditIds(long startTimeSec, long endTimeSec) {
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
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<RevInfo> root = criteriaQuery.from(RevInfo.class);
        Path<Long> revId = root.get(RevInfo_.revInfoId);
        Path<Date> revDate = root.get(RevInfo_.revDate);
        criteriaQuery.multiselect(revId, revDate);
        // Includes the start of interval but excludes the end of interval.
        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.greaterThanOrEqualTo(root.get(RevInfo_.revDate), startDate),
                criteriaBuilder.lessThan(root.get(RevInfo_.revDate), endDate));
        criteriaQuery.where(predicate);

        SortedMap<Long, Date> map = new TreeMap<Long, Date>();
        try {
            List<Tuple> list = getEntityManager().createQuery(criteriaQuery).getResultList();
            for (Tuple tuple : list) {
                map.put(tuple.get(revId), tuple.get(revDate));
            }
        } catch (NoResultException ignored) {}
        return map;
    }

    /**
     * Finds and records all data changes for the given audit revision ids.
     *
     * @param revIds      audit revision ids
     * @param entityClass the class of entity to process
     * @param doChunks    process multiple revIds in sql IN clause
     */
    public List<Object[]> fetchDataChanges(Collection<Long> revIds, Class entityClass, boolean doChunks) {
        List<Object[]> dataChanges = new ArrayList<Object[]>();

        if (revIds == null || revIds.size() == 0) {
            return dataChanges;
        }
        // Chunks revIds as necessary to limit sql "in" clause to 1000 elements, or 1 element if not chunking.
        final int IN_CLAUSE_LIMIT = doChunks ? 1000 : 1;
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
                } catch (AuditException e) {
                    if (doChunks) {
                        // Retries sublist one rev at a time in order to skip the one causing problems.
                        dataChanges.addAll(fetchDataChanges(sublist, entityClass, false));
                    } else {
                        // Log and ignore single rev id problem.
                        logger.debug("Cannot query " + entityClass.getSimpleName() + " with audit rev " + id);
                    }
                } catch (NoResultException e) {
                    // Ignore, continue querying.
                }
                sublist.clear();
            }
        }
        return dataChanges;
    }

}
