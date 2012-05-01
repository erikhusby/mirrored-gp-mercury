package org.broadinstitute.sequel.entity.sample;

import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventTraverser;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of sample metadata.
 */
@Entity
public class SampleSheet {

    @Id
    @SequenceGenerator(name = "SEQ_SAMPLE_SHEET", sequenceName = "SEQ_SAMPLE_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMPLE_SHEET")
    private Long sampleSheetId;

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
     * {@link org.broadinstitute.sequel.entity.labevent.LabEvent}.  To avoid this problem, we
     * try to re-use {@link SampleSheet}s whenever
     * possible.  Many {@link org.broadinstitute.sequel.entity.reagent.Reagent reagent addition} events
     * don't alter the molecular state (at least not in
     * a way that we need to track), for example.  In
     * those situations, we just re-use the same {@link SampleSheet}.
     *
     * Plenty of transfer events also don't change {@link org.broadinstitute.sequel.entity.vessel.MolecularState},
     * so we don't want to copy {@link SampleSheet}s for those.
     *
     * This means that when we encounter a {@link org.broadinstitute.sequel.entity.vessel.MolecularState}-changing
     * event, we have to "branch" the {@link SampleSheet}.  Put another
     * way, we have to copy the sample sheet so that
     * when we apply our {@link org.broadinstitute.sequel.entity.vessel.MolecularState} changes, we're not
     * making it look like all previous {@link org.broadinstitute.sequel.entity.vessel.LabVessel}s also
     * contain the modified state.
     *
     * When {@link org.broadinstitute.sequel.entity.labevent.LabEvent#applyMolecularStateChanges()}  is
     * changed, the {@link org.broadinstitute.sequel.entity.labevent.LabEvent} decides whether it's making
     * a {@link org.broadinstitute.sequel.entity.vessel.MolecularState} change that requires this
     * cloning operation.
     *
     * Under the covers, the implementation is probably doing
     * a full copy of all the {@link SampleInstance} data.
     *
     * @return the new, copied {@link SampleSheet}
     */
    //todo jmt fix this
    @Transient
    private Map<LabVessel,Set<StateChange>> containerToStateChanges = new HashMap<LabVessel,Set<StateChange>>();

    @OneToMany(targetEntity = BSPSample.class)
    private Collection<StartingSample> startingSamples = new HashSet<StartingSample>();

    public SampleSheet() {}

    public Collection<StartingSample> getStartingSamples() {
        return startingSamples;
    }

    public void addStartingSample(StartingSample startingSample) {
        startingSamples.add(startingSample);
    }

    public Collection<LabVessel> getVessels() {
        return containerToStateChanges.keySet();
    }

    /**
     *
     * @param labTangible the piece of plastic which
     *                  contains this {@link SampleSheet}
     * @param project If null, no change to the {@link org.broadinstitute.sequel.entity.project.Project}
     *                relationships in {@link StartingSample#getRootProject()}.
     *
     *                If not null, the effect is that in the
     *                context of {@link LabVessel}, this {@link SampleSheet}
     *                should be associated to {@link org.broadinstitute.sequel.entity.project.Project}.  Specifying
     *                a {@link org.broadinstitute.sequel.entity.project.Project} here essentially tells the
     *                system to "override" the {@link StartingSample#getRootProject()}  with this project, from here on down
     *                in the event graph.
     * @param readBucket Similar behavior to setting the {@link org.broadinstitute.sequel.entity.project.Project}.
     *
     *                   If  null, the {@link StartingSample#getRootReadBucket()} is used as the {@link org.broadinstitute.sequel.entity.analysis.ReadBucket} for this {@link SampleSheet} in the context
     *                   of {@link LabVessel}.
     *
     *                   If not null, the effect should be that from here
     *                   on down in the transfer graph, any mention of this
     *                   {@link SampleSheet} should have its reads visible
     *                   in {@link org.broadinstitute.sequel.entity.analysis.ReadBucket}.
     * @param molecularStateChange In this {@link LabVessel}, is there a
     *                             {@link org.broadinstitute.sequel.entity.vessel.MolecularState molecular state change?}
     *                             If null, there is no change.  If {@param molecularStateChange} is
     *                             set, then this {@link SampleSheet} in the context of {@param labTangible}
     *                             has the given {@param molecularStateChange} added to it.
     */
    public void addStateChange(LabVessel vessel, StateChange stateChange) {
        addToVessel(vessel);
        containerToStateChanges.get(vessel).add(stateChange);
    }

    public void addToVessel(LabVessel vessel) {
        if (!containerToStateChanges.containsKey(vessel)) {
            containerToStateChanges.put(vessel,new HashSet<StateChange>());
        }
    }

    public Collection<SampleInstance> getSampleInstances(LabVessel container) {
        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if (!containerToStateChanges.containsKey(container)) {
            throw new RuntimeException("This sample sheet isn't contained by " + container.getLabCentricName());
        }

        for (StartingSample startingSample : startingSamples) {
            SampleInstanceImpl sampleInstance = startingSample.createSampleInstance();
            for (StateChange stateChange : LabEventTraverser.getStateChangesPriorToAndIncluding(this, container)) {
                // ordering of the state changes is critical...
                // doing it root-to-branch means that "nearest ancestor"
                //
                sampleInstance.applyChange(stateChange);
            }
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }

    public Collection<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (StartingSample startingSample : startingSamples) {
            SampleInstanceImpl sampleInstance = startingSample.createSampleInstance();
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }

}
