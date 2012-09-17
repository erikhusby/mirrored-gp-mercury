package org.broadinstitute.sequel.entity.workflow;


import org.broadinstitute.sequel.control.workflow.WorkflowParser;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves what {@link WorkflowDescription}s might be active
 * for a given event name.
 */
public class WorkflowResolver {

    public static final String TEST_WORKFLOW_1 = "TestWorkflow1.bpmn";

    public static final String TEST_WORKFLOW_2 = "TestWorkflow2.bpmn";

    public static final String[] ALL_WORKFLOWS = new String[] {TEST_WORKFLOW_1,TEST_WORKFLOW_2};

    /**
     * Which {@link WorkflowDescription}s mention #eventName?
     * @param eventName
     * @return
     */
    public Set<WorkflowDescription> getActiveWorkflows(String eventName) {
        if (eventName == null) {
            throw new RuntimeException("eventName cannot be null");
        }
        final Set<WorkflowDescription> workflows = new HashSet<WorkflowDescription>();
        for (String workflowDefinition : ALL_WORKFLOWS) {
            WorkflowParser workflowParser = new WorkflowParser(Thread.currentThread().getContextClassLoader().getResourceAsStream(workflowDefinition));

            for (String eventNameInWorkflow : workflowParser.getMapNameToTransitionList().keySet()) {
                if (eventName.equalsIgnoreCase(eventNameInWorkflow)) {
                    workflows.add(new WorkflowDescription(workflowParser.getWorkflowName(), null,null));
                }
            }
        }
        return workflows;
    }
}
