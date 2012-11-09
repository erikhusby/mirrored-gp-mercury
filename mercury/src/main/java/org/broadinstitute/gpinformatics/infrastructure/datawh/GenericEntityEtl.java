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
     * writes the records to the data file.
     *
     * @param lastRev      beginning of the interval to look for entity changes.
     * @param etlRev       end of the interval to look for entity changes.
     * @param etlDateStr   etlDate formatted as YYYYMMDDHHMMSS
     * @return the number of records created in the data file (deletes and modifies).
     */
    public int doEtl(long lastRev, long etlRev, String etlDateStr) {
        // Retrieves the Envers-formatted list of entity changes in the given revision range.
        List<Object[]> dataChanges = auditReaderEtl.fetchDataChanges(lastRev, etlRev, getEntityClass());

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        // Writes the records.
        processChanges(dataChanges, dataFile, etlDateStr);

        return dataFile.getRecordCount();
    }

    /**
     * Iterates on the modified Mercury entities, converts them to sqlLoader records, and
     * writes the records to the data file.
     * This code was broken out for testability.
     * @param dataChanges
     * @param dataFile
     * @param etlDateStr
     */
    private void processChanges(List<Object[]> dataChanges, DataFile dataFile, String etlDateStr) {
        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();

        try {
            for (Object[] dataChange : dataChanges) {

                // Collects the deleted vs added/modified entityIds.
                RevisionType revType = (RevisionType) dataChange[2];
                boolean isDelete = revType.equals(RevisionType.DEL);
                Object entity = dataChange[0];
                Long entityId = entityId(entity);
                if (isDelete) {
                    deletedEntityIds.add(entityId);

                    // Writes the deletion records.  Relies on all sqlLoader control files having entityId before
                    // any other entity field.  Also for status records to be deletable by the one field.
                    String record = genericRecord(etlDateStr, isDelete, entityId);
                    dataFile.write(record);
                } else {
                    changedEntityIds.add(entityId);
                }
            }

            // Each entity ETL will either make status records, or entity records, not both.
            // For entity ETL, only the latest version needs to be recorded regardless of what changed.
            // For status ETL, iterates again to record every status change along with the change date.
            if (isEntityEtl()) {
                changedEntityIds.removeAll(deletedEntityIds);

                for (Long entityId : changedEntityIds) {
                    String record = entityRecord(etlDateStr, false, entityId);
                    dataFile.write(record);
                }

            } else {
                for (Object[] dataChange : dataChanges) {
                    RevisionType revType = (RevisionType) dataChange[2];
                    boolean isDelete = revType.equals(RevisionType.DEL);
                    if (!isDelete) {
                        Object entity = dataChange[0];
                        Long entityId = entityId(entity);

                        // Db trips up on referential integrity of status records if the deleted
                        // entities are not skipped.
                        if (!deletedEntityIds.contains(entityId)) {
                            RevInfo revInfo = (RevInfo) dataChange[1];
                            Date revDate = revInfo.getRevDate();
                            String record = entityStatusRecord(etlDateStr, revDate, entity, isDelete);
                            dataFile.write(record);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }
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

        String getFilename() {
            return filename;
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