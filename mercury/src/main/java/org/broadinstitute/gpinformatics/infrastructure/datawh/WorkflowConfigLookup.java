package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.io.Serializable;
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
    private Log logger = LogFactory.getLog(getClass());
    private WorkflowLoader workflowLoader;
    private Map<String, List<WorkflowConfigDenorm>> mapEventToWorkflows = null;
    private static final int CONFIG_ID_CACHE_SIZE = 4;
    private final LRUMap configIdCache = new LRUMap(CONFIG_ID_CACHE_SIZE);
    int cacheHit = 0; //instrumentation variable for testing

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
        initWorkflowConfigDenorm();
    }

    /**
     * Builds 1:N mapping of event name to denorm workflow configs that contain that event name.
     */
    public void initWorkflowConfigDenorm() {
        mapEventToWorkflows = new HashMap<String, List<WorkflowConfigDenorm>>();
        WorkflowConfig workflowConfig = workflowLoader.load();
        Collection<WorkflowConfigDenorm> configs = WorkflowConfigDenorm.parse(workflowConfig);
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
    public WorkflowConfigDenorm lookupWorkflowConfig(String eventName, LabBatch labBatch, Date eventDate) {

        // Checks for a cache hit, which may be a null.
        String cacheKey = eventName + labBatch.getWorkflowName() + eventDate.toString();
        synchronized (configIdCache) {
            if (configIdCache.containsKey(cacheKey)) {
                ++cacheHit;
                return (WorkflowConfigDenorm) configIdCache.get(cacheKey);
            }
        }

        String workflowName = labBatch.getWorkflowName();
        if (workflowName == null) {
            logger.debug("Product " + labBatch.getBusinessKey() + " has no workflow name");
            synchronized (configIdCache) {
                configIdCache.put(cacheKey, null);
            }
            return null;
        }

        List<WorkflowConfigDenorm> denormConfigs = mapEventToWorkflows.get(eventName);
        if (denormConfigs == null) {
            logger.debug("No WorkflowConfig records have event " + eventName);
            synchronized (configIdCache) {
                configIdCache.put(cacheKey, null);
            }
            return null;
        }

        // Iterates on the sorted list of workflow configs to find a match having latest effective date.
        for (WorkflowConfigDenorm denorm : denormConfigs) {
            if (workflowName.equals(denorm.getProductWorkflowName()) && eventDate.after(denorm.getEffectiveDate())) {
                synchronized (configIdCache) {
                    configIdCache.put(cacheKey, denorm);
                }
                return denorm;
            }
        }
        logger.debug("No denormalized workflow config for product " + workflowName + " having eventName " + eventName
                + " on date " + eventDate.toString());
        synchronized (configIdCache) {
            configIdCache.put(cacheKey, null);
        }
        return null;
    }
}