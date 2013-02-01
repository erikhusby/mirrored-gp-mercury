package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a transfer between positions in two vessel containers
 * todo jmt if the JAXB references multiple source racks, but transfers out of only one, there is no association between lab vessel and lab event.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class CherryPickTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition sourcePosition;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition targetPosition;

    @Index(name = "ix_cpt_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public CherryPickTransfer(VesselContainer<?> sourceVesselContainer, VesselPosition sourcePosition,
            VesselContainer<?> targetVesselContainer, VesselPosition targetPosition, LabEvent labEvent) {
        this.labEvent = labEvent;
        this.sourceVessel = sourceVesselContainer.getEmbedder();
        sourceVesselContainer.getCherryPickTransfersFrom().add(this);
        this.sourcePosition = sourcePosition;
        this.targetVessel = targetVesselContainer.getEmbedder();
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

    public VesselContainer<?> getTargetVesselContainer() {
        return targetVessel.getContainerRole();
    }

    public VesselPosition getTargetPosition() {
        return targetPosition;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    /**
     * Constructs a string this is likely to be unique for this transfer
     * @return concatenation of key fields
     */
    public String getKey() {
        return sourceVessel.getLabel() + "|" + sourcePosition + "|" + targetVessel.getLabel() + "|" +
                targetPosition;
    }
}
