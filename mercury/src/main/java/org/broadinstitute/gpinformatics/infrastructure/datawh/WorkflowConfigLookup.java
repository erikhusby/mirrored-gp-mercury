package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

@Stateful
public class WorkflowConfigLookup implements Serializable {
    private static Log logger = LogFactory.getLog(WorkflowConfigLookup.class);
    private WorkflowLoader workflowLoader;
    private Map<String, List<WorkflowConfigDenorm>> mapEventToWorkflows = null;
    private static final int CONFIG_ID_CACHE_SIZE = 4;
    private final LRUMap configIdCache = new LRUMap(CONFIG_ID_CACHE_SIZE);
    int cacheHit = 0; //instrumentation variable for testing

    // The synthetic workflowConfig records needed to etl BSP events.
    private static Collection<WorkflowConfigDenorm> syntheticWorkflowConfigs = new ArrayList<>();
    private static Date NOV_1_2012;

    static {
        // This should move to WorkflowConfigDao if it ever exists.
        try {
            NOV_1_2012 = ExtractTransform.secTimestampFormat.parse("20121101000000");
        } catch (ParseException e) {
            logger.error("Cannot create syntheticWorkflowConfigs.");
        }
        syntheticWorkflowConfigs.add(new WorkflowConfigDenorm(NOV_1_2012, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLE_RECEIPT.getName(), LabEventType.SAMPLE_RECEIPT.getName(), false));
        syntheticWorkflowConfigs.add(new WorkflowConfigDenorm(NOV_1_2012, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName(),
                LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName()));
        syntheticWorkflowConfigs.add(new WorkflowConfigDenorm(NOV_1_2012, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLES_EXTRACTION_START.getName(), LabEventType.SAMPLES_EXTRACTION_START.getName()));
        syntheticWorkflowConfigs.add(new WorkflowConfigDenorm(NOV_1_2012, "BSP", "0", "BSP", "0",
                LabEventType.SAMPLE_IMPORT.getName(), LabEventType.SAMPLE_IMPORT.getName()));
    }


    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
        initWorkflowConfigDenorm();
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
                workflows = new ArrayList<WorkflowConfigDenorm>();
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
        Collection<WorkflowConfigDenorm> denormConfigs = WorkflowConfigDenorm.parse(workflowLoader.load());
        denormConfigs.addAll(syntheticWorkflowConfigs);
        return denormConfigs;
    }
}