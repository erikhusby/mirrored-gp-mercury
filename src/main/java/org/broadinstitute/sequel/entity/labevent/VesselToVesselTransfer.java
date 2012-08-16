package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * Represents a transfer from one tube to another, with no racks.  Compare to CherryPickTransfer,
 * which includes racks.
 */
@Entity
@Audited
public class VesselToVesselTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_VESSEL_TO_VESSEL_TRANSFER", sequenceName = "SEQ_VESSEL_TO_VESSEL_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VESSEL_TO_VESSEL_TRANSFER")
    private Long tubeToTubeTransferId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetLabVessel;

    @Index(name = "ix_vtvt_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public VesselToVesselTransfer(LabVessel sourceVessel, LabVessel targetLabVessel, LabEvent labEvent) {
        this.sourceVessel = sourceVessel;
        this.targetLabVessel = targetLabVessel;
        this.labEvent = labEvent;
    }

    protected VesselToVesselTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public LabVessel getTargetLabVessel() {
        return targetLabVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
