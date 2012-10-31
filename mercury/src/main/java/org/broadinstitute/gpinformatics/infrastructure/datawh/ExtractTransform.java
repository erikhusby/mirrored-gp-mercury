package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.ejb.Schedule;
import javax.inject.Singleton;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is a JEE scheduled bean that does the initial parts of ETL for the data warehouse.
 *
 * Extract processing is first, and it uses Envers AuditReader to get relevant data from the
 * _AUD tables.  These tables contain changes to entities marked with a revision.  For most
 * entities ETL only wants the latest version.  However there are several historical status
 * tables that will need to iterate over the relevant range of revisions, typically all changes
 * since the last ETL run, and extract the status and status dates.
 *
 * Transform processing is next.  It maps the entities to DW import table records, writes
 * them to a data file that is named with a etl_date.  The same etl_date is used for all DW
 * data processed by this run of ExtractTransform.
 *
 * Created with IntelliJ IDEA.
 * User: epolk
 * Date: 10/29/12
 */

@Singleton
public class ExtractTransform {
    private static String DATAFILE_DIR = "/seq/lims/datawh/dev/new";
    private static String LAST_TIMESTAMP_FILE = "last_etl_timestamp";
    private static String READY_FILE_SUFFIX = "_is_ready";
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    /** Record delimiter expected in sqlLoader file. */
    private final String DELIM = ",";

    /**
     * Runs one pass of incremental ETL.
     */
    @Schedule(hour="*/15")
    private void incrementalETL() {
        final long etlDate = System.currentTimeMillis();
        final String etlDateStr = fullDateFormat.format(new Date(etlDate));
        long lastDate = 0L;

        lastDate = readLastTimestamp();

        AuditReader auditReader = AuditReaderFactory.get(new GenericDao().getEntityManager());
        doBillableItem(lastDate, etlDate, etlDateStr, auditReader, "billable_item");
        /*
        doProductOrderSample(lastDate, etlDate, etlDateStr, auditReader, "product_order_sample");
        doProductOrderSampleStatus(lastDate, etlDate, etlDateStr, auditReader, "product_order_sample_status");
        doProductOrder(lastDate, etlDate, etlDateStr, auditReader, "product_order");
        doProductOrderStatus(lastDate, etlDate, etlDateStr, auditReader, "product_order_status");
        doResearchProjectCohort(lastDate, etlDate, etlDateStr, auditReader, "research_project_cohort");
        doResearchProjectFunding(lastDate, etlDate, etlDateStr, auditReader, "research_project_funding");
        doResearchProjectIRB(lastDate, etlDate, etlDateStr, auditReader, "research_project_irb");
        doProjectPerson(lastDate, etlDate, etlDateStr, auditReader, "research_project_person");
        doResearchProject(lastDate, etlDate, etlDateStr, auditReader, "research_project");
        doResearchProjectStatus(lastDate, etlDate, etlDateStr, auditReader, "research_project_status");
        doPriceItem(lastDate, etlDate, etlDateStr, auditReader, "price_item");
        doProduct(lastDate, etlDate, etlDateStr, auditReader, "product");
        doProductAddOn(lastDate, etlDate, etlDateStr, auditReader, "product_add_on");
        */
        writeLastTimestampFile(etlDate);
        writeIsReadyFile(etlDateStr);
    }

    /**
     * Does extract, transform, and writing sqlLoader file for Mercury entity.
     * Only the most recent version of a modified entity is recorded.
     *
     * @param lastDate beginning of the time interval to look for entity changes.
     * @param etlDate end of the time interval to look for entity changes.
     * @param auditReader the Envers audit reader.
     * @param baseFilename for naming the sqlLoader file.
     */
    private void doBillableItem(long lastDate, long etlDate, String etlDateStr, AuditReader auditReader, String baseFilename) {
        List<Object[]> dataChanges = fetchDataChanges(lastDate, etlDate, auditReader, BillableItem.class);
        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        try {
            Writer writer = fileWriter(etlDateStr, baseFilename);
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                BillableItem entity = (BillableItem)dataChange[0];
                RevisionType revType = (RevisionType)dataChange[2];
                Long entityId = entity.getBillableItemId();

                // Writes a DW deletion record if entity was deleted.
                if (revType.equals(RevisionType.DEL)) {
                    String record = transform(entity, etlDateStr, true);
                    writer.write(record);
                    deletedEntityIds.add(entityId);
                } else {
                    // Collects deduplicated entity ids in order to lookup the latest version once.
                    changedEntityIds.add(entityId);
                }

            }

            // Makes records for latest version of the changed entity.
            changedEntityIds.removeAll(deletedEntityIds);
            for (Long entityId : changedEntityIds) {
                BillableItem entity = (new BillableItemDao()).findByBillableItemId(entityId);
                writer.write(transform(entity, etlDateStr, false));
            }
        } catch (IOException e) {
            logger.error("Problem writing " + etlDateStr + "_" + baseFilename);
        }
    }

    /**
     * Transforms Mercury entity to DW record.
     * @param entity the mercury entity
     * @param etlDateStr etl run timestamp
     * @param isDelete indicates the Mercury entity was deleted
     * @return DW record, a delimited string for sqlLoader
     */
    private String transform(BillableItem entity, String etlDateStr, boolean isDelete) {
        StringBuilder rec = startRecord(etlDateStr, isDelete);
        rec.append(entity.getBillableItemId());
        if (!isDelete) {
            rec.append(DELIM)
                    .append(entity.getProductOrderSample().getProductOrderSampleId()).append(DELIM)
                    .append(entity.getPriceItem().getPriceItemId()).append(DELIM)
                    .append(entity.getCount());
        }
        return rec.toString();
    }

    /**
     * Finds and records all data changes that happened in the given interval.
     *
     * @param lastDate start of interval
     * @param etlDate end of interval, inclusive
     * @param reader auditReader to use
     * @param entityClass the class of entity to process
     */
    private List<Object[]> fetchDataChanges(long lastDate, long etlDate, AuditReader reader, Class entityClass) {

        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.revisionProperty("timestamp").gt(lastDate))
                .add(AuditEntity.revisionProperty("timestamp").le(etlDate));
        List<Object[]> dataChanges = query.getResultList();
        return dataChanges;
    }

    /**
     * Utility method that returns a writer to a new sqlLoader data file named 'etlDateStr'_'baseFilename'.dat
     * @param etlDateStr etl run time, for filename
     * @param baseFilename for filename
     * @return Writer to the new file
     * @throws IOException
     */
    private Writer fileWriter(String etlDateStr, String baseFilename) throws IOException {
        File dwFile = new File (DATAFILE_DIR, etlDateStr + "_" + baseFilename + ".dat");
        return new BufferedWriter(new FileWriter(dwFile));
    }

    /**
     * Utility method that returns a new DW record containing the common start fields.
     * @param etlDateStr first field
     * @param isDelete second field
     * @return  StringBuilder that ends with a record delimiter
     */
    private StringBuilder startRecord(String etlDateStr, boolean isDelete) {
        return new StringBuilder()
                .append(etlDateStr).append(DELIM)
                .append(isDelete ? "T":"F").append(DELIM);
    }

    /**
     * Reads the timestamp file from the last incremental ETL run.
     * @return the msec timestamp
     */
    private long readLastTimestamp() {
        BufferedReader rdr = null;
        try {
            File file = new File (DATAFILE_DIR, LAST_TIMESTAMP_FILE);
            rdr = new BufferedReader(new FileReader(file));
            String s = rdr.readLine();
            return Long.parseLong(s);
        } catch (FileNotFoundException e) {
            logger.error("Missing file: " + LAST_TIMESTAMP_FILE);
            return 0L;
        } catch (IOException e) {
            logger.error("Error processing file " + LAST_TIMESTAMP_FILE, e);
            return 0L;
        } catch (NumberFormatException e) {
            logger.error("Cannot parse mSec timestamp in" + LAST_TIMESTAMP_FILE, e);
            return 0L;
        } finally {
            try {
                if (rdr != null) {
                    rdr.close();
                }
            } catch (IOException e) {
                logger.error("Cannot close file: " + LAST_TIMESTAMP_FILE, e);
            }
        }
    }

    /**
     * Writes the file used by this class in the next incremental ETL run, to know when the last run was.
     * @param etlDate mSec date of the etl run to record
     */
    private void writeLastTimestampFile(long etlDate) {
        try {
            File file = new File (DATAFILE_DIR, LAST_TIMESTAMP_FILE);
            FileWriter fw = new FileWriter(file);
            fw.write(String.valueOf(etlDate));
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + LAST_TIMESTAMP_FILE);
        }
    }

    /**
     * Writes the file used by cron job to know when this etl run has completed and data file processing can start.
     * @param etlDateStr used to name the file
     */
    private void writeIsReadyFile(String etlDateStr) {
        try {
            File file = new File (DATAFILE_DIR, etlDateStr + READY_FILE_SUFFIX);
            FileWriter fw = new FileWriter(file);
            fw.write("is_ready");
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + etlDateStr + READY_FILE_SUFFIX, e);
        }
    }

}

