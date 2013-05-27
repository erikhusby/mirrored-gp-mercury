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
 * @param <AUDITED_ENTITY_CLASS>  the class that gets audited and referenced by backfill entity id range.
 * @param <ETL_DATA_SOURCE_CLASS> the class that is used to create sqlLoader records.  Typically ETL_DATA_SOURCE_CLASS
 *                                is the same class as AUDITED_ENTITY_CLASS, and only differs from AUDITED_ENTITY_CLASS
 *                                in cross-entity etl subclasses.
 */
public abstract class GenericEntityEtl<AUDITED_ENTITY_CLASS, ETL_DATA_SOURCE_CLASS> {
    public static final String IN_CLAUSE_PLACEHOLDER = "__IN_CLAUSE__";
    /** Envers-defined constant indicating the entity. */
    static final int AUDIT_READER_ENTITY_IDX = 0;
    /** Envers-defined constant indicating the revInfo. */
    static final int AUDIT_READER_REV_INFO_IDX = 1;
    /** Envers-defined constant indicating the change type. */
    static final int AUDIT_READER_TYPE_IDX = 2;

    /** Equivalent to AUDITED_ENTITY_CLASS.class, i.e. the audited entity class handled by the subclass. */
    public Class entityClass;
    /** The entity-related name of the data file, and must sync with the ETL cron script and control file. */
    public String baseFilename;

    protected final Log logger = LogFactory.getLog(getClass());
    protected AuditReaderDao auditReaderDao;
    /** The Mercury or Athena dao needed by the subclass. */
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
    abstract Long entityId(AUDITED_ENTITY_CLASS entity);

    /** Returns Criteria.Path to entityId given an entity root. */
    abstract Path rootId(Root root);

    /** Returns sqlLoader records for the ETL_DATA_SOURCE_CLASS-typed entity given by entityId. */
    abstract Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId);

    /** Returns multiple sqlLoader records for the entity.  Override for fact table etl. */
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, ETL_DATA_SOURCE_CLASS entity) {
        Collection<String> records = new ArrayList<String>();
        if (entity != null) {
            records.add(dataRecord(etlDateStr, isDelete, entity));
        }
        return records;
    }

    /** Returns a single sqlLoader record for the entity. */
    abstract String dataRecord(String etlDateStr, boolean isDelete, ETL_DATA_SOURCE_CLASS entity);

    /**
     * Converts the generic AUDITED_ENTITY_CLASS entity ids to ETL_DATA_SOURCE_CLASS entity ids.
     * Default is pass-through; override for cross-etl behavior,
     * possibly by using the lookupAssociatedIds() method.
     */
    protected Collection<Long> convertAuditedEntityIdToDataSourceEntityId(Collection<Long> entityIds) {
        return entityIds;
    }

    /**
     * Converts the generic AUDITED_ENTITY_CLASS entities to ETL_DATA_SOURCE_CLASS entities.
     * Default is pass-through; override for cross-etl behavior.
     */
    protected Collection<ETL_DATA_SOURCE_CLASS> convertAuditedEntityToDataSourceEntity(
            Collection<AUDITED_ENTITY_CLASS> entities) {

        return (Collection<ETL_DATA_SOURCE_CLASS>)entities;
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
        List<Object[]> auditEntities = auditReaderDao.fetchDataChanges(revIds, entityClass, true);
        AuditLists auditLists = fetchAuditIds(auditEntities);

        // The convert calls optionally convert entity types for cross-entity etl classes.
        Collection<Long> deletedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditLists.deletedEntityIds);
        Collection<Long> changedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditLists.changedEntityIds);

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

        // No-op unless the implementing class is the requested entity class.  Not an error.
        if (!entityClass.equals(requestedClass)) {
            return 0;
        }

        Collection<AUDITED_ENTITY_CLASS> auditEntities = entitiesInRange(startId, endId);
        Collection<ETL_DATA_SOURCE_CLASS> entities = convertAuditedEntityToDataSourceEntity(auditEntities);

        return writeRecords(entities, etlDateStr);
    }

    /** Returns entities having id in the given range, including endpoints. */
    protected Collection<AUDITED_ENTITY_CLASS> entitiesInRange(final long startId, final long endId) {
        return dao.findAll(entityClass,
                new GenericDao.GenericDaoCallback<AUDITED_ENTITY_CLASS>() {
                    @Override
                    public void callback(CriteriaQuery<AUDITED_ENTITY_CLASS> cq, Root<AUDITED_ENTITY_CLASS> root) {
                        CriteriaBuilder cb = dao.getEntityManager().getCriteriaBuilder();
                        cq.where(cb.between(rootId(root), startId, endId));
                    }
                });
    }

    /** Parses AuditReader output into more useful lists of entities. */
    @DaoFree
    protected AuditLists fetchAuditIds(Collection<Object[]> auditEntities) {
        Set<Long> deletedEntityIds = new HashSet<Long>();
        Set<Long> changedEntityIds = new HashSet<Long>();

        for (Object[] dataChange : auditEntities) {
            RevisionType revType = (RevisionType) dataChange[AUDIT_READER_TYPE_IDX];
            boolean isDelete = revType == RevisionType.DEL;
            AUDITED_ENTITY_CLASS entity = (AUDITED_ENTITY_CLASS)dataChange[AUDIT_READER_ENTITY_IDX];
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

    /** DTO class used for audit reader data. */
    protected class AuditLists {
        final Collection<Long> deletedEntityIds;
        final Collection<Long> changedEntityIds;
        final Collection<RevInfoPair> revInfoPairs;

        public AuditLists(Collection<Long> deletedEntityIds, Collection<Long> changedEntityIds,
                          Collection<RevInfoPair> revInfoPairs) {
            this.deletedEntityIds = deletedEntityIds;
            this.changedEntityIds = changedEntityIds;
            this.revInfoPairs = revInfoPairs;
        }
    }

    /** DTO class for entity-date pairs, only used for the status record generating etl classes. */
    protected class RevInfoPair {
        final AUDITED_ENTITY_CLASS revEntity;
        final Date revDate;

        RevInfoPair(AUDITED_ENTITY_CLASS revEntity, Date revDate) {
            this.revEntity = revEntity;
            this.revDate = revDate;
        }
    }


    /** Writes the sqlLoader data file records for the given entity changes. */
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

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename(), e);

        } finally {
            dataFile.close();
        }
        return dataFile.getRecordCount();
     }

    /** Writes the sqlLoader data file records for the given entity changes. */
    @DaoFree
    protected int writeRecords(Collection<ETL_DATA_SOURCE_CLASS> entities, String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));

        try {
            // Writes the records.
            for (ETL_DATA_SOURCE_CLASS entity : entities) {
                for (String record : dataRecords(etlDateStr, false, entity)) {
                    dataFile.write(record);
                }
            }

        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);

        } finally {
            dataFile.close();
        }
        return dataFile.getRecordCount();
    }

    /**
     *  Given a set of entity ids, queries for associated ids, the association given by the sql query string.
     *
     * @param ids  entity ids
     * @param queryString  containing 'entity_id' as returned column name, and IN_CLAUSE_PLACEHOLDER between parens.
     * @return Set of associated ids, deduplicated
     */
    public Collection<Long> lookupAssociatedIds(Collection<Long> ids, String queryString) {
        Collection<Long> associatedIds = new HashSet<Long>();

        // Cannot use JPASplitter because it doesn't support native query (fails querying PO_SAMPLE_RISK_JOIN_AUD).
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