package org.broadinstitute.sequel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SampleSheetImpl implements SampleSheet {

    private Map<LabTangible,Collection<StateChange>> containerToStateChanges = new HashMap<LabTangible,Collection<StateChange>>();
    
    private Collection<StartingSample> startingSamples = new HashSet<StartingSample>();
    
    public SampleSheetImpl() {}

    @Override
    public Collection<StartingSample> getStartingSamples() {
        return startingSamples;
    }

    @Override
    public void addStartingSample(StartingSample startingSample) {
        startingSamples.add(startingSample);    
    }

    @Override
    public Collection<LabTangible> getLabTangibles() {
        return containerToStateChanges.keySet();
    }

    @Override
    public void addStateChange(LabTangible labTangible, StateChange stateChange) {
        addToTangible(labTangible);
        containerToStateChanges.get(labTangible).add(stateChange);
    }

    @Override
    public void addToTangible(LabTangible labTangible) {
        if (!containerToStateChanges.containsKey(labTangible)) {
             containerToStateChanges.put(labTangible,new HashSet<StateChange>());
        }
    }
    
    @Override
    public Collection<SampleInstance> getSampleInstances(LabVessel container) {
        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>(); 
        if (!containerToStateChanges.containsKey(container)) {
            throw new RuntimeException("This sample sheet isn't contained by " + container.getLabCentricName());
        }

        for (StartingSample startingSample : startingSamples) {
            SampleInstanceImpl sampleInstance = startingSample.createSampleInstance();
            for (StateChange stateChange : LabEventTraverser.getStateChangesPriorToAndIncluding(this,container)) {
                sampleInstance.applyChange(stateChange);
            }    
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }
}
