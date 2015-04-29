package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

/**
 * Represents a transfer from one tube to another, with no racks.  Compare to CherryPickTransfer,
 * which includes racks.
 */
@Entity
@Audited
public class VesselToVesselTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Index(name = "ix_vtvt_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public VesselToVesselTransfer(LabVessel sourceVessel, LabVessel targetVessel, LabEvent labEvent) {
        this.sourceVessel = sourceVessel;
        sourceVessel.getVesselToVesselTransfersThisAsSource().add(this);
        this.targetVessel = targetVessel;
        sourceVessel.getVesselToVesselTransfersThisAsTarget().add(this);
        this.labEvent = labEvent;
    }

    protected VesselToVesselTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public LabVessel getTargetVessel() {
        return targetVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
