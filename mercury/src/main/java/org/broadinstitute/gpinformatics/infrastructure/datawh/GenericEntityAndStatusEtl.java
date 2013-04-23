package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Base class for etl'ing entities that also have status etl.
 * @param <T> the class that gets audited and referenced by backfill entity id range.
 * @param <C> the class that is used to create sqlLoader records.  Typically C is the same class as T,
 *            and only differs from T in cross-entity etl subclasses.
 */
public abstract class GenericEntityAndStatusEtl<T, C> extends GenericEntityEtl<T, C> {

    /** The entity-related name of the data file, and must sync with the ETL cron script and control file. */
    public String baseStatusFilename;

    protected GenericEntityAndStatusEtl(Class entityClass, String baseFilename, String baseStatusFilename, GenericDao dao) {
        super(entityClass, baseFilename, dao);
        this.baseStatusFilename = baseStatusFilename;
    }

    /**
     * Makes a sqlloader record from entity status fields, revision date, and etl date.
     *
     * @param etlDateStr date
     * @param revDate    Envers revision date
     * @param entity     the Envers versioned entity
     * @param isDelete   indicates deleted entity
     * @return delimited SqlLoader record, or null if none
     */
    abstract String statusRecord(String etlDateStr, Date revDate, C entity, boolean isDelete);


    /**
     * Converts the generic T entity to C entity.
     * Default is pass-through; override for cross-etl behavior.
     */
    protected C convertTtoC(T entities) {
        return (C)entities;
    }


    @Override
    protected AuditLists fetchAuditIds(Collection<Object[]> auditEntities) {
        Set<Long> deletedEntityIds = new HashSet<Long>();
        Set<Long> changedEntityIds = new HashSet<Long>();
        List<RevInfoPair> revInfoPairs = new ArrayList<RevInfoPair>();

        for (Object[] dataChange : auditEntities) {
            RevisionType revType = (RevisionType) dataChange[AUDIT_READER_TYPE_IDX];
            boolean isDelete = revType == RevisionType.DEL;

            T entity = (T) dataChange[AUDIT_READER_ENTITY_IDX];
            Long entityId = entityId(entity);

            if (isDelete) {
                deletedEntityIds.add(entityId);
            } else {
                changedEntityIds.add(entityId);

                RevInfo revInfo = (RevInfo) dataChange[AUDIT_READER_REV_INFO_IDX];
                revInfoPairs.add(new RevInfoPair(entity, revInfo.getRevDate()));
            }
        }
        changedEntityIds.removeAll(deletedEntityIds);

        return new AuditLists(deletedEntityIds, changedEntityIds, revInfoPairs);
    }


    @Override
    protected int writeRecords(Collection<Long> deletedEntityIds, Collection<Long> changedEntityIds,
                               Collection<RevInfoPair> revInfoPairs, String etlDateStr) {


        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile statusFile = new DataFile(dataFilename(etlDateStr, baseStatusFilename));

        try {
            // Deletion records only contain the entityId field.
            for (Long entityId : deletedEntityIds) {
                String record = genericRecord(etlDateStr, true, entityId);
                dataFile.write(record);
                statusFile.write(record);
            }

            for (Long entityId : changedEntityIds) {
                Collection<String> records = dataRecords(etlDateStr, false, entityId);
                if (records.size() == 0) {
                    deletedEntityIds.add(entityId);
                }
                for (String record : records) {
                    dataFile.write(record);
                }
            }
            // Removes ids of no longer existent entities.
            changedEntityIds.removeAll(deletedEntityIds);

            // Records every audited status change.
            for (RevInfoPair pair : revInfoPairs) {
                Long entityId = entityId(pair.getRevEntity());

                // Db referential integrity errors happen on status for non-existent entities.
                if (changedEntityIds.contains(entityId)) {
                    Date revDate = pair.getRevDate();
                    C entity = convertTtoC(pair.getRevEntity());
                    String record = statusRecord(etlDateStr, revDate, entity, false);
                    statusFile.write(record);
                }
            }
            return dataFile.getRecordCount() + statusFile.getRecordCount();

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename() + " or " + statusFile.getFilename(), e);
            return dataFile.getRecordCount() + statusFile.getRecordCount();

        } finally {
            dataFile.close();
            statusFile.close();
        }

     }

    @Override
    protected int writeRecords(Collection<C> entities, String etlDateStr) {

        Date statusDate = null;
        try {
            statusDate = ExtractTransform.secTimestampFormat.parse(etlDateStr);
        } catch (ParseException e) {
            logger.warn("Cannot parse date string '" + etlDateStr + "'");
            statusDate = new Date();
        }

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile statusFile = new DataFile(dataFilename(etlDateStr, baseStatusFilename));

        try {
            // Writes the data and status records.
            for (C entity : entities) {
                for (String record : dataRecords(etlDateStr, false, entity)) {
                    dataFile.write(record);
                }
                String record = statusRecord(etlDateStr, statusDate, entity, false);
                statusFile.write(record);
            }

            return dataFile.getRecordCount() + statusFile.getRecordCount();

        } catch (IOException e) {
            logger.error("Error while writing " + dataFile.getFilename() + " or " + statusFile.getFilename(), e);
            return dataFile.getRecordCount() + statusFile.getRecordCount();

        } finally {
            dataFile.close();
            statusFile.close();
        }
    }

}
