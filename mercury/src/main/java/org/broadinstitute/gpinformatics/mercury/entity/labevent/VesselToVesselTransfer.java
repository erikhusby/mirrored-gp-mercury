package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Represents a transfer from one tube to another, with no racks.  Compare to CherryPickTransfer,
 * which includes racks.
 */
@Entity
@Audited
public class VesselToVesselTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "SOURCE_VESSEL")
    private LabVessel sourceVessel;


    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "TARGET_VESSEL")
    private LabVessel targetVessel;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "LAB_EVENT")
    private LabEvent labEvent;

    public VesselToVesselTransfer(LabVessel sourceVessel, LabVessel targetVessel, LabEvent labEvent) {
        this.sourceVessel = sourceVessel;
        this.targetVessel = targetVessel;
        this.labEvent = labEvent;
        this.sourceVessel.getVesselToVesselTransfersThisAsSource().add(this);
        this.targetVessel.getVesselToVesselTransfersThisAsTarget().add(this);
    }

    protected VesselToVesselTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public LabVessel getTargetVessel() {
        return targetVessel;
    }

    /** For fixups only. */
    void setTargetVessel(LabVessel targetVessel) {
        this.targetVessel = targetVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
