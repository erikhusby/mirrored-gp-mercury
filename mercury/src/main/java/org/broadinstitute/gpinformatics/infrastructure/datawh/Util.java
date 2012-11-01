package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import java.util.List;

/**
 * Utility methods for data warehouse ETL.
 */
public class Util {

    /**
     * Finds and records all data changes that happened in the given interval.
     *
     * @param lastDate    start of interval
     * @param etlDate     end of interval, inclusive
     * @param reader      auditReader to use
     * @param entityClass the class of entity to process
     */
    static List<Object[]> fetchDataChanges(long lastDate, long etlDate, AuditReader reader, Class entityClass) {

        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionProperty("timestamp").gt(lastDate))
                .add(AuditEntity.revisionProperty("timestamp").le(etlDate));
        List<Object[]> dataChanges = query.getResultList();
        return dataChanges;
    }

    /**
     * Returns a sqlLoader data file name.
     *
     * @param etlDateStr   etl run time, for filename
     * @param baseFilename for filename
     * @return the file name
     */
    static String dataFilename(String etlDateStr, String baseFilename) {
        return ExtractTransform.getDatafileDir() + "/" + etlDateStr + "_" + baseFilename + ".dat";
    }

    /**
     * Returns a new DW record containing the common start fields.
     * All data file records begin with etlDate and isDelete.
     *
     * @param etlDateStr first field
     * @param isDelete   second field
     * @return StringBuilder that ends with a record delimiter
     */
    static StringBuilder startRecord(String etlDateStr, boolean isDelete) {
        return new StringBuilder()
                .append(etlDateStr).append(ExtractTransform.DELIM)
                .append(isDelete ? "T" : "F").append(ExtractTransform.DELIM);
    }

    static String makeRecord(String etlDateStr, boolean isDelete, Object... fields) {
        StringBuilder rec = Util.startRecord(etlDateStr, isDelete);
        for (Object field : fields) {
            rec.append(field).append(ExtractTransform.DELIM);
        }
        return rec.toString();
    }

}