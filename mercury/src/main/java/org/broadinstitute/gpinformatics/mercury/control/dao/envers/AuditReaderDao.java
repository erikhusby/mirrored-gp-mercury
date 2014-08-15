package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import com.sun.xml.ws.developer.Stateful;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.tools.Pair;

import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
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
        return fetchAuditIds(startTimeSec, endTimeSec, IS_ANY_USER);
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
     * @return Map of rev info id and the rev's timestamp.
     * @throws IllegalArgumentException if params are not whole second values.
     */
    public SortedMap<Long, Date> fetchAuditIds(long startTimeSec, long endTimeSec, String username) {
        Date startDate = new Date(startTimeSec * MSEC_IN_SEC);
        Date endDate = new Date(endTimeSec * MSEC_IN_SEC);

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<RevInfo> root = criteriaQuery.from(RevInfo.class);
        Path<Long> revId = root.get(RevInfo_.revInfoId);
        Path<Date> revDate = root.get(RevInfo_.revDate);
        criteriaQuery.multiselect(revId, revDate);

        // Includes the start of interval but excludes the end of interval.
        Predicate predicate;
        if (IS_ANY_USER.equals(username)) {
            predicate = criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get(RevInfo_.revDate), startDate),
                    criteriaBuilder.lessThan(root.get(RevInfo_.revDate), endDate));
        } else if (IS_NULL_USER.equals(username) || StringUtils.isNotBlank(username)) {
            predicate = criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get(RevInfo_.revDate), startDate),
                    criteriaBuilder.lessThan(root.get(RevInfo_.revDate), endDate),
                    criteriaBuilder.isNull(root.get(RevInfo_.username)));
        } else {
            predicate = criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get(RevInfo_.revDate), startDate),
                    criteriaBuilder.lessThan(root.get(RevInfo_.revDate), endDate),
                    criteriaBuilder.equal(root.get(RevInfo_.username), username));
        }
        criteriaQuery.where(predicate);

        SortedMap<Long, Date> map = new TreeMap<>();
        try {
            List<Tuple> list = getEntityManager().createQuery(criteriaQuery).getResultList();
            for (Tuple tuple : list) {
                map.put(tuple.get(revId), tuple.get(revDate));
            }
        } catch (NoResultException ignored) {}
        return map;
    }

    /**
     * Finds data changes for all entity types over a collection of revision ids.
     * @param revIds
     * @return a list of AuditedRevDtos sorted by revId.
     */
    public List<AuditedRevDto> fetchAuditedRevs(Set<Long> revIds) {
        List<AuditedRevDto> auditedRevDtos = new ArrayList<>();

        for (Long revId : revIds) {
            RevInfo revInfo = getEntityManager().find(RevInfo.class, revId);

            auditedRevDtos.add(new AuditedRevDto(revId, revInfo.getRevDate(), revInfo.getUsername(),
                    revInfo.getModifiedEntityNames()));
        }
        Collections.sort(auditedRevDtos, AuditedRevDto.BY_REV_ID);
        return auditedRevDtos;
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

    public Collection<String> getClassnamesModifiedAtRevision(Number revId) {
        Collection<String> classnames = new ArrayList<>();
        Set<Pair<String, Class>> modifiedEntityTypes =
                getAuditReader().getCrossTypeRevisionChangesReader().findEntityTypes(revId);
        for (Pair<String, Class> modifiedEntityType : modifiedEntityTypes) {
            classnames.add(modifiedEntityType.getFirst());
        }
        return classnames;
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
     * Returns the revId of a version of the entity immediately prior to the version at the given revision id.
     * Returns null if entity can't be found, or doesn't have a Long primary key.
     */
    public Long getPreviousVersionRevId(Long entityId, Class cls, long revId) {
        // The previous rev will then be the top of the ordered list.
        List<Long> list = getPreviousVersionRevIds(entityId, cls, revId);
        return (list != null) ? list.get(0) : null;
    }


    /**
     * Returns all revIds of a version of an entity prior to the version at the given revision id.
     * Returns null if entity can't be found, or doesn't have a Long primary key.
     */
    public List<Long> getPreviousVersionRevIds(Long entityId, Class cls, long revId) {
        List<Long> previousVersionRevIds = new ArrayList<>();
        if (entityId != null) {
            final Date revDate = fetchRevDate(revId);
            if (revDate != null) {
                List<Object[]> list = getAuditReader().createQuery()
                        .forRevisionsOfEntity(cls, false, true)
                        .add(AuditEntity.id().eq(entityId))
                        .add(AuditEntity.revisionProperty("revDate").le(revDate))
                        .getResultList();

                // Reverse order the transactions (newest one first).  Mercury does not always
                // have increasing values of revId with successive transaction commits, so use
                // revDate as primary and revId as secondary.
                Collections.sort(list, new Comparator<Object[]>() {
                    @Override
                    public int compare(Object[] o1, Object[] o2) {
                        RevInfo revInfo1 = (RevInfo)o1[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
                        RevInfo revInfo2 = (RevInfo)o2[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
                        if (revInfo1.getRevDate().equals(revInfo2.getRevDate())) {
                            return revInfo2.getRevInfoId().compareTo(revInfo1.getRevInfoId());
                        } else {
                            return revInfo2.getRevDate().compareTo(revInfo1.getRevDate());
                        }
                    }
                });

                // The query included the revDate in the search endpoint, but it's not really a
                // point, it's a time interval, albeit very small.  It's conceivable that within that
                // interval the revId transaction might be found before or after other transactions.
                //
                // Skips over the given revId and revIds from any transactions that come after it.
                // Since it's in one time quantum, the revId was used for the ordering, imperfect as it may be.

                for (Iterator<Object[]> iter = list.iterator(); iter.hasNext(); ) {
                    RevInfo revInfo = (RevInfo)iter.next()[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
                    if (revInfo.getRevDate().before(revDate) || revInfo.getRevInfoId() < revId) {
                        previousVersionRevIds.add(revInfo.getRevInfoId());
                    }
                }
            }
        }
        return previousVersionRevIds;
    }

    private Date fetchRevDate(Long revId) {
        RevInfo revInfo = getEntityManager().find(RevInfo.class, revId);
        return (revInfo != null) ? revInfo.getRevDate() : null;
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
