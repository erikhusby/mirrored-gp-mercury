package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
public class IlluminaRunChamber extends RunChamber {

    /** For JPA. */
    protected IlluminaRunChamber() {
    }

    public IlluminaRunChamber(String label) {
        super(label);
    }

    @ManyToOne
    private IlluminaFlowcell flowcell;
    
    private int laneNumber;

    @ManyToOne
    private LabVessel library;
    
    public IlluminaRunChamber(IlluminaFlowcell flowcell, int laneNumber,LabVessel library) {
        super("don't know");
        this.flowcell = flowcell;
        this.laneNumber = laneNumber;
        this.library = library;
    }

    @Override
    public String getChamberName() {
        return Integer.toString(this.laneNumber);
    }

    public int getLaneNumber() {
        return this.laneNumber;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.ILLUMINA_RUN_CHAMBER;
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.RUN_CHAMBER;
    }

    /**
     * Hmmm...if Stalker's flowcellLoaded app sends us barcode
     * scans of reagents that are going onto the sequencer,
     * we could capture these as reagent addition events
     * and then...
     * @return
     */
    @Override
    public Set<Reagent> getReagentContents() {
        final Set<Reagent> sequencerReagents = new HashSet<>();
        for (LabEvent event: getEvents()) {
            sequencerReagents.addAll(event.getReagents());
        }
        return sequencerReagents;
    }
}
