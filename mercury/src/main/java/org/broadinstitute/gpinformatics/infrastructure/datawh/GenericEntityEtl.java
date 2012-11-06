package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
     * @param isDelete indicates deleted entity
     * @return delimited SqlLoader record, or null if entity does not support status recording
     */
    abstract String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete);

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

        // Retrieves the Envers-formatted list of entity changes in the given revision range.
        List<Object[]> dataChanges = auditReaderEtl.fetchDataChanges(lastRev, etlRev, getEntityClass());

        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        try {
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                Object entity = dataChange[0];
                RevInfo revInfo = (RevInfo) dataChange[1];
                RevisionType revType = (RevisionType) dataChange[2];

                Long entityId = entityId(entity);
                Date revDate = revInfo.getRevDate();
                boolean isDelete = revType.equals(RevisionType.DEL);

                // Each entity ETL will either make status records, or entity records, not both.
                if (!isEntityEtl()) {
                    String statusRecord = entityStatusRecord(etlDateStr, revDate, entity, isDelete);
                    dataFile.write(statusRecord);
                } else {
                    // Writes a DW deletion record if entity was deleted; if not a delete
                    // collects deduplicated entity ids to lookup the current entity.
                    if (isDelete) {
                        String record = genericRecord(etlDateStr, isDelete, entityId);
                        dataFile.write(record);
                        deletedEntityIds.add(entityId);
                    } else {
                        changedEntityIds.add(entityId);
                    }
                }
            }
            // Writes a record for latest version of each non-deleted, changed entity.
            changedEntityIds.removeAll(deletedEntityIds);
            for (Long entityId : changedEntityIds) {
                String record = entityRecord(etlDateStr, false, entityId);
                dataFile.write(record);
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + filename, e);
        } finally {
            dataFile.close();
        }
        return dataFile.getRecordCount();
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
                .append(format(isDelete));
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
     * Returns String, or empty string if null, and quotes string if DELIM occurs.
     * @param string to format
     */
    public static String format(String string) {
        if (string == null) {
            return "";
        }
        if (string.contains(ExtractTransform.DELIM)) {
            // Escapes all embedded double quotes by doubling them: " becomes ""
            return "\"" + string.replaceAll("\"", "\"\"") + "\"";
        }
        return string;
    }

    /**
     * Returns String, or empty string if null.
     * @param num to format
     */
    public static <T extends Number > String format(T num) {
        return (num != null ? num.toString() : "");
    }

    /** Class to wrap/manage writing to the data file. */
    private class DataFile {
        private final String filename;
        private BufferedWriter writer = null;
        private int lineCount = 0;

        DataFile(String filename) {
            this.filename = filename;
        }

        int getRecordCount() {
            return lineCount;
        }

        void write(String record) throws IOException {
            if (record == null) {
                return;
            }
            lineCount++;
            if (writer == null) {
                writer = new BufferedWriter(new FileWriter(filename));
            }
            writer.write(lineCount + ExtractTransform.DELIM + record);
            writer.newLine();
        }

        void close() {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.error("Problem closing file " + filename);
            }
        }
    }
}