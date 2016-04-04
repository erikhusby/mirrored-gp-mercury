package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: jowalsh
 * Date: 4/2/16
 */
public class InfiniumRunProcessor {

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public ChipWellResults process(StaticPlate chip) {
        ChipWellResults chipWellResults = new ChipWellResults();
        String chipBarcode = chip.getLabel();
        File runDirectory = getRunDirectory(chipBarcode);
        boolean isChipCompleted = true;
        if (runDirectory.exists()) {
            List<String> idatFiles = listIdatFiles(runDirectory);
            for (VesselPosition vesselPosition: chip.getVesselGeometry().getVesselPositions()) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                        chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
                if (sampleInstancesAtPositionV2 != null && !sampleInstancesAtPositionV2.isEmpty()) {
                    String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition.name());
                    String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition.name());
                    if (idatFiles.contains(red) && idatFiles.contains(green)) {
                        chipWellResults.getCompletedWells().add(vesselPosition);
                    } else {
                        isChipCompleted = false;
                    }
                }
            }
        }

        chipWellResults.setCompleted(isChipCompleted);
        return chipWellResults;
    }

    private List<String> listIdatFiles(File runDirectory) {
        File[] files = runDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(".idat");
            }
        });

        List<String> filenames = new ArrayList<>();
        for (File f: files) {
            if (f.length() > infiniumStarterConfig.getMinimumIdatFileLength()) {
                filenames.add(f.getName());
            }
        }

        return filenames;
    }

    private File getRunDirectory(String chipBarcode) {
        File rootDir = new File(infiniumStarterConfig.getDataPath());
        return new File(rootDir, chipBarcode);
    }

    public class ChipWellResults {
        private boolean completed;
        private List<VesselPosition> completedWells;

        public List<VesselPosition> getCompletedWells() {
            if (completedWells == null)
                completedWells = new ArrayList<>();
            return completedWells;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

}
