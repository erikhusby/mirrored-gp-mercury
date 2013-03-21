package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * Performs the extract and transform parts of ETL for the data warehouse.
 *
 * For incremental etl, Envers AuditReader is used to get relevant data from the _AUD tables which contain
 * changed data and a revision number.  ETL only wants the latest (current) version of an entity,
 * but for status history, it's necessary to iterate over the relevant range of revisions (all
 * changes since the last ETL run), and extract the status from the entity and obtain the status
 * date from the corresponding Envers rev info.
 *
 * Entity data is then converted to sqlLoader records.  Record format is defined in the
 * sqlLoader control files (located in mercury/src/main/db/datawh/control), one for each type of
 * data file created by ETL.
 *
 * For backfill etl, the entities are obtained from the EntityManager, regardless of their audit history.
 */

@Stateful
@Path("etl")
public class ExtractTransform {
    /** Record delimiter expected in sqlLoader file. */
    public static final String DELIM = ",";
    /** This filename matches what cron job expects. */
    public static final String READY_FILE_SUFFIX = "_is_ready";
    /** This date format matches what cron job expects in filenames, and in SqlLoader data files. */
    public static final SimpleDateFormat secTimestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    /** Name of file that contains the mSec time of the last etl run. */
    public static final String LAST_ETL_FILE = "last_etl_run";
    /** Name of the file that contains the hash of the last exported workflow config data. */
    public static final String LAST_WF_CONFIG_HASH_FILE = "last_wf_config_hash";
    /** Name of subdirectory under configured ETL root dir where new sqlLoader files are put. */
    public static final String DATAFILE_SUBDIR = "/new";
    /** Name of directory where sqlLoader files are put. */
    private static String datafileDir;

    private static final long MSEC_IN_SEC = 1000L;
    private static final long SEC_IN_MIN = 60L;
    // Number of digits in the number representing seconds since start of epoch.
    private final int TIMESTAMP_SECONDS_SIZE = 10;
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private static final Semaphore mutex = new Semaphore(1);
    private static long incrementalRunStartTime = System.currentTimeMillis();  // only useful for logging
    private static boolean loggedConfigError = false;
    private EtlConfig etlConfig = null;

    @Inject
    private AuditReaderDao auditReaderDao;
    @Inject
    private Deployment deployment;
    @Inject
    private EventEtl eventEtl;
    @Inject
    private LabBatchEtl labBatchEtl;
    @Inject
    private LabVesselEtl labVesselEtl;
    @Inject
    private PriceItemEtl priceItemEtl;
    @Inject
    private ProductEtl productEtl;
    @Inject
    private ProductOrderAddOnEtl productOrderAddOnEtl;
    @Inject
    private ProductOrderEtl productOrderEtl;
    @Inject
    private ProductOrderSampleEtl productOrderSampleEtl;
    @Inject
    private ProductOrderSampleStatusEtl productOrderSampleStatusEtl;
    @Inject
    private ProductOrderStatusEtl productOrderStatusEtl;
    @Inject
    private ProjectPersonEtl projectPersonEtl;
    @Inject
    private ResearchProjectCohortEtl researchProjectCohortEtl;
    @Inject
    private ResearchProjectEtl researchProjectEtl;
    @Inject
    private ResearchProjectFundingEtl researchProjectFundingEtl;
    @Inject
    private ResearchProjectIrbEtl researchProjectIrbEtl;
    @Inject
    private ResearchProjectStatusEtl researchProjectStatusEtl;
    @Inject
    private WorkflowConfigEtl workflowConfigEtl;
    @Inject
    private RiskItemEtl riskItemEtl;
    @Inject
    private LedgerEntryEtl ledgerEntryEtl;

    public ExtractTransform() {
    }

    public ExtractTransform(AuditReaderDao auditReaderDao,
                            EventEtl eventEtl,
                            LabBatchEtl labBatchEtl,
                            LabVesselEtl labVesselEtl,
                            PriceItemEtl priceItemEtl,
                            ProductEtl productEtl,
                            ProductOrderAddOnEtl productOrderAddOnEtl,
                            ProductOrderEtl productOrderEtl,
                            ProductOrderSampleEtl productOrderSampleEtl,
                            ProductOrderSampleStatusEtl productOrderSampleStatusEtl,
                            ProductOrderStatusEtl productOrderStatusEtl,
                            ProjectPersonEtl projectPersonEtl,
                            ResearchProjectCohortEtl researchProjectCohortEtl,
                            ResearchProjectEtl researchProjectEtl,
                            ResearchProjectFundingEtl researchProjectFundingEtl,
                            ResearchProjectIrbEtl researchProjectIrbEtl,
                            ResearchProjectStatusEtl researchProjectStatusEtl,
                            WorkflowConfigEtl workflowConfigEtl,
                            RiskItemEtl riskItemEtl,
                            LedgerEntryEtl ledgerEntryEtl) {
        this.auditReaderDao = auditReaderDao;
        this.eventEtl = eventEtl;
        this.labBatchEtl = labBatchEtl;
        this.labVesselEtl = labVesselEtl;
        this.priceItemEtl = priceItemEtl;
        this.productEtl = productEtl;
        this.productOrderAddOnEtl = productOrderAddOnEtl;
        this.productOrderEtl = productOrderEtl;
        this.productOrderSampleEtl = productOrderSampleEtl;
        this.productOrderSampleStatusEtl = productOrderSampleStatusEtl;
        this.productOrderStatusEtl = productOrderStatusEtl;
        this.projectPersonEtl = projectPersonEtl;
        this.researchProjectCohortEtl = researchProjectCohortEtl;
        this.researchProjectEtl = researchProjectEtl;
        this.researchProjectFundingEtl = researchProjectFundingEtl;
        this.researchProjectIrbEtl = researchProjectIrbEtl;
        this.researchProjectStatusEtl = researchProjectStatusEtl;
        this.workflowConfigEtl = workflowConfigEtl;
        this.riskItemEtl = riskItemEtl;
        this.ledgerEntryEtl = ledgerEntryEtl;
    }

    /**
     * Runs on-demand ETL on one entity class, to force a refresh the Data Warehouse, for example after
     * adding a new field to be exported.
     *
     * @param entityClassname The fully qualified classname of the Mercury class to ETL.
     * @param startId First entity id of a range of ids to backfill.  Optional query param that defaults to min id.
     * @param endId Last entity id of a range of ids to backfill.  Optional query param that defaults to max id.
     */
    @Path("backfill/{entityClassname}")
    @PUT
    public Response onDemandEtl(@PathParam("entityClassname") String entityClassname,
                                @DefaultValue("0") @QueryParam("startId") long startId,
                                @DefaultValue("-1") @QueryParam("endId") long endId) {

        initConfig();
        Response.Status status = backfillEtl(entityClassname, startId, endId);
        return Response.status(status).build();
    }

    /**
     * Runs a normal, incremental ETL to avoid waiting up to 15 minutes when testing.
     */
    @Path("incremental")
    @PUT
    public Response onDemandIncrementalEtl() {
        initConfig();
        incrementalEtl();
        return Response.status(ClientResponse.Status.ACCEPTED).build();
    }

    /**
     * Extracts data from operational database, transforms the data to data warehouse records,
     * and writes the records to files, one per DW table.
     * @return record count, or -1 if could not run
     */
    public int incrementalEtl() {

        // If previous run is still busy it is unusual but not an error.  Only one incrementalEtl
        // may run at a time.  Does not queue a new job if busy, to avoid snowball effect if system is
        // busy for a long time, for whatever reason.
        if (!mutex.tryAcquire()) {
            int minutes = minutesSince(incrementalRunStartTime);
            if (minutes > 0) {
                logger.info("Skipping new ETL run since previous run is still busy after " + minutes + " minutes.");
            }
            return -1;
        }
        try {
            // Bails if target directory is missing.
            String dataDir = getDatafileDir();
            if (null == dataDir || dataDir.length() == 0) {
                if (!loggedConfigError) {
                    logger.info("ETL data file directory is not configured. ETL will not be run.");
                    loggedConfigError = true;
                }
                return -1;
            } else if (!(new File(dataDir)).exists()) {
                if (!loggedConfigError) {
                    logger.error("ETL data file directory is missing: " + dataDir);
                    loggedConfigError = true;
                }
                return -1;
            }

            // The same etl_date is used for all DW data processed by one ETL run.
            final long currentEtlSec = System.currentTimeMillis() / MSEC_IN_SEC;
            final String etlDateStr = secTimestampFormat.format(new Date(currentEtlSec * MSEC_IN_SEC));
            incrementalRunStartTime = System.currentTimeMillis();

            final long lastEtlSec = readLastEtlRun();
            // Allows last timestamp to be in the future thereby shutting off incremental etl.
            if (lastEtlSec >= currentEtlSec) {
                return 0;
            }
            return incrementalEtl(lastEtlSec, currentEtlSec, etlDateStr);
        } finally {
            mutex.release();
        }
    }

    /**
     * Testable helper method that does the incremental etl work, has no mutex.
     * @param startTimeSec start of ETL interval, in seconds
     * @param endTimeSec end of ETL interval, in seconds
     * @param etlDateStr y-m-d formatted etl time, for filenames
     * @return number of etl records
     */
    public int incrementalEtl(long startTimeSec, long endTimeSec, String etlDateStr) {
        if (0L == startTimeSec) {
            logger.warn("Cannot determine time of last incremental ETL.  ETL will not be run.");
            return 0;
        }
        // Gets the audit revision ids for the given interval.
        Collection<Long> revIds = auditReaderDao.fetchAuditIds(startTimeSec, endTimeSec);
        logger.debug ("Incremental ETL found " + revIds.size() + " changes.");
        if (revIds.size() == 0) {
            writeLastEtlRun(endTimeSec);
            return 0;
        }

        int recordCount = 0;
        // The order of ETL is not significant since import tables have no referential integrity.
        recordCount += productEtl.doEtl(revIds, etlDateStr);
        recordCount += priceItemEtl.doEtl(revIds, etlDateStr);
        recordCount += researchProjectEtl.doEtl(revIds, etlDateStr);
        recordCount += researchProjectStatusEtl.doEtl(revIds, etlDateStr);
        recordCount += projectPersonEtl.doEtl(revIds, etlDateStr);
        recordCount += researchProjectIrbEtl.doEtl(revIds, etlDateStr);
        recordCount += researchProjectFundingEtl.doEtl(revIds, etlDateStr);
        recordCount += researchProjectCohortEtl.doEtl(revIds, etlDateStr);
        recordCount += productOrderSampleEtl.doEtl(revIds, etlDateStr);
        recordCount += productOrderSampleStatusEtl.doEtl(revIds, etlDateStr);
        recordCount += productOrderEtl.doEtl(revIds, etlDateStr);
        recordCount += productOrderStatusEtl.doEtl(revIds, etlDateStr);
        recordCount += productOrderAddOnEtl.doEtl(revIds, etlDateStr);
        // event datamart
        recordCount += labBatchEtl.doEtl(revIds, etlDateStr);
        recordCount += labVesselEtl.doEtl(revIds, etlDateStr);
        recordCount += workflowConfigEtl.doEtl(revIds, etlDateStr);
        recordCount += eventEtl.doEtl(revIds, etlDateStr);

        recordCount += riskItemEtl.doEtl(revIds, etlDateStr);
        recordCount += ledgerEntryEtl.doEtl(revIds, etlDateStr);

        writeLastEtlRun(endTimeSec);
        if (recordCount > 0) {
            writeIsReadyFile(etlDateStr);
            logger.debug("Incremental ETL created " + recordCount + " data records in " +
                    minutesSince(incrementalRunStartTime) + " seconds.");
        }
        return recordCount;
    }

    /**
     * Does ETL for instances of a single entity class having ids in the specified range.
     *
     * Backfill ETL can run independently of periodic incremental ETL, and doesn't affect its state.
     * The generated standard sqlLoader data file should be picked up and processed normally by the
     * periodic cron job.
     *
     * @param entityClassname The fully qualified classname of the Mercury class to ETL.
     * @param startId First entity id of a range of ids to backfill.  Optional query param that defaults to min id.
     * @param endId Last entity id of a range of ids to backfill.  Optional query param that defaults to max id.
     */
    public Response.Status backfillEtl(String entityClassname, long startId, long endId) {
        String dataDir = getDatafileDir();
        if (null == dataDir || dataDir.length() == 0) {
            logger.info("ETL data file directory is not configured. Backfill ETL will not be run.");
            return Response.Status.INTERNAL_SERVER_ERROR;
        } else if (!(new File(dataDir)).exists()) {
            logger.error("ETL data file directory is missing: " + dataDir);
            return Response.Status.INTERNAL_SERVER_ERROR;
        }

        final String etlDateStr = secTimestampFormat.format(new Date());

        Class entityClass;
        try {
            entityClass = Class.forName(entityClassname);
        } catch (ClassNotFoundException e) {
            logger.error("Unknown class " + entityClassname);
            return Response.Status.NOT_FOUND;
        }

        if (endId == -1) {
            endId = Long.MAX_VALUE;
        }
        if (startId < 0 ||endId < startId) {
            logger.error("Invalid entity id range " + startId + " to " + endId);
            return Response.Status.BAD_REQUEST;
        }

        logger.debug("ETL backfill of " + entityClass.getName() + " having ids " + startId + " to " + endId);
        long backfillStartTime = System.currentTimeMillis();

        int recordCount = 0;
        // The one of these that matches the entityClass will make ETL records, others are no-ops.
        recordCount += productEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += priceItemEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += researchProjectEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += researchProjectStatusEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += projectPersonEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += researchProjectIrbEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += researchProjectFundingEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += researchProjectCohortEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += productOrderSampleEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += productOrderSampleStatusEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += productOrderEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += productOrderStatusEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += productOrderAddOnEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        // event datamart
        recordCount += labBatchEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += labVesselEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += workflowConfigEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += eventEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);

        recordCount += riskItemEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);
        recordCount += ledgerEntryEtl.doBackfillEtl(entityClass, startId, endId, etlDateStr);

        if (recordCount > 0) {
            writeIsReadyFile(etlDateStr);
        }
        logger.info("Backfill ETL created " + recordCount + " " + entityClass.getSimpleName()
                + " data records in " + (int) ((System.currentTimeMillis() - backfillStartTime) / MSEC_IN_SEC) + " seconds.");

        return Response.Status.NO_CONTENT;
    }

    /**
     * Reads the last incremental ETL run file and returns start of this etl interval.
     * Returns seconds if file contains seconds or milliseconds.
     * @return the end time in seconds of last incremental ETL, or 0 if missing or unparsable.
     */
    public long readLastEtlRun() {
        try {
            // Extracts the first 10 digits.
            String content = readEtlFile(LAST_ETL_FILE);
            if (content.length() >= TIMESTAMP_SECONDS_SIZE) {
                return Long.parseLong(content.substring(0, TIMESTAMP_SECONDS_SIZE));
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        logger.warn("Invalid timestamp in " + LAST_ETL_FILE);
        return 0L;

    }

    /**
     * Writes the file used by this class in the next incremental ETL run, to know where the last run finished.
     * @param end indicator of end of this etl
     * @return true if file was written ok
     */
    public boolean writeLastEtlRun(long end) {
        return writeEtlFile(LAST_ETL_FILE, String.valueOf(end));
    }

    /**
     * Writes the file used by cron job to know when this etl run has completed and data file processing can start.
     * @param etlDateStr used to name the file
     */
    public void writeIsReadyFile(String etlDateStr) {
        writeEtlFile(etlDateStr + READY_FILE_SUFFIX, " ");
    }

    /**
     * Utility method that reads the file's contents.
     * @param filename is the leaf name of the file, expected to be in the etl data directory
     * @return content as one string
     */
    public static String readEtlFile(String filename) {
        String dirname = getDatafileDir();
        Reader rdr = null;
        try {
            rdr = new FileReader(new File (dirname, filename));
            return IOUtils.toString(rdr);
        } catch (FileNotFoundException e) {
            logger.warn("Missing file: " + filename);
            return "";
        } catch (IOException e) {
            logger.error("Error processing file " + filename, e);
            return "";
        } finally {
            IOUtils.closeQuietly(rdr);
        }
    }

    /**
     * Utility method that writes the file's contents.
     * @param filename is the leaf name of the file, expected to be in the etl data directory
     * @param content overwrites what is in the file
     * @return true if file was written ok
     */
    public static boolean writeEtlFile(String filename, String content) {
        String dirname = getDatafileDir();
        Writer fw = null;
        try {
            fw = new FileWriter(new File (dirname, filename), false);
            IOUtils.write(content, fw);
            return true;
        } catch (IOException e) {
            logger.error("Error writing file " + LAST_ETL_FILE);
            return false;
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

    public static String getDatafileDir() {
        return datafileDir;
    }

    public static void setDatafileDir(String s) {
        datafileDir = s;
    }

    public static Semaphore getMutex() {
        return mutex;
    }

    public static long getIncrementalRunStartTime() {
        return incrementalRunStartTime;
    }

    /** Gets relevant configuration from the .yaml file */
    private void initConfig() {
        if (null == etlConfig) {
            etlConfig = (EtlConfig) MercuryConfiguration.getInstance().getConfig(EtlConfig.class, deployment);
        }
        if (null == datafileDir) {
            setDatafileDir(etlConfig.getDatawhEtlDirRoot() + DATAFILE_SUBDIR);
        }
    }

    /** Returns the whole number of minutes since the given mSec timestamp, rounded up. */
    private int minutesSince(long msecTimestamp) {
        return (int)Math.ceil((System.currentTimeMillis() - msecTimestamp) / MSEC_IN_SEC / SEC_IN_MIN);
    }
}

