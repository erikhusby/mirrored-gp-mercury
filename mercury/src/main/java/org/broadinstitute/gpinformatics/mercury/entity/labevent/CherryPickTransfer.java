package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;

/**
 * Represents a transfer between positions in two vessel containers
 * todo jmt if the JAXB references multiple source racks, but transfers out of only one, there is no association between lab vessel and lab event.
 */
@Entity
@Audited
public class CherryPickTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition sourcePosition;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel ancillarySourceVessel;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition targetPosition;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel ancillaryTargetVessel;

    @Index(name = "ix_cpt_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public CherryPickTransfer(
            VesselContainer<?> sourceVesselContainer,
            VesselPosition sourcePosition,
            @Nullable LabVessel ancillarySourceVessel,
            VesselContainer<?> targetVesselContainer,
            VesselPosition targetPosition,
            @Nullable LabVessel ancillaryTargetVessel,
            LabEvent labEvent) {
        if (sourceVesselContainer == null) {
            throw new RuntimeException("sourceVesselContainer must not be null");
        }
        if (sourcePosition == null) {
            throw new RuntimeException("sourcePosition must not be null");
        }
        this.ancillarySourceVessel = ancillarySourceVessel;
        if (targetVesselContainer == null) {
            throw new RuntimeException("targetVesselContainer must not be null");
        }
        if (targetPosition == null) {
            throw new RuntimeException("targetPosition must not be null");
        }
        this.ancillaryTargetVessel = ancillaryTargetVessel;
        this.labEvent = labEvent;
        sourceVessel = sourceVesselContainer.getEmbedder();
        sourceVesselContainer.getCherryPickTransfersFrom().add(this);
        this.sourcePosition = sourcePosition;
        targetVessel = targetVesselContainer.getEmbedder();
        targetVesselContainer.getCherryPickTransfersTo().add(this);
        this.targetPosition = targetPosition;
    }

    protected CherryPickTransfer() {
    }

    public VesselContainer<?> getSourceVesselContainer() {
        return sourceVessel.getContainerRole();
    }

    public VesselPosition getSourcePosition() {
        return sourcePosition;
    }

    /**
     * For fixups only.
     */
    void setSourcePosition(VesselPosition sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    @Nullable
    public LabVessel getAncillarySourceVessel() {
        return ancillarySourceVessel;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return targetVessel.getContainerRole();
    }

    public VesselPosition getTargetPosition() {
        return targetPosition;
    }

    @Nullable
    public LabVessel getAncillaryTargetVessel() {
        return ancillaryTargetVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    /**
     * Constructs a string this is likely to be unique for this transfer
     * @return concatenation of key fields
     */
    public String getKey() {
        return getVesselTransferId() + "|" + sourceVessel.getLabel() + "|" + sourcePosition + "|" +
               targetVessel.getLabel() + "|" + targetPosition;
    }
}
