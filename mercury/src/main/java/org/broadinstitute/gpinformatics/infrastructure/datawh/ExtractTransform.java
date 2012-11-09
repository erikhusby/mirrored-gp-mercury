package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * This is a JEE scheduled bean that does the initial parts of ETL for the data warehouse.
 *
 * Envers AuditReader is used to get relevant data from the _AUD tables which contain changed
 * data and a revision number.  For most entities, ETL only wants the latest (current) version,
 * but for status history, it's necessary to iterate over the relevant range of revisions (all
 * changes since the last ETL run), and extract the status from the entity and obtain the status
 * date from the corresponding Envers rev info.
 *
 * Entity data is then converted to sqlLoader records.  Record format is defined in the
 * sqlLoader control files (located in mercury/src/main/db/datawh/control), one for each type of
 * data file created by ETL.
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
    /** This date format matches what cron job expects in filenames, and in SqlLoader data files. */
    public static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");


    private static final String LAST_ETL_FILE = "last_etl_run";
    private static final long MSEC_IN_MINUTE = 60 * 1000;
    private static final Logger logger = Logger.getLogger(ExtractTransform.class);
    private static final Semaphore mutex = new Semaphore(1);
    private static long currentRunStartTime = 0;  // only useful for logging
    /** Directory for sqlLoader data files. */
    private static String datafileDir = null;
    private static final String DATAFILE_SUBDIR = "/new";

    @Inject
    private Deployment deployment;

    @Inject
    private AuditReaderEtl auditReaderEtl;

    @Inject
    private BillableItemEtl billableItemEtl;
    @Inject
    private ProductEtl productEtl;
    @Inject
    private ProductOrderEtl productOrderEtl;
    @Inject
    private ProductOrderSampleEtl productOrderSampleEtl;
    @Inject
    private ProductOrderSampleStatusEtl productOrderSampleStatusEtl;
    @Inject
    private ProductOrderStatusEtl productOrderStatusEtl;
    @Inject
    private PriceItemEtl priceItemEtl;
    @Inject
    private ResearchProjectEtl researchProjectEtl;
    @Inject
    private ResearchProjectStatusEtl researchProjectStatusEtl;
    @Inject
    private ProjectPersonEtl projectPersonEtl;
    @Inject
    private ResearchProjectIrbEtl researchProjectIrbEtl;
    @Inject
    private ResearchProjectFundingEtl researchProjectFundingEtl;
    @Inject
    private ResearchProjectCohortEtl researchProjectCohortEtl;
    @Inject
    private ProductOrderAddOnEtl productOrderAddOnEtl;

    /** public getter */
    public static String getDatafileDir() {
        return datafileDir;
    }

    /**
     * JEE auto-schedules incremental ETL.
     */
    @Schedule(hour="*", minute="*", persistent=false)
    //@TransactionTimeout(value=24, unit=TimeUnit.HOURS)
    private void incrementalEtl() {

        // If previous run is still busy it is unusual but not an error.  Only one incrementalEtl
        // may run at a time.  Does not queue a new job if busy, to avoid snowball effect if system is
        // busy for a long time, for whatever reason.
        if (!mutex.tryAcquire()) {
            logger.info("Skipping new ETL run since previous run is still busy ("
                    + (int)Math.ceil((System.currentTimeMillis() - currentRunStartTime) / MSEC_IN_MINUTE) + " minutes).");
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
            final String etlDateStr = fullDateFormat.format(etlDate);
            currentRunStartTime = etlDate.getTime();

            final long lastRev = readLastEtlRun();
            final long etlRev = auditReaderEtl.currentRevNumber(etlDate);
            if (lastRev == etlRev) {
                logger.info("Incremental ETL found no changes since rev " + lastRev);
                return;
            }
            logger.info("Doing incremental ETL for rev numbers " + lastRev + " to " + etlRev);
            if (0L == lastRev) {
                logger.warn("Cannot determine time of last incremental ETL.  Doing a full ETL.");
            }

            int recordCount = 0;
            // The order of ETL is not significant since import tables have no referential integrity.
            recordCount += productEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += priceItemEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += researchProjectEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += researchProjectStatusEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += projectPersonEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += researchProjectIrbEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += researchProjectFundingEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += researchProjectCohortEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += billableItemEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += productOrderSampleEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += productOrderSampleStatusEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += productOrderEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += productOrderStatusEtl.doEtl(lastRev, etlRev, etlDateStr);
            recordCount += productOrderAddOnEtl.doEtl(lastRev, etlRev, etlDateStr);

            boolean lastEtlFileWritten = writeLastEtlRun(etlRev);
            if (recordCount > 0 && lastEtlFileWritten) {
                writeIsReadyFile(etlDateStr);
                logger.info("Incremental ETL created " + recordCount + " data records in "
                        + (int)Math.ceil((System.currentTimeMillis() - currentRunStartTime) / 1000.) + " seconds.");
            }

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
            logger.warn("Missing file: " + LAST_ETL_FILE);
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
     * @return true if file was written ok
     */
    private boolean writeLastEtlRun(long etlRev) {
        try {
            File file = new File (datafileDir, LAST_ETL_FILE);
            FileWriter fw = new FileWriter(file, false);
            fw.write(String.valueOf(etlRev));
            fw.close();
            return true;
        } catch (IOException e) {
            logger.error("Error writing file " + LAST_ETL_FILE);
            return false;
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

