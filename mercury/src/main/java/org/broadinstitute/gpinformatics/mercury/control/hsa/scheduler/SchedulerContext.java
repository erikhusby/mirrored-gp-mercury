package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenAppContext;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class SchedulerContext {

    @Inject
    private SchedulerController schedulerController;

    @Inject
    private DragenAppContext dragenAppContext;

    public SchedulerContext() {
    }

    public SchedulerContext(SchedulerController schedulerController,
                            DragenAppContext dragenAppContext) {
        this.schedulerController = schedulerController;
        this.dragenAppContext = dragenAppContext;
    }

    public SchedulerController getInstance() {
        return schedulerController;
    }
}
