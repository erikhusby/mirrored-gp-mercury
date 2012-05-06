package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.Entity;

/**
 * Something that contains samples and is
 * loaded onto a sequencing instrument.
 *
 * 454 PTP, Illumina flowcell, Ion chip,
 * pacbio plate
 */
@Entity
public abstract class RunCartridge extends LabVessel {

    public RunCartridge(String label) {
        super(label);
    }

    protected RunCartridge() {
    }

    abstract public Iterable<RunChamber> getChambers();

    abstract public String getCartridgeName();

    abstract public String getCartridgeBarcode();

}
