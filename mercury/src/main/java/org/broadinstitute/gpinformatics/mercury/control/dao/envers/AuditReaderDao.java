package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import com.sun.xml.ws.developer.Stateful;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ExtractTransform;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo_;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
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
import java.util.Date;
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
     * Finds data changes for one entity type over a collection of revision ids.
     *
     * @param entityClassName the class name of the entity.
     * @param revIds collection of audit revision ids to search through.
     * @return list of EnversAudit objects.
     */
    public List<EnversAudit> fetchDataChanges(Collection<Long> revIds, Class entityClassName) {
        List<EnversAudit> enversAudits = new ArrayList<>();
        // Does the AuditReader query and converts each object array into EnversAudit.
        for (Object[] enversTriple : fetchDataChanges(revIds, entityClassName, true)) {
            Object obj = enversTriple[AuditReaderDao.AUDIT_READER_ENTITY_IDX];
            RevInfo revInfo = (RevInfo) enversTriple[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
            RevisionType revType = (RevisionType) enversTriple[AuditReaderDao.AUDIT_READER_TYPE_IDX];
            enversAudits.add(new EnversAudit(obj, revInfo, revType));
        }
        return enversAudits;
    }

    // Allows "unrolling" the batch to handle AuditReader failures on individual records.
    private List<Object[]> fetchDataChanges(Collection<Long> revIds, Class<?> entityClass, boolean doChunks) {
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
                        List<Object[]> objects = fetchDataChanges(sublist, entityClass, false);
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

    public <T> T getPreviousVersion(T entity, Class cls, long revId) {
        // JiraTicket does not have a Long primary key.
        if (cls.equals(JiraTicket.class)) {
            return null;
        }

        Long entityId = ReflectionUtil.getEntityId(entity, cls);

        AuditReader reader = getAuditReader();
        Number previousRevId = (Number) reader.createQuery()
                .forRevisionsOfEntity(entity.getClass(), false, true)
                .addProjection(AuditEntity.revisionNumber().max())
                .add(AuditEntity.id().eq(entityId))
                .add(AuditEntity.revisionNumber().lt(revId))
                .getSingleResult();

        if (previousRevId != null) {
            return (T) reader.find(entity.getClass(), entityId, previousRevId);
        } else {
            return null;
        }
    }
}
