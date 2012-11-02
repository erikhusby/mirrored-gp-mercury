package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract public class GenericEntityEtl {
    private Logger logger = Logger.getLogger(this.getClass());

    @Inject
    private Util util;

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
    abstract String makeRecord(String etlDateStr, boolean isDelete, Long entityId);

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
        List<Object[]> dataChanges = util.fetchDataChanges(lastRev, etlRev, getEntityClass());

        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        String filename = util.dataFilename(etlDateStr, getBaseFilename());
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                Object entity = dataChange[0];
                RevisionType revType = (RevisionType) dataChange[2];
                Long entityId = entityId(entity);

                // Writes a DW deletion record if entity was deleted.
                if (revType.equals(RevisionType.DEL)) {

                    String record = util.makeRecord(etlDateStr, true, entityId);
                    writer.write(record);
                    writer.newLine();
                    recordCount++;

                    deletedEntityIds.add(entityId);
                } else {
                    // Collects deduplicated entity ids in order to lookup the latest version once.
                    changedEntityIds.add(entityId);
                }
            }

            // Writes a record for latest version of each of the changed entity.
            changedEntityIds.removeAll(deletedEntityIds);
            for (Long entityId : changedEntityIds) {
                String record =  makeRecord(etlDateStr, false, entityId);
                if (record == null) {
                    logger.info("Cannot export " + getEntityClass().getSimpleName() + " having id " + entityId + " no longer exists.");
                } else {
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
        return recordCount;
    }
}