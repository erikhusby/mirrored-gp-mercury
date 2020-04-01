package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForIdatTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;

@Dependent
public class WaitForIdatTaskHandler extends AbstractTaskHandler<WaitForIdatTask> {

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Override
    public void handleTask(WaitForIdatTask task, SchedulerContext schedulerContext) {
        LabVessel labVessel = task.getState().getLabVessels().iterator().next();
        if (!OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class)) {
            throw new RuntimeException("Expect only chip well's in WaitForIdatTask's");
        }
        PlateWell chipWell = OrmUtil.proxySafeCast(labVessel, PlateWell.class);
        String position = chipWell.getVesselPosition().name();
        String chipBarcode = chipWell.getPlate().getLabel();

        String red = String.format("%s_%s_Red.idat", chipBarcode, position);
        String green = String.format("%s_%s_Grn.idat", chipBarcode, position);
        File dataFolder = new File(ConcordanceCalculator.convertFilePaths(infiniumStarterConfig.getDataPath()));
        File chipFolder = new File(dataFolder, chipBarcode);

        File redIdat = new File(chipFolder, red);
        File greenIdat = new File(chipFolder, green);

        if (redIdat.exists() && greenIdat.exists()) {
            task.setStatus(Status.COMPLETE);
        }
    }

    public void setInfiniumStarterConfig(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }
}
