package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Audited
public class IlluminaSequencingRun extends SequencingRun {

    public static final SimpleDateFormat RUNFORMAT = new SimpleDateFormat("yyMMdd");

    public IlluminaSequencingRun(final IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Long operator,
                                 boolean isTestRun,
                                 Date runDate, OutputDataLocation dataLocation) {
        super(runName, runBarcode, machineName, operator, isTestRun, runDate, flowcell, dataLocation);
    }

    protected IlluminaSequencingRun() {
    }
}
