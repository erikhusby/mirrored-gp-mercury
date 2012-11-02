package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.poi.ss.formula.functions.T;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility methods for data warehouse ETL.
 */
@Stateless
public class Util {

    /** This date format matches what cron job expects in filenames, and in SqlLoader data files. */
    public static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

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
     * Converts fields to a data record.
     * @param etlDateStr date
     * @param isDelete indicates a deleted entity
     * @param fields the fields to be put in the data record
     * @return formatted data record
     */
    public static String makeRecord(String etlDateStr, boolean isDelete, Object... fields) {
        StringBuilder rec = new StringBuilder()
                .append(etlDateStr)
                .append(ExtractTransform.DELIM)
                .append(isDelete ? "T" : "F");
        for (Object field : fields) {
            rec.append(ExtractTransform.DELIM)
                    .append(field);
        }
        return rec.toString();
    }

    /**
     * Returns formatted date string, or empty string if date is null.
     * @param date the date to format
     */
    public static String format(Date date) {
        return (date != null ? fullDateFormat.format(date) : "");
    }

    /**
     * Returns T or F string for the boolean.
     * @param bool to format
     */
    public static String format(boolean bool) {
        return (bool ? "T" : "F");
    }

    /**
     * Returns String or empty string if null, and quotes string if DELIM occurs.
     * @param string to format
     */
    public static String format(String string) {
        if (string == null) {
            return "";
        }
        if (string.contains(ExtractTransform.DELIM)) {
            // Escapes all embedded " by doubling them, i.e. ""
            return "\"" + string.replaceAll("\"", "\"\"") + "\"";
        }
        return string;
    }

    /**
     * Returns String or empty string if null.
     * @param i to format
     */
    public static String format(Integer i) {
        return (i != null ? i.toString() : "");
    }

    /**
     * Returns String or empty string if null.
     * @param i to format
     */
    public static String format(Long i) {
        return (i != null ? i.toString() : "");
    }

    /**
     * Returns String or empty string if null.
     * @param i to format
     */
    public static String format(BigDecimal i) {
        return (i != null ? i.toString() : "");
    }


}