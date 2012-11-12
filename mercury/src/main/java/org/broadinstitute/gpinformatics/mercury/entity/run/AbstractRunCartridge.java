package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import java.util.Collection;

public abstract class AbstractRunCartridge extends RunCartridge {


    protected AbstractRunCartridge(String label) {
        super(label);
    }

    protected AbstractRunCartridge() {
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

}
