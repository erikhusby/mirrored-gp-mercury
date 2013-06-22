package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "runCartridge")
    Set<SequencingRun> sequencingRuns = new HashSet<>();

    public RunCartridge(String label) {
        super(label);
    }

    protected RunCartridge() {
    }

    abstract public Iterable<RunChamber> getChambers();

    abstract public String getCartridgeName();

    abstract public String getCartridgeBarcode();

    public Set<SequencingRun> getSequencingRuns() {
        return sequencingRuns;
    }

    public void addSequencingRun(SequencingRun sequencingRun) {
        this.sequencingRuns.add(sequencingRun);
    }

    public abstract List<VesselAndPosition> getNearestTubeAncestorsForLanes();
}
