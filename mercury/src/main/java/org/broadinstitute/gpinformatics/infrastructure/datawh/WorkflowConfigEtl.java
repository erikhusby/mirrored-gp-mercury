package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

/**
 * This entity etl class draws Workflow info from WorkflowConfig.
 * WorkflowConfig does not have a primary key, so ETL adds one from the hash of fields,
 * and because of this, ETL cannot update existing records, only add new records.
 */
@Stateful
public class WorkflowConfigEtl extends GenericEntityEtl {
    private Log logger = LogFactory.getLog(getClass());
    private WorkflowLoader workflowLoader;
    static final String WORKFLOW_BASE_FILENAME = "workflow";
    static final String PROCESS_BASE_FILENAME = "workflow_process";

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
    }

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return WorkflowConfig.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return null;
    }

    /**
     * This is unused, and it is meaningless for derived class WorkflowConfigDenorm.
     */
    @Override
    Long entityId(Object entity) {
        return null;
    }

    /**
     * Does an etl of workflow config if the current version has changed since last etl.
     * Keeps a hash of the config contents in a file to know when there was a change.
     * @param revIds ignored
     * @param etlDateStr used in the sqlloader filename
     * @return number of records exported
     */
    @Override
    public int doEtl(Collection<Long> revIds, String etlDateStr) {
        // Does nothing if no change in WorkflowConfig, indicated by the hash.
        HashMatchResult res = hashesMatch(workflowLoader);
        if (res.isMatch) {
            return 0;
        }

        int count = doEtlFiles(res.denormConfig, etlDateStr);
        writeWorkflowConfigHash(res.hashValue);

        return count;
    }

    /**
     * Does an unconditional etl of workflow config.
     * @param entityClass this class
     * @param startId ignored
     * @param endId ignored
     * @param etlDateStr used in the sqlloader filenam
     * @return number of records exported
     */
    @Override
    public int doBackfillEtl(Class entityClass, long startId, long endId, String etlDateStr) {
        // No-op unless the implementing class is the requested entity class.
        if (!getEntityClass().equals(entityClass) || !isEntityEtl()) {
            return 0;
        }
        Collection<WorkflowConfigDenorm> denormConfig = WorkflowConfigDenorm.parse(workflowLoader.load());
        return doEtlFiles(denormConfig, etlDateStr);
    }

    /** Creates the wrapped Writer to the sqlLoader data files and exports flattened records. */
    private int doEtlFiles(Collection<WorkflowConfigDenorm> denorms, String etlDateStr) {
        int count = 0;

        String filename = dataFilename(etlDateStr, WORKFLOW_BASE_FILENAME);
        DataFile dataFile = null;
        try {
            dataFile = new DataFile(filename);
            exportWorkflow(denorms, dataFile, etlDateStr);
            count += dataFile.getRecordCount();
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }

        try {
            filename = dataFilename(etlDateStr, PROCESS_BASE_FILENAME);
            dataFile = new DataFile(filename);
            exportProcess(denorms, dataFile, etlDateStr);
            count += dataFile.getRecordCount();
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }

        return count;
    }

    /**
     * Unused method since we've overridden the caller.
     */
    @Override
    Collection<String> entityRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return Collections.EMPTY_LIST;
    }

    /**
     * Unused method since we've overridden the caller.
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        return Collections.EMPTY_LIST;
    }

    /** Writes the Workflow records to a sqlLoader file */
    private void exportWorkflow(Collection<WorkflowConfigDenorm>flatConfig, DataFile dataFile, String etlDateStr)
            throws IOException {
        Set<Long> ids = new HashSet<Long>();
        for (WorkflowConfigDenorm denorm : flatConfig) {
            // Deduplicates the workflow records.
            if (!ids.contains(denorm.getWorkflowId())) {
                ids.add(denorm.getWorkflowId());
                String record = workflowRecord(etlDateStr, false, denorm);
                dataFile.write(record);
            }
        }
    }
    /** Writes the Process records to a sqlLoader file */
    private void exportProcess(Collection<WorkflowConfigDenorm>flatConfig, DataFile dataFile, String etlDateStr)
            throws IOException {
        Set<Long> ids = new HashSet<Long>();
        for (WorkflowConfigDenorm denorm : flatConfig) {
            // Deduplicates the process records.
            if (!ids.contains(denorm.getProcessId())) {
                ids.add(denorm.getProcessId());
                String record = processRecord(etlDateStr, false, denorm);
                dataFile.write(record);
            }
        }
    }

    /**
     * Makes a Process ETL data record from a WorkflowConfigDenorm, in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr etl date string
     * @param isDelete deletion indicator
     * @param entity denormalized WorkflowConfig
     * @return delimited SqlLoader record
     */
    private String workflowRecord(String etlDateStr, boolean isDelete, WorkflowConfigDenorm entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getWorkflowId(),
                entity.getProductWorkflowName(),
                entity.getProductWorkflowVersion()
        );
    }

    /**
     * Makes a Process ETL data record from a WorkflowConfigDenorm, in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr etl date string
     * @param isDelete deletion indicator
     * @param entity denormalized WorkflowConfig
     * @return  delimited SqlLoader record
     */
    private String processRecord(String etlDateStr, boolean isDelete, WorkflowConfigDenorm entity) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getProcessId(),
                    entity.getWorkflowProcessName(),
                    entity.getWorkflowProcessVersion(),
                    entity.getWorkflowStepName(),
                    entity.getWorkflowStepEventName()
            );
    }

    /** This entity does not make status records. */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object entity, boolean isDelete) {
        return null;
    }

    /** This entity does support add/modify records via primary key. */
    @Override
    boolean isEntityEtl() {
        return true;
    }



    /** Reads the persisted magic number representing the last known workflow config, for versioning purposes. */
    private long readWorkflowConfigHash() {
        try {
            return Long.parseLong(ExtractTransform.readEtlFile(ExtractTransform.LAST_WF_CONFIG_HASH_FILE));
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse " + ExtractTransform.LAST_WF_CONFIG_HASH_FILE + " : " + e);
            return 0L;
        }
    }

    /** Writes the magic number representing the last known workflow config, for versioning purposes. */
    private void writeWorkflowConfigHash(long number) {
        ExtractTransform.writeEtlFile(ExtractTransform.LAST_WF_CONFIG_HASH_FILE, String.valueOf(number));
    }

    /**
     * Gets the flattened version of workflow config, calculates its hash, compares to the
     * previously exported one's hash.
     * @return HashMatchResult
     */
    private HashMatchResult hashesMatch(WorkflowLoader workflowLoader) {
        Collection<WorkflowConfigDenorm> flatConfig = WorkflowConfigDenorm.parse(workflowLoader.load());
        long currentHashValue = hash(flatConfig);
        long previousHashValue = readWorkflowConfigHash();
        return new HashMatchResult((currentHashValue == previousHashValue), currentHashValue, flatConfig);
    }

    private class HashMatchResult {
        boolean isMatch;
        long hashValue;
        Collection<WorkflowConfigDenorm> denormConfig;

        HashMatchResult(boolean match, long hashValue, Collection<WorkflowConfigDenorm> denormConfig) {
            isMatch = match;
            this.hashValue = hashValue;
            this.denormConfig = denormConfig;
        }
    }
}
