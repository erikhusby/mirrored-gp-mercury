package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.datawh.LabEventEtl.EventFactDto;
import org.broadinstitute.gpinformatics.infrastructure.datawh.SequencingSampleFactEtl.SequencingRunDto;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import javax.annotation.Nonnull;
import javax.ejb.TransactionManagement;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.transaction.UserTransaction;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Semaphore;

import static javax.ejb.TransactionManagementType.BEAN;

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
@TransactionManagement(BEAN)
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

    /** This class uses Bean Managed Transactions in order to set a longer session timeout. */
    @Inject
    private UserTransaction utx;

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
            LabMetricEtl labMetricEtl,
            FctCreateEtl fctCreateEtl,
            FctLoadEtl fctLoadEtl,
            AbandonVesselEtl abandonVesselEtl,
            ArrayProcessFlowEtl arrayProcessFlowEtl,
            FixUpEtl fixUpEtl,
            LabEventMetadataEtl labEventMetadataEtl
    ) {
        etlInstances.add(labEventEtl);
        etlInstances.add(labVesselEtl);
        etlInstances.add(priceItemEtl);
        etlInstances.add(productEtl);
        etlInstances.add(productOrderAddOnEtl);
        etlInstances.add(productOrderEtl);
        etlInstances.add(regulatoryInfoEtl);
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
        etlInstances.add(labMetricEtl);
        etlInstances.add(fctCreateEtl);
        etlInstances.add(fctLoadEtl);
        etlInstances.add(abandonVesselEtl);
        etlInstances.add(arrayProcessFlowEtl);
        etlInstances.add(fixUpEtl);
        etlInstances.add(labEventMetadataEtl);
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
     * This time period (in seconds) should be well beyond the expected time that a committing
     * transaction may need in order to finish and become visible to AuditReader. See GPLIM-3590.
     */
    static final int TRANSACTION_COMPLETION_GUARDBAND = 10;

    /**
     * Extracts data from operational database, transforms the data into data warehouse records,
     * and writes the records to files, one per DW table.
     *
     * @param requestedStart start of interval of audited changes, in yyyyMMddHHmmss format,
     *                       or "0" to use previous end time.
     * @param requestedEnd   end of interval of audited changes, in yyyyMMddHHmmss format, or "0" for now
     *                       (minus the TRANSACTION_COMPLETION_GUARDBAND). "0" will cause updating of the
     *                       lastEtlRun file. Excludes transactions whose commit date is the end time.
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
                if (!loggedConfigError && startTimeSec == 0L) {
                    log.warn("Skipping periodic ETL because the file with the last ETL run time can't be read");
                    return -1;
                }
            } else {
                startTimeSec = parseTimestamp(requestedStart).getTime() / MSEC_IN_SEC;
            }

            long endTimeSec;
            if (isZero(requestedEnd)) {
                endTimeSec = (System.currentTimeMillis() / MSEC_IN_SEC) - TRANSACTION_COMPLETION_GUARDBAND;
            } else {
                endTimeSec = parseTimestamp(requestedEnd).getTime() / MSEC_IN_SEC;
            }

            if (startTimeSec < endTimeSec) {
                Triple<Integer, String, Exception> countDateException = incrementalEtl(startTimeSec, endTimeSec);
                if (countDateException != null && countDateException.getRight() != null) {
                    log.error(countDateException.getRight());
                    return -1;
                }
                if (countDateException == null || countDateException.getLeft() == null ||
                        countDateException.getMiddle() == null) {
                    return -1;
                }

                // Updates the lastEtlRun file with the actual end of etl, but only when doing etl ending now,
                // which is the case for timer-driven incremental etl.
                if (isZero(requestedEnd)) {
                    writeLastEtlRun(parseTimestamp(countDateException.getMiddle()).getTime() / MSEC_IN_SEC);
                }

                if (countDateException.getLeft() > 0) {
                    writeIsReadyFile(countDateException.getMiddle());
                    log.debug("Incremental ETL created " + countDateException.getLeft() + " data records in " +
                            minutesSince(incrementalRunStartTime) + " minutes");
                }

                return countDateException.getLeft();
            }

        } catch (ParseException e) {
            log.error("Cannot parse start time '" + requestedStart + "' or end time '" + requestedEnd + "'");
        }
        return -1;
    }

    // Returns the record count and the date of the actual end of etl interval for this run, which will
    // be on a whole second boundary.
    private @Nonnull Triple<Integer, String, Exception> incrementalEtl(final long startTimeSec, final long endTimeSec) {
        final MutableTriple<Integer, String, Exception> countDateException = MutableTriple.of(null, null, null);

        // If previous run is still busy it is unusual but not an error.  Only one incrementalEtl
        // may run at a time.  Does not queue a new job if busy, to avoid snowball effect if system is
        // busy for a long time, for whatever reason.
        if (!mutex.tryAcquire()) {
            int minutes = minutesSince(incrementalRunStartTime);
            if (minutes > 0) {
                log.info("Skipping new ETL run since previous run is still busy after " + minutes + " minutes");
            }
            return countDateException;
        }
        try {
            incrementalRunStartTime = System.currentTimeMillis();
            sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                @Override
                public void apply() {
                    try {
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
                                recordCount += etlInstance.doIncrementalEtl(revsAndDate.left.keySet(), actualEtlDateStr);
                            }
                        }
                        countDateException.setLeft(recordCount);
                        countDateException.setMiddle(actualEtlDateStr);

                    } catch (Exception e) {
                        countDateException.setRight(e);

                    } finally {
                        // Reset state of all Hibernate entities (the ETL process is read-only)
                        auditReaderDao.clear();
                    }
                }
            });
        } finally {
            mutex.release();
        }

        return countDateException;
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
    public Response backfillEtl(String entityClassname, final long startId, long endId) {
        Pair<Boolean,String> fileConfig = validateFileConfig();
        if( !fileConfig.getLeft() ) {
            return createErrorResponse(fileConfig.getRight());
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

        String msg = null;
        msg = "Starting ETL backfill of " + entityClass.getName() + " having ids " + startId + " to " + endId;
        log.debug(msg);
        long backfillStartTime = System.currentTimeMillis();

        final long finalEndId = endId;
        final Class<?> finalEntityClass = entityClass;
        final MutableTriple<Integer, String, Exception> countDateException = new MutableTriple<>(null, null, null);

        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                try {
                    int recordCount = 0;
                    // The one of these that matches the entityClass will make ETL records, others are no-ops.
                    String etlDateStr = formatTimestamp(new Date());

                    for (GenericEntityEtl<?, ?> etlInstance : etlInstances) {
                        recordCount += etlInstance.doBackfillEtl(finalEntityClass, startId, finalEndId, etlDateStr);
                    }
                    countDateException.setLeft(recordCount);
                    countDateException.setMiddle(etlDateStr);
                } catch (Exception e) {
                    countDateException.setRight(e);
                }
            }
        });
        if (countDateException.getRight() != null) {
            log.error(countDateException.getRight());
            return createErrorResponse(countDateException.getRight().getMessage());
        } else {
            if (countDateException.getLeft() != null && countDateException.getLeft() > 0) {
                writeIsReadyFile(countDateException.getMiddle());
            }
            msg += "\nCreated " + countDateException.getLeft() + " " +  " data records in " +
                    (int) ((System.currentTimeMillis() - backfillStartTime) / MSEC_IN_SEC) + " seconds\n";
            return createInfoResponse(msg, Response.Status.OK);
        }
    }

    /**
     * Does ETL for event_fact and sequencing_sample_fact for any downstream data related to a vessel <br/>
     * Backfill ETL can run independently of periodic incremental ETL, and doesn't affect its state.
     * The generated standard sqlLoader data file should be picked up and processed normally by the
     * periodic cron job.
     *
     * @param barcode    The vessel which needs all downstream data to be refreshed
     */
    public Response backfillEtlForVessel(final String barcode) {

        Pair<Boolean,String> fileConfig = validateFileConfig();
        if( !fileConfig.getLeft() ) {
            return createErrorResponse(fileConfig.getRight());
        }

        String msg = null;
        msg = "Starting ETL backfill of descendants of vessel " + barcode;
        log.debug(msg);
        long backfillStartTime = System.currentTimeMillis();

        final MutableTriple<Integer, String, Exception> countDateException = new MutableTriple<>();

        sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
            @Override
            public void apply() {
                try {

                    if (utx != null) {
                        utx.begin();
                        utx.setTransactionTimeout(GenericEntityEtl.TRANSACTION_TIMEOUT);
                    }

                    String etlDateStr = formatTimestamp(new Date());

                    LabVesselEtl labVesselEtl = (LabVesselEtl) getEtlInstance(LabVesselEtl.class.getCanonicalName());
                    LabEventEtl labEventEtl = (LabEventEtl) getEtlInstance(LabEventEtl.class.getCanonicalName());
                    ArrayProcessFlowEtl arrayEventEtl = (ArrayProcessFlowEtl) getEtlInstance(ArrayProcessFlowEtl.class.getCanonicalName());
                    SequencingSampleFactEtl seqRunEtl = (SequencingSampleFactEtl) getEtlInstance(
                            SequencingSampleFactEtl.class.getCanonicalName());

                    MutableTriple<Integer, String, Exception> etlCountDateException =
                            labVesselEtl.backfillEtlForVessel(barcode, etlDateStr, labEventEtl, arrayEventEtl, seqRunEtl );

                    countDateException.setLeft(etlCountDateException.getLeft());
                    countDateException.setMiddle(etlCountDateException.getMiddle());
                    countDateException.setRight(etlCountDateException.getRight());


                } catch (Exception e) {
                    countDateException.setRight(e);
                } finally {
                    if (utx != null) {
                        try {
                            utx.rollback();
                        } catch (Exception txException ) {
                            log.error("Rollback failed", txException );
                        }
                    }
                }
            }
        });

        if (countDateException.getRight() != null) {
            log.error(countDateException.getRight());
            return createErrorResponse(countDateException.getRight().getMessage());
        } else {
            if (countDateException.getLeft() != null && countDateException.getLeft() > 0) {
                writeIsReadyFile(countDateException.getMiddle());
            }
            msg += "\nCreated " + countDateException.getLeft() + " " +  " data records in " +
                   (int) ((System.currentTimeMillis() - backfillStartTime) / MSEC_IN_SEC) + " seconds\n";
            return createInfoResponse(msg, Response.Status.OK);
        }
    }

    /**
     * Shared functionality to validate ability to produce extract files
     * @return Pair of fail status and failure error message if FALSE
     */
    private Pair<Boolean,String> validateFileConfig(){
        String dataDir = getDatafileDir();
        if (StringUtils.isBlank(dataDir)) {
            return Pair.of( Boolean.FALSE, "ETL data file directory is not configured. Backfill ETL will not be run" );
        } else if (!(new File(dataDir)).exists()) {
            return Pair.of( Boolean.FALSE, "ETL data file directory is missing: " + dataDir );
        } else if (!(new File(dataDir)).canRead()) {
            return Pair.of( Boolean.FALSE, "Cannot read the ETL data file directory: " + dataDir );
        } else if (!(new File(dataDir)).canWrite()) {
            return Pair.of( Boolean.FALSE, "Cannot write to the ETL data file directory: " + dataDir );
        } else {
            return Pair.of( Boolean.TRUE, dataDir);
        }
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

    /**
     * Supports REST call to analyze a single event
     * @param labEventId The event ID to process
     * @return Denormalized lab event warehouse data
     */
    public Collection<EventFactDto> analyzeEvent(long labEventId) {
        List<EventFactDto> dtos = labEventEtlAnalysis.makeEventFacts(labEventId);
        // Prevent Hibernate recursive relationship stack overflow error at XA commit
        auditReaderDao.clear();
        return dtos;
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
        log.error(msg);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
    }
    private Response createInfoResponse(String msg, Response.Status status) {
        log.debug(msg);
        return Response.status(status).entity(msg).build();
    }

    /**
     * This is a test hook, these are all proxy classes,
     * e.g. org.broadinstitute.gpinformatics.infrastructure.datawh.ProductOrderAddOnEtl$Proxy$_$$_Weld$Proxy$
     */
    public List<String> getEtlInstanceNames() {
        List<String> list = new ArrayList<>();
        for (GenericEntityEtl genericEntityEtl : etlInstances) {
            list.add(genericEntityEtl.getClass().getCanonicalName());
        }
        return list;
    }

    public GenericEntityEtl getEtlInstance( String canonicalName ) {
        for (GenericEntityEtl genericEntityEtl : etlInstances) {
            // ETL instances are all weld proxy classes,
            // e.g. org.broadinstitute.gpinformatics.infrastructure.datawh.ProductOrderAddOnEtl$Proxy$_$$_Weld$Proxy$
            if( genericEntityEtl.getClass().getCanonicalName().startsWith( canonicalName ) ) {
                return genericEntityEtl;
            }
        }
        throw new IllegalArgumentException( "No etlInstance for [" + canonicalName + "] class name" );
    }
}
