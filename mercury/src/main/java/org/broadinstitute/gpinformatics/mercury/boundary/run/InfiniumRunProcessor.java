package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans run folder for the finished idat pairs for each sample well in a chip
 */
public class InfiniumRunProcessor {

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public InfiniumRunProcessor() {
    }

    //For testing purposes
    public InfiniumRunProcessor(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }

    public ChipWellResults process(StaticPlate chip) {
        ChipWellResults chipWellResults = new ChipWellResults();
        Map<VesselPosition, Boolean> wellCompleteMap = new HashMap<>();
        String chipBarcode = chip.getLabel();
        File runDirectory = getRunDirectory(chipBarcode);
        boolean isChipCompleted = true;
        if (runDirectory.exists()) {
            List<String> idatFiles = listIdatFiles(runDirectory);
            for (VesselPosition vesselPosition: chip.getVesselGeometry().getVesselPositions()) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                        chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
                if (sampleInstancesAtPositionV2 != null && !sampleInstancesAtPositionV2.isEmpty()) {
                    chipWellResults.getPositionWithSampleInstance().add(vesselPosition);
                    String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition.name());
                    String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition.name());
                    boolean complete = idatFiles.contains(red) && idatFiles.contains(green);
                    wellCompleteMap.put(vesselPosition, complete);
                    if (!complete) {
                        isChipCompleted = false;
                    }
                }
            }
        }

        chipWellResults.setCompleted(isChipCompleted);
        chipWellResults.setWellCompleteMap(wellCompleteMap);
        return chipWellResults;
    }

    private List<String> listIdatFiles(File runDirectory) {
        File[] files = runDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.toLowerCase().endsWith(".idat");
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
        private Map<VesselPosition, Boolean> wellCompleteMap;
        private List<VesselPosition> positionWithSampleInstance;

        public Map<VesselPosition, Boolean> getWellCompleteMap() {
            return wellCompleteMap;
        }

        public void setWellCompleteMap(Map<VesselPosition, Boolean> wellCompleteMap) {
            this.wellCompleteMap = wellCompleteMap;
        }

        public List<VesselPosition> getPositionWithSampleInstance() {
            if (positionWithSampleInstance == null)
                positionWithSampleInstance = new ArrayList<>();
            return positionWithSampleInstance;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

}
