package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;

/**
 * This abstract class defines the makeup of each event handler.
 */
public abstract class AbstractTaskHandler {

    /**
     * The handleEvent method is where each metrics task type handler will define the logic to upload metrics
     * @param task task triggered by the state machine. Tasks have access to parent state and Task specific entities.
     * @param schedulerContext context for the scheduler if needed. Used for Slurm tasks and Dragen simulations)
     */
    public abstract void handleTask(Task task, SchedulerContext schedulerContext) ;

}
