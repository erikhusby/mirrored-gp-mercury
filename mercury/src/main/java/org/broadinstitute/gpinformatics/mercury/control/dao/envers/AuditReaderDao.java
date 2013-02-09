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
import java.util.*;

/**
 * Access to Mercury AuditReader for data warehouse.
 */
@ApplicationScoped
public class AuditReaderDao extends GenericDao {
    private AuditReader auditReader = AuditReaderFactory.get(getEntityManager());

    void setAuditReader(AuditReader ar) {
        auditReader = ar;
    }

    /**
     * Finds the audit info ids for data changes that happened in the given interval.
     * Must use REV_INFO timestamp in interval, and not id because in Mercury
     * the ids were observed to be not always monotonic, which caused ETL to miss
     * some changes.
     *
     * @param lastEtlTimestamp     start of interval
     * @param currentEtlTimestamp  end of interval
     */
    public Collection<Long> fetchAuditIds(long lastEtlTimestamp, long currentEtlTimestamp) {
        Date startDate = new Date(lastEtlTimestamp);
        Date endDate = new Date(currentEtlTimestamp);

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<RevInfo> root = criteriaQuery.from(RevInfo.class);
        criteriaQuery.select(root.get(RevInfo_.revInfoId));
        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.greaterThan(root.get(RevInfo_.revDate), startDate),
                criteriaBuilder.lessThanOrEqualTo(root.get(RevInfo_.revDate), endDate));
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
                AuditQuery query = auditReader.createQuery()
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

        /**
     * Finds the date a recent audit rev, typically the most recent.
     * @return timestamp represented as mSec since start of the epoch
     */
    public long fetchLatestAuditDate(String audTableName) {
        String queryString = "SELECT TO_CHAR(rev_date,'YYYYMMDDHH24MISSRR')||'0' FROM REV_INFO " +
                " WHERE rev_info_id = (SELECT MAX(rev) FROM " + audTableName + ")";
        Query query = getEntityManager().createNativeQuery(queryString);
        try {
            String result = (String)query.getSingleResult();
            long timestamp = ExtractTransform.msecTimestampFormat.parse(result).getTime();
            return timestamp;
        } catch (Exception e) {
            return 0L;
        }
    }

}
