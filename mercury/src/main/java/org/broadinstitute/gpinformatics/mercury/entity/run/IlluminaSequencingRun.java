package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Date;
import java.util.HashSet;

@Entity
@Audited
public class IlluminaSequencingRun extends SequencingRun {

    public IlluminaSequencingRun(final IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Person operator,
                                 boolean isTestRun,
                                 Date runDate) {
        super(runName, runBarcode, machineName, operator, isTestRun, runDate, new HashSet<RunCartridge>(){{add(flowcell);}});
    }

    protected IlluminaSequencingRun() {
    }
}
