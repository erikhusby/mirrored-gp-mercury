package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * This entity etl class is a bit different than others, because WorkflowConfig is not being persisted
 * by Mercury app.  Some of the implications are:
 *
 *   - WorkflowConfig does not have a primary key, so ETL needs to add one.  The id must be generated
 *   deterministically from the relevant content of the workflow config record, so that old DW db
 *   references are preserved after a re-export of workflow config.
 *
 *   - ETL only creates new records for WorkflowConfig deltas that have a new version in the xml file.
 *   A change to the config without a new version value is treated as a correction that overwrites the
 *   corresponding DW record, which is probably correct behavior.
 *
 *   - Because not every change is versioned, and also because WorkflowConfig records may be deleted by
 *   a developer, if ETL ever has to re-export all events and workflow configs there may be different
 *   workflow configs associated with historical event records.  Worst case, an event cannot be mapped
 *   to any workflow anymore.
 */
@Stateless
public class WorkflowConfigEtl extends GenericEntityEtl {

    private WorkflowLoader workflowLoader;

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
	this.workflowLoader = workflowLoader;
    }

    /** Name of the file that contains the hash of the last exported workflow config data. */
    public static final String LAST_WF_CONFIG_HASH_FILE = "last_wf_config_hash";

    /**
     * @{inheritDoc}
     */
    @Override
    Class getEntityClass() {
        return WorkflowConfig.class;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    String getBaseFilename() {
        return "workflow_config";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    Long entityId(Object entity) {
        return ((WorkflowConfigDenorm)entity).getWorkflowConfigDenormId();
    }

    /** Calculates an aggregate hash of workflow config steps. */
    private long hash(Collection<WorkflowConfigDenorm> denorms) {
        long h = 1125899906842597L; // prime
        for (WorkflowConfigDenorm denorm : denorms) {
            // Reuses the existing hash of this record, which is its id.
            h = 31*h + denorm.getWorkflowConfigDenormId();
        }
        return h;
    }

    /**
     * Does an etl of workflow config if the current version has changed since last etl.
     * Keeps a hash of the config contents in a file to know when there was a change.
     * @param lastRev ignored
     * @param etlRev ignored
     * @param etlDateStr used in the sqlloader filename
     * @return number of records exported
     */
    @Override
    public int doEtl(long lastRev, long etlRev, String etlDateStr) {

        // Gets the flattened version of workflow config, calculates its hash, compares to the
        // previously exported one's hash, and if different, exports the new version.
        Collection<WorkflowConfigDenorm> flatConfig = WorkflowConfigDenorm.parse(workflowLoader.load());
        long currentHashValue = hash(flatConfig);
        long previousHashValue = readWorkflowConfigHash();
        if (currentHashValue == previousHashValue) {
            return 0;
        }

        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        // Exports all flattened records and updates the hash.
        exportWorkflowConfigSteps(flatConfig, dataFile, etlDateStr);
        writeWorkflowConfigHash(currentHashValue);

        return dataFile.getRecordCount();
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
        // Creates the wrapped Writer to the sqlLoader data file.
        String filename = dataFilename(etlDateStr, getBaseFilename());
        DataFile dataFile = new DataFile(filename);

        // Exports all flattened records and updates the hash.
        Collection<WorkflowConfigDenorm> flatConfig = WorkflowConfigDenorm.parse(workflowLoader.load());
        exportWorkflowConfigSteps(flatConfig, dataFile, etlDateStr);
        writeWorkflowConfigHash(hash(flatConfig));

        return dataFile.getRecordCount();
    }

    /**
     * Unused method since we've overridden the caller.
     */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return Collections.EMPTY_LIST;
    }

    /**
     * Unused method since we've overridden the caller.
     */
    @Override
    Collection<String> entityRecordsInRange(final long startId, final long endId, String etlDateStr, boolean isDelete) {
        return Collections.EMPTY_LIST;
    }

    /** Writes the denormalized config records to a sqlLoader file */
    void exportWorkflowConfigSteps(Collection<WorkflowConfigDenorm>flatConfig, DataFile dataFile, String etlDateStr) {
        try {
            for (WorkflowConfigDenorm wcstep : flatConfig) {
                String record = entityRecord(etlDateStr, false, wcstep);
                dataFile.write(record);
            }
        } catch (IOException e) {
            logger.error("Error while writing file " + dataFile.getFilename(), e);
        } finally {
            dataFile.close();
        }

    }

    /**
     * Makes a data record from an entity, in a format that matches the corresponding SqlLoader control file.
     * @param entity Mercury Entity
     * @return delimited SqlLoader record
     */
    String entityRecord(String etlDateStr, boolean isDelete, WorkflowConfigDenorm entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getWorkflowConfigDenormId(),
                entity.getEffectiveDate(),
                entity.getProductWorkflowName(),
                entity.getProductWorkflowVersion(),
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
    long readWorkflowConfigHash() {
        try {
            return Long.parseLong(ExtractTransform.readEtlFile(LAST_WF_CONFIG_HASH_FILE));
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse " + LAST_WF_CONFIG_HASH_FILE + " : " + e);
            return 0L;
        }
    }

    /** Writes the magic number representing the last known workflow config, for versioning purposes. */
    void writeWorkflowConfigHash(long number) {
        ExtractTransform.writeEtlFile(LAST_WF_CONFIG_HASH_FILE, String.valueOf(number));
    }

}
