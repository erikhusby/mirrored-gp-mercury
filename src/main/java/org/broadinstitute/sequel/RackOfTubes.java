package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * A rack of tubes
 */
public class RackOfTubes extends AbstractLabVessel implements SBSSectionable {

    public RackOfTubes(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getContainedVessels() {
        throw new RuntimeException("I haven't been written yet.");
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
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
