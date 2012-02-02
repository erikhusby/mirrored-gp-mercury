package org.broadinstitute.sequel;

import java.util.Collection;
import java.util.Collections;

public class PlateWell extends AbstractLabVessel {

    private StaticPlate plate;
    
    public PlateWell(StaticPlate p,WellName wellName) {
        plate = p;
        
    }

    @Override
    public LabVessel getContainingVessel() {
        return plate;        
    }

    @Override
    public Collection<LabVessel> getContainedVessels() {
        return Collections.emptyList();
    }

    @Override
    public void addContainedVessel(LabVessel child) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleInstance> getSampleInstances() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
