package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.BillableItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.*;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.Session;
import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

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
public class ExtractTransform {
    private static String DATAFILE_DIR = "/seq/lims/datawh/dev/new";
    private static String LAST_TIMESTAMP_FILE = "last_etl_timestamp";
    private static String READY_FILE_SUFFIX = "_is_ready";
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    /** Record delimiter expected in sqlLoader file. */
    private final String DELIM = ",";

    private void incrementalETL() {
        final long etlDate = System.currentTimeMillis();
        long lastDate = 0L;

        // Obtains the timestamp of the last ETL run
        try {
            File file = new File (DATAFILE_DIR, LAST_TIMESTAMP_FILE);
            BufferedReader rdr = new BufferedReader(new FileReader(file));
            String s = rdr.readLine();
            Long.parseLong(s);
            rdr.close();
        } catch (FileNotFoundException e) {
            logger.error("Missing file: " + LAST_TIMESTAMP_FILE);
            return;
        } catch (IOException e) {
            logger.error("Error processing file " + LAST_TIMESTAMP_FILE, e);
            return;
        } catch (NumberFormatException e) {
            logger.error("Cannot parse mSec timestamp in" + LAST_TIMESTAMP_FILE, e);
            return;
        }
        AuditReader auditReader = AuditReaderFactory.get(new GenericDao().getEntityManager());
        doBillableItem(lastDate, etlDate, auditReader, "billable_item");
        /*
        doProductOrderSample(lastDate, etlDate, auditReader, "product_order_sample");
        doProductOrderSampleStatus(lastDate, etlDate, auditReader, "product_order_sample_status");
        doProductOrder(lastDate, etlDate, auditReader, "product_order");
        doProductOrderStatus(lastDate, etlDate, auditReader, "product_order_status");
        doResearchProjectCohort(lastDate, etlDate, auditReader, "research_project_cohort");
        doResearchProjectFunding(lastDate, etlDate, auditReader, "research_project_funding");
        doResearchProjectIRB(lastDate, etlDate, auditReader, "research_project_irb");
        doProjectPerson(lastDate, etlDate, auditReader, "research_project_person");
        doResearchProject(lastDate, etlDate, auditReader, "research_project");
        doResearchProjectStatus(lastDate, etlDate, auditReader, "research_project_status");
        doPriceItem(lastDate, etlDate, auditReader, "price_item");
        doProduct(lastDate, etlDate, auditReader, "product");
        doProductAddOn(lastDate, etlDate, auditReader, "product_add_on");
        */
        writeLastTimestampFile(etlDate);
        writeIsReadyFile(etlDate);
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

    private Writer fileWriter(long etlDate, String baseFilename) throws IOException {
        File dwFile = new File (DATAFILE_DIR, String.valueOf(etlDate) + "_" + baseFilename + ".dat");
        return new BufferedWriter(new FileWriter(dwFile));
    }

    private void doBillableItem(long lastDate, long etlDate, AuditReader auditReader, String baseFilename) {
        String formattedDate = fullDateFormat.format(new Date(etlDate));
        List<Object[]> dataChanges = fetchDataChanges(lastDate, etlDate, auditReader, BillableItem.class);
        Set<Long> changedEntityIds = new HashSet<Long>();
        Set<Long> deletedEntityIds = new HashSet<Long>();
        try {
            Writer writer = fileWriter(etlDate, baseFilename);
            for (Object[] dataChange : dataChanges) {
                // Splits the result array.
                BillableItem entity = (BillableItem)dataChange[0];
                RevisionType revType = (RevisionType)dataChange[2];
                Long entityId = entity.getBillableItemId();

                // Writes a DW deletion record if entity was deleted.
                if (revType.equals(RevisionType.DEL)) {
                    String record = transform(entity, formattedDate, true);
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
                writer.write(transform(entity, formattedDate, false));
            }
        } catch (IOException e) {
            logger.error("Problem writing " + etlDate + "_" + baseFilename);
        }
    }

    /** Creates the DW record with common start fields, ending with trailing delimiter. */
    private StringBuilder startRecord(String etlDate, boolean isDelete) {
        return (new StringBuilder()).append(etlDate).append(DELIM).append(isDelete ? "T":"F").append(DELIM);
    }

    private String transform(BillableItem entity, String etlDate, boolean isDelete) {
        StringBuilder rec = startRecord(etlDate, isDelete);
        if (isDelete) {
            rec.append(entity.getBillableItemId());
        } else {
            rec.append(entity.getProductOrderSample().getProductOrderSampleId())
                    .append(DELIM)
                    .append(entity.getPriceItem().getPriceItemId())
                    .append(DELIM)
                    .append(entity.getCount());
        }
        return rec.toString();
    }


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

    private void writeIsReadyFile(long etlDate) {
        try {
            File file = new File (DATAFILE_DIR, String.valueOf(etlDate) + READY_FILE_SUFFIX);
            FileWriter fw = new FileWriter(file);
            fw.write("is_ready");
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + etlDate + READY_FILE_SUFFIX, e);
        }
    }

}

