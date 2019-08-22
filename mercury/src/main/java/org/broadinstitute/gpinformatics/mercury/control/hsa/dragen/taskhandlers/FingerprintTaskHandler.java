package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.enterprise.context.Dependent;

@Dependent
public class FingerprintTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(FingerprintTaskHandler.class);

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        FingerprintTask fpTask = OrmUtil.proxySafeCast(task, FingerprintTask.class);
        State state = fpTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, FingerprintState.class)) {
            throw new RuntimeException("Expect only a fingerprint state for a fingerprint metrics task.");
        }

        FingerprintState fingerprintState = OrmUtil.proxySafeCast(state, FingerprintState.class);
    }
}
