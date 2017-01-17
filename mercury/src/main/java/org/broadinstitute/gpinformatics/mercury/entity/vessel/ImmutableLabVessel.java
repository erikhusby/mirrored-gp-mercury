package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;

import java.util.Set;

/**
 * Returned by VesselContainers with non-persisted containees (e.g. plate wells that don't have samples or reagents
 * associated with them).  Allows consistent access getSampleInstances.
 */
public class ImmutableLabVessel extends LabVessel {

    private final VesselContainer<?> vesselContainer;
    private final VesselPosition vesselPosition;

    public ImmutableLabVessel(VesselContainer<?> vesselContainer, VesselPosition vesselPosition) {
        super(vesselContainer.getEmbedder().getLabel() + vesselPosition);
        this.vesselContainer = vesselContainer;
        this.vesselPosition = vesselPosition;
    }

    @Override
    public Set<SampleInstanceV2> getSampleInstancesV2() {
        return vesselContainer.getSampleInstancesAtPositionV2(vesselPosition);
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return null;
    }

    @Override
    public ContainerType getType() {
        return null;
    }
}
