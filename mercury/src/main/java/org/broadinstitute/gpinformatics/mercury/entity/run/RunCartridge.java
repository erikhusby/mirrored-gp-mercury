package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * Something that contains samples and is
 * loaded onto a sequencing instrument.
 *
 * 454 PTP, Illumina flowcell, Ion chip,
 * pacbio plate
 */
@Entity
@Audited
public abstract class RunCartridge extends LabVessel {

    @ManyToOne(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "seq_run_run_cartridges")
    SequencingRun sequencingRun;

    public RunCartridge(String label) {
        super(label);
    }

    protected RunCartridge() {
    }

    abstract public Iterable<RunChamber> getChambers();

    abstract public String getCartridgeName();

    abstract public String getCartridgeBarcode();

    public SequencingRun getSequencingRun() {
        return sequencingRun;
    }
}
