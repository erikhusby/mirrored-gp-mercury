package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Performs the extract and transform parts of ETL for the data warehouse.
 * <p/>
 * For incremental etl, Envers AuditReader is used to get relevant data from the _AUD tables which contain
 * changed data and a revision number.  ETL only wants the latest (current) version of an entity,
 * but for status history, it's necessary to iterate over the relevant range of revisions (all
 * changes since the last ETL run), and extract the status from the entity and obtain the status
 * date from the corresponding Envers rev info.
 * <p/>
 * Entity data is then converted to sqlLoader records.  Record format is defined in the
 * sqlLoader control files (located in mercury/src/main/db/datawh/control), one for each type of
 * data file created by ETL.
 * <p/>
 * For backfill etl, the entities are obtained from the EntityManager, regardless of their audit history.
 */

@ApplicationScoped
public class ExtractTransform implements Serializable {
    private static final long serialVersionUID = 20130517L;
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

    static final long MSEC_IN_SEC = 1000L;
    static final long SEC_IN_MIN = 60L;
    static final int ETL_BATCH_SIZE = 500;

    // Number of digits in the number representing seconds since start of epoch.
    private static final int TIMESTAMP_SECONDS_SIZE = 10;
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private static final Semaphore mutex = new Semaphore(1);
    private static long incrementalRunStartTime = System.currentTimeMillis();  // only useful for logging
    private static boolean loggedConfigError = false;
    private static EtlConfig etlConfig = null;
    private final Collection<GenericEntityEtl> etlInstances = new HashSet<GenericEntityEtl>();

    @Inject
    private AuditReaderDao auditReaderDao;
    @Inject
    private Deployment deployment;
    @Inject
    private SessionContextUtility sessionContextUtility;

    public ExtractTransform() {
    }

    @Inject
    public ExtractTransform(
            LabEventEtl labEventEtl,
            LabBatchEtl labBatchEtl,
            LabVesselEtl labVesselEtl,
            PriceItemEtl priceItemEtl,
            ProductEtl productEtl,
            ProductOrderAddOnEtl productOrderAddOnEtl,
            ProductOrderEtl productOrderEtl,
            ProductOrderSampleEtl productOrderSampleEtl,
            ProjectPersonEtl projectPersonEtl,
            ResearchProjectCohortEtl researchProjectCohortEtl,
            ResearchProjectEtl researchProjectEtl,
            ResearchProjectFundingEtl researchProjectFundingEtl,
            ResearchProjectIrbEtl researchProjectIrbEtl,
            WorkflowConfigEtl workflowConfigEtl,
            RiskItemEtl riskItemEtl,
            LedgerEntryCrossEtl ledgerEntryCrossEtl,
            LedgerEntryEtl ledgerEntryEtl,
            SequencingRunEtl sequencingRunEtl,
            SequencingSampleFactEtl sequencingSampleFactEtl
    ) {

        etlInstances.add(labEventEtl);
        etlInstances.add(labBatchEtl);
        etlInstances.add(labVesselEtl);
        etlInstances.add(priceItemEtl);
        etlInstances.add(productEtl);
        etlInstances.add(productOrderAddOnEtl);
        etlInstances.add(productOrderEtl);
        etlInstances.add(productOrderSampleEtl);
        etlInstances.add(projectPersonEtl);
        etlInstances.add(researchProjectCohortEtl);
        etlInstances.add(researchProjectEtl);
        etlInstances.add(researchProjectFundingEtl);
        etlInstances.add(researchProjectIrbEtl);
        etlInstances.add(workflowConfigEtl);
        etlInstances.add(riskItemEtl);
        etlInstances.add(ledgerEntryCrossEtl);
        etlInstances.add(ledgerEntryEtl);
        etlInstances.add(sequencingRunEtl);
        etlInstances.add(sequencingSampleFactEtl);
    }

    /** Constructor for testing. */
    public ExtractTransform(AuditReaderDao auditReaderDao, SessionContextUtility sessionContextUtility,
                            Collection<GenericEntityEtl> etlInstances) {
        this.auditReaderDao = auditReaderDao;
        this.sessionContextUtility = sessionContextUtility;
        this.etlInstances.addAll(etlInstances);
    }

    /**
     * Extracts data from operational database, transforms the data into data warehouse records,
     * and writes the records to files, one per DW table.
     *
     * @param requestedStart start of interval of audited changes, in yyyyMMddHHmmss format,
     *                 or "0" to use previous end time.
     * @param requestedEnd end of interval of audited changes, in yyyyMMddHHmmss format, or "0" for now.
     *               Excludes endpoint.  "0" will cause updating of the lastEtlRun file.
     * @return count of records created, or -1 if could not run
     */
    public int incrementalEtl(String requestedStart, String requestedEnd) {
        final String ZERO = "0";

        // Bails if target directory is missing.
        String dataDir = getDatafileDir();
        if (null == dataDir || dataDir.length() == 0) {
            if (!loggedConfigError) {
                logger.warn("ETL data file directory is not configured. ETL will not be run.");
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
        // Forces start and end to be whole second boundaries, which the auditReaderDao needs.
        try {
            long startTimeSec;
            if (ZERO.equals(requestedStart)) {
                startTimeSec = readLastEtlRun();
                if (startTimeSec == 0L) {
                    logger.warn("Cannot determine time of last incremental ETL.  ETL will not be run.");
                    return -1;
                }
            } else {
                startTimeSec = secTimestampFormat.parse(requestedStart).getTime() / MSEC_IN_SEC;
            }

            long endTimeSec;
            if (ZERO.equals(requestedEnd)) {
                endTimeSec = System.currentTimeMillis() / MSEC_IN_SEC;
            } else {
                endTimeSec = secTimestampFormat.parse(requestedEnd).getTime() / MSEC_IN_SEC;
            }

            if (startTimeSec < endTimeSec) {
                ImmutablePair<Integer, String> countAndDate = incrementalEtl(startTimeSec, endTimeSec);
                if (countAndDate == null) {
                    return -1;
                }

                // Updates the lastEtlRun file with the actual end of etl, but only when doing etl ending now,
                // which is the case for timer-driven incremental etl.
                if (ZERO.equals(requestedEnd)) {
                    writeLastEtlRun(secTimestampFormat.parse(countAndDate.right).getTime() / MSEC_IN_SEC);
                }

                if (countAndDate.left > 0) {
                    writeIsReadyFile(countAndDate.right);
                    logger.debug("Incremental ETL created " + countAndDate.left + " data records in " +
                            minutesSince(incrementalRunStartTime) + " minutes.");
                }

                return countAndDate.left;
            }

        } catch (ParseException e) {
            logger.error("Cannot parse start time '" + requestedStart + "' or end time '" + requestedEnd + "'");
        }
        return -1;
    }

    // Returns the record count and the date of the actual end of etl interval for this run, which will
    // be on a whole second boundary.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private ImmutablePair<Integer, String> incrementalEtl(final long startTimeSec, final long endTimeSec) {
        // If previous run is still busy it is unusual but not an error.  Only one incrementalEtl
        // may run at a time.  Does not queue a new job if busy, to avoid snowball effect if system is
        // busy for a long time, for whatever reason.
        if (!mutex.tryAcquire()) {
            int minutes = minutesSince(incrementalRunStartTime);
            if (minutes > 0) {
                logger.info("Skipping new ETL run since previous run is still busy after " + minutes + " minutes.");
            }
            return null;
        }

        final List<Integer> count = new ArrayList<Integer>(1);
        final List<String> date = new ArrayList<String>(1);

        try {
            incrementalRunStartTime = System.currentTimeMillis();
            sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                @Override
                public void apply() {
                    // Gets the audit revisions for the given interval.
                    SortedMap<Long, Date> revs = auditReaderDao.fetchAuditIds(startTimeSec, endTimeSec);

                    // Limits batch size and sets end date accordingly.
                    ImmutablePair<SortedMap<Long, Date>, Long> revsAndDate = limitBatchSize(revs, endTimeSec);

                    if (revsAndDate.left.size() < revs.size()) {
                        logger.debug("ETL run will only process " + revsAndDate.left.size() + " of the "
                                + revs.size() + " changes.");
                    } else {
                        logger.debug("Incremental ETL found " + revs.size() + " changes.");
                    }

                    final String actualEtlDateStr = secTimestampFormat.format(
                            new Date(revsAndDate.right * MSEC_IN_SEC));

                    int recordCount = 0;
                    if (revsAndDate.left.size() > 0) {
                        // The order of ETL is not significant since import tables have no referential integrity.
                        for (GenericEntityEtl etlInstance : etlInstances) {
                            recordCount += etlInstance.doEtl(revsAndDate.left.keySet(), actualEtlDateStr);
                        }
                    }
                    count.add(0, recordCount);
                    date.add(0, actualEtlDateStr);

                }
            });

        } finally {
            mutex.release();
        }

        return (count.size() > 0 && date.size() > 0) ? new ImmutablePair(count.get(0), date.get(0)) : null;
    }

    // Limits batch size and adjusts end time accordingly.
    static ImmutablePair<SortedMap<Long, Date>, Long> limitBatchSize(SortedMap<Long, Date> revs, long endTimeSec) {
        long batchEndSec = endTimeSec;
        if (revs.size() > ETL_BATCH_SIZE) {
            Long itemBeyondEnd = null;
            int i = 0;
            // Counts off items until the size is reached, and then sets the new end date to be after the
            // last rev in the batch, on a whole second boundary.  This also ensures the etl interval is
            // at least one second, so that etl makes forward progress.  After the end time is set, all
            // the revs in the interval must be accepted, even if it exceeds the specified batch size, since
            // the interval will never be queried again.
            for (Map.Entry<Long, Date> rev : revs.entrySet()) {
                ++i;
                if (i == ETL_BATCH_SIZE) {
                    batchEndSec = (rev.getValue().getTime() / MSEC_IN_SEC) + 1;
                } else if (i > ETL_BATCH_SIZE && (rev.getValue().getTime() / MSEC_IN_SEC) >= batchEndSec) {
                    itemBeyondEnd = rev.getKey();
                    break;
                }
            }
            if (itemBeyondEnd == null) {
                // No revs were excluded, so use the original end time.
                batchEndSec = endTimeSec;
            } else {
                revs = revs.headMap(itemBeyondEnd);
            }
        }
        return new ImmutablePair<SortedMap<Long, Date>, Long>(revs, batchEndSec);
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
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response.Status backfillEtl(String entityClassname, final long startId, long endId) {
        String dataDir = getDatafileDir();
        if (null == dataDir || dataDir.length() == 0) {
            logger.info("ETL data file directory is not configured. Backfill ETL will not be run.");
            return Response.Status.INTERNAL_SERVER_ERROR;
        } else if (!(new File(dataDir)).exists()) {
            logger.error("ETL data file directory is missing: " + dataDir);
            return Response.Status.INTERNAL_SERVER_ERROR;
        }

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

        final long finalEndId = endId;
        final Class finalEntityClass = entityClass;
        final List<Integer> count = new ArrayList<Integer>(1);
        final List<String> date = new ArrayList<String>(1);

        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                int recordCount = 0;
                // The one of these that matches the entityClass will make ETL records, others are no-ops.
                String etlDateStr = secTimestampFormat.format(new Date());

                for (GenericEntityEtl etlInstance : etlInstances) {
                    recordCount += etlInstance.doEtl(finalEntityClass, startId, finalEndId, etlDateStr);
                }
                count.add(recordCount);
                date.add(etlDateStr);
            }
        });
        if (count.size() > 0 && date.size() > 0) {
            int recordCount = count.get(0);
            String etlDateStr = date.get(0);

            if (recordCount > 0) {
                writeIsReadyFile(etlDateStr);
            }
            logger.info("Backfill ETL created " + recordCount +
                    " data records in " + (int) ((System.currentTimeMillis() - backfillStartTime) / MSEC_IN_SEC) + " seconds.");
        }
        return Response.Status.NO_CONTENT;

    }

    /**
     * Reads the last incremental ETL run file and returns start of this etl interval.
     * Returns seconds if file contains seconds or milliseconds.
     * @return the end time in seconds of last incremental ETL, or 0 if missing or unparsable.
     */
    public static long readLastEtlRun() {
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
    public static boolean writeLastEtlRun(long end) {
        return writeEtlFile(LAST_ETL_FILE, String.valueOf(end));
    }

    /**
     * Writes the file used by cron job to know when this etl run has completed and data file processing can start.
     * @param etlDateStr used to name the file
     */
    public static void writeIsReadyFile(String etlDateStr) {
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
    void initConfig() {
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

