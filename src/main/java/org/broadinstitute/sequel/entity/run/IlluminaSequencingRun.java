package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.person.Person;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.Date;
import java.util.HashSet;

@NamedQueries({
        @NamedQuery(
                name = "IlluminaSequencingRun.findByRunName",
                query = "select r from IlluminaSequencingRun r where runName = :runName"
        )
})
@Entity
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
