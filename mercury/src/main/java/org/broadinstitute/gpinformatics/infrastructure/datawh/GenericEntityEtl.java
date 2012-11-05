package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract public class GenericEntityEtl {
    Logger logger = Logger.getLogger(this.getClass());

    @Inject
    private AuditReaderEtl auditReaderEtl;

    /**
     * Specifies the class entity handled by the overriding etl.
     * @return entity class
     */
    abstract Class getEntityClass();

    /**
     * Specifies the entity-related name of the data file, and must match the corresponding
     * SqlLoader control file.
     * @return
     */
    abstract String getBaseFilename();

    /**
     * Returns the JPA key for the entity.
     * @param entity entity having an id
     * @return the id
     */
    abstract Long entityId(Object entity);

    /**
     * Makes a data record from selected entity fields, in a format that matches the corresponding
     * SqlLoader control file.
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    abstract String entityRecord(String etlDateStr, boolean isDelete, Long entityId);

    /**
     * Makes a data record from entity status fields, and possible the Envers revision date,
     * in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr date
     * @param revDate Envers revision date
     * @param revObject the Envers versioned entity
     * @return delimited SqlLoader record, or null if entity does not support status recording
     */
    abstract String entityStatusRecord(String etlDateStr, Date revDate, Object revObject);

    /** Returns true if entity etl record supports entity ETL via primary key.  Status records do not. */
    abstract boolean isEntityEtl();

    /**
     * Iterates on the modified Mercury entities, converts them to sqlLoader records, and
     * write them to the data file.
     * Only the most recent version of a modified entity is recorded.
     *
     * @param lastRev      beginning of the interval to look for entity changes.
     * @param etlRev       end of the interval to look for entity changes.
     * @param etlDateStr   etlDate formatted as YYYYMMDDHHMMSS
     * @return the number of records created in the data file (deletes and modifies).
     */
    public int doEtl(long lastRev, long etlRev, String etlDateStr) {
        int recordCount = 0;
        List<Object[]> dataChanges = auditReaderEtl.fetchDataChanges(lastRev, etlRev, getEntityClass());

        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        String filename = dataFilename(etlDateStr, getBaseFilename());
        BufferedWriter writer = null;

        try {
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                Object entity = dataChange[0];
                RevInfo revInfo = (RevInfo)dataChange[1];
                RevisionType revType = (RevisionType) dataChange[2];
                Long entityId = entityId(entity);

                // For ETL classes that track all status changes, just collects status here
                // since there is no status audit table nor primary keys.
                Date revDate = revInfo.getRevDate();

                // Entity ETL will either make status records, or entity records,
                if (!isEntityEtl()) {
                    String statusRecord = entityStatusRecord(etlDateStr, revDate, entity);
                    if (statusRecord != null) {
                        if (writer == null) writer = new BufferedWriter(new FileWriter(filename));
                        writer.write(statusRecord);
                        writer.newLine();
                        recordCount++;
                    }
                } else {
                    // Writes a DW deletion record if entity was deleted, or collects deduplicated entity ids
                    // in order to lookup the latest version once.
                    if (revType.equals(RevisionType.DEL)) {
                        String record = genericRecord(etlDateStr, true, entityId);
                        if (writer == null) writer = new BufferedWriter(new FileWriter(filename));
                        writer.write(record);
                        writer.newLine();
                        recordCount++;

                        deletedEntityIds.add(entityId);
                    } else {
                        // Entity ids of add/modify changes.
                        changedEntityIds.add(entityId);
                    }
                }
            }
            // Writes a record for latest version of each of the changed entity.
            changedEntityIds.removeAll(deletedEntityIds);
            for (Long entityId : changedEntityIds) {
                String record =  entityRecord(etlDateStr, false, entityId);
                if (record != null) {
                    if (writer == null) writer = new BufferedWriter(new FileWriter(filename));
                    writer.write(record);
                    writer.newLine();
                    recordCount++;
                }
            }
        } catch (IOException e) {
            logger.error("Problem writing " + etlDateStr + "_" + getBaseFilename());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.error("Problem closing " + etlDateStr + "_" + getBaseFilename());
            }
        }
        //XXX delete this!!
        try {
            Thread.sleep(30L * 1000L);
        } catch (InterruptedException e) {
            //ignore
        }
        return recordCount;
    }

    /**
     * Builds a data file name.
     *
     * @param etlDateStr   etl run time
     * @param baseFilename data class name
     * @return the data file name
     */
    public static String dataFilename(String etlDateStr, String baseFilename) {
        return ExtractTransform.getDatafileDir() + "/" + etlDateStr + "_" + baseFilename + ".dat";
    }

    /**
     * Converts fields to a data record.
     * @param etlDateStr date
     * @param isDelete indicates a deleted entity
     * @param fields the fields to be put in the data record
     * @return formatted data record
     */
    public static String genericRecord(String etlDateStr, boolean isDelete, Object... fields) {
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
        return (date != null ? ExtractTransform.fullDateFormat.format(date) : "");
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