package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Utility methods for data warehouse ETL.
 */
@Stateless
public class AuditReaderEtl {

   // An arbitrary dao just to get the entity manager.
    @Inject
    private BillableItemDao dao;

    private AuditReader auditReader = null;

    /**
     * Returns an auditReader, doing one-time init if needed.
     * @return the auditReader
     */
    private AuditReader getAuditReader() {
        if (auditReader == null) {
            auditReader = AuditReaderFactory.get(dao.getEntityManager());
        }
        return auditReader;
    }

    /**
     * Returns the current (i.e. highest) audited entity revision number used before
     * the reference date.
     *
     * @param etlDate the reference date
     * @return revision number
     */
    public long currentRevNumber(Date etlDate) {
        Number revNumber = getAuditReader().getRevisionNumberForDate(etlDate);
        return revNumber.longValue();
    }

    /**
     * Returns the date for a given revNumber.
     * @param revNumber
     * @return
     */
    public Date dateForRevNumber(long revNumber) {
        return getAuditReader().getRevisionDate(revNumber);
    }

    /**
     * Finds and records all data changes that happened in the given interval.
     *
     * @param lastRev     start of interval
     * @param etlRev      end of interval
     * @param entityClass the class of entity to process
     */
    public List<Object[]> fetchDataChanges(long lastRev, long etlRev, Class entityClass) {

        AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionNumber().gt(lastRev))
                .add(AuditEntity.revisionNumber().le(etlRev));

        List<Object[]> dataChanges = query.getResultList();
        return dataChanges;
    }

}