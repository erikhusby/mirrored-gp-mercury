package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * Represents a transfer between positions in two vessel containers
 */
@Entity
public class CherryPickTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_CHERRY_PICK_TRANSFER", sequenceName = "SEQ_CHERRY_PICK_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CHERRY_PICK_TRANSFER")
    private Long cherryPickTransferId;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition sourcePosition;

    @ManyToOne(fetch = FetchType.LAZY)
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
        this.sourcePosition = sourcePosition;
        this.targetVessel = targetVesselContainer.getEmbedder();
        this.targetPosition = targetPosition;
    }

    protected CherryPickTransfer() {
    }

    public VesselContainer<?> getSourceVesselContainer() {
        return OrmUtil.proxySafeCast(sourceVessel, VesselContainerEmbedder.class).getVesselContainer();
    }

    public VesselPosition getSourcePosition() {
        return sourcePosition;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return OrmUtil.proxySafeCast(targetVessel, VesselContainerEmbedder.class).getVesselContainer();
    }

    public VesselPosition getTargetPosition() {
        return targetPosition;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
