package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.*;
import java.util.Date;
import java.util.concurrent.Semaphore;

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

@Stateless
public class ExtractTransform {
    /** Record delimiter expected in sqlLoader file. */
    public static final String DELIM = ",";
    /** This filename matches what cron job expects. */
    public static final String READY_FILE_SUFFIX = "_is_ready";

    private static final String LAST_ETL_FILE = "last_etl_run";
    private static final long MSEC_IN_MINUTE = 60 * 1000;
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);
    private static final Semaphore mutex = new Semaphore(1);
    private static long currentRunStartTime = 0;  // only useful for logging
    /** Directory for sqlLoader data files. */
    private static String datafileDir = null;
    private static final String DATAFILE_SUBDIR = "/new";

    @Inject
    private BillableItemEtl billableItemEtl;
    @Inject
    private ProductEtl productEtl;
    @Inject
    private ProductOrderEtl productOrderEtl;

    @Inject
    private Deployment deployment;

    @Inject
    private Util util;

    public static String getDatafileDir() {
        return datafileDir;
    }

    /**
     * JEE auto-schedules incremental ETL.
     */
    @Schedule(hour="*", minute="*", persistent=false)
    private void incrementalEtl() {

        // If previous run is still busy it is unusual but not an error.  Only one incrementalEtl
        // may run at a time.  Does not queue if busy to avoid snowball effect if system is
        // busy for a long time, for whatever reason.
        if (!mutex.tryAcquire()) {
            logger.info("Previous incremental ETL is still running after "
                    + ((System.currentTimeMillis() - currentRunStartTime) / MSEC_IN_MINUTE) + " minutes.");
            return;
        }
        try {
            EtlConfig etlConfig = (EtlConfig) MercuryConfiguration.getInstance().getConfig(EtlConfig.class, deployment);
            datafileDir = etlConfig.getDatawhEtlDirRoot() + DATAFILE_SUBDIR;

            // Bails if target directory is missing.
            if (datafileDir == null || datafileDir.length() == 0 || !(new File(datafileDir)).exists()) {
                logger.fatal("ETL data file directory is missing: " + datafileDir);
                return;
            }

            // The same etl_date is used for all DW data processed by one ETL run.
            final Date etlDate = new Date();
            final String etlDateStr = util.fullDateFormat.format(etlDate);
            currentRunStartTime = etlDate.getTime();

            final long lastRev = readLastEtlRun();
            final long etlRev = util.currentRevNumber(etlDate);
            if (lastRev == etlRev) {
                logger.info("Incremental ETL found no changes since rev " + lastRev);
                return;
            }
            logger.info("Doing incremental ETL for rev numbers " + lastRev + " to " + etlRev);
            if (0L == lastRev) {
                logger.warn("Cannot determine time of last incremental ETL.  Doing a full ETL.");
            }

            int recordCount = 0;
            recordCount += billableItemEtl.doEtl(lastRev, etlRev, etlDateStr);

            /*
            doProductOrderSample(lastDate, etlDate, etlDateStr, auditReader, "product_order_sample");
            doProductOrderSampleStatus(lastDate, etlDate, etlDateStr, auditReader, "product_order_sample_status");
            */
            recordCount += productOrderEtl.doEtl(lastRev, etlRev, etlDateStr);
            /*
            doProductOrderStatus(lastDate, etlDate, etlDateStr, auditReader, "product_order_status");
            doResearchProjectCohort(lastDate, etlDate, etlDateStr, auditReader, "research_project_cohort");
            doResearchProjectFunding(lastDate, etlDate, etlDateStr, auditReader, "research_project_funding");
            doResearchProjectIRB(lastDate, etlDate, etlDateStr, auditReader, "research_project_irb");
            doProjectPerson(lastDate, etlDate, etlDateStr, auditReader, "research_project_person");
            doResearchProject(lastDate, etlDate, etlDateStr, auditReader, "research_project");
            doResearchProjectStatus(lastDate, etlDate, etlDateStr, auditReader, "research_project_status");
            doPriceItem(lastDate, etlDate, etlDateStr, auditReader, "price_item");
            */
            recordCount += productEtl.doEtl(lastRev, etlRev, etlDateStr);
            /*
            doProductAddOn(lastDate, etlDate, etlDateStr, auditReader, "product_order_add_on");
            */
            writeLastEtlRun(etlRev);
            writeIsReadyFile(etlDateStr);
            logger.info("Incremental ETL created " + recordCount + " data records in "
                    + Math.ceil((System.currentTimeMillis() - currentRunStartTime) / 1000.) + " seconds.");

        } finally {
            mutex.release();
        }
    }

    /**
     * Reads the last incremental ETL run file and returns start of this etl interval.
     * @return the end rev of last incremental ETL
     */
    private long readLastEtlRun() {
        BufferedReader rdr = null;
        try {
            File file = new File (datafileDir, LAST_ETL_FILE);
            rdr = new BufferedReader(new FileReader(file));
            String s = rdr.readLine();
            return Long.parseLong(s);
        } catch (FileNotFoundException e) {
            logger.error("Missing file: " + LAST_ETL_FILE);
            return 0L;
        } catch (IOException e) {
            logger.error("Error processing file " + LAST_ETL_FILE, e);
            return 0L;
        } catch (NumberFormatException e) {
            logger.error("Cannot parse " + LAST_ETL_FILE, e);
            return 0L;
        } finally {
            try {
                if (rdr != null) {
                    rdr.close();
                }
            } catch (IOException e) {
                logger.error("Cannot close file: " + LAST_ETL_FILE, e);
            }
        }
    }

    /**
     * Writes the file used by this class in the next incremental ETL run, to know where the last run finished.
     * @param etlRev last rev of the etl run to record
     */
    private void writeLastEtlRun(long etlRev) {
        try {
            File file = new File (datafileDir, LAST_ETL_FILE);
            FileWriter fw = new FileWriter(file, false);
            fw.write(String.valueOf(etlRev));
            fw.close();
        } catch (IOException e) {
            logger.error("Error writing file " + LAST_ETL_FILE);
        }
    }

    /**
     * Writes the file used by cron job to know when this etl run has completed and data file processing can start.
     * @param etlDateStr used to name the file
     */
    private void writeIsReadyFile(String etlDateStr) {
        try {
            File file = new File (datafileDir, etlDateStr + READY_FILE_SUFFIX);
            FileWriter fw = new FileWriter(file, false);
            fw.write(" ");
            fw.close();
        } catch (IOException e) {
            logger.error("Error creating file " + etlDateStr + READY_FILE_SUFFIX, e);
        }
    }

}

