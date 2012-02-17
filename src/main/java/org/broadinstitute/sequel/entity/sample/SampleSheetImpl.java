package org.broadinstitute.sequel.entity.sample;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.labevent.LabEventTraverser;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SampleSheetImpl implements SampleSheet {

    private Map<LabVessel,Collection<StateChange>> containerToStateChanges = new HashMap<LabVessel,Collection<StateChange>>();
    
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
    public Collection<LabVessel> getVessels() {
        return containerToStateChanges.keySet();
    }

    @Override
    public void addStateChange(LabVessel vessel, StateChange stateChange) {
        addToVessel(vessel);
        containerToStateChanges.get(vessel).add(stateChange);
    }

    @Override
    public void addToVessel(LabVessel vessel) {
        if (!containerToStateChanges.containsKey(vessel)) {
             containerToStateChanges.put(vessel,new HashSet<StateChange>());
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

    @Override
    public Collection<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (StartingSample startingSample : startingSamples) {
            SampleInstanceImpl sampleInstance = startingSample.createSampleInstance();
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }
}
