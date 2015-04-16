package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.datawh.LabEventEtl.EventFactDto;
import org.broadinstitute.gpinformatics.infrastructure.datawh.SequencingSampleFactEtl.SequencingRunDto;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
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
 * <p/>
 * Concurrency management is handled through a semaphore, and this is why this is not defined as a @Singleton
 * and using @ConcurrencyManagement(ConcurrencyManagementType.BEAN).  CDI does not have concurrency management
 * and that is why it's handled manually.
 */

@RequestScoped
public class ExtractTransform implements Serializable {
    private static final long serialVersionUID = 20130517L;
    /**
     * Record delimiter expected in sqlLoader file.
     */
    public static final String DELIMITER = ",";

    /**
     * This filename matches what cron job expects.
     */
    public static final String READY_FILE_SUFFIX = "_is_ready";

    /**
     * This date format matches what cron job expects in filenames, and in SqlLoader data files.
     */
    private static final String TIMESTAMP_FORMAT_STRING = "yyyyMMddHHmmss";

    /**
     * Format a timestamp for output.
     */
    public static final Format TIMESTAMP_FORMATTER = FastDateFormat.getInstance(TIMESTAMP_FORMAT_STRING);

    public static String formatTimestamp(Date timestamp) {
        return TIMESTAMP_FORMATTER.format(timestamp);
    }

    /**
     * Parse a timestamp from a string.
     */
    public static Date parseTimestamp(String text) throws ParseException {
        return new SimpleDateFormat(TIMESTAMP_FORMAT_STRING).parse(text);
    }

    /**
     * Name of file that contains the mSec time of the last etl run.
     */
    public static final String LAST_ETL_FILE = "last_etl_run";

    /**
     * Name of the file that contains the hash of the last exported workflow config data.
     */
    public static final String LAST_WF_CONFIG_HASH_FILE = "last_wf_config_hash";

    /**
     * Name of subdirectory under configured ETL root dir where new sqlLoader files are put.
     */
    public static final String DATAFILE_SUBDIR = File.separator + "new";

    /**
     * Name of directory where sqlLoader files are put.
     */
    private static String datafileDir;

    static final long MSEC_IN_SEC = 1000L;
    static final long SEC_IN_MIN = 60L;
    static final int ETL_BATCH_SIZE = 500;

    /**
     * Number of digits in the number representing seconds since start of epoch.
     */
    private static final int TIMESTAMP_SECONDS_SIZE = 10;

    private static final Log log = LogFactory.getLog(ExtractTransform.class);

    /**
     * Handle concurrency issues with the semaphore.
     */
    private static final Semaphore mutex = new Semaphore(1);

    private static long incrementalRunStartTime = System.currentTimeMillis();  // only useful for logging
    private static boolean loggedConfigError = false;
    private static EtlConfig etlConfig = null;
    private final Collection<GenericEntityEtl> etlInstances = new HashSet<>();

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private Deployment deployment;

    @Inject
    private SessionContextUtility sessionContextUtility;

    // A separate instance from one used by incremental etl.
    @Inject
    private LabEventEtl labEventEtlAnalysis;

    // A separate instance from one used by incremental etl.
    @Inject
    SequencingSampleFactEtl sequencingSampleFactEtlAnalysis;

    public ExtractTransform() {
    }

    @Inject
    public ExtractTransform(
            LabEventEtl labEventEtl,
            LabVesselEtl labVesselEtl,
            PriceItemEtl priceItemEtl,
            ProductEtl productEtl,
            ProductOrderAddOnEtl productOrderAddOnEtl,
            ProductOrderEtl productOrderEtl,
            RegulatoryInfoEtl regulatoryInfoEtl,
            PDORegulatoryInfoEtl pdoRegulatoryInfoEtl,
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
            SequencingSampleFactEtl sequencingSampleFactEtl,
            BillingSessionEtl billingSessionEtl,
            LabMetricEtl labMetricEtl
    ) {
        etlInstances.add(labEventEtl);
        etlInstances.add(labVesselEtl);
        etlInstances.add(priceItemEtl);
        etlInstances.add(productEtl);
        etlInstances.add(productOrderAddOnEtl);
        etlInstances.add(productOrderEtl);
        etlInstances.add(regulatoryInfoEtl);
        etlInstances.add(pdoRegulatoryInfoEtl);
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
        etlInstances.add(billingSessionEtl);
        etlInstances.add(labMetricEtl);
    }

    /**
     * Constructor for testing.
     */
    public ExtractTransform(AuditReaderDao auditReaderDao, SessionContextUtility sessionContextUtility,
                            Collection<GenericEntityEtl> etlInstances) {
        this.auditReaderDao = auditReaderDao;
        this.sessionContextUtility = sessionContextUtility;
        this.etlInstances.addAll(etlInstances);
    }

    /**
     * "0" is used as a sentinel value for start/end times,
     *
     * @see ExtractTransform#incrementalEtl
     */
    private static final String ZERO = "0";

    private static boolean isZero(String value) {
        return ZERO.equals(value);
    }

    /**
     * Extracts data from operational database, transforms the data into data warehouse records,
     * and writes the records to files, one per DW table.
     *
     * @param requestedStart start of interval of audited changes, in yyyyMMddHHmmss format,
     *                       or "0" to use previous end time.
     * @param requestedEnd   end of interval of audited changes, in yyyyMMddHHmmss format, or "0" for now.
     *                       Excludes endpoint.  "0" will cause updating of the lastEtlRun file.
     * @return count of records created, or -1 if could not run
     */
    public int incrementalEtl(String requestedStart, String requestedEnd) {

        // Bails if target directory is not defined, is missing or cannot read it.
        String dataDir = getDatafileDir();
        if (StringUtils.isBlank(dataDir)) {
            if (!loggedConfigError) {
                log.warn("ETL data file directory is not configured. ETL will not be run.");
                loggedConfigError = true;
            }
            return -1;
        }
        if (!(new File(dataDir)).exists()) {
            if (!loggedConfigError) {
                log.error("ETL data file directory is missing: " + dataDir);
                loggedConfigError = true;
            }
            return -1;
        } else if (!(new File(dataDir)).canRead()) {
            if (!loggedConfigError) {
                log.error("Cannot read from the ETL data file directory: " + dataDir);
                loggedConfigError = true;
            }
            return -1;
        }

        // The same etl_date is used for all DW data processed by one ETL run.
        // Forces start and end to be whole second boundaries, which the auditReaderDao needs.
        try {
            long startTimeSec;
            if (isZero(requestedStart)) {
                startTimeSec = readLastEtlRun();
                if (startTimeSec == 0L) {
                    log.warn("Cannot start ETL because the run time of the last incremental ETL can't be read");
                    return -1;
                }
            } else {
                startTimeSec = parseTimestamp(requestedStart).getTime() / MSEC_IN_SEC;
            }

            long endTimeSec;
            if (isZero(requestedEnd)) {
                endTimeSec = System.currentTimeMillis() / MSEC_IN_SEC;
            } else {
                endTimeSec = parseTimestamp(requestedEnd).getTime() / MSEC_IN_SEC;
            }

            if (startTimeSec < endTimeSec) {
                ImmutablePair<Integer, String> countAndDate = incrementalEtl(startTimeSec, endTimeSec);
                if (countAndDate == null) {
                    return -1;
                }

                // Updates the lastEtlRun file with the actual end of etl, but only when doing etl ending now,
                // which is the case for timer-driven incremental etl.
                if (isZero(requestedEnd)) {
                    writeLastEtlRun(parseTimestamp(countAndDate.right).getTime() / MSEC_IN_SEC);
                }

                if (countAndDate.left > 0) {
                    writeIsReadyFile(countAndDate.right);
                    log.debug("Incremental ETL created " + countAndDate.left + " data records in " +
                            minutesSince(incrementalRunStartTime) + " minutes");
                }

                return countAndDate.left;
            }

        } catch (ParseException e) {
            log.error("Cannot parse start time '" + requestedStart + "' or end time '" + requestedEnd + "'");
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
                log.info("Skipping new ETL run since previous run is still busy after " + minutes + " minutes");
            }
            return null;
        }
        
        log.trace("Starting incremental ETL");
        final List<Integer> count = new ArrayList<>(1);
        final List<String> date = new ArrayList<>(1);

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
                        log.debug("ETL run will only process " + revsAndDate.left.size() + " of the "
                                  + revs.size() + " changes");
                    } else {
                        log.debug("Incremental ETL found " + revs.size() + " changes");
                    }

                    String actualEtlDateStr = formatTimestamp(new Date(revsAndDate.right * MSEC_IN_SEC));

                    int recordCount = 0;
                    if (!revsAndDate.left.isEmpty()) {
                        // The order of ETL is not significant since import tables have no referential integrity.
                        for (GenericEntityEtl<?, ?> etlInstance : etlInstances) {
                            recordCount += etlInstance.doEtl(revsAndDate.left.keySet(), actualEtlDateStr);
                        }
                    }
                    count.add(0, recordCount);
                    date.add(0, actualEtlDateStr);
                }
            });

        } catch (Exception e) {
            log.error("Error during ETL: ", e);
        } finally {
            mutex.release();
        }

        return (!count.isEmpty() && !date.isEmpty()) ? new ImmutablePair<>(count.get(0), date.get(0)) : null;
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
        return new ImmutablePair<>(revs, batchEndSec);
    }


    /**
     * Does ETL for instances of a single entity class having ids in the specified range.
     * <p/>
     * Backfill ETL can run independently of periodic incremental ETL, and doesn't affect its state.
     * The generated standard sqlLoader data file should be picked up and processed normally by the
     * periodic cron job.
     *
     * @param entityClassname The fully qualified classname of the Mercury class to ETL.
     * @param startId         First entity id of a range of ids to backfill.  Optional query param that defaults to min id.
     * @param endId           Last entity id of a range of ids to backfill.  Optional query param that defaults to max id.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response backfillEtl(String entityClassname, final long startId, long endId) {
        String dataDir = getDatafileDir();
        if (StringUtils.isBlank(dataDir)) {
            return createErrorResponse("ETL data file directory is not configured. Backfill ETL will not be run");
        } else if (!(new File(dataDir)).exists()) {
            return createErrorResponse("ETL data file directory is missing: " + dataDir);
        } else if (!(new File(dataDir)).canRead()) {
            return createErrorResponse("Cannot read the ETL data file directory: " + dataDir);
        } else if (!(new File(dataDir)).canWrite()) {
            return createErrorResponse("Cannot write to the ETL data file directory: " + dataDir);
        }

        Class entityClass;
        try {
            entityClass = Class.forName(entityClassname);
        } catch (ClassNotFoundException e) {
            return createInfoResponse("Unknown class " + entityClassname, Response.Status.NOT_FOUND);
        }

        if (endId == -1) {
            endId = Long.MAX_VALUE;
        }

        if (startId < 0 || endId < startId) {
            return createInfoResponse("Invalid entity id range " + startId + " to " + endId, Response.Status.BAD_REQUEST);
        }

        log.debug("Starting ETL backfill of " + entityClass.getName() + " having ids " + startId + " to " + endId);
        long backfillStartTime = System.currentTimeMillis();

        final long finalEndId = endId;
        final Class<?> finalEntityClass = entityClass;
        final List<Integer> count = new ArrayList<>(1);
        final List<String> date = new ArrayList<>(1);

        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                try {
                    int recordCount = 0;
                    // The one of these that matches the entityClass will make ETL records, others are no-ops.
                    String etlDateStr = formatTimestamp(new Date());

                    for (GenericEntityEtl<?, ?> etlInstance : etlInstances) {
                        recordCount += etlInstance.doEtl(finalEntityClass, startId, finalEndId, etlDateStr);
                    }
                    count.add(recordCount);
                    date.add(etlDateStr);
                } catch (Exception e) {
                    log.error("Error during ETL: ", e);
                }
            }
        });
        String msg = null;
        if (!count.isEmpty() && !date.isEmpty()) {
            int recordCount = count.get(0);
            String etlDateStr = date.get(0);

            if (recordCount > 0) {
                writeIsReadyFile(etlDateStr);
            }
            msg = "Created " + recordCount + " " +  " data records in " +
                  (int) ((System.currentTimeMillis() - backfillStartTime) / MSEC_IN_SEC) + " seconds";
        } else {
            msg = "Backfill ETL created no data records";
        }
        return createInfoResponse(msg, Response.Status.OK);
    }

    /**
     * Reads the last incremental ETL run file and returns start of this etl interval.
     * Returns seconds if file contains seconds or milliseconds.
     *
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
        log.warn("Invalid timestamp in " + LAST_ETL_FILE);
        return 0L;

    }

    /**
     * Writes the file used by this class in the next incremental ETL run, to know where the last run finished.
     *
     * @param end indicator of end of this etl
     * @return true if file was written ok
     */
    public static boolean writeLastEtlRun(long end) {
        return writeEtlFile(LAST_ETL_FILE, String.valueOf(end));
    }

    /**
     * Writes the file used by cron job to know when this etl run has completed and data file processing can start.
     *
     * @param etlDateStr used to name the file
     */
    public static void writeIsReadyFile(String etlDateStr) {
        writeEtlFile(etlDateStr + READY_FILE_SUFFIX, " ");
    }

    /**
     * Utility method that reads the file's contents.
     *
     * @param filename is the leaf name of the file, expected to be in the etl data directory
     * @return content as one string
     */
    public static String readEtlFile(String filename) {
        Reader rdr = null;
        try {
            rdr = new FileReader(new File(getDatafileDir(), filename));
            return IOUtils.toString(rdr);
        } catch (FileNotFoundException e) {
            log.warn("Missing file: " + filename);
            return "";
        } catch (IOException e) {
            log.error("Error processing file " + filename, e);
            return "";
        } finally {
            IOUtils.closeQuietly(rdr);
        }
    }

    /**
     * Utility method that writes the file's contents.
     *
     * @param filename is the leaf name of the file, expected to be in the etl data directory
     * @param content  overwrites what is in the file
     * @return true if file was written ok
     */
    public static boolean writeEtlFile(String filename, String content) {
        Writer fw = null;
        try {
            fw = new FileWriter(new File(getDatafileDir(), filename), false);
            IOUtils.write(content, fw);
            return true;
        } catch (IOException e) {
            log.error("Error writing the ETL file " + LAST_ETL_FILE);
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

    public Collection<EventFactDto> analyzeEvent(long labEventId) {
        return labEventEtlAnalysis.makeEventFacts(labEventId);
    }

    public Collection<SequencingRunDto> analyzeSequencingRun(long sequencingRunId) {
        return sequencingSampleFactEtlAnalysis.makeSequencingRunDtos(sequencingRunId);
    }

    /**
     * Gets relevant configuration from the .yaml file
     */
    void initConfig() {
        if (etlConfig == null) {
            etlConfig = (EtlConfig) MercuryConfiguration.getInstance().getConfig(EtlConfig.class, deployment);
        }

        if (datafileDir == null) {
            datafileDir = etlConfig.getDatawhEtlDirRoot() + DATAFILE_SUBDIR;

            // Data Warehouse requires a datafile directory.
            log.info("The ETL Data Warehouse is" + (!StringUtils.isBlank(datafileDir) ? "" : " not") + " running");
        }
    }

    /**
     * Returns the whole number of minutes since the given mSec timestamp, rounded up.
     */
    private int minutesSince(long msecTimestamp) {
        return (int) Math.ceil((System.currentTimeMillis() - msecTimestamp) / MSEC_IN_SEC / SEC_IN_MIN);
    }

    private Response createErrorResponse(String msg) {
        return createErrorResponse(msg, Response.Status.INTERNAL_SERVER_ERROR);
    }
    private Response createErrorResponse(String msg, Response.Status status) {
        log.warn(msg);
        return Response.status(status).entity(msg).build();
    }
    private Response createInfoResponse(String msg, Response.Status status) {
        log.debug(msg);
        return Response.status(status).entity(msg).build();
    }


}
