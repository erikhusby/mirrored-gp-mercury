package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import java.util.Collection;

public abstract class AbstractRunCartridge extends RunCartridge {


    protected AbstractRunCartridge(String label) {
        super(label);
    }

    protected AbstractRunCartridge() {
    }

}
