package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;

/**
 * Utility methods for data warehouse ETL.
 */
@Stateless
public class Util {

    // An arbitrary dao just to get the entity manager.
    @Inject
    private BillableItemDao dao;

    private AuditReader auditReader = null;

    /**
     * Returns an auditReader, doing one-time init if needed.
     * @return the auditReader
     */
    AuditReader getAuditReader() {
        if (auditReader == null) {
            auditReader = AuditReaderFactory.get(dao.getEntityManager());
        }
        return auditReader;
    }

    /**
     * Returns the current (i.e. highest) audited entity revision number.
     * @return revision number
     */
    public long currentRevNumber(Date etlDate) {
        Number revNumber = getAuditReader().getRevisionNumberForDate(etlDate);
        return revNumber.longValue();
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

/*
        AuditQuery query = reader.createQuery().forEntitiesAtRevision(entityClass, revisionNumber);

        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionProperty("revDate").gt(lastDate))
                .add(AuditEntity.revisionProperty("revDate").le(etlDate));
*/
        List<Object[]> dataChanges = query.getResultList();
        return dataChanges;
    }

    /**
     * Returns a sqlLoader data file name.
     *
     * @param etlDateStr   etl run time, for filename
     * @param baseFilename for filename
     * @return the file name
     */
    public String dataFilename(String etlDateStr, String baseFilename) {
        return ExtractTransform.getDatafileDir() + "/" + etlDateStr + "_" + baseFilename + ".dat";
    }

    /**
     * Returns a new DW record containing the common start fields.
     * All data file records begin with etlDate and isDelete.
     *
     * @param etlDateStr first field
     * @param isDelete   second field
     * @return StringBuilder that ends with a record delimiter
     */
    public StringBuilder startRecord(String etlDateStr, boolean isDelete) {
        return new StringBuilder()
                .append(etlDateStr).append(ExtractTransform.DELIM)
                .append(isDelete ? "T" : "F").append(ExtractTransform.DELIM);
    }

    /**
     * Converts fields to a data record.
     * @param etlDateStr date
     * @param isDelete indicates a deleted entity
     * @param fields the fields to be put in the data record
     * @return formatted data record
     */
    public String makeRecord(String etlDateStr, boolean isDelete, Object... fields) {
        StringBuilder rec = new StringBuilder()
                .append(etlDateStr).append(ExtractTransform.DELIM)
                .append(isDelete ? "T" : "F").append(ExtractTransform.DELIM);
        for (Object field : fields) {
            rec.append(field).append(ExtractTransform.DELIM);
        }
        return rec.toString();
    }

}