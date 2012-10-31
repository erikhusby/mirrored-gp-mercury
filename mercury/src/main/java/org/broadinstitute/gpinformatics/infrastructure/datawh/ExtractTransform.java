package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.envers.*;

import javax.ejb.Schedule;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is a JEE scheduled bean that does the initial parts of ETL for the data warehouse.
 *
 * Extract processing is done first, using Envers AuditReader to get relevant data from the
 * _AUD tables.  These tables contain changes to entities marked with a revision.  For most
 * entities ETL only wants the latest version.  However there are several historical status
 * tables that will need to iterate over the relevant range of revisions, typically all changes
 * since the last ETL run, and extract the status and status dates.
 *
 * Transform processing maps the entities to sqlLoader records.  Record format is defined in
 * the sqlLoader control files in mercury/src/main/db/datawh/control, one for each type of
 * data file created by Java.  There is no link or integrity check between sqlLoader and Java.
 *
 * Created with IntelliJ IDEA.
 * User: epolk
 * Date: 10/29/12
 */

@Singleton
public class ExtractTransform {
    static final String DATAFILE_DIR = "/seq/lims/datawh/dev/new";
    private static final String LAST_TIMESTAMP_FILE = "last_etl_timestamp";
    private static final String READY_FILE_SUFFIX = "_is_ready";
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    /** Record delimiter expected in sqlLoader file. */
    static final String DELIM = ",";

    @Inject
    private ProductOrder dao;
    @Inject
    private BillableItemEtl billableItemEtl;

    /**
     * Runs one pass of incremental ETL.
     */
    @Schedule(minute="*/2")
    private void incrementalETL() {
        // The same etl_date is used for all DW data processed by one ETL run.
        final long etlDate = System.currentTimeMillis();
        final String etlDateStr = fullDateFormat.format(new Date(etlDate));
        long lastDate = 0L;

        lastDate = readLastTimestamp();
        AuditReader auditReader = AuditReaderFactory.get(dao.getEntityManager());
        billableItemEtl.doETL(lastDate, etlDate, etlDateStr, auditReader);
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

