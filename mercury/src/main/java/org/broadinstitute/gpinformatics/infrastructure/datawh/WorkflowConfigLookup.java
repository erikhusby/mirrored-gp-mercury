package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateful
public class WorkflowConfigLookup implements Serializable {
    private static Log logger = LogFactory.getLog(WorkflowConfigLookup.class);

    private WorkflowConfig workflowConfig;
    private Map<String, List<WorkflowConfigDenorm>> mapEventToWorkflows = null;
    private static final int CONFIG_ID_CACHE_SIZE = 64;
    private final LRUMap configIdCache = new LRUMap(CONFIG_ID_CACHE_SIZE);
    int cacheHit = 0; //instrumentation variable for testing

    /** Overrides that allow etl to capture events not found in WorkflowConfig (e.g. BSP events). */
    private static final Collection<WorkflowConfigDenorm> SYNTHETIC_WORKFLOW_CONFIGS = new ArrayList<>();

    /** Overrides that allow etl to accept events without a batch name. */
    private static final Collection<String> ACCEPT_WITHOUT_BATCH_NAME = new ArrayList<String>(){{
        add(LabEventType.PICO_PLATING_BUCKET.getName());
    }};

    private static Date FIRST_EFFECTIVE_WORKFLOW_DATE;

    private static final boolean PDO_NOT_NEEDED = false;
    private static final boolean BATCH_NOT_NEEDED = false;

    static {
        // This should move to WorkflowConfigDao if it ever exists.
        try {
            // Set to Nov 1, 2012, which was the date when Mercury first went online.
            FIRST_EFFECTIVE_WORKFLOW_DATE = ExtractTransform.parseTimestamp("20121101000000");
        } catch (ParseException e) {
            logger.error("Cannot create syntheticWorkflowConfigs.");
        }
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLE_RECEIPT.getName(), LabEventType.SAMPLE_RECEIPT.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.COLLABORATOR_TRANSFER.getName(), LabEventType.COLLABORATOR_TRANSFER.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName(),
                LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.AUTO_DAUGHTER_PLATE_CREATION.getName(),
                LabEventType.AUTO_DAUGHTER_PLATE_CREATION.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLES_EXTRACTION_START.getName(), LabEventType.SAMPLES_EXTRACTION_START.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLE_IMPORT.getName(), LabEventType.SAMPLE_IMPORT.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLE_PACKAGE.getName(), LabEventType.SAMPLE_PACKAGE.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "Activity", "0", "Activity", "0",
                LabEventType.ACTIVITY_BEGIN.getName(), LabEventType.ACTIVITY_BEGIN.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "Activity", "0", "Activity", "0",
                LabEventType.ACTIVITY_END.getName(), LabEventType.ACTIVITY_END.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
        SYNTHETIC_WORKFLOW_CONFIGS.add(new WorkflowConfigDenorm(FIRST_EFFECTIVE_WORKFLOW_DATE, "InstrumentQC", "0", "InstrumentQC", "0",
                LabEventType.INSTRUMENT_QC.getName(), LabEventType.INSTRUMENT_QC.getName(),
                PDO_NOT_NEEDED, BATCH_NOT_NEEDED, false));
    }


    /**
     * Builds 1:N mapping of event name to denorm workflow configs that contain that event name.
     */
    public void initWorkflowConfigDenorm() {
        mapEventToWorkflows = new HashMap<>();
        Collection<WorkflowConfigDenorm> configs = getDenormConfigs();
        for (WorkflowConfigDenorm config : configs) {
            List<WorkflowConfigDenorm> workflows = mapEventToWorkflows.get(config.getWorkflowStepEventName());
            if (workflows == null) {
                workflows = new ArrayList<>();
                mapEventToWorkflows.put(config.getWorkflowStepEventName(), workflows);
            }
            workflows.add(config);
        }

        // Sorts each of the workflow lists by descending effective date for easier lookup by date.
        for (List<WorkflowConfigDenorm> workflowList : mapEventToWorkflows.values()) {
            Collections.sort(workflowList, new Comparator<WorkflowConfigDenorm>() {
                @Override
                public int compare(WorkflowConfigDenorm o1, WorkflowConfigDenorm o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }
    }

    /**
     * Returns the id of the relevant WorkflowConfig denormalized record.
     *
     * @return null if no id found
     */
    public WorkflowConfigDenorm lookupWorkflowConfig(String eventName, String workflowName, Date eventDate) {

        // Checks for a cache hit, which may be a null.
        String cacheKey = eventName + workflowName + (eventDate != null ? eventDate.toString() : "null");

        synchronized (configIdCache) {
            if (configIdCache.containsKey(cacheKey)) {
                ++cacheHit;
                return (WorkflowConfigDenorm) configIdCache.get(cacheKey);
            }
        }

        WorkflowConfigDenorm match = null;
        List<WorkflowConfigDenorm> denormConfigs = null;

        if (eventName != null) {
            denormConfigs = mapEventToWorkflows.get(eventName);
        }

        if (denormConfigs != null) {
            // Iterates on the sorted list of workflow configs to find a match having latest effective date.
            for (WorkflowConfigDenorm denorm : denormConfigs) {
                if (workflowName != null && workflowName.equals(denorm.getProductWorkflowName())
                        && eventDate != null && eventDate.after(denorm.getEffectiveDate())
                        || denormConfigs.size() == 1) {
                    match = denorm;
                    break;
                }
            }
        }
        if (match == null) {
            logger.debug("No WorkflowConfig records apply to event " + eventName + " in workflow " + workflowName +
                    " on date " + eventDate.toString());
        }

        synchronized (configIdCache) {
            configIdCache.put(cacheKey, match);
        }
        return match;
    }

    /** Returns the workflowConfigDenorms obtained from WorkflowConfig plus the synthetic ones needed for etl. */
    public Collection<WorkflowConfigDenorm> getDenormConfigs() {
        Collection<WorkflowConfigDenorm> denormConfigs = WorkflowConfigDenorm.parse(workflowConfig);
        denormConfigs.addAll(SYNTHETIC_WORKFLOW_CONFIGS);
        return denormConfigs;
    }

    public static boolean isSynthetic(String workflowName){
        for( WorkflowConfigDenorm workflowConfigDenorm : SYNTHETIC_WORKFLOW_CONFIGS  ){
            if( workflowConfigDenorm.getProductWorkflowName() == workflowName ) {
                return true;
            }
        }
        return false;

    }

    /** Returns true if event can be ETL'd without a batch name. */
    public static boolean needsBatch(String workflowStepEventName) {
        return !ACCEPT_WITHOUT_BATCH_NAME.contains(workflowStepEventName);
    }

    @Inject
    public void setWorkflowConfig(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
        initWorkflowConfigDenorm();
    }

}
