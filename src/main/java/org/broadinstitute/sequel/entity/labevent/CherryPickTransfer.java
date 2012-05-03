package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselContainerEmbedder;

import javax.persistence.Entity;
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
    private String sourcePosition;
    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel targetVessel;
    private String targetPosition;
    @ManyToOne
    private AbstractLabEvent labEvent;

    public CherryPickTransfer(VesselContainer<?> sourceVesselContainer, String sourcePosition,
            VesselContainer<?> targetVesselContainer, String targetPosition) {
        this.sourceVessel = sourceVesselContainer.getEmbedder();
        this.sourcePosition = sourcePosition;
        this.targetVessel = targetVesselContainer.getEmbedder();
        this.targetPosition = targetPosition;
    }

    protected CherryPickTransfer() {
    }

    public VesselContainer<?> getSourceVesselContainer() {
        return ((VesselContainerEmbedder<?>)sourceVessel).getVesselContainer();
    }

    public String getSourcePosition() {
        return sourcePosition;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return ((VesselContainerEmbedder<?>) targetVessel).getVesselContainer();
    }

    public String getTargetPosition() {
        return targetPosition;
    }
}
