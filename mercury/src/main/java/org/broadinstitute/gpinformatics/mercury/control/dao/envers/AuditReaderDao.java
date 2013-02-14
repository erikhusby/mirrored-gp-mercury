package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;

/**
 * Access to Mercury AuditReader for data warehouse.
 */
@ApplicationScoped
public class AuditReaderDao extends GenericDao {

    private AuditReader getAuditReader() {
        return AuditReaderFactory.get(getEntityManager());
    }

    /**
     * Finds the audit info ids for data changes that happened in the given interval.
     * Must use REV_INFO timestamp in interval, and not id because in Mercury
     * the ids were observed to be not always monotonic, which caused ETL to miss
     * some changes.
     *
     * Subtlety here with the timestamps:  revDate has 10 mSec resolution but JPA on our
     * RevInfo table empirically only permits comparing revDate with a param that has whole
     * second resolution.  Using native query is no help since Oracle to_timestamp cannot
     * convert sub-second values.
     * Etl must not etl the same event twice nor miss audited events regardless of their
     * revDate, which may have hundredth of second values from 0 through 99.
     * The solution used is to have date-based etl (i.e. incremental etl) set the endDate
     * to be the floor of the current mSec time, which can possibly leave the very most
     * recent events to be picked up later.  In addition the interval passed in is truncated
     * and the query must include start point (revDate hundredths >= 0) and exclude end point
     * (revDate hundredths = 0).  The start date is managed by the caller.
     *
     * @param lastEtlTimestamp     start of interval in mSec units, truncated to second resolution
     * @param currentEtlTimestamp  end of interval in mSec units, truncated to second resolution
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public Collection<Long> fetchAuditIds(long lastEtlTimestamp, long currentEtlTimestamp)
            throws IllegalArgumentException {

        if (lastEtlTimestamp % 1000 != 0 || currentEtlTimestamp % 1000 != 0) {
            throw new IllegalArgumentException ("Etl interval must be whole second aligned.");
        }
        Date startDate = new Date(lastEtlTimestamp);
        Date endDate = new Date(currentEtlTimestamp);

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
