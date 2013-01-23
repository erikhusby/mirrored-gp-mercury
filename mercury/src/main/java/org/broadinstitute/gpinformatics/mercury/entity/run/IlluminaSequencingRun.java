package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

@Entity
@Audited
@Table(schema = "mercury")
public class IlluminaSequencingRun extends SequencingRun {

    public IlluminaSequencingRun(final IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Long operator,
                                 boolean isTestRun,
                                 Date runDate) {
        super(runName, runBarcode, machineName, operator, isTestRun, runDate, Collections.singleton(((RunCartridge)flowcell)));
    }

    protected IlluminaSequencingRun() {
    }

    /**
     * Returns the Illumina flowcell for this Illumina run. In general, sequencing runs may have multiple run
     * cartridges. However, Illumina runs only have a single flowcell, as enforced by the IlluminaSequencingRun
     * constructor.
     *
     * @return the flowcell for this run
     */
    public IlluminaFlowcell getFlowcell() {
        return OrmUtil.proxySafeCast(getSampleCartridge().iterator().next(), IlluminaFlowcell.class);
    }
}
