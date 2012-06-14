package org.broadinstitute.sequel.entity.labevent;


import java.util.Collection;

public class SageUnloadingEvent extends LabEvent {

    
    @Override
    public LabEventName getEventName() {
        return LabEventName.SAGE_UNLOADED;
    }

    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }
}
