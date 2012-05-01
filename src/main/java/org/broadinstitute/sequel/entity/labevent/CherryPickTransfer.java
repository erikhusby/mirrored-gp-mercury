package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.VesselContainer;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Represents a transfer between positions in two vessel containers
 */
@Entity
public class CherryPickTransfer {
    @Id
    private Long cherryPickTransferId;

    private VesselContainer sourceVesselContainer;
    private String sourcePosition;
    private VesselContainer targetVesselContainer;
    private String targetPosition;

    public CherryPickTransfer(VesselContainer sourceVesselContainer, String sourcePosition,
            VesselContainer targetVesselContainer, String targetPosition) {
        this.sourceVesselContainer = sourceVesselContainer;
        this.sourcePosition = sourcePosition;
        this.targetVesselContainer = targetVesselContainer;
        this.targetPosition = targetPosition;
    }

    protected CherryPickTransfer() {
    }

    public VesselContainer getSourceVesselContainer() {
        return sourceVesselContainer;
    }

    public String getSourcePosition() {
        return sourcePosition;
    }

    public VesselContainer getTargetVesselContainer() {
        return targetVesselContainer;
    }

    public String getTargetPosition() {
        return targetPosition;
    }
}
