package org.broadinstitute.sequel;


import java.util.Collection;

/**
 * Event processed when a run starts on a sequencer.
 */
public class RunStartedEvent extends AbstractLabEvent {


    @Override
    public LabEventName getEventName() {
        return LabEventName.RUN_STARTED;
    }

    @Override
    public boolean isBillable() {
        return false;
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
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
