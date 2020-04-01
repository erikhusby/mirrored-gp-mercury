package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.GsUtilTaskBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.PushIdatsToCloudTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.util.Set;

@Dependent
public class PushIdatsToCloudHandler extends AbstractTaskHandler<PushIdatsToCloudTask> {

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Inject
    private DragenConfig dragenConfig;

    @Override
    public void handleTask(PushIdatsToCloudTask task, SchedulerContext schedulerContext) {
        Set<LabVessel> labVessels = task.getState().getLabVessels();
        if (labVessels == null || labVessels.size() != 1) {
            throw new RuntimeException("Expect a single plate well to upload.");
        }
        LabVessel labVessel = labVessels.iterator().next();
        if (!OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class)) {
            throw new RuntimeException("Expect a plate well.");
        }

        PlateWell chipWell = OrmUtil.proxySafeCast(labVessel, PlateWell.class);
        PushIdatsToCloudTask pushIdatsToCloudTask = OrmUtil.proxySafeCast(task, PushIdatsToCloudTask.class);
        String color = pushIdatsToCloudTask.getIdatColor();
        String chipBarcode = chipWell.getPlate().getLabel();
        String position = chipWell.getVesselPosition().name();
        String idatName = String.format("%s_%s_%s.idat", chipBarcode, position, color);
        File chipDir = new File(ConcordanceCalculator.convertFilePaths(infiniumStarterConfig.getDataPath()), chipBarcode);
        File idatFile = new File(chipDir, idatName);

        GsUtilTaskBuilder taskBuilder = new GsUtilTaskBuilder().
                parallelCompositeUploadThreshold("150MB").
                parallelProcessCount(1).
                parallelThreadCount(4).
                cp(idatFile, dragenConfig.getArraysBucket());
        pushIdatsToCloudTask.setCommandLineArgument(taskBuilder.build());
    }

    // For Testing
    public void setInfiniumStarterConfig(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }

    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }
}
