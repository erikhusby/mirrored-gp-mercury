package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.EnversAudit;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static final int TRANSACTION_TIMEOUT = 2 * 60 * 60; // in seconds.

    // Gathering data over a large number of entities in a revision (PDO 1500 sample sets)
    //  causes Hibernate to consume too much memory.  Incorporate clearing Hibernate session when this batch size is hit.
    // Takes 30% less time and 36% of the memory hit on a 1500 sample revision in Hibernate 4/JBoss 7
    public static final int JPA_CLEAR_THRESHOLD = 75;

    /** The quote character for ETL values. */
    private static final String QUOTE = "\"";

    /** The empty formatted value. */
    private static final String EMPTY_VALUE = "";

    protected final Set<Long> errorIds = new HashSet<>();
    protected Exception errorException = null;

    public final Class<AUDITED_ENTITY_CLASS> entityClass;
    /**
     * The entity-related name of the data file, and must sync with the ETL cron script and control file.
     */
    public final String baseFilename;
    /** The name of the audit table */
    public final String auditTableName;
    /** The name of the column in the audit table that represents entity id. */
    public final String auditTableEntityIdColumnName;

    protected final Log logger = LogFactory.getLog(getClass());
    protected AuditReaderDao auditReaderDao;
    /**
     * The Mercury or Athena dao needed by the subclass.
     */
    protected final GenericDao dao;

    @Inject
    public void setAuditReaderDao(AuditReaderDao auditReaderDao) {
        this.auditReaderDao = auditReaderDao;
    }

    protected GenericEntityEtl() {
        this(null, null, null, null, null);
    }

    protected GenericEntityEtl(Class<AUDITED_ENTITY_CLASS> entityClass, String baseFilename,
                               String auditTableName, String auditTableEntityIdColumnName, GenericDao dao) {
        this.entityClass = entityClass;
        this.baseFilename = baseFilename;
        this.auditTableName = auditTableName;
        this.auditTableEntityIdColumnName = auditTableEntityIdColumnName;
        this.dao = dao;
    }

    /**
     * Returns the entityId for the audited entity.
     *
     * @param entity is the audited entity
     * @return the entityId
     */
    abstract Long entityId(AUDITED_ENTITY_CLASS entity);

    /**
     * Returns the entityId for the data source entity.
     *
     * @param entity is a data source entity
     * @return the entityId
     */
    protected Long dataSourceEntityId(ETL_DATA_SOURCE_CLASS entity) {
        // This default works when AUDITED_ENTITY_CLASS = ETL_DATA_SOURCE_CLASS
        return entityId((AUDITED_ENTITY_CLASS)entity);
    }

    /**
     * Returns Criteria.Path to entityId given an entity root.
     */
    abstract Path rootId(Root<AUDITED_ENTITY_CLASS> root);

    /**
     * Returns sqlLoader records for the ETL_DATA_SOURCE_CLASS-typed entity given by entityId.
     */
    abstract Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId);

    /**
     * Returns multiple sqlLoader records for the entity.  Override for fact table etl.
     */
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, ETL_DATA_SOURCE_CLASS entity) {
        Collection<String> records = new ArrayList<>();
        if (entity != null) {
            records.add(dataRecord(etlDateStr, isDelete, entity));
        }
        return records;
    }

    /**
     * Returns a single sqlLoader record for the entity.
     */
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

        //noinspection unchecked
        return (Collection<ETL_DATA_SOURCE_CLASS>) entities;
    }

    /**
     * Allows subclass to query related _AUD tables to pick up changes to ETL_DATA_SOURCE_CLASS objects.
     * Use this when there is no entity class for the related _AUD table; cross-entity etl can be used
     * when there is an entity class.
     * @param revIds audit revs
     * @return ETL_DATA_SOURCE_CLASS entity ids
     */
    protected Collection<Long> fetchAdditionalModifies(Collection<Long>revIds) {
        return Collections.emptySet();
    }

    /**
     * Iterates on the Mercury entities having changes, generates and writes sqlLoader records.
     *
     * @param revIds     list of audit revision ids
     * @param etlDateStr etlDate formatted as YYYYMMDDHHMMSS
     *
     * @return the number of records created in the data file (deletes, modifies, and adds).
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int doIncrementalEtl(Set<Long> revIds, String etlDateStr) throws Exception {
        try {

            // Retrieves the Envers-formatted list of entity changes in the given revision range.
            // Subclass may add additional entity ids based on custom rev query.
            List<EnversAudit> auditEntities = auditReaderDao.fetchEnversAudits(revIds, entityClass);
            AuditLists<AUDITED_ENTITY_CLASS> auditLists = fetchAuditIds(auditEntities, fetchAdditionalModifies(revIds));

            // The convert calls optionally convert entity types for cross-entity etl classes.
            Collection<Long> deletedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditLists.deletedEntityIds);
            Collection<Long> modifiedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditLists.modifiedEntityIds);
            Collection<Long> addedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditLists.addedEntityIds);

            int count =  writeEtlDataFile(deletedEntityIds, modifiedEntityIds, addedEntityIds, auditLists.revInfoPairs,
                    etlDateStr);

            auditReaderDao.clear();

            return count;

        } catch (Exception e) {
            logger.error(getClass().getSimpleName() + " incremental ETL failed", e);
            throw e;
        }
    }

    int writeEtlDataFile(Collection<Long> deletedEntityIds,
                         Collection<Long> modifiedEntityIds,
                         Collection<Long> addedEntityIds,
                         Collection<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs,
                         String etlDateStr) throws Exception {
        try {
            return writeRecords(deletedEntityIds, modifiedEntityIds, addedEntityIds, revInfoPairs, etlDateStr);
        } finally {
            postEtlLogging();
        }
    }

    /**
     * Does ETL by entity ids, such as for backfill.  Also finds deleted entities in the given range.
     *
     * @param requestedClass the requested entity class, possibly one not handled by this etl class
     * @param startId        entity id start of range, includes endpoint.
     * @param endId          entity id end of range, includes endpoint.
     * @param etlDateStr     etlDate formatted as YYYYMMDDHHMMSS
     *
     * @return the number of records created in the data file (deletes, modifies, adds).
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int doBackfillEtl(Class<?> requestedClass, long startId, long endId, String etlDateStr) throws Exception {

        // No-op unless the implementing class is the requested entity class.  Not an error.
        if (!entityClass.equals(requestedClass)) {
            return 0;
        }
        try {
            Collection<Long> auditClassDeletedIds = fetchDeletedEntityIds(startId, endId);
            Collection<Long> auditClassModifiedIds = new ArrayList<>();

            for (AUDITED_ENTITY_CLASS auditEntity : entitiesInRange(startId, endId)) {
                Long entityId = entityId(auditEntity);
                if (!auditClassDeletedIds.contains(entityId)) {
                    auditClassModifiedIds.add(entityId);
                }
            }
            // Converts entity types for cross-entity etl classes.
            Collection<Long> modifiedEntityIds = convertAuditedEntityIdToDataSourceEntityId(auditClassModifiedIds);

            // Must not delete cross-etl entities when doing backfill. Tests for cross-etl by checking for
            // identical audit class entity ids and data source class entity ids, which should only be true
            // when the two classes are the same, i.e. no cross-etl.
            Collection<Long> deletedEntityIds = new ArrayList<>(
                    convertAuditedEntityIdToDataSourceEntityId(auditClassDeletedIds));
            deletedEntityIds.retainAll(auditClassDeletedIds);
            if (deletedEntityIds.size() != auditClassDeletedIds.size()) {
                deletedEntityIds.clear();
            }

            int count =  writeEtlDataFile(deletedEntityIds, modifiedEntityIds, Collections.<Long>emptyList(),
                    Collections.<RevInfoPair<AUDITED_ENTITY_CLASS>>emptyList(), etlDateStr);

            auditReaderDao.clear();
            return count;

        } catch (Exception e) {
            logger.error(getClass().getSimpleName() + " backfill ETL failed", e);
            throw e;

        } finally {
            postEtlLogging();
        }
    }

    // Queries the _AUD table directly since AuditReader has no API for this.
    // In the future we may need to add index on entityId on the audit tables.
    private Collection<Long> fetchDeletedEntityIds(long startId, long endId) {
        String queryString = String.format("select %s from %s where %s between %d and %d and revtype = 2",
                auditTableEntityIdColumnName,
                auditTableName,
                auditTableEntityIdColumnName,
                startId,
                endId);
        Query query = dao.getEntityManager().createNativeQuery(queryString);

        // Makes NUMBER(38) be a Long instead of BigDecimal
        query.unwrap(SQLQuery.class).addScalar(auditTableEntityIdColumnName, LongType.INSTANCE);
        return query.getResultList();
    }

    /**
     * Returns entities having id in the given range, including endpoints.
     */
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

    /**
     * Parses AuditReader output into more useful lists of entities.
     *
     * @param auditEntities is collection of EnversAudit already sorted by increasing rev date.
     * @param additionalModifies is a collection of entity ids to be added to the modified entities.
     * @return lists of entity ids for deleted, added, and modified entities; and maybe a list of RevInfoPairs.
     */
    @DaoFree
    private AuditLists<AUDITED_ENTITY_CLASS> fetchAuditIds(Collection<EnversAudit> auditEntities,
                                                           Collection<Long> additionalModifies) {
        Set<Long> deletedEntityIds = new HashSet<>();
        Set<Long> modifiedEntityIds = new HashSet<>(additionalModifies);
        Set<Long> addedEntityIds = new HashSet<>();
        List<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs = new ArrayList<>();

        for (EnversAudit enversAudit : auditEntities) {
            AUDITED_ENTITY_CLASS entity = (AUDITED_ENTITY_CLASS) enversAudit.getEntity();
            Long entityId = entityId(entity);
            switch(enversAudit.getRevType()) {
            case ADD:
                addedEntityIds.add(entityId);
                break;
            case MOD:
                modifiedEntityIds.add(entityId);
                break;
            case DEL:
                deletedEntityIds.add(entityId);
                break;
            }
            addRevInfoPairs(revInfoPairs, enversAudit.getRevInfo(), entity);
        }

        // A given entity id should appear in only one of the collections.
        modifiedEntityIds.removeAll(deletedEntityIds);
        addedEntityIds.removeAll(deletedEntityIds);
        modifiedEntityIds.removeAll(addedEntityIds);

        return new AuditLists<>(deletedEntityIds, modifiedEntityIds, addedEntityIds, revInfoPairs);
    }

    protected void addRevInfoPairs(Collection<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs,
                                   RevInfo revInfo,
                                   AUDITED_ENTITY_CLASS entity) {
        // No-op in this class.  Sub-class implements this in order to have revInfoPairs saved.
    }

    /**
     * DTO class used for audit reader data.
     */
    class AuditLists<ENTITY_CLASS> {
        final Collection<Long> deletedEntityIds;
        final Collection<Long> modifiedEntityIds;
        final Collection<Long> addedEntityIds;
        final Collection<RevInfoPair<ENTITY_CLASS>> revInfoPairs;

        public AuditLists(Collection<Long> deletedEntityIds, Collection<Long> modifiedEntityIds,
                          Collection<Long> addedEntityIds, Collection<RevInfoPair<ENTITY_CLASS>> revInfoPairs) {
            this.deletedEntityIds = deletedEntityIds;
            this.modifiedEntityIds = modifiedEntityIds;
            this.addedEntityIds = addedEntityIds;
            this.revInfoPairs = revInfoPairs;
        }
    }

    /**
     * DTO class for entity-date pairs, only used for the status record generating etl classes.
     */
    class RevInfoPair<ENTITY_CLASS> {
        final ENTITY_CLASS revEntity;
        final Date revDate;

        RevInfoPair(ENTITY_CLASS revEntity, Date revDate) {
            this.revEntity = revEntity;
            this.revDate = revDate;
        }
    }


    /**
     * Writes the sqlLoader data file records for the given entity changes.
     */
    protected int writeRecords(Collection<Long> deletedEntityIds,
            Collection<Long> modifiedEntityIds,
            Collection<Long> addedEntityIds,
            Collection<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs,
            String etlDateStr) throws Exception {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));

        try {
            // Deletion records only contain the entityId field.
            for (Long entityId : deletedEntityIds) {
                String record = genericRecord(etlDateStr, true, entityId);
                dataFile.write(record);
            }

            Collection<Long> nonDeletedIds = new ArrayList<>();
            nonDeletedIds.addAll(modifiedEntityIds);
            nonDeletedIds.addAll(addedEntityIds);

            for (Long entityId : nonDeletedIds) {
                try {
                    Collection<String> records = dataRecords(etlDateStr, false, entityId);
                    for (String record : records) {
                        dataFile.write(record);
                    }
                } catch (Exception e) {
                    // For data-specific Mercury exceptions on one entity, log it and continue, since
                    // these are permanent. For systemic exceptions such as when BSP is down, re-throw
                    // the exception in order to stop this run of ETL and allow a retry in a few minutes.
                    if (isSystemException(e)) {
                        throw e;
                    } else {
                        if (errorException == null) {
                            errorException = e;
                        }
                        errorIds.add(entityId);
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename(), e);
            throw e;

        } finally {
            dataFile.close();
        }
        return dataFile.getRecordCount();
    }

    protected boolean isSystemException(Exception e) {
        return !e.getClass().getName().contains("broadinstitute");
    }

    /**
     * Writes the sqlLoader data file records for the given entity changes.
     */
    @DaoFree
    protected int writeRecords(Collection<ETL_DATA_SOURCE_CLASS> entities,
                               Collection<Long>deletedEntityIds,
                               String etlDateStr) throws Exception {

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));

        try {
            // Deletion records only contain the entityId field.
            for (Long entityId : deletedEntityIds) {
                String record = genericRecord(etlDateStr, true, entityId);
                dataFile.write(record);
            }
            // Writes the records.
            for (ETL_DATA_SOURCE_CLASS entity : entities) {
                if (!deletedEntityIds.contains(dataSourceEntityId(entity))) {
                    try {
                        for (String record : dataRecords(etlDateStr, false, entity)) {
                            dataFile.write(record);
                        }
                    } catch (Exception e) {
                        // For data-specific Mercury exceptions on one entity, log it and continue, since
                        // these are permanent. For systemic exceptions such as when BSP is down, re-throw
                        // the exception in order to stop this run of ETL and allow a retry in a few minutes.
                        if (isSystemException(e)) {
                            throw e;
                        } else {
                            if (errorException == null) {
                                errorException = e;
                            }
                            errorIds.add(dataSourceEntityId(entity));
                        }
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename(), e);
            throw e;

        } finally {
            dataFile.close();
        }
        return dataFile.getRecordCount();
    }

    /**
     * Given a set of entity ids, queries for associated ids, the association given by the sql query string.
     *
     * @param ids         entity ids
     * @param queryString containing 'entity_id' as returned column name, and IN_CLAUSE_PLACEHOLDER between parens.
     *
     * @return Set of associated ids, deduplicated
     */
    public Collection<Long> lookupAssociatedIds(Collection<Long> ids, String queryString) {
        Collection<Long> associatedIds = new HashSet<>();

        // Cannot use JPASplitter because it doesn't support native query (fails querying PO_SAMPLE_RISK_JOIN_AUD).
        for (Collection<Long> split : BaseSplitter.split(ids)) {
            String inClause = StringUtils.join(split, ",");
            Query query = dao.getEntityManager().
                    createNativeQuery(queryString.replaceFirst(IN_CLAUSE_PLACEHOLDER, inClause));
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
     *
     * @return the data file name
     */
    public static String dataFilename(String etlDateStr, String baseFilename) {
        return ExtractTransform.getDatafileDir() + File.separator + etlDateStr + "_" + baseFilename + ".dat";
    }

    /**
     * Converts fields to a data record.
     *
     * @param etlDateStr date
     * @param isDelete   indicates a deleted entity
     * @param fields     the fields to be put in the data record
     *
     * @return formatted data record
     */
    public static String genericRecord(String etlDateStr, boolean isDelete, Object... fields) {
        StringBuilder rec = new StringBuilder()
                .append(etlDateStr)
                .append(ExtractTransform.DELIMITER)
                .append(format(isDelete));
        for (Object field : fields) {
            rec.append(ExtractTransform.DELIMITER).append(field);
        }
        return rec.toString();
    }

    /**
     * Returns formatted date string, or "" string if date is null.
     *
     * @param date the date to format
     */
    public static String format(Date date) {
        return (date != null ? ExtractTransform.formatTimestamp(date) : EMPTY_VALUE);
    }

    /**
     * Returns T or F string for the boolean.
     *
     * @param bool to format
     */
    public static String format(boolean bool) {
        return (bool ? "T" : "F");
    }

    /**
     * Returns String, or "" string if null, and quotes string if DELIM occurs.
     *
     * @param string to format
     */
    public static String format(String string) {
        if (string == null) {
            return EMPTY_VALUE;
        }
        // Newlines are not allowed in the output.
        string = string.replace('\n', ' ');
        // If a delimiter is present, we need to quote the contents. Escape all embedded quotes by doubling them.
        if (string.contains(ExtractTransform.DELIMITER)) {
            return QUOTE + string.replaceAll(QUOTE, QUOTE + QUOTE) + QUOTE;
        }
        return string;
    }

    /**
     * Returns String, or "" string if null.
     *
     * @param num to format
     */
    public static <T extends Number> String format(T num) {
        return (num != null ? num.toString() : EMPTY_VALUE);
    }

    /**
     * Class to wrap/manage writing to the data file.
     */
    protected static class DataFile {
        private final String filename;
        private BufferedWriter writer;
        private int lineCount = 0;

        DataFile(String filename) {
            this.filename = filename;

            // There are cases (fixup tests) where records are appended to existing files
            // Adjust line counter as required
            java.nio.file.Path path = FileSystems.getDefault().getPath(filename);
            if(Files.exists(path) ) {
                BufferedReader lineCounter = null;
                try {
                    lineCounter = Files.newBufferedReader(path, Charset.defaultCharset());
                    while( lineCounter.readLine() != null ) {
                        lineCount++;
                    }
                    // Roll counter back to accommodate empty last line
                    lineCount--;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(lineCounter);
                }
            }
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
                writer = new BufferedWriter(new FileWriter(filename, true));
            }
            writer.write(lineCount + ExtractTransform.DELIMITER + record);
            writer.newLine();
        }

        void close() {
            IOUtils.closeQuietly(writer);
        }
    }

    public static long HASH_PRIME = 1125899906842597L;
    public static long HASH_MULTIPLIER = 31L;

    /**
     * Calculates a hash on String.
     * From http://stackoverflow.com/questions/1660501/what-is-a-good-64bit-hash-function-in-java-for-textual-strings
     */
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
            sb.append(ExtractTransform.DELIMITER).append(s);
        }
        return GenericEntityEtl.hash(sb.toString());
    }

    /**
     * Tells ETL class to do per-run logging, at the end of the run.
     */
    protected void postEtlLogging() {
        if (errorException != null) {
            logger.info("ETL failed on " + getClass().getSimpleName() +
                        (errorIds.size() > 0 ? " for ids " + StringUtils.join(errorIds, ", ") : ""), errorException);
            errorException = null;
            errorIds.clear();
        }
    }
}
