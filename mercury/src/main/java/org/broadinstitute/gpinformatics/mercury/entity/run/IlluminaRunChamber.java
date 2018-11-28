package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
public class IlluminaRunChamber extends RunChamber {

    /** For JPA. */
    protected IlluminaRunChamber() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FLOWCELL")
    private IlluminaFlowcell flowcell;
    
    private int laneNumber;

    public IlluminaRunChamber(IlluminaFlowcell flowcell, int laneNumber) {
        super(flowcell.getLabel() + "_lane_" + laneNumber);
        this.flowcell = flowcell;
        this.laneNumber = laneNumber;
    }

    @Override
    public String getChamberName() {
        return Integer.toString(laneNumber);
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public IlluminaFlowcell getFlowcell() {
        return flowcell;
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
