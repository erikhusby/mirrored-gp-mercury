package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.hibernate.SQLQuery;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.LongType;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Base class for entity etl.
 *
 * @param <T> the class that gets audited and referenced by backfill entity id range.
 * @param <C> the class that is used to create sqlLoader records.  Typically C is the same class as T,
 *            and only differs from T in cross-entity etl subclasses.
 */
public abstract class GenericEntityEtl<T, C> {
    public static final String IN_CLAUSE_PLACEHOLDER = "__IN_CLAUSE__";
    protected static final int AUDIT_READER_ENTITY_IDX = 0;
    protected static final int AUDIT_READER_REV_INFO_IDX = 1;
    protected static final int AUDIT_READER_TYPE_IDX = 2;

    public Class entityClass;  // equivalent to T.class

    /** The entity-related name of the data file, and must sync with the ETL cron script and control file. */
    public String baseFilename;

    protected Log logger = LogFactory.getLog(getClass());
    protected AuditReaderDao auditReaderDao;
    protected GenericDao dao;

    @Inject
    public void setAuditReaderDao(AuditReaderDao auditReaderDao) {
        this.auditReaderDao = auditReaderDao;
    }

    protected GenericEntityEtl() {
    }

    protected GenericEntityEtl(Class entityClass, String baseFilename, GenericDao dao) {
        this.entityClass = entityClass;
        this.baseFilename = baseFilename;
        this.dao = dao;
    }

    /**
     * Returns the JPA key for the entity.
     * @param entity entity having an id
     * @return the id
     */
    abstract Long entityId(T entity);

    /** Returns Criteria.Path to entityId given an entity root. */
    abstract Path rootId(Root root);

    /** Returns sqlLoader records for the C-typed entity given by entityId. */
    abstract Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId);

    /** Returns multiple sqlLoader records for the entity.  Override for fact table etl. */
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, C entity) {
        Collection<String> records = new ArrayList<String>();
        if (entity != null) {
            records.add(dataRecord(etlDateStr, isDelete, entity));
        }
        return records;
    }

    /** Returns a single sqlLoader record for the entity. */
    abstract String dataRecord(String etlDateStr, boolean isDelete, C entity);

    /**
     * Converts the generic T entity ids to C entity ids.
     * Default is pass-through; override for cross-etl behavior,
     * possibly by using the lookupAssociatedIds() method.
     */
    protected Collection<Long> convertIdsTtoC(Collection<Long> entityIds) {
        return entityIds;
    }

    /**
     * Converts the generic T entities to C entities.
     * Default is pass-through; override for cross-etl behavior.
     */
    protected Collection<C> convertTtoC(Collection<T> entities) {
        return (Collection<C>)entities;
    }

    /**
     * Iterates on the Mercury entities having changes, generates and writes sqlLoader records.
     *
     * @param revIds list of audit revision ids
     * @param etlDateStr etlDate formatted as YYYYMMDDHHMMSS
     * @return the number of records created in the data file (deletes and modifies).
     */
    public int doEtl(Collection<Long> revIds, String etlDateStr) {

        // Retrieves the Envers-formatted list of entity changes in the given revision range.
        List<Object[]> auditEntities = auditReaderDao.fetchDataChanges(revIds, entityClass);
        AuditLists auditLists = fetchAuditIds(auditEntities);

        // The convert calls optionally convert entity types for cross-entity etl classes.
        Collection<Long> deletedEntityIds = convertIdsTtoC(auditLists.deletedEntityIds);
        Collection<Long> changedEntityIds = convertIdsTtoC(auditLists.changedEntityIds);

        return writeRecords(deletedEntityIds, changedEntityIds, auditLists.revInfoPairs, etlDateStr);
    }

    /**
     * Does ETL by entity ids, such as for backfill.
     *
     * WILL NOT etl deleted entities -- must use incremental etl for that.
     *
     * @param requestedClass  the requested entity class, possibly one not handled by this etl class
     * @param startId entity id start of range, includes endpoint.
     * @param endId entity id end of range, includes endpoint.
     * @param etlDateStr etlDate formatted as YYYYMMDDHHMMSS
     * @return the number of records created in the data file (deletes and modifies).
     */
    public int doEtl(Class requestedClass, long startId, long endId, String etlDateStr) {

        // No-op unless the implementing class is the requested entity class.
        if (!entityClass.equals(requestedClass)) {
            return 0;
        }

        Collection<T> auditEntities = entitiesInRange(startId, endId);
        Collection<C> entities = convertTtoC(auditEntities);

        return writeRecords(entities, etlDateStr);
    }

    /** Returns entities having id in the given range, including endpoints. */
    protected Collection<T> entitiesInRange(final long startId, final long endId) {
        return dao.findAll(entityClass,
                new GenericDao.GenericDaoCallback<T>() {
                    @Override
                    public void callback(CriteriaQuery<T> cq, Root<T> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(rootId(root), startId, endId));
                    }
                });
    }

    @DaoFree
    protected AuditLists fetchAuditIds(Collection<Object[]> auditEntities) {
        Set<Long> deletedEntityIds = new HashSet<Long>();
        Set<Long> changedEntityIds = new HashSet<Long>();

        for (Object[] dataChange : auditEntities) {
            RevisionType revType = (RevisionType) dataChange[2];
            boolean isDelete = revType == RevisionType.DEL;
            T entity = (T)dataChange[0];
            Long entityId = entityId(entity);
            if (isDelete) {
                deletedEntityIds.add(entityId);
            } else {
                changedEntityIds.add(entityId);
            }
        }
        changedEntityIds.removeAll(deletedEntityIds);

        return new AuditLists(deletedEntityIds, changedEntityIds, Collections.EMPTY_LIST);
     }

    // DTO class used for audit reader data.
    protected class AuditLists {
        Collection<Long> deletedEntityIds;
        Collection<Long> changedEntityIds;
        Collection<RevInfoPair> revInfoPairs;

        public AuditLists(Collection<Long> deletedEntityIds, Collection<Long> changedEntityIds,
                          Collection<RevInfoPair> revInfoPairs) {
            this.deletedEntityIds = deletedEntityIds;
            this.changedEntityIds = changedEntityIds;
            this.revInfoPairs = revInfoPairs;
        }
    }

    // DTO class for entity-date pairs, only used for the status record generating etl classes.
    protected class RevInfoPair {
        private T revEntity;
        private Date revDate;

        T getRevEntity() {
            return revEntity;
        }

        Date getRevDate() {
            return revDate;
        }

        RevInfoPair(T revEntity, Date revDate) {
            this.revEntity = revEntity;
            this.revDate = revDate;
        }
    }



    @DaoFree
    protected int writeRecords(Collection<Long> deletedEntityIds, Collection<Long> changedEntityIds,
                               Collection<RevInfoPair> revInfoPairs, String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));

        try {
            // Deletion records only contain the entityId field.
            for (Long entityId : deletedEntityIds) {
                String record = genericRecord(etlDateStr, true, entityId);
                dataFile.write(record);
            }

            for (Long entityId : changedEntityIds) {
                Collection<String> records = dataRecords(etlDateStr, false, entityId);
                for (String record : records) {
                    dataFile.write(record);
                }
            }
            return dataFile.getRecordCount();

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename(), e);
            return dataFile.getRecordCount();

        } finally {
            dataFile.close();
        }
     }

    @DaoFree
    protected int writeRecords(Collection<C> entities, String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, baseFilename);
        DataFile dataFile = new DataFile(filename);

        try {
            // Writes the records.
            for (C entity : entities) {
                for (String record : dataRecords(etlDateStr, false, entity)) {
                    dataFile.write(record);
                }
            }
            return dataFile.getRecordCount();

        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
            return dataFile.getRecordCount();

        } finally {
            dataFile.close();
        }
    }

    /**
     *  Given a set of entity ids, queries for associated ids.
     *  Chunks as necessary to limit sql "in" clause to 1000 elements.
     *
     *  Cannot use JPASplitter because it doesn't support native query (fails querying PO_SAMPLE_RISK_JOIN_AUD).
     *
     * @param ids  entity ids
     * @param queryString  containing 'entity_id' as returned column name, and IN_CLAUSE_PLACEHOLDER between parens.
     * @return Set of associated ids, deduplicated
     */
    public Collection<Long> lookupAssociatedIds(Collection<Long> ids, String queryString) {
        Collection<Long> associatedIds = new HashSet<Long>();

        for (Collection<Long> split : BaseSplitter.split(ids)) {
            String inClause = StringUtils.join(split, ",");
            Query query = dao.getEntityManager().createNativeQuery(queryString.replaceFirst(IN_CLAUSE_PLACEHOLDER, inClause));
            // Makes NUMBER(38) be a Long instead of BigDecimal
            query.unwrap(SQLQuery.class).addScalar("entity_id", LongType.INSTANCE);

            associatedIds.addAll(query.getResultList());
        }
        return associatedIds;
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