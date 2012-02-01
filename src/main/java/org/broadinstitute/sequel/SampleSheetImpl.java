package org.broadinstitute.sequel;

import java.util.Collection;
import java.util.HashSet;

public class SampleSheetImpl implements SampleSheet {

    private final Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
    
    public SampleSheetImpl() {}
    
    @Override
    public Collection<SampleInstance> getSamples() {
        return sampleInstances;
    }

    @Override
    public void addSample(SampleInstance sampleInstance) {
        sampleInstances.add(sampleInstance);
    }

    @Override
    public SampleSheet createBranch() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean contains(Goop sample) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addLabVessel(LabTangible labTangible, Project project, ReadBucket readBucket, MolecularState molecularStateChange) {
        // add the state change to the list
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
