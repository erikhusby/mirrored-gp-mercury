package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.VesselContainer;

/**
 * Represents a transfer between positions in two vessel containers
 */
public class CherryPickTransfer {
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
