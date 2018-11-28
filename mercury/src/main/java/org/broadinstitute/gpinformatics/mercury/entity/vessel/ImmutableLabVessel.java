package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.Collection;
import java.util.Set;

/**
 * Returned by VesselContainers with non-persisted containees (e.g. plate wells that don't have samples or reagents
 * associated with them).  Allows consistent access to SampleInstances.
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

    @Override
    public void addMetric(LabMetric labMetric) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addAbandonedVessel(AbandonVessel vessels) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addReagent(Reagent reagent) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addToContainer(VesselContainer<?> vesselContainer) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addJiraTicket(JiraTicket jiraTicket) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addInPlaceEvent(LabEvent labEvent) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addNonReworkLabBatchStartingVessel(LabBatchStartingVessel labBatchStartingVessel) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addBucketEntry(BucketEntry bucketEntry) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addNonReworkLabBatch(LabBatch labBatch) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addReworkLabBatch(LabBatch reworkLabBatch) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addDilutionReferences(LabBatchStartingVessel dilutionReferences) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addSample(MercurySample mercurySample) {
        throw new RuntimeException("Immutable LabVessel");
    }

    @Override
    public void addAllSamples(Collection<MercurySample> mercurySamples) {
        throw new RuntimeException("Immutable LabVessel");
    }
}
