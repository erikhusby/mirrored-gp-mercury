package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * A collection of sample metadata.
 */
public interface SampleSheet {

    public Collection<SampleInstance> getSamples();

    public void addSample(SampleInstance sampleInstance);

    /**
     * 
     * @param labTangible the piece of plastic which
     *                  contains this {@link SampleSheet}
     * @param project If null, no change to the {@link Project}
     *                relationships in {@link org.broadinstitute.sequel.StartingSample#getRootProject()}.
     *                
     *                If not null, the effect is that in the
     *                context of {@link LabVessel}, this {@link SampleSheet}
     *                should be associated to {@link Project}.  Specifying
     *                a {@link Project} here essentially tells the
     *                system to "override" the {@link org.broadinstitute.sequel.StartingSample#getRootProject()}  with this project, from here on down
     *                in the event graph.
     * @param readBucket Similar behavior to setting the {@link Project}.
     *                   
     *                   If  null, the {@link org.broadinstitute.sequel.StartingSample#getRootReadBucket()} is used as the {@link ReadBucket} for this {@link SampleSheet} in the context
     *                   of {@link LabVessel}.
     *                   
     *                   If not null, the effect should be that from here
     *                   on down in the transfer graph, any mention of this
     *                   {@link SampleSheet} should have its reads visible
     *                   in {@link ReadBucket}.
     * @param molecularStateChange In this {@link LabVessel}, is there a 
     *                             {@link MolecularState molecular state change?}
     *                             If null, there is no change.  If {@param molecularStateChange} is
     *                             set, then this {@link SampleSheet} in the context of {@param labTangible}
     *                             has the given {@param molecularStateChange} added to it.
     */
    public void addLabVessel(LabTangible labTangible,
                             Project project,
                             ReadBucket readBucket,
                             MolecularState molecularStateChange);

    public Collection<StateChange> getStateChanges();

    /**
     * Some lab reactions permanently alter the
     * state of the samples inside this vessel.  For
     * instance, adding a molecular index under the
     * right conditions results in a molecular state
     * change that applies to this container and all
     * containers derived from this container.
     *
     * Let's assume that because of scaling fears,
     * we don't want to make a new sample sheet
     * for every transfer because doing so would mean
     * copying potentially thousands of rows per
     * {@link LabEvent}.  To avoid this problem, we
     * try to re-use {@link SampleSheet}s whenever
     * possible.  Many {@link Reagent reagent addition} events
     * don't alter the molecular state (at least not in
     * a way that we need to track), for example.  In
     * those situations, we just re-use the same {@link SampleSheet}.
     *
     * Plenty of transfer events also don't change {@link MolecularState},
     * so we don't want to copy {@link SampleSheet}s for those.
     *
     * This means that when we encounter a {@link MolecularState}-changing
     * event, we have to "branch" the {@link SampleSheet}.  Put another
     * way, we have to copy the sample sheet so that
     * when we apply our {@link MolecularState} changes, we're not
     * making it look like all previous {@link LabVessel}s also
     * contain the modified state.
     *
     * When {@link LabEvent#applyMolecularStateChanges()}  is
     * changed, the {@link LabEvent} decides whether it's making
     * a {@link MolecularState} change that requires this
     * cloning operation.
     *
     * Under the covers, the implementation is probably doing
     * a full copy of all the {@link SampleInstance} data.
     *
     * @return the new, copied {@link SampleSheet}
     */
   
}
