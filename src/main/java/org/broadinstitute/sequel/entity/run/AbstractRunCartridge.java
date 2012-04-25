package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.AbstractLabVessel;

import java.util.Collection;

public abstract class AbstractRunCartridge extends AbstractLabVessel implements RunCartridge {


    protected AbstractRunCartridge(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

}
