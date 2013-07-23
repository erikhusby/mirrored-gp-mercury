package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Base class for etl'ing entities that also have status etl.
 *
 * @param <AUDITED_ENTITY_CLASS>  the class that gets audited and referenced by backfill entity id range.
 * @param <ETL_DATA_SOURCE_CLASS> the class that is used to create sqlLoader records.  Typically ETL_DATA_SOURCE_CLASS
 *                                is the same class as AUDITED_ENTITY_CLASS, and only differs from AUDITED_ENTITY_CLASS
 *                                in cross-entity etl subclasses.
 */
public abstract class GenericEntityAndStatusEtl<AUDITED_ENTITY_CLASS, ETL_DATA_SOURCE_CLASS>
        extends GenericEntityEtl<AUDITED_ENTITY_CLASS, ETL_DATA_SOURCE_CLASS> {

    /**
     * The entity-related name of the data file, and must sync with the ETL cron script and control file.
     */
    public String baseStatusFilename;

    protected GenericEntityAndStatusEtl() {
    }

    protected GenericEntityAndStatusEtl(Class entityClass, String baseFilename, String baseStatusFilename,
                                        GenericDao dao) {
        super(entityClass, baseFilename, dao);
        this.baseStatusFilename = baseStatusFilename;
    }

    /**
     * Makes a sqlloader record from entity status fields, revision date, and etl date.
     *
     * @param etlDateStr date
     * @param isDelete   indicates deleted entity
     * @param entity     the Envers versioned entity
     * @param revDate    Envers revision date
     *
     * @return delimited SqlLoader record, or null if none
     */
    abstract String statusRecord(String etlDateStr, boolean isDelete, ETL_DATA_SOURCE_CLASS entity, Date revDate);

    /**
     * Converts the generic AUDITED_ENTITY_CLASS entity to ETL_DATA_SOURCE_CLASS entity.
     * Default is pass-through; override for cross-etl behavior.
     */
    protected ETL_DATA_SOURCE_CLASS convertTtoC(AUDITED_ENTITY_CLASS entities) {
        return (ETL_DATA_SOURCE_CLASS) entities;
    }

    @Override
    protected void addRevInfoPairs(Collection<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs, Object[] enversTriple,
                                   AUDITED_ENTITY_CLASS entity) {
        RevInfo revInfo = (RevInfo) enversTriple[AuditReaderDao.AUDIT_READER_REV_INFO_IDX];
        revInfoPairs.add(new RevInfoPair(entity, revInfo.getRevDate()));
    }

    @Override
    protected int writeRecords(Collection<Long> deletedEntityIds,
                               Collection<Long> modifiedEntityIds,
                               Collection<Long> addedEntityIds,
                               Collection<RevInfoPair<AUDITED_ENTITY_CLASS>> revInfoPairs,
                               String etlDateStr) {

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

            Collection<Long> nonDeletedIds = new ArrayList<>();
            nonDeletedIds.addAll(modifiedEntityIds);
            nonDeletedIds.addAll(addedEntityIds);

            for (Long entityId : nonDeletedIds) {
                Collection<String> records = dataRecords(etlDateStr, false, entityId);
                if (records.size() == 0) {
                    deletedEntityIds.add(entityId);
                }
                for (String record : records) {
                    dataFile.write(record);
                }
            }
            // Removes ids of no longer existent entities.
            modifiedEntityIds.removeAll(deletedEntityIds);

            // Records every audited status change.
            for (RevInfoPair<AUDITED_ENTITY_CLASS> pair : revInfoPairs) {
                Long entityId = entityId(pair.revEntity);

                // Db referential integrity errors happen on status for non-existent entities.
                if (modifiedEntityIds.contains(entityId)) {
                    Date revDate = pair.revDate;
                    ETL_DATA_SOURCE_CLASS entity = convertTtoC(pair.revEntity);
                    String record = statusRecord(etlDateStr, false, entity, revDate);
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
    protected int writeRecords(Collection<ETL_DATA_SOURCE_CLASS> entities, String etlDateStr) {

        Date statusDate;
        try {
            statusDate = ExtractTransform.parseTimestamp(etlDateStr);
        } catch (ParseException e) {
            logger.warn("Cannot parse date string '" + etlDateStr + "'");
            statusDate = new Date();
        }

        // Creates the wrapped Writer to the sqlLoader data file.
        DataFile dataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile statusFile = new DataFile(dataFilename(etlDateStr, baseStatusFilename));

        try {
            // Writes the data and status records.
            for (ETL_DATA_SOURCE_CLASS entity : entities) {
                for (String record : dataRecords(etlDateStr, false, entity)) {
                    dataFile.write(record);
                }
                String record = statusRecord(etlDateStr, false, entity, statusDate);
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
