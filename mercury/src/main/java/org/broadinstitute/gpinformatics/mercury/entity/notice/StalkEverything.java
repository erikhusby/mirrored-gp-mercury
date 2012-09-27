package org.broadinstitute.gpinformatics.mercury.entity.notice;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

public class StalkEverything extends AbstractStalker {

    @Override
    public void stalk(LabEvent event) {
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public void stalk() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
