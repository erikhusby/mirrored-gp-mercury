package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import java.io.File;
import java.util.Date;

public class Bcl2FastqFolderUtil {

    private final IlluminaSequencingRun illuminaSequencingRun;
    private final File runFolder;
    private final File hsaFolder;

    public Bcl2FastqFolderUtil(IlluminaSequencingRun illuminaSequencingRun) {
        this.illuminaSequencingRun = illuminaSequencingRun;

        runFolder = new File(illuminaSequencingRun.getRunDirectory());
        if (!runFolder.exists()) {
            throw new RuntimeException("Run folder doesn't exists " + runFolder.getPath());
        }

        hsaFolder = new File(runFolder, "hsa");
        if (!hsaFolder.exists()) {
            hsaFolder.mkdir();
        }
    }

    public File createNewAnalysisFolder() {
        File analysisFolder = new File(hsaFolder, DateUtils.getFileDateTime(new Date()));
        if (!analysisFolder.exists()) {
            analysisFolder.mkdir();
        }
        return analysisFolder;
    }
}
