package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.PushIdatsToCloudTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForIdatTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForInfiniumMetric;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Wait for the metrics for a given chipwell
 */
@Dependent
public class WaitForInfiniumMetricsTaskHandler extends AbstractTaskHandler {

    @Inject
    private ArraysQcDao arraysQcDao;

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        if (!OrmUtil.proxySafeIsInstance(task, WaitForInfiniumMetric.class)) {
            throw new RuntimeException("Expect only WaitForInfiniumMetric in WaitForInfiniumMetricsTaskHandler");
        }

        WaitForInfiniumMetric waitForInfiniumMetric = OrmUtil.proxySafeCast(task, WaitForInfiniumMetric.class);
        LabVessel labVessel = waitForInfiniumMetric.getState().getLabVessels().iterator().next();
        if (!OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class)) {
            throw new RuntimeException("Expect only chip well's in WaitForIdatTask's");
        }
        PlateWell chipWell = OrmUtil.proxySafeCast(labVessel, PlateWell.class);
        List<ArraysQc> arraysQc = arraysQcDao.findByBarcodes(Collections.singletonList(chipWell.getLabel()));
        if (arraysQc.size() > 0) {
            task.setStatus(Status.COMPLETE);
        } else {
            task.setStatus(Status.RUNNING);
        }
    }

    public void setArraysQcDao(ArraysQcDao arraysQcDao) {
        this.arraysQcDao = arraysQcDao;
    }
}
