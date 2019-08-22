package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class SchedulerContext {

    @Inject
    private SchedulerController schedulerController;

    public SchedulerContext() {
    }

    public SchedulerContext(SchedulerController schedulerController) {
        this.schedulerController = schedulerController;
    }

    public SchedulerController getInstance() {
        return schedulerController;
    }
}
