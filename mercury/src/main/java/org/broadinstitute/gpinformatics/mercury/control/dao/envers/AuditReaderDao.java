package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import com.sun.xml.ws.developer.Stateful;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.hibernate.SQLQuery;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.type.DateType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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

    // An "Envers triple" is an Object array having elements {entity, RevInfo, RevisionType}.
    // Since it's defined by a 3rd party there's not much we can do about beautifying or generifying it.
    /** Indicates the entity. */
    public static final int AUDIT_READER_ENTITY_IDX = 0;
    /** Indicates the revInfo. */
    public static final int AUDIT_READER_REV_INFO_IDX = 1;
    /** Indicates the change type. */
    public static final int AUDIT_READER_TYPE_IDX = 2;

    public static final String IS_ANY_USER = "IS_ANY_USER";
    public static final String IS_NULL_USER = "IS_NULL_USER";

    /**
     * Finds the audit info ids for data changes that happened in the given interval.
     *
     * @param startTimeSec     start of interval, in seconds
     * Audit revDate has 10 mSec resolution but JPA on our RevInfo table empirically only
     * permits comparing revDate with a param that has whole second resolution.
     * @param endTimeSec  end of interval, in seconds
     * @return Map of rev info id and the rev's timestamp.
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public SortedMap<Long, Date> fetchAuditIds(long startTimeSec, long endTimeSec) {
        SortedMap<Long, Date> revs = new TreeMap<>();
        for (AuditedRevDto auditedRevDto : fetchAuditIds(startTimeSec, endTimeSec, IS_ANY_USER, null)) {
            revs.put(auditedRevDto.getRevId(), auditedRevDto.getRevDate());
        }
        return revs;
    }

    /**
     * Finds the audit info ids for data changes that happened in the given interval
     * by the given user.
     *
     * @param startTimeSec   Start of interval, in seconds.
     * Audit revDate has 10 mSec resolution but JPA on our RevInfo table empirically only
     * permits comparing revDate with a param that has whole second resolution.
     * @param endTimeSec  End of interval, in seconds.
     * @param username    Name of the user making the change, or IS_ANY_USER for any user,
     *                    or IS_NULL_USER to find revs where user is null.
     * @param classname  if non-null, finds only revs of this type of class
     * @return List of audited revs, sorted by descending date.
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public List<AuditedRevDto> fetchAuditIds(long startTimeSec, long endTimeSec, String username, String classname) {
        // This was changed to a native query in GPLIM-3098 because AuditReader is excessively verbose with sql,
        // possibly due to revchanges being a optional Envers feature and not fully or well integrated.  For a
        // comparison, a UI audit trail search of two months with AuditReader code took 3238 sql queries and
        // 10.0 seconds.  The same search with this native query code took 1 sql query and under 0.5 second.
        String queryString = " select rev, rev_date, username, entityname from revchanges, rev_info " +
                             " where rev_info_id = rev and rev_date >= :startDate  and rev_date < :endDate ";
        if (IS_NULL_USER.equals(username)) {
            queryString += " and username is null ";
        } else if (StringUtils.isNotBlank(username) && !IS_ANY_USER.equals(username)) {
            queryString += " and username = :username ";
        }
        if (StringUtils.isNotBlank(classname)) {
            queryString += " and entityname = :entityname ";
        }
        queryString += " order by rev_date desc ";

        Query query = getEntityManager().createNativeQuery(queryString);
        query.setParameter("startDate", new Date(startTimeSec * MSEC_IN_SEC), TemporalType.TIMESTAMP);
        query.setParameter("endDate", new Date(endTimeSec * MSEC_IN_SEC), TemporalType.TIMESTAMP);
        if (queryString.contains(":username")) {
            //noinspection JpaQueryApiInspection
            query.setParameter("username", username);
        }
        if (queryString.contains(":entityname")) {
            //noinspection JpaQueryApiInspection
            query.setParameter("entityname", classname);
        }
        // Fixes the return types.
        query.unwrap(SQLQuery.class)
                .addScalar("rev", LongType.INSTANCE)
                .addScalar("rev_date", DateType.INSTANCE)
                .addScalar("username", StringType.INSTANCE)
                .addScalar("entityname", StringType.INSTANCE);

        try {
            List<AuditedRevDto> audits = new ArrayList<>();
            // Aggregates entity name by rev in the AuditedRevDtos.
            AuditedRevDto auditedRevDto = null;
            for (Object[] result : (List<Object[]>)query.getResultList()) {
                Long revId = (Long)result[0];
                if (auditedRevDto != null && auditedRevDto.getRevId().compareTo(revId) != 0) {
                    audits.add(auditedRevDto);
                    auditedRevDto = null;
                }
                if (auditedRevDto == null) {
                    auditedRevDto = new AuditedRevDto(revId, (Date)result[1], (String)result[2], new HashSet<String>());
                }
                auditedRevDto.getEntityTypeNames().add((String)result[3]);
            }
            if (auditedRevDto != null) {
                audits.add(auditedRevDto);
            }
            return audits;
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Finds EnversAudit dtos for one entity type over a collection of revision ids.
     *
     * @param revIds collection of audit revision ids to search through.
     * @param entityClassName the class name of the entity.
     * @return list of EnversAudit objects.
     */
    public List<EnversAudit> fetchEnversAudits(Set<Long> revIds, Class entityClassName) {
        List<EnversAudit> enversAudits = new ArrayList<>();
        // Does the AuditReader query and converts each object array into EnversAudit.
        for (Object[] enversTriple : fetchEnversAudits(revIds, entityClassName, true)) {
            Object obj = enversTriple[AuditReaderDao.AUDIT_READER_ENTITY_IDX];
            RevInfo revInfo = (RevInfo) enversTriple[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
            RevisionType revType = (RevisionType) enversTriple[AuditReaderDao.AUDIT_READER_TYPE_IDX];
            enversAudits.add(new EnversAudit(obj, revInfo, revType));
        }
        return enversAudits;
    }

    // Allows "unrolling" the batch to handle AuditReader failures on individual records.
    private List<Object[]> fetchEnversAudits(Collection<Long> revIds, Class<?> entityClass, boolean doChunks) {
        List<Object[]> dataChanges = new ArrayList<>();

        if (revIds == null || revIds.size() == 0) {
            return dataChanges;
        }

        // Chunks revIds as necessary to limit sql "in" clause to 1000 elements, or 1 element if not chunking.
        final int IN_CLAUSE_LIMIT = doChunks ? 1000 : 1;
        Collection<Long> sublist = new ArrayList<>();
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
                        List<Object[]> objects = fetchEnversAudits(sublist, entityClass, false);
                        dataChanges.addAll(objects);
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

    /**
     * Returns a version of the entity at the given revision id, or null
     * if entity doesn't have a Long primary key, or entity not found at that version.
     */
    public <T> T getEntityAtVersion(Long entityId, Class cls, long revId) {
        if (entityId != null) {
            return (T) getAuditReader().find(cls, entityId, revId);
        }
        return null;
    }

    /**
     * Returns the revId of an entity immediately prior to the version at the given revision id.
     * Returns null if entity can't be found, or doesn't have a Long primary key.
     */
    public Long getPreviousVersionRevId(Long entityId, Class cls, long revId) {
        RevInfo revInfo = getEntityManager().find(RevInfo.class, revId);
        if (revInfo == null) {
            return null;
        }
        boolean ONLY_RETURN_ENTITIES = false;
        boolean INCLUDE_DELETES = true;
        Date previousRevDate = (Date)getAuditReader().createQuery()
                .forRevisionsOfEntity(cls, ONLY_RETURN_ENTITIES, INCLUDE_DELETES)
                .addProjection(AuditEntity.revisionProperty("revDate").function("max"))
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionProperty("revDate").lt(revInfo.getRevDate()))
                .getSingleResult();
        Long previousRevId = (Long)getAuditReader().createQuery()
                .forRevisionsOfEntity(cls, ONLY_RETURN_ENTITIES, INCLUDE_DELETES)
                .addProjection(AuditEntity.revisionNumber().max())
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionProperty("revDate").eq(previousRevDate))
                .getSingleResult();
        return previousRevId;
    }

    /** Returns sorted list of all usernames found in REV_INFO. */
    public List<String> getAllAuditUsername() {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
        Root<RevInfo> root = criteriaQuery.from(RevInfo.class);
        criteriaQuery.select(root.get(RevInfo_.username));
        criteriaQuery.distinct(true);
        criteriaQuery.orderBy(criteriaBuilder.asc(criteriaBuilder.upper(root.get(RevInfo_.username))));
        try {
            return getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            logger.warn("No usernames found in REV_INFO.");
        }
        return (Collections.emptyList());
    }

}
