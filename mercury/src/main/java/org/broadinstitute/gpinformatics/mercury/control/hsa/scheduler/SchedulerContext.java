package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

public class SchedulerContext {
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
