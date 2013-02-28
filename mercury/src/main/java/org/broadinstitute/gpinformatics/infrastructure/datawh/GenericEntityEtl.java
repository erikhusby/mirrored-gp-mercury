package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class GenericEntityEtl {
    protected final Log logger = LogFactory.getLog(getClass());

    private AuditReaderDao auditReaderDao;

    @Inject
    public void setAuditReaderDao(AuditReaderDao auditReaderDao) {
        this.auditReaderDao = auditReaderDao;
    }

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
     *
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    abstract Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId);

    /**
     * Returns sqlLoader data records for entities having id in the given range.
     * @param startId start of the entity id range.
     * @param endId end of the entity id range.
     * @param etlDateStr the etl date to put in each record.
     * @param isDelete the delete flag to put in each record.
     * @return collection of strings, one per data file record.
     */
    abstract Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete);

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

    /**
     * Returns true if entity etl record supports entity ETL via primary key.  Status records do not.
     */
    abstract boolean isEntityEtl();

    /**
     * Iterates on the modified Mercury entities, converts them to sqlLoader records, and
     * writes the records to the data file.
     *
     * @param revIds       list of audit revision ids
     * @param etlDateStr   etlDate formatted as YYYYMMDDHHMMSS
     * @return the number of records created in the data file (deletes and modifies).
     */
    public int doEtl(Collection<Long> revIds, String etlDateStr) {
        // Retrieves the Envers-formatted list of entity changes in the given revision range.
        List<Object[]> dataChanges = auditReaderDao.fetchDataChanges(revIds, getEntityClass());

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        // Writes the records.
        processChanges(dataChanges, dataFile, etlDateStr);

        return dataFile.getRecordCount();
    }

    /**
     * Iterates on the modified Mercury entities obtained from AuditReader.  Converts them to sqlLoader records, and
     * writes the records to the data file.  This code was broken out for testability.
     * @param dataChanges
     * @param dataFile
     * @param etlDateStr
     */
    private void processChanges(List<Object[]> dataChanges, DataFile dataFile, String etlDateStr) {
        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        List<Object> statusEntities = new ArrayList<Object>();
        List<Date> statusDates = new ArrayList<Date>();

        try {

            for (Object[] dataChange : dataChanges) {
                RevisionType revType = (RevisionType) dataChange[2];
                boolean isDelete = revType == RevisionType.DEL;
                Object entity = dataChange[0];
                Long entityId = entityId(entity);
                if (isDelete) {
                    // Makes a set of ids of deleted entities
                    deletedEntityIds.add(entityId);

                    // Writes the deletion records.  Currently all entities are unique on the entityId field.
                    // For status record Etl, all status for the entity will be deleted.
                    String record = genericRecord(etlDateStr, isDelete, entityId);
                    dataFile.write(record);
                } else {
                    // Makes a set of ids of added/modified entities
                    changedEntityIds.add(entityId);

                    // List of status change entities and dates
                    if (!isEntityEtl()) {
                        RevInfo revInfo = (RevInfo) dataChange[1];
                        statusDates.add(revInfo.getRevDate());
                        statusEntities.add(entity);
                    }
                }
            }


            // Each entityEtl class will either make status records, or entity records, not both.
            // For entity records, only the latest version needs to be recorded regardless of what changed.
            // For status records, iterates on the statusEntities to record every status change.
            if (isEntityEtl()) {
                changedEntityIds.removeAll(deletedEntityIds);

                for (Long entityId : changedEntityIds) {
                    for (String record : entityRecord(etlDateStr, false, entityId)) {
                        dataFile.write(record);
                    }
                }

            } else {
                Iterator<Date> statusDateIter = statusDates.iterator();
                for (Object entity : statusEntities) {
                    Long entityId = entityId(entity);

                    // Db trips up on referential integrity if deleted entity statuses are not skipped.
                    if (!deletedEntityIds.contains(entityId)) {
                        Date revDate = statusDateIter.next();
                        String record = entityStatusRecord(etlDateStr, revDate, entity, false);
                        dataFile.write(record);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }
    }

    public int doBackfillEtl(Class entityClass, long startId, long endId, String etlDateStr) {
        // No-op unless the implementing class is the requested entity class.
        if (!getEntityClass().equals(entityClass) || !isEntityEtl()) {
            return 0;
        }

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        try {
            // Writes the records.
            for (String record : entityRecordsInRange(startId, endId, etlDateStr, false)) {
                dataFile.write(record);
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
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
     * Returns formatted date string, or "" string if date is null.
     * @param date the date to format
     */
    public static String format(Date date) {
        return (date != null ? ExtractTransform.secTimestampFormat.format(date) : "\"\"");
    }

    /**
     * Returns T or F string for the boolean.
     * @param bool to format
     */
    public static String format(boolean bool) {
        return (bool ? "T" : "F");
    }

    /**
     * Returns String, or "" string if null, and quotes string if DELIM occurs.
     * @param string to format
     */
    public static String format(String string) {
        if (string == null) {
            return "\"\"";
        }
        if (string.contains(ExtractTransform.DELIM)) {
            // Escapes all embedded double quotes by doubling them: " becomes ""
            return "\"" + string.replaceAll("\"", "\"\"") + "\"";
        }
        return string;
    }

    /**
     * Returns String, or "" string if null.
     * @param num to format
     */
    public static <T extends Number > String format(T num) {
        return (num != null ? num.toString() : "\"\"");
    }

    /** Class to wrap/manage writing to the data file. */
    protected static class DataFile {
        private final String filename;
        private BufferedWriter writer;
        private int lineCount;

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
            IOUtils.closeQuietly(writer);
        }
    }

    public static long HASH_PRIME = 1125899906842597L;
    public static long HASH_MULTIPLIER = 31L;

    /** Calculates a hash on String. */
    public static long hash(String string) {
        long h = HASH_PRIME;
        int len = string.length();
        for (int i = 0; i < len; i++) {
            h = HASH_MULTIPLIER * h + string.charAt(i);
        }
        return h;
    }

    /**
     * Concatenates each string with a delimiter, then calculates a hash on the whole thing.
     */
    public static long hash(String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(ExtractTransform.DELIM).append(s);
        }
        return GenericEntityEtl.hash(sb.toString());
    }

    /** Calculates a hash on all workflow config elements. */
    public static long hash(Collection<WorkflowConfigDenorm> denorms) {
        long h = HASH_PRIME;
        for (WorkflowConfigDenorm denorm : denorms) {
            // Reuses the existing hash of this record, which is its id.
            h = HASH_MULTIPLIER * h + denorm.getWorkflowConfigDenormId();
        }
        return h;
    }

}