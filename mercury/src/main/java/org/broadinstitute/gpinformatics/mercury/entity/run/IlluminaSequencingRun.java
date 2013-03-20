package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Date;

@Entity
@Audited
public class IlluminaSequencingRun extends SequencingRun {

    public static final String RUN_FORMAT_PATTERN = "yyMMdd";

    public IlluminaSequencingRun(final IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Long operator,
                                 boolean isTestRun,
                                 Date runDate, OutputDataLocation dataLocation, String runDirectory) {
        super(runName, runBarcode, machineName, operator, isTestRun, runDate, flowcell, dataLocation, runDirectory);
    }

    protected IlluminaSequencingRun() {
    }
}
