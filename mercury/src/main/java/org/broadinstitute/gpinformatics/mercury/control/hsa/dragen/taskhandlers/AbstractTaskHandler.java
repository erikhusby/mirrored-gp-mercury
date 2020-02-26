package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForIdatTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

/**
 * This abstract class defines the makeup of each event handler.
 */
public abstract class AbstractTaskHandler<T extends Task> {

    /**
     * The handleEvent method is where each metrics task type handler will define the logic to upload metrics/Fingerprints
     * @param task task triggered by the state machine. Tasks have access to parent state and Task specific entities.
     * @param schedulerContext context for the scheduler if needed. Used for Slurm tasks and Dragen simulations)
     */
    public abstract void handleTask(T task, SchedulerContext schedulerContext) ;

}
